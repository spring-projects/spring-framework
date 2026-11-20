/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.jcache.interceptor;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.core.MethodClassKey;
import org.springframework.util.ReflectionUtils;

/**
 * Abstract implementation of {@link JCacheOperationSource} that caches operations
 * for methods and implements a fallback policy: 1. specific target method;
 * 2. declaring method.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 4.1
 * @see org.springframework.cache.interceptor.AbstractFallbackCacheOperationSource
 */
public abstract class AbstractFallbackJCacheOperationSource implements JCacheOperationSource {

	/**
	 * Canonical value held in cache to indicate no cache operation was
	 * found for this method, and we don't need to look again.
	 */
	private static final Object NULL_CACHING_MARKER = new Object();


	protected final Log logger = LogFactory.getLog(getClass());

	private final Map<MethodClassKey, Object> operationCache = new ConcurrentHashMap<>(1024);


	@Override
	public boolean hasCacheOperation(Method method, @Nullable Class<?> targetClass) {
		return (getCacheOperation(method, targetClass, false) != null);
	}

	@Override
	public @Nullable JCacheOperation<?> getCacheOperation(Method method, @Nullable Class<?> targetClass) {
		return getCacheOperation(method, targetClass, true);
	}

	private @Nullable JCacheOperation<?> getCacheOperation(Method method, @Nullable Class<?> targetClass, boolean cacheNull) {
		if (ReflectionUtils.isObjectMethod(method)) {
			return null;
		}

		MethodClassKey cacheKey = new MethodClassKey(method, targetClass);
		Object cached = this.operationCache.get(cacheKey);

		if (cached != null) {
			return (cached != NULL_CACHING_MARKER ? (JCacheOperation<?>) cached : null);
		}
		else {
			JCacheOperation<?> operation = computeCacheOperation(method, targetClass);
			if (operation != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Adding cacheable method '" + method.getName() + "' with operation: " + operation);
				}
				this.operationCache.put(cacheKey, operation);
			}
			else if (cacheNull) {
				this.operationCache.put(cacheKey, NULL_CACHING_MARKER);
			}
			return operation;
		}
	}

	private @Nullable JCacheOperation<?> computeCacheOperation(Method method, @Nullable Class<?> targetClass) {
		// Don't allow non-public methods, as configured.
		if (allowPublicMethodsOnly() && !Modifier.isPublic(method.getModifiers())) {
			return null;
		}
		// Skip setBeanFactory method on BeanFactoryAware.
		if (method.getDeclaringClass() == BeanFactoryAware.class) {
			return null;
		}

		// The method may be on an interface, but we need metadata from the target class.
		// If the target class is null, the method will be unchanged.
		Method specificMethod = AopUtils.getMostSpecificMethod(method, targetClass);

		// First try is the method in the target class.
		JCacheOperation<?> operation = findCacheOperation(specificMethod, targetClass);
		if (operation != null) {
			return operation;
		}
		if (specificMethod != method) {
			// Fallback is to look at the original method.
			operation = findCacheOperation(method, targetClass);
			if (operation != null) {
				return operation;
			}
		}
		return null;
	}


	/**
	 * Subclasses need to implement this to return the caching operation
	 * for the given method, if any.
	 * @param method the method to retrieve the operation for
	 * @param targetType the target class
	 * @return the cache operation associated with this method
	 * (or {@code null} if none)
	 */
	protected abstract @Nullable JCacheOperation<?> findCacheOperation(Method method, @Nullable Class<?> targetType);

	/**
	 * Should only public methods be allowed to have caching semantics?
	 * <p>The default implementation returns {@code false}.
	 */
	protected boolean allowPublicMethodsOnly() {
		return false;
	}

}
