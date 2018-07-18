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

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.AbstractCacheInvoker;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheOperationInvoker;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

/**
 * A base interceptor for JSR-107 cache annotations.
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
@SuppressWarnings("serial")
abstract class AbstractCacheInterceptor<O extends AbstractJCacheOperation<A>, A extends Annotation>
		extends AbstractCacheInvoker implements Serializable {

	protected final Log logger = LogFactory.getLog(getClass());


	protected AbstractCacheInterceptor(CacheErrorHandler errorHandler) {
		super(errorHandler);
	}


	@Nullable
	protected abstract Object invoke(CacheOperationInvocationContext<O> context, CacheOperationInvoker invoker)
			throws Throwable;


	/**
	 * Resolve the cache to use.
	 * @param context the invocation context
	 * @return the cache to use (never null)
	 */
	protected Cache resolveCache(CacheOperationInvocationContext<O> context) {
		Collection<? extends Cache> caches = context.getOperation().getCacheResolver().resolveCaches(context);
		Cache cache = extractFrom(caches);
		if (cache == null) {
			throw new IllegalStateException("Cache could not have been resolved for " + context.getOperation());
		}
		return cache;
	}

	/**
	 * Convert the collection of caches in a single expected element.
	 * <p>Throw an {@link IllegalStateException} if the collection holds more than one element
	 * @return the single element or {@code null} if the collection is empty
	 */
	@Nullable
	static Cache extractFrom(Collection<? extends Cache> caches) {
		if (CollectionUtils.isEmpty(caches)) {
			return null;
		}
		else if (caches.size() == 1) {
			return caches.iterator().next();
		}
		else {
			throw new IllegalStateException("Unsupported cache resolution result " + caches +
					": JSR-107 only supports a single cache.");
		}
	}

}
