/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.util;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Default implementation of {@link MultiValueMap} that wraps a plain {@code Map}.
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
public class DefaultMultiValueMap<K, V> implements MultiValueMap<K, V> {

	private final Map<K, List<V>> wrappee;

	/**
	 * Constructs a new intance of the {@code DefaultMultiValueMap} wrapping a plain {@link LinkedHashMap}.
	 */
	public DefaultMultiValueMap() {
		this(new LinkedHashMap<K, List<V>>());
	}

	/**
	 * Constructs a new intance of the {@code DefaultMultiValueMap} wrapping the given map.
	 *
	 * @param wrappee the map to be wrapped
	 */
	public DefaultMultiValueMap(Map<K, List<V>> wrappee) {
		Assert.notNull(wrappee, "'wrappee' must not be null");
		this.wrappee = wrappee;
	}

	/*
 	 * MultiValueMap implementation
	 */

	public void add(K key, V value) {
		List<V> values = wrappee.get(key);
		if (values == null) {
			values = new LinkedList<V>();
			wrappee.put(key, values);
		}
		values.add(value);
	}

	public V getFirst(K key) {
		List<V> values = wrappee.get(key);
		return values != null ? values.get(0) : null;
	}

	public void set(K key, V value) {
		List<V> values = new LinkedList<V>();
		values.add(value);
		wrappee.put(key, values);
	}

	/*
	 * Map implementation
	 */

	public int size() {
		return wrappee.size();
	}

	public boolean isEmpty() {
		return wrappee.isEmpty();
	}

	public boolean containsKey(Object key) {
		return wrappee.containsKey(key);
	}

	public boolean containsValue(Object value) {
		return wrappee.containsValue(value);
	}

	public List<V> get(Object key) {
		return wrappee.get(key);
	}

	public List<V> put(K key, List<V> value) {
		return wrappee.put(key, value);
	}

	public List<V> remove(Object key) {
		return wrappee.remove(key);
	}

	public void putAll(Map<? extends K, ? extends List<V>> m) {
		wrappee.putAll(m);
	}

	public void clear() {
		wrappee.clear();
	}

	public Set<K> keySet() {
		return wrappee.keySet();
	}

	public Collection<List<V>> values() {
		return wrappee.values();
	}

	public Set<Entry<K, List<V>>> entrySet() {
		return wrappee.entrySet();
	}

	@Override
	public int hashCode() {
		return wrappee.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return this.wrappee.equals(obj);
	}

	@Override
	public String toString() {
		return wrappee.toString();
	}

}
