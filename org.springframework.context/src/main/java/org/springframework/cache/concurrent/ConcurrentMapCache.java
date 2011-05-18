/*
 * Copyright 2010-2011 the original author or authors.
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

package org.springframework.cache.concurrent;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.DefaultValueWrapper;

/**
 * Simple {@link Cache} implementation based on the JDK 1.5+
 * java.util.concurrent package. Useful for testing or simple caching scenarios.
 *
 * <b>Note:</b>As {@link ConcurrentHashMap} (the default implementation used) does not allow null values to be stored 
 * this class will replace them with a predefined, internal object. This behaviour can be changed through the {@link #ConcurrentMapCache(ConcurrentMap, String, boolean)}
 * constructor.
 * 
 * @author Costin Leau
 */
public class ConcurrentMapCache<K, V> implements Cache<K, V> {

	private static class NullHolder implements Serializable {
		private static final long serialVersionUID = 1L;
	}

	private static final Object NULL_HOLDER = new NullHolder();
	private final ConcurrentMap<K, V> store;
	private final String name;
	private final boolean allowNullValues;

	public ConcurrentMapCache() {
		this("");
	}

	public ConcurrentMapCache(String name) {
		this(new ConcurrentHashMap<K, V>(), name, true);
	}

	public ConcurrentMapCache(ConcurrentMap<K, V> delegate, String name, boolean allowNullValues) {
		this.store = delegate;
		this.name = name;
		this.allowNullValues = allowNullValues;
	}

	public String getName() {
		return name;
	}

	public boolean getAllowNullValues() {
		return allowNullValues;
	}

	public ConcurrentMap<K, V> getNativeCache() {
		return store;
	}

	public void clear() {
		store.clear();
	}

	public ValueWrapper<V> get(Object key) {
		V v = store.get(key);
		return (v != null ? new DefaultValueWrapper<V>(filterNull(v)) : null);
	}

	@SuppressWarnings("unchecked")
	public void put(K key, V value) {
		if (allowNullValues && value == null) {
			Map map = store;
			map.put(key, NULL_HOLDER);
		} else {
			store.put(key, value);
		}
	}

	public void evict(Object key) {
		store.remove(key);
	}

	protected V filterNull(V val) {
		if (allowNullValues && val == NULL_HOLDER) {
			return null;
		}
		return val;
	}
}