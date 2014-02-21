/*
 * Copyright 2002-2014 the original author or authors.
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

import javax.cache.annotation.CacheRemoveAll;

import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheOperationInvoker;
import org.springframework.cache.jcache.model.CacheRemoveAllOperation;

/**
 * Intercept methods annotated with {@link CacheRemoveAll}.
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
@SuppressWarnings("serial")
public class CacheRemoveAllInterceptor
		extends AbstractCacheInterceptor<CacheRemoveAllOperation, CacheRemoveAll> {

	@Override
	protected Object invoke(CacheOperationInvocationContext<CacheRemoveAllOperation> context,
			CacheOperationInvoker invoker) {
		CacheRemoveAllOperation operation = context.getOperation();

		final boolean earlyRemove = operation.isEarlyRemove();

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
		catch (CacheOperationInvoker.ThrowableWrapper t) {
			Throwable ex = t.getOriginal();
			if (!earlyRemove && operation.getExceptionTypeFilter().match(ex.getClass())) {
				removeAll(context);
			}
			throw t;
		}
	}

	protected void removeAll(CacheOperationInvocationContext<CacheRemoveAllOperation> context) {
		Cache cache = resolveCache(context);
		if (logger.isTraceEnabled()) {
			logger.trace("Invalidating entire cache '" + cache.getName() + "' for operation "
					+ context.getOperation());
		}
		cache.clear();
	}

}
