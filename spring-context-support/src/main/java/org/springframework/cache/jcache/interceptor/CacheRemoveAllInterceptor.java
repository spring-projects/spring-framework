/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.jcache.interceptor;

import javax.cache.annotation.CacheRemoveAll;

import org.jspecify.annotations.Nullable;

import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheOperationInvoker;

/**
 * Intercept methods annotated with {@link CacheRemoveAll}.
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
@SuppressWarnings("serial")
class CacheRemoveAllInterceptor extends AbstractCacheInterceptor<CacheRemoveAllOperation, CacheRemoveAll> {

	protected CacheRemoveAllInterceptor(CacheErrorHandler errorHandler) {
		super(errorHandler);
	}


	@Override
	protected @Nullable Object invoke(
			CacheOperationInvocationContext<CacheRemoveAllOperation> context, CacheOperationInvoker invoker) {

		CacheRemoveAllOperation operation = context.getOperation();
		boolean earlyRemove = operation.isEarlyRemove();
		if (earlyRemove) {
			removeAll(context);
		}

		try {
			Object result = invoker.invoke();
			if (!earlyRemove) {
				removeAll(context);
			}
			return result;
		}
		catch (CacheOperationInvoker.ThrowableWrapper ex) {
			Throwable original = ex.getOriginal();
			if (!earlyRemove && operation.getExceptionTypeFilter().match(original)) {
				removeAll(context);
			}
			throw ex;
		}
	}

	protected void removeAll(CacheOperationInvocationContext<CacheRemoveAllOperation> context) {
		Cache cache = resolveCache(context);
		if (logger.isTraceEnabled()) {
			logger.trace("Invalidating entire cache '" + cache.getName() + "' for operation " +
					context.getOperation());
		}
		doClear(cache, context.getOperation().isEarlyRemove());
	}

}
