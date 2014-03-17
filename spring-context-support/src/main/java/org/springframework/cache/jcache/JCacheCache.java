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

package org.springframework.cache.jcache;

import java.io.Serializable;

import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.cache.Cache} implementation on top of a
 * {@link javax.cache.Cache} instance.
 *
 * <p>Note: This class has been updated for JCache 1.0, as of Spring 4.0.
 *
 * @author Juergen Hoeller
 * @since 3.2
 */
public class JCacheCache implements Cache {

	private static final Object NULL_HOLDER = new NullHolder();

	private final javax.cache.Cache<Object, Object> cache;

	private final boolean allowNullValues;


	/**
	 * Create an {@link org.springframework.cache.jcache.JCacheCache} instance.
	 * @param jcache backing JCache Cache instance
	 */
	public JCacheCache(javax.cache.Cache<Object, Object> jcache) {
		this(jcache, true);
	}

	/**
	 * Create an {@link org.springframework.cache.jcache.JCacheCache} instance.
	 * @param jcache backing JCache Cache instance
	 * @param allowNullValues whether to accept and convert null values for this cache
	 */
	public JCacheCache(javax.cache.Cache<Object, Object> jcache, boolean allowNullValues) {
		Assert.notNull(jcache, "Cache must not be null");
		this.cache = jcache;
		this.allowNullValues = allowNullValues;
	}


	@Override
	public final String getName() {
		return this.cache.getName();
	}

	@Override
	public final javax.cache.Cache<Object, Object> getNativeCache() {
		return this.cache;
	}

	public final boolean isAllowNullValues() {
		return this.allowNullValues;
	}

	@Override
	public ValueWrapper get(Object key) {
		Object value = this.cache.get(key);
		return (value != null ? new SimpleValueWrapper(fromStoreValue(value)) : null);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T get(Object key, Class<T> type) {
		Object value = fromStoreValue(this.cache.get(key));
		if (value != null && type != null && !type.isInstance(value)) {
			throw new IllegalStateException("Cached value is not of required type [" + type.getName() + "]: " + value);
		}
		return (T) value;
	}

	@Override
	public void put(Object key, Object value) {
		this.cache.put(key, toStoreValue(value));
	}

	@Override
	public void evict(Object key) {
		this.cache.remove(key);
	}

	@Override
	public void clear() {
		this.cache.removeAll();
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
	private static class NullHolder implements Serializable {
	}

}
