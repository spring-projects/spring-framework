/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.cache.jcache;

import java.util.Collection;
import java.util.LinkedHashSet;

import javax.cache.Status;

import org.springframework.cache.Cache;
import org.springframework.cache.support.AbstractCacheManager;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.cache.CacheManager} implementation
 * backed by a JCache {@link javax.cache.CacheManager}.
 *
 * @author Juergen Hoeller
 * @since 3.2
 */
public class JCacheCacheManager extends AbstractCacheManager {

	private javax.cache.CacheManager cacheManager;

	private boolean allowNullValues = true;


	/**
	 * Set the backing JCache {@link javax.cache.CacheManager}.
	 */
	public void setCacheManager(javax.cache.CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	/**
	 * Return the backing JCache {@link javax.cache.CacheManager}.
	 */
	public javax.cache.CacheManager getCacheManager() {
		return this.cacheManager;
	}

	/**
	 * Specify whether to accept and convert null values for all caches
	 * in this cache manager.
	 * <p>Default is "true", despite JSR-107 itself not supporting null values.
	 * An internal holder object will be used to store user-level null values.
	 */
	public void setAllowNullValues(boolean allowNullValues) {
		this.allowNullValues = allowNullValues;
	}

	/**
	 * Return whether this cache manager accepts and converts null values
	 * for all of its caches.
	 */
	public boolean isAllowNullValues() {
		return this.allowNullValues;
	}


	@Override
	protected Collection<Cache> loadCaches() {
		Assert.notNull(this.cacheManager, "A backing CacheManager is required");
		Status status = this.cacheManager.getStatus();
		Assert.isTrue(Status.STARTED.equals(status),
				"A 'started' JCache CacheManager is required - current cache is " + status.toString());

		Collection<Cache> caches = new LinkedHashSet<Cache>();
		for (javax.cache.Cache<?,?> jcache : this.cacheManager.getCaches()) {
			caches.add(new JCacheCache(jcache, this.allowNullValues));
		}
		return caches;
	}

	@Override
	public Cache getCache(String name) {
		Cache cache = super.getCache(name);
		if (cache == null) {
			// check the JCache cache again
			// (in case the cache was added at runtime)
			javax.cache.Cache<?,?> jcache = this.cacheManager.getCache(name);
			if (jcache != null) {
				cache = new JCacheCache(jcache, this.allowNullValues);
				addCache(cache);
			}
		}
		return cache;
	}

}
