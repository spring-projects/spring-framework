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
	protected CacheKeyInvocationContext<A> createCacheKeyInvocationContext(
			CacheOperationInvocationContext<O> context) {
		return new DefaultCacheKeyInvocationContext<A>(context.getOperation(), context.getTarget(), context.getArgs());
	}

}
