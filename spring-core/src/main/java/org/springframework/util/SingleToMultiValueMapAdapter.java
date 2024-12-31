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

import org.jspecify.annotations.Nullable;

/**
 * Adapts a given {@link MultiValueMap} to the {@link Map} contract. The
 * difference with {@link MultiValueMapAdapter} is that this class delegates to
 * a {@code Map<K, V>}, whereas {@link MultiValueMapAdapter} needs a
 * {@code Map<K, List<V>>}. {@link MultiToSingleValueMapAdapter} adapts in the
 * opposite direction as this class.
 *
 * @author Arjen Poutsma
 * @since 6.2
 * @param <K> the key type
 * @param <V> the value element type
 */
@SuppressWarnings("serial")
final class SingleToMultiValueMapAdapter<K, V> implements MultiValueMap<K, V>, Serializable {

	private final Map<K, V> targetMap;

	private transient @Nullable Collection<List<V>> values;

	private transient @Nullable Set<Entry<K, List<V>>> entries;


	/**
	 * Wrap the given target {@link Map} as a {@link MultiValueMap} adapter.
	 * @param targetMap the plain target {@code Map}
	 */
	public SingleToMultiValueMapAdapter(Map<K, V> targetMap) {
		Assert.notNull(targetMap, "'targetMap' must not be null");
		this.targetMap = targetMap;
	}


	// MultiValueMap implementation

	@Override
	public @Nullable V getFirst(K key) {
		return this.targetMap.get(key);
	}

	@Override
	public void add(K key, @Nullable V value) {
		if (!this.targetMap.containsKey(key)) {
			this.targetMap.put(key, value);
		}
		else {
			throw new UnsupportedOperationException("Duplicate key: " + key);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void addAll(K key, List<? extends V> values) {
		if (!this.targetMap.containsKey(key)) {
			put(key, (List<V>) values);
		}
		else {
			throw new UnsupportedOperationException("Duplicate key: " + key);
		}
	}

	@Override
	public void addAll(MultiValueMap<K, V> values) {
		values.forEach(this::addAll);
	}

	@Override
	public void set(K key, @Nullable V value) {
		this.targetMap.put(key, value);
	}

	@Override
	public void setAll(Map<K, V> values) {
		this.targetMap.putAll(values);
	}

	@Override
	public Map<K, V> toSingleValueMap() {
		return Collections.unmodifiableMap(this.targetMap);
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
	public boolean containsValue(@Nullable Object value) {
		Iterator<Entry<K, List<V>>> i = entrySet().iterator();
		if (value == null) {
			while (i.hasNext()) {
				Entry<K, List<V>> e = i.next();
				if (e.getValue() == null || e.getValue().isEmpty()) {
					return true;
				}
			}
		}
		else {
			while (i.hasNext()) {
				Entry<K, List<V>> e = i.next();
				if (value.equals(e.getValue())) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public @Nullable List<V> get(Object key) {
		V value = this.targetMap.get(key);
		return (value != null) ? Collections.singletonList(value) : null;
	}

	@Override
	public @Nullable List<V> put(K key, List<V> values) {
		if (values.isEmpty()) {
			V result = this.targetMap.put(key, null);
			return (result != null) ? Collections.singletonList(result) : null;
		}
		else if (values.size() == 1) {
			V result = this.targetMap.put(key, values.get(0));
			return (result != null) ? Collections.singletonList(result) : null;
		}
		else {
			throw new UnsupportedOperationException("Duplicate key: " + key);
		}
	}

	@Override
	public @Nullable List<V> remove(Object key) {
		V result = this.targetMap.remove(key);
		return (result != null) ? Collections.singletonList(result) : null;
	}

	@Override
	public void putAll(Map<? extends K, ? extends List<V>> map) {
		for (Entry<? extends K, ? extends List<V>> entry : map.entrySet()) {
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
	public Collection<List<V>> values() {
		Collection<List<V>> values = this.values;
		if (values == null) {
			Collection<V> targetValues = this.targetMap.values();
			values = new AbstractCollection<>() {
				@Override
				public Iterator<List<V>> iterator() {
					Iterator<V> targetIterator = targetValues.iterator();
					return new Iterator<>() {
						@Override
						public boolean hasNext() {
							return targetIterator.hasNext();
						}

						@Override
						public List<V> next() {
							return Collections.singletonList(targetIterator.next());
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
	public Set<Entry<K, List<V>>> entrySet() {
		Set<Entry<K, List<V>>> entries = this.entries;
		if (entries == null) {
			Set<Entry<K, V>> targetEntries = this.targetMap.entrySet();
			entries = new AbstractSet<>() {
				@Override
				public Iterator<Entry<K, List<V>>> iterator() {
					Iterator<Entry<K, V>> targetIterator = targetEntries.iterator();
					return new Iterator<>() {
						@Override
						public boolean hasNext() {
							return targetIterator.hasNext();
						}

						@Override
						public Entry<K, List<V>> next() {
							Entry<K, V> entry = targetIterator.next();
							return new AbstractMap.SimpleImmutableEntry<>(entry.getKey(),
									Collections.singletonList(entry.getValue()));
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
	public void forEach(BiConsumer<? super K, ? super List<V>> action) {
		this.targetMap.forEach((k, v) -> action.accept(k, Collections.singletonList(v)));
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (other instanceof Map<?,?> otherMap && size() == otherMap.size()) {
			try {
				for (Entry<K, List<V>> e : entrySet()) {
					K key = e.getKey();
					List<V> values = e.getValue();
					if (values == null) {
						if (otherMap.get(key) != null || !otherMap.containsKey(key)) {
							return false;
						}
					}
					else {
						if (!values.equals(otherMap.get(key))) {
							return false;
						}
					}
				}
				return true;
			}
			catch (ClassCastException | NullPointerException ignored) {
				// fall through
			}
		}
		return false;
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
