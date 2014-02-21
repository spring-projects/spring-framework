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

import javax.cache.annotation.CacheKeyInvocationContext;
import javax.cache.annotation.CachePut;

import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheOperationInvoker;
import org.springframework.cache.jcache.model.CachePutOperation;

/**
 * Intercept methods annotated with {@link CachePut}.
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
@SuppressWarnings("serial")
public class CachePutInterceptor extends AbstractKeyCacheInterceptor<CachePutOperation, CachePut> {

	@Override
	protected Object invoke(CacheOperationInvocationContext<CachePutOperation> context,
			CacheOperationInvoker invoker) {
		CacheKeyInvocationContext<CachePut> invocationContext = createCacheKeyInvocationContext(context);
		CachePutOperation operation = context.getOperation();

		final boolean earlyPut = operation.isEarlyPut();
		final Object value = invocationContext.getValueParameter().getValue();

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
		catch (CacheOperationInvoker.ThrowableWrapper t) {
			Throwable ex = t.getOriginal();
			if (!earlyPut && operation.getExceptionTypeFilter().match(ex.getClass())) {
				cacheValue(context, value);
			}
			throw t;
		}
	}

	protected void cacheValue(CacheOperationInvocationContext<CachePutOperation> context, Object value) {
		Object key = generateKey(context);
		Cache cache = resolveCache(context);
		cache.put(key, value);
	}

}
