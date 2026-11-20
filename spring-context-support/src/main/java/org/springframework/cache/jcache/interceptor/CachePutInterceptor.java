/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.jcache.interceptor;

import javax.cache.annotation.CacheKeyInvocationContext;
import javax.cache.annotation.CachePut;

import org.jspecify.annotations.Nullable;

import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheOperationInvoker;

/**
 * Intercept methods annotated with {@link CachePut}.
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
@SuppressWarnings("serial")
class CachePutInterceptor extends AbstractKeyCacheInterceptor<CachePutOperation, CachePut> {

	public CachePutInterceptor(CacheErrorHandler errorHandler) {
		super(errorHandler);
	}


	@Override
	protected @Nullable Object invoke(
			CacheOperationInvocationContext<CachePutOperation> context, CacheOperationInvoker invoker) {

		CachePutOperation operation = context.getOperation();
		CacheKeyInvocationContext<CachePut> invocationContext = createCacheKeyInvocationContext(context);

		boolean earlyPut = operation.isEarlyPut();
		Object value = invocationContext.getValueParameter().getValue();
		if (earlyPut) {
			cacheValue(context, value);
		}

		try {
			Object result = invoker.invoke();
			if (!earlyPut) {
				cacheValue(context, value);
			}
			return result;
		}
		catch (CacheOperationInvoker.ThrowableWrapper ex) {
			Throwable original = ex.getOriginal();
			if (!earlyPut && operation.getExceptionTypeFilter().match(original)) {
				cacheValue(context, value);
			}
			throw ex;
		}
	}

	protected void cacheValue(CacheOperationInvocationContext<CachePutOperation> context, Object value) {
		Object key = generateKey(context);
		Cache cache = resolveCache(context);
		doPut(cache, key, value);
	}

}
