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

package org.springframework.cache.guava;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheBuilderSpec;
import com.google.common.cache.CacheLoader;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.AbstractCacheManager;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * {@link CacheManager} implementation that can be assigned a set of custom
 * {@link GuavaCache} instances. Each {@link #getCache} request will retrieve
 * the named {@link GuavaCache} instance. If a named instance is not found, then a
 * new {@link GuavaCache} will be created based on a default Guava {@link CacheBuilder}.
 *
 * <p> The caches are set through a set of {@link GuavaCacheFactoryBean}. Each
 * factory bean can be customized with a Guava {@link CacheBuilder} or {@link CacheBuilderSpec},
 * passed into this CacheManager through
 * {@link GuavaCacheFactoryBean#setCacheBuilder}/{@link GuavaCacheFactoryBean#setCacheBuilderSpec}.
 </p>
 *
 * <p>Requires Google Guava 12.0 or higher.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author Biju Kunjummen
 * @since 4.0
 * @see GuavaCache
 */
public class GuavaCacheManager implements CacheManager {

	private ConcurrentMap<String, GuavaCache> cacheMap = new ConcurrentHashMap<String, GuavaCache>(16);

	private CacheBuilder<Object, Object> defaultCacheBuilder = CacheBuilder.newBuilder();

	public GuavaCacheManager() {
	}

	public void setCaches(Set<GuavaCache> caches) {
		for (GuavaCache cache: caches) {
			this.cacheMap.put(cache.getName(), cache);
		}
	}


	@Override
	public Collection<String> getCacheNames() {
		return Collections.unmodifiableSet(this.cacheMap.keySet());
	}

	@Override
	public Cache getCache(String name) {
		GuavaCache cache = this.cacheMap.get(name);
		if (cache == null) {
			synchronized (this.cacheMap) {
				cache = this.cacheMap.get(name);
				if (cache == null) {
					cache = createDefaultGuavaCache(name);
					this.cacheMap.put(name, cache);
				}
			}
		}
		return cache;
	}

	private GuavaCache createDefaultGuavaCache(String name) {
		return new GuavaCache(name, this.defaultCacheBuilder.build());
	}
}
