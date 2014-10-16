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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheBuilderSpec;
import com.google.common.cache.CacheLoader;
import org.springframework.beans.factory.FactoryBean;

/**
 * {@link FactoryBean} for creating a GuavaCache instance that can further
 * be customized with a Guava {@link CacheBuilder} or {@link CacheBuilderSpec}
 *
 * @author Biju Kunjummen
 * @since 4.1
 * @see GuavaCache
 */
public class GuavaCacheFactoryBean implements FactoryBean<GuavaCache> {
	private final String name;
	private CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder();
	private CacheLoader<Object, Object> cacheLoader;
	private final boolean allowNullValues;

	/**
	 * Create a {@link GuavaCacheFactoryBean} instance with the specified name
	 * for the created underlying {@link GuavaCache}.
	 * @param name the name of the cache
	 */
	public GuavaCacheFactoryBean(String name) {
		this.name = name;
		this.allowNullValues = true;
	}

	/**
	 * Create a {@link GuavaCacheFactoryBean} instance with the specified name
	 * for the created underlying {@link GuavaCache} and whether the cache accepts
	 * null values.
	 * @param name the name of the cache
	 * @param allowNullValues whether to accept and convert {@code null}
	 * values for this cache
	 */

	public GuavaCacheFactoryBean(String name, boolean allowNullValues) {
		this.name = name;
		this.allowNullValues = allowNullValues;
	}

	/**
	 * Specify the {@link com.google.common.cache.CacheBuilder} to use to
	 * build the underlying native {@link com.google.common.cache.Cache}
	 */
	public void setCacheBuilder(CacheBuilder<Object, Object> cacheBuilder) {
		this.cacheBuilder = cacheBuilder;
	}

	/**
	 * Specify the {@link com.google.common.cache.CacheBuilderSpec} to use to
	 * build the underlying native {@link com.google.common.cache.Cache}
	 */
	public void setCacheBuilderSpec(CacheBuilderSpec cacheBuilderSpec) {
	   this.cacheBuilder = CacheBuilder.from(cacheBuilderSpec);
	}

	/**
	 * Specify the {@link com.google.common.cache.CacheBuilderSpec} as a String
	 * to build the underlying native {@link com.google.common.cache.Cache}
	 */
	public void setCacheBuilderSpecString(String cacheBuilderSpec) {
		this.cacheBuilder = CacheBuilder.from(cacheBuilderSpec);
	}

	/**
	 * Specify the {@link com.google.common.cache.CacheLoader}
	 */
	public void setCacheLoader(CacheLoader<Object, Object> cacheLoader) {
		this.cacheLoader = cacheLoader;
	}

	public String getName() {
		return name;
	}



	@Override
	public GuavaCache getObject() {
		if (this.cacheLoader != null) {
			return new GuavaCache(this.name, this.cacheBuilder.build(this.cacheLoader), this.allowNullValues);
		}
		return new GuavaCache(this.name, this.cacheBuilder.build(), this.allowNullValues);
	}

	@Override
	public Class<?> getObjectType() {
		return GuavaCache.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}
