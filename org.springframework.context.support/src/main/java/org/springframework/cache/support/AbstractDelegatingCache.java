/*
 * Copyright 2010 the original author or authors.
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

import java.io.Serializable;
import java.util.Map;

import org.springframework.cache.Cache;
import org.springframework.util.Assert;

/**
 * Abstract base class delegating most of the {@link Map}-like methods
 * to the underlying cache.
 * 
 * <b>Note:</b>Allows null values to be stored, even if the underlying map
 * does not support them.
 * 
 * @author Costin Leau
 */
public abstract class AbstractDelegatingCache<K, V> implements Cache<K, V> {

	private static class NullHolder implements Serializable {
		private static final long serialVersionUID = 1L;
	}

	public static final Object NULL_HOLDER = new NullHolder();

	private final Map<K, V> delegate;
	private final boolean allowNullValues;

	/**
	 * Creates a new instance using the given delegate.
	 * 
	 * @param <D> map type
	 * @param delegate map delegate
	 */
	public <D extends Map<K, V>> AbstractDelegatingCache(D delegate) {
		this(delegate, false);
	}

	/**
	 * Creates a new instance using the given delegate.
	 * 
	 * @param <D> map type
	 * @param delegate map delegate
	 * @param allowNullValues flag indicating whether null values are allowed or not
	 */
	public <D extends Map<K, V>> AbstractDelegatingCache(D delegate, boolean allowNullValues) {
		Assert.notNull(delegate);
		this.delegate = delegate;
		this.allowNullValues = allowNullValues;
	}

	public boolean getAllowNullValues() {
		return allowNullValues;
	}

	public void clear() {
		delegate.clear();
	}

	public boolean containsKey(Object key) {
		return delegate.containsKey(key);
	}

	public V get(Object key) {
		return filterNull(delegate.get(key));
	}

	@SuppressWarnings("unchecked")
	public V put(K key, V value) {
		if (allowNullValues && value == null) {
			Map map = delegate;
			Object val = map.put(key, NULL_HOLDER);
			if (val == NULL_HOLDER) {
				return null;
			}
			return (V) val;
		}

		return filterNull(delegate.put(key, value));
	}

	public V remove(Object key) {
		return filterNull(delegate.remove(key));
	}

	protected V filterNull(V val) {
		if (allowNullValues && val == NULL_HOLDER) {
			return null;
		}
		return val;
	}
}