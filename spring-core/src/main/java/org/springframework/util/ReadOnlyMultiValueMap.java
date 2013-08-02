/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.util;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Wraps a {@link MultiValueMap} to make it immutable.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class ReadOnlyMultiValueMap<K, V> implements MultiValueMap<K, V> {

	private final MultiValueMap<K, V> targetMap;


	/**
	 * Create a new ReadOnlyMultiValueMap that wraps the given target map.
	 */
	public ReadOnlyMultiValueMap(MultiValueMap<K, V> targetMap) {
		this.targetMap = targetMap;
	}

	// MultiValueMap implementation

	@Override
	public void add(K key, V value) {
		throw new UnsupportedOperationException("This map is immutable.");
	}

	@Override
	public V getFirst(K key) {
		return this.targetMap.getFirst(key);
	}

	@Override
	public void set(K key, V value) {
		throw new UnsupportedOperationException("This map is immutable.");
	}

	@Override
	public void setAll(Map<K, V> values) {
		throw new UnsupportedOperationException("This map is immutable.");
	}

	@Override
	public Map<K, V> toSingleValueMap() {
		return Collections.unmodifiableMap(this.targetMap.toSingleValueMap());
	}


	// Map implementation

	@Override
	public int size() {
		return this.targetMap.size();
	}

	@Override
	public boolean isEmpty() {
		return this.targetMap.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return this.targetMap.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return this.targetMap.containsValue(value);
	}

	@Override
	public List<V> get(Object key) {
		return this.targetMap.get(key);
	}

	@Override
	public List<V> put(K key, List<V> value) {
		throw new UnsupportedOperationException("This map is immutable.");
	}

	@Override
	public List<V> remove(Object key) {
		throw new UnsupportedOperationException("This map is immutable.");
	}

	@Override
	public void putAll(Map<? extends K, ? extends List<V>> m) {
		this.targetMap.putAll(m);
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException("This map is immutable.");
	}

	@Override
	public Set<K> keySet() {
		return Collections.unmodifiableSet(this.targetMap.keySet());
	}

	@Override
	public Collection<List<V>> values() {
		return Collections.unmodifiableCollection(this.targetMap.values());
	}

	@Override
	public Set<Entry<K, List<V>>> entrySet() {
		return Collections.unmodifiableSet(this.targetMap.entrySet());
	}


	@Override
	public boolean equals(Object obj) {
		return this.targetMap.equals(obj);
	}

	@Override
	public int hashCode() {
		return this.targetMap.hashCode();
	}

	@Override
	public String toString() {
		return this.targetMap.toString();
	}

}