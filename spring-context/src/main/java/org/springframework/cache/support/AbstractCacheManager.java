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
 * Abstract base class implementing the common {@link CacheManager}
 * methods. Useful for 'static' environments where the backing caches do
 * not change.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 3.1
 */
public abstract class AbstractCacheManager implements CacheManager, InitializingBean {

	private final ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<String, Cache>();

	private Set<String> cacheNames = new LinkedHashSet<String>();


	public void afterPropertiesSet() {
		Collection<? extends Cache> caches = loadCaches();
		Assert.notEmpty(caches, "loadCaches must not return an empty Collection");
		this.cacheMap.clear();

		// preserve the initial order of the cache names
		for (Cache cache : caches) {
			this.cacheMap.put(cache.getName(), decorateCache(cache));
			this.cacheNames.add(cache.getName());
		}
	}

	protected final void addCache(Cache cache) {
		this.cacheMap.put(cache.getName(), decorateCache(cache));
		this.cacheNames.add(cache.getName());
	}

	/**
	 * Decorate the given Cache object if necessary.
	 * @param cache the Cache object to be added to this CacheManager
	 * @return the decorated Cache object to be used instead,
	 * or simply the passed-in Cache object by default
	 */
	protected Cache decorateCache(Cache cache) {
		return cache;
	}


	public Cache getCache(String name) {
		return this.cacheMap.get(name);
	}

	public Collection<String> getCacheNames() {
		return Collections.unmodifiableSet(this.cacheNames);
	}


	/**
	 * Load the caches for this cache manager. Occurs at startup.
	 * The returned collection must not be null.
	 */
	protected abstract Collection<? extends Cache> loadCaches();

}
