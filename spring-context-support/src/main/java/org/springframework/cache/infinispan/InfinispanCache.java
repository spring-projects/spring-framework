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

package org.springframework.cache.infinispan;

import org.infinispan.lifecycle.ComponentStatus;
import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.cache.Cache} implementation on top of an
 * {@link org.infinispan.Cache} instance.
 *
 * @author Philippe Marschall
 * @since 4.0
 */
public class InfinispanCache implements Cache {

	private static final Object NULL_HOLDER = NullHolder.INSTANE;

	@SuppressWarnings("rawtypes")
	private final org.infinispan.Cache cache;

	private final boolean allowNullValues;


	/**
	 * Create an {@link org.springframework.cache.infinispan.InfinispanCache} instance.
	 * @param infinispanCache backing Infinispan Cache instance
	 */
	public InfinispanCache(org.infinispan.Cache infinispanCache) {
		this(infinispanCache, true);
	}

	/**
	 * Create an {@link org.springframework.cache.infinispan.InfinispanCache} instance.
	 * @param infinispanCache backing Infinispan Cache instance
	 * @param allowNullValues whether to accept and convert null values for this cache
	 */
	public InfinispanCache(org.infinispan.Cache infinispanCache, boolean allowNullValues) {
		Assert.notNull(infinispanCache, "Cache must not be null");
		ComponentStatus status = infinispanCache.getStatus();
		Assert.isTrue(status == ComponentStatus.RUNNING,
				"A 'running' cache is required - current cache is " + status);
		this.cache = infinispanCache;
		this.allowNullValues = allowNullValues;
	}

	@Override
	public String getName() {
		return this.cache.getName();
	}

	@Override
	public org.infinispan.Cache getNativeCache() {
		return this.cache;
	}

	@Override
	public ValueWrapper get(Object key) {
		Object value = this.cache.get(key);
		return (value != null ? new SimpleValueWrapper(fromStoreValue(value)) : null);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void put(Object key, Object value) {
		this.cache.put(key, toStoreValue(value));
	}

	@Override
	public void evict(Object key) {
		this.cache.remove(key);
	}

	@Override
	public void clear() {
		this.cache.clear();
	}



	/**
	 * Convert the given value from the internal store to a user value
	 * returned from the get method (adapting {@code null}).
	 * @param storeValue the store value
	 * @return the value to return to the user
	 */
	protected Object fromStoreValue(Object storeValue) {
		if (this.allowNullValues && storeValue == NULL_HOLDER) {
			return null;
		}
		return storeValue;
	}

	/**
	 * Convert the given user value, as passed into the put method,
	 * to a value in the internal store (adapting {@code null}).
	 * @param userValue the given user value
	 * @return the value to store
	 */
	protected Object toStoreValue(Object userValue) {
		if (this.allowNullValues && userValue == null) {
			return NULL_HOLDER;
		}
		return userValue;
	}


	@SuppressWarnings("serial")
	static enum NullHolder {
		INSTANE;
	}

}
