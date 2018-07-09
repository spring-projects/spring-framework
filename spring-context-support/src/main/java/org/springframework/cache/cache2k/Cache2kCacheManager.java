/*
 * Copyright 2018-2018 the original author or authors.
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

package org.springframework.cache.cache2k;

import org.cache2k.Cache2kBuilder;
import org.cache2k.configuration.Cache2kConfiguration;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache manager implementation on top of the cache2k cache manager. The default cache2k
 * cache manager is used to resolve caches.
 *
 * @author Jens Wilke
 */
public class Cache2kCacheManager implements CacheManager {

	private final org.cache2k.CacheManager manager;

	private final Map<String, Cache> name2cache = new ConcurrentHashMap<>();

	/**
	 * Construct a spring cache manager, using the default cache2k cache manager instance.
	 *
	 * @see org.cache2k.CacheManager
	 * @see org.cache2k.CacheManager#getInstance()
	 */
	public Cache2kCacheManager() {
		manager = org.cache2k.CacheManager.getInstance();
	}

	/**
	 * Construct a spring cache manager, using a custom cache2k cache manager. This
	 * allows for the use of a different manager name and classloader.
	 *
	 * @see org.cache2k.CacheManager
	 * @see org.cache2k.CacheManager#getInstance(String)
	 * @see org.cache2k.CacheManager#getInstance(ClassLoader)
	 * @see org.cache2k.CacheManager#getInstance(ClassLoader, String)
	 */
	public Cache2kCacheManager(org.cache2k.CacheManager manager) { this.manager = manager; }

	@SuppressWarnings("unchecked")
	@Nullable
	@Override
	public Cache getCache(final String name) {
		Cache cache = name2cache.get(name);
		if (cache != null) {
			return cache;
		}
		synchronized (name2cache) {
			cache = name2cache.get(name);
			if (cache != null) {
				return cache;
			}
			cache = buildAndWrap(configureCache(Cache2kBuilder.forUnknownTypes().manager(manager), name));
			name2cache.put(name, cache);
		}
		return cache;
	}

	/**
	 * There is no way in cache2k to return all known caches. Caches may be created ad hoc
	 * only within the code and do not need to be enlisted in a configuration.
	 * Checking Spring version 4.4.1 the framework is actually not using this method.
	 *
	 * <p>To be future proof this method returns the names of caches that have been
	 * created within the cache manager. Future version of cache2k may get a flag
	 * per cache that activates caches on startup, before they are requested by the application.
	 */
	@Override
	public Collection<String> getCacheNames() {
		Set<String> cacheNames = new HashSet<>();
		for (org.cache2k.Cache<?,?> cache : manager.getActiveCaches()) {
			cacheNames.add(cache.getName());
		}
		return cacheNames;
	}

	/**
	 * Expose the native cache manager.
	 */
	public org.cache2k.CacheManager getNativeCacheManager() {
		return manager;
	}

	/**
	 * Expose the map of known wrapped caches.
	 */
	public Map<String, Cache> getCacheMap() {
		return name2cache;
	}

	private static final Callable<Object> DUMMY_CALLABLE = new Callable<Object>() {
		@Override
		public Object call() {
			return null;
		}
		public String toString() {
			return "Callable(DUMMY)";
		}
	};

	public static Cache2kBuilder<Object,Object> configureCache(Cache2kBuilder<Object, Object> builder, String name) {
		return builder
				.name(name)
				.permitNullValues(true)
				.exceptionPropagator(
					(key, exceptionInformation) ->
					new Cache.ValueRetrievalException(key, DUMMY_CALLABLE, exceptionInformation.getException()));
	}

	public static Cache2kCache buildAndWrap(Cache2kBuilder<Object, Object> builder) {
		org.cache2k.Cache<Object, Object> nativeCache = builder.build();
		Cache2kConfiguration<?,?> cfg = builder.toConfiguration();
		boolean loaderPresent = cfg.getLoader() != null || cfg.getAdvancedLoader() != null;
		return new Cache2kCache(nativeCache, loaderPresent);
	}

}
