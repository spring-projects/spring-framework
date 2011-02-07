/*
 * Copyright 2010 the original author or authors.
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

package org.springframework.cache.support;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.util.Assert;

/**
 * Abstract base class implementing the common CacheManager methods. Useful for 'static' environments where the
 * backing caches do not change.
 * 
 * @author Costin Leau
 */
public abstract class AbstractCacheManager implements CacheManager, InitializingBean {

	// fast lookup by name map
	private final ConcurrentMap<String, Cache<?, ?>> caches = new ConcurrentHashMap<String, Cache<?, ?>>();
	private Collection<String> names;

	public void afterPropertiesSet() {
		Collection<Cache<?, ?>> cacheSet = loadCaches();

		Assert.notEmpty(cacheSet);

		caches.clear();

		// preserve the initial order of the cache names
		Set<String> cacheNames = new LinkedHashSet<String>(cacheSet.size());

		for (Cache<?, ?> cache : cacheSet) {
			caches.put(cache.getName(), cache);
			cacheNames.add(cache.getName());
		}

		names = Collections.unmodifiableSet(cacheNames);
	}

	/**
	 * Loads the caches into the cache manager. Occurs at startup.
	 * The returned collection should not be null.
	 * 
	 * @param caches the collection of caches handled by the manager
	 */
	protected abstract Collection<Cache<?, ?>> loadCaches();

	/**
	 * Returns the internal cache map.
	 * 
	 * @return internal cache map
	 */
	protected final ConcurrentMap<String, Cache<?, ?>> getCacheMap() {
		return caches;
	}

	@SuppressWarnings("unchecked")
	public <K, V> Cache<K, V> getCache(String name) {
		return (Cache<K, V>) caches.get(name);
	}

	public Collection<String> getCacheNames() {
		return names;
	}
}