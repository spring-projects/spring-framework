/*
 * Copyright 2004-2009 the original author or authors.
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
package org.springframework.core.collection;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.springframework.core.style.ToStringCreator;

/**
 * A map decorator that implements <code>SharedMap</code>. By default, simply returns the map itself as the mutex.
 * Subclasses may override to return a different mutex object.
 * 
 * @author Keith Donald
 */
@SuppressWarnings("serial")
public class SharedMapDecorator<K, V> implements SharedMap<K, V>, Serializable {

	/**
	 * The wrapped, target map.
	 */
	private Map<K, V> map;

	/**
	 * Creates a new shared map decorator.
	 * @param map the map that is shared by multiple threads, to be synced
	 */
	public SharedMapDecorator(Map<K, V> map) {
		this.map = map;
	}

	// implementing Map

	public void clear() {
		map.clear();
	}

	public boolean containsKey(Object key) {
		return map.containsKey(key);
	}

	public boolean containsValue(Object value) {
		return map.containsValue(value);
	}

	public Set<Map.Entry<K, V>> entrySet() {
		return map.entrySet();
	}

	public V get(Object key) {
		return map.get(key);
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	public Set<K> keySet() {
		return map.keySet();
	}

	public V put(K key, V value) {
		return map.put(key, value);
	}

	public void putAll(Map<? extends K, ? extends V> map) {
		this.map.putAll(map);
	}

	public V remove(Object key) {
		return map.remove(key);
	}

	public int size() {
		return map.size();
	}

	public Collection<V> values() {
		return map.values();
	}

	// implementing SharedMap

	public Object getMutex() {
		return map;
	}

	public String toString() {
		return new ToStringCreator(this).append("map", map).append("mutex", getMutex()).toString();
	}
}