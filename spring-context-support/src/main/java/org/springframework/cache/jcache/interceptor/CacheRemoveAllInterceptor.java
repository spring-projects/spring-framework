/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
			if (!earlyRemove && operation.getExceptionTypeFilter().match(original.getClass())) {
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
