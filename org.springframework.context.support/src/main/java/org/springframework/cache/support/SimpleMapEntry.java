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

import java.util.Map;
import java.util.Map.Entry;

/**
 * Basic {@link Entry} implementation. 
 * 
 * @author Costin Leau
 */
public class SimpleMapEntry<K, V> implements Entry<K, V> {
	private K key;
	private V value;

	public SimpleMapEntry(K key, V value) {
		this.key = key;
		this.value = value;
	}

	public SimpleMapEntry(Map.Entry<K, V> e) {
		this.key = e.getKey();
		this.value = e.getValue();
	}

	public K getKey() {
		return key;
	}

	public V getValue() {
		return value;
	}

	public V setValue(V value) {
		V oldValue = this.value;
		this.value = value;
		return oldValue;
	}

	@SuppressWarnings("unchecked")
	public boolean equals(Object o) {
		if (!(o instanceof Map.Entry))
			return false;
		Map.Entry e = (Map.Entry) o;
		return eq(key, e.getKey()) && eq(value, e.getValue());
	}

	public int hashCode() {
		return ((key == null) ? 0 : key.hashCode()) ^ ((value == null) ? 0 : value.hashCode());
	}

	public String toString() {
		return key + "=" + value;
	}

	static boolean eq(Object o1, Object o2) {
		return (o1 == null ? o2 == null : o1.equals(o2));
	}
}