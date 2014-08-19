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

/**
 * Abstract base class implementing the common {@link CacheManager} methods.
 * Useful for 'static' environments where the backing caches do not change.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 3.1
 */
public abstract class AbstractCacheManager implements CacheManager, InitializingBean {

	private final ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<String, Cache>(16);

	private Set<String> cacheNames = new LinkedHashSet<String>(16);


	// Early cache initialization on startup

	@Override
	public void afterPropertiesSet() {
		Collection<? extends Cache> caches = loadCaches();

		// Preserve the initial order of the cache names
		this.cacheMap.clear();
		this.cacheNames.clear();
		for (Cache cache : caches) {
			addCache(cache);
		}
	}

	/**
	 * Load the initial caches for this cache manager.
	 * <p>Called by {@link #afterPropertiesSet()} on startup.
	 * The returned collection may be empty but must not be {@code null}.
	 */
	protected abstract Collection<? extends Cache> loadCaches();


	// Lazy cache initialization on access

	@Override
	public Cache getCache(String name) {
		Cache cache = lookupCache(name);
		if (cache != null) {
			return cache;
		}
		else {
			Cache missingCache = getMissingCache(name);
			if (missingCache != null) {
				addCache(missingCache);
				return lookupCache(name);  // may be decorated
			}
			return null;
		}
	}

	@Override
	public Collection<String> getCacheNames() {
		return Collections.unmodifiableSet(this.cacheNames);
	}


	// Common cache initialization delegates/callbacks

	protected final void addCache(Cache cache) {
		this.cacheMap.put(cache.getName(), decorateCache(cache));
		this.cacheNames.add(cache.getName());
	}

	protected final Cache lookupCache(String name) {
		return this.cacheMap.get(name);
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

	/**
	 * Return a missing cache with the specified {@code name} or {@code null} if
	 * such cache does not exist or could not be created on the fly.
	 * <p>Some caches may be created at runtime in the native provided. If a lookup
	 * by name does not yield any result, a subclass gets a chance to register
	 * such a cache at runtime. The returned cache will be automatically added to
	 * this instance.
	 * @param name the name of the cache to retrieve
	 * @return the missing cache or {@code null} if no such cache exists or could be
	 * created
	 * @see #getCache(String)
	 */
	protected Cache getMissingCache(String name) {
		return null;
	}

}
