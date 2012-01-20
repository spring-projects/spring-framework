/*
 * Copyright 2011 the original author or authors.
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

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/**
 * A basic, no operation {@link CacheManager} implementation suitable
 * for disabling caching, typically used for backing cache declarations
 * without an actual backing store.
 *
 * <p>Will simply accept any items into the cache not actually storing them.
 *
 * @author Costin Leau
 * @since 3.1
 * @see CompositeCacheManager
 */
public class NoOpCacheManager implements CacheManager {

	private final ConcurrentMap<String, Cache> caches = new ConcurrentHashMap<String, Cache>();
	private Set<String> names = new LinkedHashSet<String>();

	private static class NoOpCache implements Cache {

		private final String name;

		public NoOpCache(String name) {
			this.name = name;
		}

		public void clear() {
		}

		public void evict(Object key) {
		}

		public ValueWrapper get(Object key) {
			return null;
		}

		public String getName() {
			return name;
		}

		public Object getNativeCache() {
			return null;
		}

		public void put(Object key, Object value) {
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 *  This implementation always returns a {@link Cache} implementation that will not
	 *  store items. Additionally, the request cache will be remembered by the manager for consistency.
	 */
	public Cache getCache(String name) {
		Cache cache = caches.get(name);
		if (cache == null) {
			caches.putIfAbsent(name, new NoOpCache(name));
			synchronized (names) {
				names.add(name);
			}
		}

		return caches.get(name);
	}

	/**
	 * {@inheritDoc}
	 *
	 * This implementation returns the name of the caches previously requested.
	 */
	public Collection<String> getCacheNames() {
		return Collections.unmodifiableSet(names);
	}
}
