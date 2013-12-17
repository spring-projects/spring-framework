/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.cache.infinispan;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.manager.EmbeddedCacheManager;
import org.springframework.cache.Cache;
import org.springframework.cache.transaction.AbstractTransactionSupportingCacheManager;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.cache.CacheManager} implementation
 * backed by a Infinispan {@link EmbeddedCacheManager}.
 *
 * @author Philippe Marschall
 * @since 4.0
 */
public class InfinispanCacheManager extends AbstractTransactionSupportingCacheManager {

	private EmbeddedCacheManager cacheManager;

	private boolean allowNullValues = true;


	/**
	 * Create a new InfinispanCacheManager, setting the target Infinispan CacheManager
	 * through the {@link #setCacheManager} bean property.
	 */
	public InfinispanCacheManager() {
	}

	/**
	 * Create a new InfinispanCacheManager for the given backing Infinispan.
	 * @param cacheManager the backing Infinispan {@link EmbeddedCacheManager}
	 */
	public InfinispanCacheManager(EmbeddedCacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}


	/**
	 * Set the backing Infinispan {@link EmbeddedCacheManager}.
	 */
	public void setCacheManager(EmbeddedCacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	/**
	 * Return the backing Infinispan {@link EmbeddedCacheManager}.
	 */
	public EmbeddedCacheManager getCacheManager() {
		return this.cacheManager;
	}

	/**
	 * Specify whether to accept and convert null values for all caches
	 * in this cache manager.
	 * <p>Default is "true", despite Infinispan itself not supporting null values.
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
		ComponentStatus status = this.cacheManager.getStatus();
		Assert.isTrue(ComponentStatus.RUNNING == status,
				"A 'running' Infinispan CacheManager is required - current cache is " + status);

		Set<String> cacheNames = this.cacheManager.getCacheNames();
		Collection<Cache> caches = new LinkedHashSet<Cache>(cacheNames.size());

		for (String cacheName : cacheNames) {
			org.infinispan.Cache infinispanCache = this.cacheManager.getCache(cacheName, true);
			caches.add(new InfinispanCache(infinispanCache, this.allowNullValues));
		}
		return caches;
	}

	@Override
	public Cache getCache(String name) {
		Cache cache = super.getCache(name);
		if (cache == null) {
			// check the Infinispan cache again
			// (in case the cache was added at runtime)
			org.infinispan.Cache infinispanCache = this.cacheManager.getCache(name, false);
			if (infinispanCache != null) {
				cache = new InfinispanCache(infinispanCache, this.allowNullValues);
				addCache(cache);
			}
		}
		return cache;
	}
}
