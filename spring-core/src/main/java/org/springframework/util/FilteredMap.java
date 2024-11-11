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

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.springframework.lang.Nullable;

/**
 * Map that filters out values that do not match a predicate.
 * This type is used by {@link CompositeMap}.
 * @author Arjen Poutsma
 * @since 6.2
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
final class FilteredMap<K, V> extends AbstractMap<K, V> {

	private final Map<K, V> delegate;

	private final Predicate<K> filter;


	public FilteredMap(Map<K, V> delegate, Predicate<K> filter) {
		Assert.notNull(delegate, "Delegate must not be null");
		Assert.notNull(filter, "Filter must not be null");

		this.delegate = delegate;
		this.filter = filter;
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return new FilteredSet<>(this.delegate.entrySet(), entry -> this.filter.test(entry.getKey()));
	}

	@Override
	public int size() {
		int size = 0;
		for (K k : keySet()) {
			if (this.filter.test(k)) {
				size++;
			}
		}
		return size;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean containsKey(Object key) {
		if (this.delegate.containsKey(key)) {
			return this.filter.test((K) key);
		}
		else {
			return false;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	@Nullable
	public V get(Object key) {
		V value = this.delegate.get(key);
		if (value != null && this.filter.test((K) key)) {
			return value;
		}
		else {
			return null;
		}
	}

	@Override
	@Nullable
	public V put(K key, V value) {
		V oldValue = this.delegate.put(key, value);
		if (oldValue != null && this.filter.test(key)) {
			return oldValue;
		}
		else {
			return null;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	@Nullable
	public V remove(Object key) {
		V oldValue = this.delegate.remove(key);
		if (oldValue != null && this.filter.test((K) key)) {
			return oldValue;
		}
		else {
			return null;
		}
	}

	@Override
	public void clear() {
		this.delegate.clear();
	}

	@Override
	public Set<K> keySet() {
		return new FilteredSet<>(this.delegate.keySet(), this.filter);
	}

}
