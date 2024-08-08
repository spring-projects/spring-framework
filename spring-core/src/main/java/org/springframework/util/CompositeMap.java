/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.springframework.lang.Nullable;

/**
 * Composite map that combines two other maps. This type is created via
 * {@link CollectionUtils#compositeMap(Map, Map, BiFunction, Consumer)}.
 *
 * @author Arjen Poutsma
 * @since 6.2
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
final class CompositeMap<K, V> implements Map<K, V> {

	private final Map<K,V> first;

	private final Map<K,V> second;

	@Nullable
	private final BiFunction<K,V,V> putFunction;

	@Nullable
	private final Consumer<Map<K, V>> putAllFunction;


	CompositeMap(Map<K, V> first, Map<K, V> second) {
		this(first, second, null, null);
	}

	CompositeMap(Map<K, V> first, Map<K, V> second,
			@Nullable BiFunction<K, V, V> putFunction,
			@Nullable Consumer<Map<K,V>> putAllFunction) {

		Assert.notNull(first, "First must not be null");
		Assert.notNull(second, "Second must not be null");
		this.first = first;
		this.second = new FilteredMap<>(second, key -> !this.first.containsKey(key));
		this.putFunction = putFunction;
		this.putAllFunction = putAllFunction;
	}


	@Override
	public int size() {
		return this.first.size() + this.second.size();
	}

	@Override
	public boolean isEmpty() {
		return this.first.isEmpty() && this.second.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		if (this.first.containsKey(key)) {
			return true;
		}
		else {
			return this.second.containsKey(key);
		}
	}

	@Override
	public boolean containsValue(Object value) {
		if (this.first.containsValue(value)) {
			return true;
		}
		else {
			return this.second.containsValue(value);
		}
	}

	@Override
	@Nullable
	public V get(Object key) {
		V firstResult = this.first.get(key);
		if (firstResult != null) {
			return firstResult;
		}
		else {
			return this.second.get(key);
		}
	}

	@Override
	@Nullable
	public V put(K key, V value) {
		if (this.putFunction == null) {
			throw new UnsupportedOperationException();
		}
		else {
			return this.putFunction.apply(key, value);
		}
	}

	@Override
	@Nullable
	public V remove(Object key) {
		V firstResult = this.first.remove(key);
		V secondResult = this.second.remove(key);
		if (firstResult != null) {
			return firstResult;
		}
		else {
			return secondResult;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void putAll(Map<? extends K, ? extends V> m) {
		if (this.putAllFunction != null) {
			this.putAllFunction.accept((Map<K, V>) m);
		}
		else {
			for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
				put(e.getKey(), e.getValue());
			}
		}
	}

	@Override
	public void clear() {
		this.first.clear();
		this.second.clear();
	}

	@Override
	public Set<K> keySet() {
		return new CompositeSet<>(this.first.keySet(), this.second.keySet());
	}

	@Override
	public Collection<V> values() {
		return new CompositeCollection<>(this.first.values(), this.second.values());
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return new CompositeSet<>(this.first.entrySet(), this.second.entrySet());
	}

	@Override
	public String toString() {
		Iterator<Entry<K, V>> i = entrySet().iterator();
		if (!i.hasNext()) {
			return "{}";
		}

		StringBuilder sb = new StringBuilder();
		sb.append('{');
		while (true) {
			Entry<K, V> e = i.next();
			K key = e.getKey();
			V value = e.getValue();
			sb.append(key == this ? "(this Map)" : key);
			sb.append('=');
			sb.append(value == this ? "(this Map)" : value);
			if (!i.hasNext()) {
				return sb.append('}').toString();
			}
			sb.append(',').append(' ');
		}
	}
}
