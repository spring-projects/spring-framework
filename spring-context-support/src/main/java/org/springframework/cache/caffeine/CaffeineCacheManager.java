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

package org.springframework.cache.caffeine;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.CaffeineSpec;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * {@link CacheManager} implementation that lazily builds {@link CaffeineCache}
 * instances for each {@link #getCache} request. Also supports a 'static' mode
 * where the set of cache names is pre-defined through {@link #setCacheNames},
 * with no dynamic creation of further cache regions at runtime.
 *
 * <p>The configuration of the underlying cache can be fine-tuned through a
 * {@link Caffeine} builder or {@link CaffeineSpec}, passed into this
 * CacheManager through {@link #setCaffeine}/{@link #setCaffeineSpec}.
 * A {@link CaffeineSpec}-compliant expression value can also be applied
 * via the {@link #setCacheSpecification "cacheSpecification"} bean property.
 *
 * <p>Requires Caffeine 2.1 or higher.
 *
 * @author Ben Manes
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @author Brian Clozel
 * @since 4.3
 * @see CaffeineCache
 */
public class CaffeineCacheManager implements CacheManager {

	private Caffeine<Object, Object> cacheBuilder = Caffeine.newBuilder();

	@Nullable
	private CacheLoader<Object, Object> cacheLoader;

	private boolean allowNullValues = true;

	private boolean dynamic = true;

	private final Map<String, Cache> cacheMap = new ConcurrentHashMap<>(16);

	private final Collection<String> customCacheNames = new CopyOnWriteArrayList<>();


	/**
	 * Construct a dynamic CaffeineCacheManager,
	 * lazily creating cache instances as they are being requested.
	 */
	public CaffeineCacheManager() {
	}

	/**
	 * Construct a static CaffeineCacheManager,
	 * managing caches for the specified cache names only.
	 */
	public CaffeineCacheManager(String... cacheNames) {
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
				this.cacheMap.put(name, createCaffeineCache(name));
			}
			this.dynamic = false;
		}
		else {
			this.dynamic = true;
		}
	}

	/**
	 * Set the Caffeine to use for building each individual
	 * {@link CaffeineCache} instance.
	 * @see #createNativeCaffeineCache
	 * @see Caffeine#build()
	 */
	public void setCaffeine(Caffeine<Object, Object> caffeine) {
		Assert.notNull(caffeine, "Caffeine must not be null");
		doSetCaffeine(caffeine);
	}

	/**
	 * Set the {@link CaffeineSpec} to use for building each individual
	 * {@link CaffeineCache} instance.
	 * @see #createNativeCaffeineCache
	 * @see Caffeine#from(CaffeineSpec)
	 */
	public void setCaffeineSpec(CaffeineSpec caffeineSpec) {
		doSetCaffeine(Caffeine.from(caffeineSpec));
	}

	/**
	 * Set the Caffeine cache specification String to use for building each
	 * individual {@link CaffeineCache} instance. The given value needs to
	 * comply with Caffeine's {@link CaffeineSpec} (see its javadoc).
	 * @see #createNativeCaffeineCache
	 * @see Caffeine#from(String)
	 */
	public void setCacheSpecification(String cacheSpecification) {
		doSetCaffeine(Caffeine.from(cacheSpecification));
	}

	private void doSetCaffeine(Caffeine<Object, Object> cacheBuilder) {
		if (!ObjectUtils.nullSafeEquals(this.cacheBuilder, cacheBuilder)) {
			this.cacheBuilder = cacheBuilder;
			refreshCommonCaches();
		}
	}

	/**
	 * Set the Caffeine CacheLoader to use for building each individual
	 * {@link CaffeineCache} instance, turning it into a LoadingCache.
	 * @see #createNativeCaffeineCache
	 * @see Caffeine#build(CacheLoader)
	 * @see com.github.benmanes.caffeine.cache.LoadingCache
	 */
	public void setCacheLoader(CacheLoader<Object, Object> cacheLoader) {
		if (!ObjectUtils.nullSafeEquals(this.cacheLoader, cacheLoader)) {
			this.cacheLoader = cacheLoader;
			refreshCommonCaches();
		}
	}

	/**
	 * Specify whether to accept and convert {@code null} values for all caches
	 * in this cache manager.
	 * <p>Default is "true", despite Caffeine itself not supporting {@code null} values.
	 * An internal holder object will be used to store user-level {@code null}s.
	 */
	public void setAllowNullValues(boolean allowNullValues) {
		if (this.allowNullValues != allowNullValues) {
			this.allowNullValues = allowNullValues;
			refreshCommonCaches();
		}
	}

	/**
	 * Return whether this cache manager accepts and converts {@code null} values
	 * for all of its caches.
	 */
	public boolean isAllowNullValues() {
		return this.allowNullValues;
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
			cache = this.cacheMap.computeIfAbsent(name, this::createCaffeineCache);
		}
		return cache;
	}


	/**
	 * Register the given native Caffeine Cache instance with this cache manager,
	 * adapting it to Spring's cache API for exposure through {@link #getCache}.
	 * Any number of such custom caches may be registered side by side.
	 * <p>This allows for custom settings per cache (as opposed to all caches
	 * sharing the common settings in the cache manager's configuration) and
	 * is typically used with the Caffeine builder API:
	 * {@code registerCustomCache("myCache", Caffeine.newBuilder().maximumSize(10).build())}
	 * <p>Note that any other caches, whether statically specified through
	 * {@link #setCacheNames} or dynamically built on demand, still operate
	 * with the common settings in the cache manager's configuration.
 	 * @param name the name of the cache
	 * @param cache the custom Caffeine Cache instance to register
	 * @since 5.2.8
	 * @see #adaptCaffeineCache
	 */
	public void registerCustomCache(String name, com.github.benmanes.caffeine.cache.Cache<Object, Object> cache) {
		this.customCacheNames.add(name);
		this.cacheMap.put(name, adaptCaffeineCache(name, cache));
	}

	/**
	 * Adapt the given new native Caffeine Cache instance to Spring's {@link Cache}
	 * abstraction for the specified cache name.
	 * @param name the name of the cache
	 * @param cache the native Caffeine Cache instance
	 * @return the Spring CaffeineCache adapter (or a decorator thereof)
	 * @since 5.2.8
	 * @see CaffeineCache
	 * @see #isAllowNullValues()
	 */
	protected Cache adaptCaffeineCache(String name, com.github.benmanes.caffeine.cache.Cache<Object, Object> cache) {
		return new CaffeineCache(name, cache, isAllowNullValues());
	}

	/**
	 * Build a common {@link CaffeineCache} instance for the specified cache name,
	 * using the common Caffeine configuration specified on this cache manager.
	 * <p>Delegates to {@link #adaptCaffeineCache} as the adaptation method to
	 * Spring's cache abstraction (allowing for centralized decoration etc.),
	 * passing in a freshly built native Caffeine Cache instance.
	 * @param name the name of the cache
	 * @return the Spring CaffeineCache adapter (or a decorator thereof)
	 * @see #adaptCaffeineCache
	 * @see #createNativeCaffeineCache
	 */
	protected Cache createCaffeineCache(String name) {
		return adaptCaffeineCache(name, createNativeCaffeineCache(name));
	}

	/**
	 * Build a common Caffeine Cache instance for the specified cache name,
	 * using the common Caffeine configuration specified on this cache manager.
	 * @param name the name of the cache
	 * @return the native Caffeine Cache instance
	 * @see #createCaffeineCache
	 */
	protected com.github.benmanes.caffeine.cache.Cache<Object, Object> createNativeCaffeineCache(String name) {
		return (this.cacheLoader != null ? this.cacheBuilder.build(this.cacheLoader) : this.cacheBuilder.build());
	}

	/**
	 * Recreate the common caches with the current state of this manager.
	 */
	private void refreshCommonCaches() {
		for (Map.Entry<String, Cache> entry : this.cacheMap.entrySet()) {
			if (!this.customCacheNames.contains(entry.getKey())) {
				entry.setValue(createCaffeineCache(entry.getKey()));
			}
		}
	}

}
