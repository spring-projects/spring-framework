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

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import org.springframework.lang.Nullable;

/**
 * Adapts a given {@link MultiValueMap} to the {@link Map} contract. The
 * difference with {@link SingleToMultiValueMapAdapter} and
 * {@link MultiValueMapAdapter} is that this class adapts in the opposite
 * direction.
 *
 * @author Arjen Poutsma
 * @since 6.2
 * @param <K> the key type
 * @param <V> the value element type
 */
@SuppressWarnings("serial")
final class MultiToSingleValueMapAdapter<K, V> implements Map<K, V>, Serializable {

	private final MultiValueMap<K, V> targetMap;

	@Nullable
	private transient Collection<V> values;

	@Nullable
	private transient Set<Entry<K, V>> entries;


	/**
	 * Wrap the given target {@link MultiValueMap} as a {@link Map} adapter.
	 * @param targetMap the target {@code MultiValue}
	 */
	public MultiToSingleValueMapAdapter(MultiValueMap<K, V> targetMap) {
		Assert.notNull(targetMap, "'targetMap' must not be null");
		this.targetMap = targetMap;
	}

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
	public boolean containsValue(@Nullable Object value) {
		Iterator<Entry<K, V>> i = entrySet().iterator();
		if (value == null) {
			while (i.hasNext()) {
				Entry<K, V> e = i.next();
				if (e.getValue() == null) {
					return true;
				}
			}
		}
		else {
			while (i.hasNext()) {
				Entry<K, V> e = i.next();
				if (value.equals(e.getValue())) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	@Nullable
	public V get(Object key) {
		return adaptValue(this.targetMap.get(key));
	}

	@Nullable
	@Override
	public V put(K key, @Nullable V value) {
		return adaptValue(this.targetMap.put(key, adaptValue(value)));
	}

	@Override
	@Nullable
	public V remove(Object key) {
		return adaptValue(this.targetMap.remove(key));
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> map) {
		for (Entry<? extends K, ? extends V> entry : map.entrySet()) {
			put(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public void clear() {
		this.targetMap.clear();
	}

	@Override
	public Set<K> keySet() {
		return this.targetMap.keySet();
	}

	@Override
	public Collection<V> values() {
		Collection<V> values = this.values;
		if (values == null) {
			Collection<List<V>> targetValues = this.targetMap.values();
			values = new AbstractCollection<V>() {
				@Override
				public Iterator<V> iterator() {
					Iterator<List<V>> targetIterator = targetValues.iterator();
					return new Iterator<V>() {
						@Override
						public boolean hasNext() {
							return targetIterator.hasNext();
						}

						@Override
						public V next() {
							return targetIterator.next().get(0);
						}
					};
				}

				@Override
				public int size() {
					return targetValues.size();
				}
			};
			this.values = values;
		}
		return values;
	}



	@Override
	public Set<Entry<K, V>> entrySet() {
		Set<Entry<K, V>> entries = this.entries;
		if (entries == null) {
			Set<Entry<K, List<V>>> targetEntries = this.targetMap.entrySet();
			entries = new AbstractSet<>() {
				@Override
				public Iterator<Entry<K, V>> iterator() {
					Iterator<Entry<K, List<V>>> targetIterator = targetEntries.iterator();
					return new Iterator<>() {
						@Override
						public boolean hasNext() {
							return targetIterator.hasNext();
						}

						@Override
						public Entry<K, V> next() {
							Entry<K, List<V>> entry = targetIterator.next();
							return new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), entry.getValue().get(0));
						}
					};
				}

				@Override
				public int size() {
					return targetEntries.size();
				}
			};
			this.entries = entries;
		}
		return entries;
	}

	@Override
	public void forEach(BiConsumer<? super K, ? super V> action) {
		this.targetMap.forEach((k, vs) -> action.accept(k, vs.get(0)));
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (o == this) {
			return true;
		}
		else if (o instanceof Map<?,?> other) {
			if (this.size() != other.size()) {
				return false;
			}
			try {
				for (Entry<K, V> e : entrySet()) {
					K key = e.getKey();
					V value = e.getValue();
					if (value == null) {
						if (other.get(key) != null || !other.containsKey(key)) {
							return false;
						}
					}
					else {
						if (!value.equals(other.get(key))) {
							return false;
						}
					}
				}
			}
			catch (ClassCastException | NullPointerException ignore) {
				return false;
			}
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return this.targetMap.hashCode();
	}

	@Override
	public String toString() {
		return this.targetMap.toString();
	}

	@Nullable
	private V adaptValue(@Nullable List<V> values) {
		if (!CollectionUtils.isEmpty(values)) {
			return values.get(0);
		}
		else {
			return null;
		}
	}

	@Nullable
	private List<V> adaptValue(@Nullable V value) {
		if (value != null) {
			return Collections.singletonList(value);
		}
		else {
			return null;
		}
	}

}
