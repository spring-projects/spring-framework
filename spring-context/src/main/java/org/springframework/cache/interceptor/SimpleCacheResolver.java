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

package org.springframework.cache.interceptor;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.util.Assert;

/**
 * A simple {@link CacheResolver} that resolves the {@link Cache} instance(s)
 * based on a configurable {@link CacheManager} and the name of the
 * cache(s): {@link BasicCacheOperation#getCacheNames()}
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @see BasicCacheOperation#getCacheNames()
 */
public class SimpleCacheResolver implements CacheResolver {

	private final CacheManager cacheManager;

	public SimpleCacheResolver(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	@Override
	public Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context) {
		Collection<Cache> result = new ArrayList<Cache>();
		for (String cacheName : context.getOperation().getCacheNames()) {
			Cache cache = cacheManager.getCache(cacheName);
			Assert.notNull(cache, "Cannot find cache named '" + cacheName + "' for " + context.getOperation());
			result.add(cache);
		}
		return result;
	}

}
