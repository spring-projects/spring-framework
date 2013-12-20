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

package org.springframework.cache.guava;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheBuilderSpec;
import com.google.common.cache.CacheLoader;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.util.Assert;

/**
 * {@link CacheManager} implementation that lazily builds {@link GuavaCache}
 * instances for each {@link #getCache} request. Also supports a 'static' mode
 * where the set of cache names is pre-defined through {@link #setCacheNames},
 * with no dynamic creation of further cache regions at runtime.
 *
 * <p>The configuration of the underlying cache can be fine-tuned through a
 * Guava {@link CacheBuilder} or {@link CacheBuilderSpec}, passed into this
 * CacheManager through {@link #setCacheBuilder}/{@link #setCacheBuilderSpec}.
 * A {@link CacheBuilderSpec}-compliant expression value can also be applied
 * via the {@link #setCacheSpecification "cacheSpecification"} bean property.
 *
 * <p>Requires Google Guava 12.0 or higher.
 *
 * @author Juergen Hoeller
 * @since 4.0
 * @see GuavaCache
 */
public class GuavaCacheManager implements CacheManager {

	private final ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<String, Cache>(16);

	private boolean dynamic = true;

	private CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder();

	private CacheLoader<Object, Object> cacheLoader;

	private boolean allowNullValues = true;


	/**
	 * Construct a dynamic GuavaCacheManager,
	 * lazily creating cache instances as they are being requested.
	 */
	public GuavaCacheManager() {
	}

	/**
	 * Construct a static GuavaCacheManager,
	 * managing caches for the specified cache names only.
	 */
	public GuavaCacheManager(String... cacheNames) {
		setCacheNames(Arrays.asList(cacheNames));
	}


	/**
	 * Specify the set of cache names for this CacheManager's 'static' mode.
	 * <p>The number of caches and their names will be fixed after a call to this method,
	 * with no creation of further cache regions at runtime.
	 */
	public void setCacheNames(Collection<String> cacheNames) {
		if (cacheNames != null) {
			for (String name : cacheNames) {
				this.cacheMap.put(name, createGuavaCache(name));
			}
			this.dynamic = false;
		}
	}

	/**
	 * Set the Guava CacheBuilder to use for building each individual
	 * {@link GuavaCache} instance.
	 * @see #createNativeGuavaCache
	 * @see com.google.common.cache.CacheBuilder#build()
	 */
	public void setCacheBuilder(CacheBuilder<Object, Object> cacheBuilder) {
		Assert.notNull(cacheBuilder, "CacheBuilder must not be null");
		this.cacheBuilder = cacheBuilder;
	}

	/**
	 * Set the Guava CacheBuilderSpec to use for building each individual
	 * {@link GuavaCache} instance.
	 * @see #createNativeGuavaCache
	 * @see com.google.common.cache.CacheBuilder#from(CacheBuilderSpec)
	 */
	public void setCacheBuilderSpec(CacheBuilderSpec cacheBuilderSpec) {
		this.cacheBuilder = CacheBuilder.from(cacheBuilderSpec);
	}

	/**
	 * Set the Guava cache specification String to use for building each
	 * individual {@link GuavaCache} instance. The given value needs to
	 * comply with Guava's {@link CacheBuilderSpec} (see its javadoc).
	 * @see #createNativeGuavaCache
	 * @see com.google.common.cache.CacheBuilder#from(String)
	 */
	public void setCacheSpecification(String cacheSpecification) {
		this.cacheBuilder = CacheBuilder.from(cacheSpecification);
	}

	/**
	 * Set the Guava CacheLoader to use for building each individual
	 * {@link GuavaCache} instance, turning it into a LoadingCache.
	 * @see #createNativeGuavaCache
	 * @see com.google.common.cache.CacheBuilder#build(CacheLoader)
	 * @see com.google.common.cache.LoadingCache
	 */
	public void setCacheLoader(CacheLoader<Object, Object> cacheLoader) {
		this.cacheLoader = cacheLoader;
	}

	/**
	 * Specify whether to accept and convert {@code null} values for all caches
	 * in this cache manager.
	 * <p>Default is "true", despite Guava itself not supporting {@code null} values.
	 * An internal holder object will be used to store user-level {@code null}s.
	 */
	public void setAllowNullValues(boolean allowNullValues) {
		this.allowNullValues = allowNullValues;
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
	public Cache getCache(String name) {
		Cache cache = this.cacheMap.get(name);
		if (cache == null && this.dynamic) {
			synchronized (this.cacheMap) {
				cache = this.cacheMap.get(name);
				if (cache == null) {
					cache = createGuavaCache(name);
					this.cacheMap.put(name, cache);
				}
			}
		}
		return cache;
	}

	/**
	 * Create a new GuavaCache instance for the specified cache name.
	 * @param name the name of the cache
	 * @return the Spring GuavaCache adapter (or a decorator thereof)
	 */
	protected Cache createGuavaCache(String name) {
		return new GuavaCache(name, createNativeGuavaCache(name), isAllowNullValues());
	}

	/**
	 * Create a native Guava Cache instance for the specified cache name.
	 * @param name the name of the cache
	 * @return the native Guava Cache instance
	 */
	protected com.google.common.cache.Cache<Object, Object> createNativeGuavaCache(String name) {
		if (this.cacheLoader != null) {
			return this.cacheBuilder.build(this.cacheLoader);
		}
		else {
			return this.cacheBuilder.build();
		}
	}

}
