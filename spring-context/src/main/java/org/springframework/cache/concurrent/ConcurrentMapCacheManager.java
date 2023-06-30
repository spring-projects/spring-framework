/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.cache.concurrent;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.serializer.support.SerializationDelegate;
import org.springframework.lang.Nullable;

/**
 * {@link CacheManager} implementation that lazily builds {@link ConcurrentMapCache}
 * instances for each {@link #getCache} request. Also supports a 'static' mode where
 * the set of cache names is pre-defined through {@link #setCacheNames}, with no
 * dynamic creation of further cache regions at runtime.
 *
 * <p>Note: This is by no means a sophisticated CacheManager; it comes with no
 * cache configuration options. However, it may be useful for testing or simple
 * caching scenarios. For advanced local caching needs, consider
 * {@link org.springframework.cache.jcache.JCacheCacheManager},
 * {@link org.springframework.cache.ehcache.EhCacheCacheManager},
 * {@link org.springframework.cache.caffeine.CaffeineCacheManager}.
 *
 * @author Juergen Hoeller
 * @since 3.1
 * @see ConcurrentMapCache
 */
public class ConcurrentMapCacheManager implements CacheManager, BeanClassLoaderAware {

	private final ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<>(16);

	private boolean dynamic = true;

	private boolean allowNullValues = true;

	private boolean storeByValue = false;

	@Nullable
	private SerializationDelegate serialization;


	/**
	 * Construct a dynamic ConcurrentMapCacheManager,
	 * lazily creating cache instances as they are being requested.
	 */
	public ConcurrentMapCacheManager() {
	}

	/**
	 * Construct a static ConcurrentMapCacheManager,
	 * managing caches for the specified cache names only.
	 */
	public ConcurrentMapCacheManager(String... cacheNames) {
		setCacheNames(Arrays.asList(cacheNames));
	}


	/**
	 * Specify the set of cache names for this CacheManager's 'static' mode.
	 * <p>The number of caches and their names will be fixed after a call to this method,
	 * with no creation of further cache regions at runtime.
	 * <p>Calling this with a {@code null} collection argument resets the
	 * mode to 'dynamic', allowing for further creation of caches again.
	 */
	public void setCacheNames(@Nullable Collection<String> cacheNames) {
		if (cacheNames != null) {
			for (String name : cacheNames) {
				this.cacheMap.put(name, createConcurrentMapCache(name));
			}
			this.dynamic = false;
		}
		else {
			this.dynamic = true;
		}
	}

	/**
	 * Specify whether to accept and convert {@code null} values for all caches
	 * in this cache manager.
	 * <p>Default is "true", despite ConcurrentHashMap itself not supporting {@code null}
	 * values. An internal holder object will be used to store user-level {@code null}s.
	 * <p>Note: A change of the null-value setting will reset all existing caches,
	 * if any, to reconfigure them with the new null-value requirement.
	 */
	public void setAllowNullValues(boolean allowNullValues) {
		if (allowNullValues != this.allowNullValues) {
			this.allowNullValues = allowNullValues;
			// Need to recreate all Cache instances with the new null-value configuration...
			recreateCaches();
		}
	}

	/**
	 * Return whether this cache manager accepts and converts {@code null} values
	 * for all of its caches.
	 */
	public boolean isAllowNullValues() {
		return this.allowNullValues;
	}

	/**
	 * Specify whether this cache manager stores a copy of each entry ({@code true}
	 * or the reference ({@code false} for all of its caches.
	 * <p>Default is "false" so that the value itself is stored and no serializable
	 * contract is required on cached values.
	 * <p>Note: A change of the store-by-value setting will reset all existing caches,
	 * if any, to reconfigure them with the new store-by-value requirement.
	 * @since 4.3
	 */
	public void setStoreByValue(boolean storeByValue) {
		if (storeByValue != this.storeByValue) {
			this.storeByValue = storeByValue;
			// Need to recreate all Cache instances with the new store-by-value configuration...
			recreateCaches();
		}
	}

	/**
	 * Return whether this cache manager stores a copy of each entry or
	 * a reference for all its caches. If store by value is enabled, any
	 * cache entry must be serializable.
	 * @since 4.3
	 */
	public boolean isStoreByValue() {
		return this.storeByValue;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.serialization = new SerializationDelegate(classLoader);
		// Need to recreate all Cache instances with new ClassLoader in store-by-value mode...
		if (isStoreByValue()) {
			recreateCaches();
		}
	}


	@Override
	public Collection<String> getCacheNames() {
		return Collections.unmodifiableSet(this.cacheMap.keySet());
	}

	@Override
	@Nullable
	public Cache getCache(String name) {
		Cache cache = this.cacheMap.get(name);
		if (cache == null && this.dynamic) {
			cache = this.cacheMap.computeIfAbsent(name, this::createConcurrentMapCache);
		}
		return cache;
	}

	private void recreateCaches() {
		for (Map.Entry<String, Cache> entry : this.cacheMap.entrySet()) {
			entry.setValue(createConcurrentMapCache(entry.getKey()));
		}
	}

	/**
	 * Create a new ConcurrentMapCache instance for the specified cache name.
	 * @param name the name of the cache
	 * @return the ConcurrentMapCache (or a decorator thereof)
	 */
	protected Cache createConcurrentMapCache(String name) {
		SerializationDelegate actualSerialization = (isStoreByValue() ? this.serialization : null);
		return new ConcurrentMapCache(name, new ConcurrentHashMap<>(256), isAllowNullValues(), actualSerialization);
	}

}
