/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.jcache.interceptor;

import java.lang.annotation.Annotation;

import javax.cache.annotation.CacheKeyInvocationContext;

import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.KeyGenerator;

/**
 * A base interceptor for JSR-107 key-based cache annotations.
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @param <O> the operation type
 * @param <A> the annotation type
 */
@SuppressWarnings("serial")
abstract class AbstractKeyCacheInterceptor<O extends AbstractJCacheKeyOperation<A>, A extends Annotation>
		extends AbstractCacheInterceptor<O, A> {

	protected AbstractKeyCacheInterceptor(CacheErrorHandler errorHandler) {
		super(errorHandler);
	}


	/**
	 * Generate a key for the specified invocation.
	 * @param context the context of the invocation
	 * @return the key to use
	 */
	protected Object generateKey(CacheOperationInvocationContext<O> context) {
		KeyGenerator keyGenerator = context.getOperation().getKeyGenerator();
		Object key = keyGenerator.generate(context.getTarget(), context.getMethod(), context.getArgs());
		if (logger.isTraceEnabled()) {
			logger.trace("Computed cache key " + key + " for operation " + context.getOperation());
		}
		return key;
	}

	/**
	 * Create a {@link CacheKeyInvocationContext} based on the specified invocation.
	 * @param context the context of the invocation.
	 * @return the related {@code CacheKeyInvocationContext}
	 */
	protected CacheKeyInvocationContext<A> createCacheKeyInvocationContext(CacheOperationInvocationContext<O> context) {
		return new DefaultCacheKeyInvocationContext<>(context.getOperation(), context.getTarget(), context.getArgs());
	}

}
