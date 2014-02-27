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
import java.util.Collections;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.util.Assert;

/**
 * A base {@link CacheResolver} implementation that requires the concrete
 * implementation to provide the collection of cache name(s) based on the
 * invocation context.
 *
 * @author Stephane Nicoll
 */
public abstract class BaseCacheResolver implements CacheResolver, InitializingBean {

	private CacheManager cacheManager;

	protected BaseCacheResolver(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	protected BaseCacheResolver() {
	}

	/**
	 * Set the {@link CacheManager} that this instance should use.
	 */
	public void setCacheManager(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	/**
	 * Return the {@link CacheManager} that this instance use.
	 */
	public CacheManager getCacheManager() {
		return cacheManager;
	}

	@Override
	public void afterPropertiesSet()  {
		Assert.notNull(cacheManager, "CacheManager must not be null");
	}

	@Override
	public Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context) {
		Collection<String> cacheNames = getCacheNames(context);
		if (cacheNames == null) {
			return Collections.emptyList();
		}
		else {
			Collection<Cache> result = new ArrayList<Cache>();
			for (String cacheName : cacheNames) {
				Cache cache = cacheManager.getCache(cacheName);
				Assert.notNull(cache, "Cannot find cache named '" + cacheName + "' for " + context.getOperation());
				result.add(cache);
			}
			return result;
		}
	}

	/**
	 * Provide the name of the cache(s) to resolve against the current cache manager.
	 * <p>It is acceptable to return {@code null} to indicate that no cache could
	 * be resolved for this invocation.
	 *
	 * @param context the context of the particular invocation
	 * @return the cache name(s) to resolve or {@code null} if no cache should be resolved
	 */
	protected abstract Collection<String> getCacheNames(CacheOperationInvocationContext<?> context);

}
