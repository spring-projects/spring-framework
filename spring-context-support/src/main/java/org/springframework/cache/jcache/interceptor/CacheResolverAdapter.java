/*
 * Copyright 2002-2018 the original author or authors.
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

import java.util.Collection;
import java.util.Collections;
import javax.cache.annotation.CacheInvocationContext;

import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.jcache.JCacheCache;
import org.springframework.util.Assert;

/**
 * Spring's {@link CacheResolver} implementation that delegates to a standard
 * JSR-107 {@link javax.cache.annotation.CacheResolver}.
 * <p>Used internally to invoke user-based JSR-107 cache resolvers.
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
class CacheResolverAdapter implements CacheResolver {

	private final javax.cache.annotation.CacheResolver target;


	/**
	 * Create a new instance with the JSR-107 cache resolver to invoke.
	 */
	public CacheResolverAdapter(javax.cache.annotation.CacheResolver target) {
		Assert.notNull(target, "JSR-107 CacheResolver is required");
		this.target = target;
	}


	/**
	 * Return the underlying {@link javax.cache.annotation.CacheResolver}
	 * that this instance is using.
	 */
	protected javax.cache.annotation.CacheResolver getTarget() {
		return this.target;
	}

	@Override
	public Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context) {
		if (!(context instanceof CacheInvocationContext<?>)) {
			throw new IllegalStateException("Unexpected context " + context);
		}
		CacheInvocationContext<?> cacheInvocationContext = (CacheInvocationContext<?>) context;
		javax.cache.Cache<Object, Object> cache = this.target.resolveCache(cacheInvocationContext);
		if (cache == null) {
			throw new IllegalStateException("Could not resolve cache for " + context + " using " + this.target);
		}
		return Collections.singleton(new JCacheCache(cache));
	}

}
