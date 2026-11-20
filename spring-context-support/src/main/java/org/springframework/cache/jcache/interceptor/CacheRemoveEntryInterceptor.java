/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.jcache.interceptor;

import javax.cache.annotation.CacheRemove;

import org.jspecify.annotations.Nullable;

import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheOperationInvoker;

/**
 * Intercept methods annotated with {@link CacheRemove}.
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
@SuppressWarnings("serial")
class CacheRemoveEntryInterceptor extends AbstractKeyCacheInterceptor<CacheRemoveOperation, CacheRemove> {

	protected CacheRemoveEntryInterceptor(CacheErrorHandler errorHandler) {
		super(errorHandler);
	}


	@Override
	protected @Nullable Object invoke(
			CacheOperationInvocationContext<CacheRemoveOperation> context, CacheOperationInvoker invoker) {

		CacheRemoveOperation operation = context.getOperation();
		boolean earlyRemove = operation.isEarlyRemove();
		if (earlyRemove) {
			removeValue(context);
		}

		try {
			Object result = invoker.invoke();
			if (!earlyRemove) {
				removeValue(context);
			}
			return result;
		}
		catch (CacheOperationInvoker.ThrowableWrapper ex) {
			Throwable original = ex.getOriginal();
			if (!earlyRemove && operation.getExceptionTypeFilter().match(original)) {
				removeValue(context);
			}
			throw ex;
		}
	}

	private void removeValue(CacheOperationInvocationContext<CacheRemoveOperation> context) {
		Object key = generateKey(context);
		Cache cache = resolveCache(context);
		if (logger.isTraceEnabled()) {
			logger.trace("Invalidating key [" + key + "] on cache '" + cache.getName() +
					"' for operation " + context.getOperation());
		}
		doEvict(cache, key, context.getOperation().isEarlyRemove());
	}

}
