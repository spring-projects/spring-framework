/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.cache.support;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;

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

	private final ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<>(16);


	// Early cache initialization on startup

	@Override
	public void afterPropertiesSet() {
		initializeCaches();
	}

	/**
	 * Initialize the static configuration of caches.
	 * <p>Triggered on startup through {@link #afterPropertiesSet()};
	 * can also be called to re-initialize at runtime.
	 * @since 4.2.2
	 * @see #loadCaches()
	 */
	public void initializeCaches() {
		Collection<? extends Cache> caches = loadCaches();

		synchronized (this.cacheMap) {
			this.cacheMap.clear();
			for (Cache cache : caches) {
				this.cacheMap.put(cache.getName(), decorateCache(cache));
			}
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
	@Nullable
	public Cache getCache(String name) {
		return this.cacheMap.computeIfAbsent(name, k -> {
			Cache missingCache = getMissingCache(k);
			return missingCache == null ? null : decorateCache(missingCache);
		});
	}

	@Override
	public Collection<String> getCacheNames() {
		return Collections.unmodifiableSet(cacheMap.keySet());
	}


	// Common cache initialization delegates for subclasses

	/**
	 * Check for a registered cache of the given name.
	 * In contrast to {@link #getCache(String)}, this method does not trigger
	 * the lazy creation of missing caches via {@link #getMissingCache(String)}.
	 * @param name the cache identifier (must not be {@code null})
	 * @return the associated Cache instance, or {@code null} if none found
	 * @since 4.1
	 * @see #getCache(String)
	 * @see #getMissingCache(String)
	 */
	@Nullable
	protected final Cache lookupCache(String name) {
		return this.cacheMap.get(name);
	}

	/**
	 * Dynamically register an additional Cache with this manager.
	 * @param cache the Cache to register
	 * @deprecated as of Spring 4.3, in favor of {@link #getMissingCache(String)}
	 */
	@Deprecated
	protected final void addCache(Cache cache) {
		this.cacheMap.put(cache.getName(), decorateCache(cache));
	}

	// Overridable template methods for cache initialization

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
	 * Return a missing cache with the specified {@code name}, or {@code null} if
	 * such a cache does not exist or could not be created on demand.
	 * <p>Caches may be lazily created at runtime if the native provider supports it.
	 * If a lookup by name does not yield any result, an {@code AbstractCacheManager}
	 * subclass gets a chance to register such a cache at runtime. The returned cache
	 * will be automatically added to this cache manager.
	 * @param name the name of the cache to retrieve
	 * @return the missing cache, or {@code null} if no such cache exists or could be
	 * created on demand
	 * @since 4.1
	 * @see #getCache(String)
	 */
	@Nullable
	protected Cache getMissingCache(String name) {
		return null;
	}

}
