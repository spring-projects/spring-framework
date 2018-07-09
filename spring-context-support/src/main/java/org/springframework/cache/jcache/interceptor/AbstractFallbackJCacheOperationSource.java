/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cache.jcache.interceptor;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.support.AopUtils;
import org.springframework.core.MethodClassKey;
import org.springframework.lang.Nullable;

/**
 * Abstract implementation of {@link JCacheOperationSource} that caches attributes
 * for methods and implements a fallback policy: 1. specific target method;
 * 2. declaring method.
 *
 * <p>This implementation caches attributes by method after they are first used.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 4.1
 * @see org.springframework.cache.interceptor.AbstractFallbackCacheOperationSource
 */
public abstract class AbstractFallbackJCacheOperationSource implements JCacheOperationSource {

	/**
	 * Canonical value held in cache to indicate no caching attribute was
	 * found for this method and we don't need to look again.
	 */
	private static final Object NULL_CACHING_ATTRIBUTE = new Object();


	protected final Log logger = LogFactory.getLog(getClass());

	private final Map<MethodClassKey, Object> cache = new ConcurrentHashMap<>(1024);


	@Override
	public JCacheOperation<?> getCacheOperation(Method method, Class<?> targetClass) {
		MethodClassKey cacheKey = new MethodClassKey(method, targetClass);
		Object cached = this.cache.get(cacheKey);

		if (cached != null) {
			return (cached != NULL_CACHING_ATTRIBUTE ? (JCacheOperation<?>) cached : null);
		}
		else {
			JCacheOperation<?> operation = computeCacheOperation(method, targetClass);
			if (operation != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Adding cacheable method '" + method.getName() + "' with operation: " + operation);
				}
				this.cache.put(cacheKey, operation);
			}
			else {
				this.cache.put(cacheKey, NULL_CACHING_ATTRIBUTE);
			}
			return operation;
		}
	}

	@Nullable
	private JCacheOperation<?> computeCacheOperation(Method method, Class<?> targetClass) {
		// Don't allow no-public methods as required.
		if (allowPublicMethodsOnly() && !Modifier.isPublic(method.getModifiers())) {
			return null;
		}

		// The method may be on an interface, but we need attributes from the target class.
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
	@Nullable
	protected abstract JCacheOperation<?> findCacheOperation(Method method, Class<?> targetType);

	/**
	 * Should only public methods be allowed to have caching semantics?
	 * <p>The default implementation returns {@code false}.
	 */
	protected boolean allowPublicMethodsOnly() {
		return false;
	}

}
