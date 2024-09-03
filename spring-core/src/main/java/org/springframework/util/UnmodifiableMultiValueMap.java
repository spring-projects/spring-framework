/*
 * Copyright 2002-2023 the original author or authors.
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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.lang.Nullable;

/**
 * Unmodifiable wrapper for {@link MultiValueMap}.
 *
 * @author Arjen Poutsma
 * @since 6.0
 * @param <K> the key type
 * @param <V> the value element type
 */
final class UnmodifiableMultiValueMap<K,V> implements MultiValueMap<K,V>, Serializable {

	private static final long serialVersionUID = -8697084563854098920L;

	@SuppressWarnings("serial")
	private final MultiValueMap<K, V> delegate;

	@Nullable
	private transient Set<K> keySet;

	@Nullable
	private transient Set<Entry<K, List<V>>> entrySet;

	@Nullable
	private transient Collection<List<V>> values;


	@SuppressWarnings("unchecked")
	public UnmodifiableMultiValueMap(MultiValueMap<? extends K, ? extends V> delegate) {
		Assert.notNull(delegate, "Delegate must not be null");
		this.delegate = (MultiValueMap<K, V>) delegate;
	}


	// delegation

	@Override
	public int size() {
		return this.delegate.size();
	}

	@Override
	public boolean isEmpty() {
		return this.delegate.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return this.delegate.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return this.delegate.containsValue(value);
	}

	@Override
	@Nullable
	public List<V> get(Object key) {
		List<V> result = this.delegate.get(key);
		return (result != null ? Collections.unmodifiableList(result) : null);
	}

	@Override
	public V getFirst(K key) {
		return this.delegate.getFirst(key);
	}

	@Override
	public List<V> getOrDefault(Object key, List<V> defaultValue) {
		List<V> result = this.delegate.getOrDefault(key, defaultValue);
		if (result != defaultValue) {
			result = Collections.unmodifiableList(result);
		}
		return result;
	}

	@Override
	public void forEach(BiConsumer<? super K, ? super List<V>> action) {
		this.delegate.forEach((k, vs) -> action.accept(k, Collections.unmodifiableList(vs)));
	}

	@Override
	public Map<K, V> toSingleValueMap() {
		return this.delegate.toSingleValueMap();
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || this.delegate.equals(other));
	}

	@Override
	public int hashCode() {
		return this.delegate.hashCode();
	}

	@Override
	public String toString() {
		return this.delegate.toString();
	}


	// lazy init

	@Override
	public Set<K> keySet() {
		if (this.keySet == null) {
			this.keySet = Collections.unmodifiableSet(this.delegate.keySet());
		}
		return this.keySet;
	}

	@Override
	public Set<Entry<K, List<V>>> entrySet() {
		if (this.entrySet == null) {
			this.entrySet = new UnmodifiableEntrySet<>(this.delegate.entrySet());
		}
		return this.entrySet;
	}

	@Override
	public Collection<List<V>> values() {
		if (this.values == null) {
			this.values = new UnmodifiableValueCollection<>(this.delegate.values());
		}
		return this.values;
	}

	// unsupported

	@Nullable
	@Override
	public List<V> put(K key, List<V> value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<V> putIfAbsent(K key, List<V> value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putAll(Map<? extends K, ? extends List<V>> m) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<V> remove(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void add(K key, @Nullable V value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addAll(K key, List<? extends V> values) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addAll(MultiValueMap<K, V> values) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addIfAbsent(K key, @Nullable V value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void set(K key, @Nullable V value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setAll(Map<K, V> values) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void replaceAll(BiFunction<? super K, ? super List<V>, ? extends List<V>> function) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object key, Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean replace(K key, List<V> oldValue, List<V> newValue) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<V> replace(K key, List<V> value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<V> computeIfAbsent(K key, Function<? super K, ? extends List<V>> mappingFunction) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<V> computeIfPresent(K key, BiFunction<? super K, ? super List<V>, ? extends List<V>> remappingFunction) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<V> compute(K key, BiFunction<? super K, ? super List<V>, ? extends List<V>> remappingFunction) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<V> merge(K key, List<V> value, BiFunction<? super List<V>, ? super List<V>, ? extends List<V>> remappingFunction) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}


	private static class UnmodifiableEntrySet<K,V> implements Set<Map.Entry<K, List<V>>>, Serializable {

		private static final long serialVersionUID = 2407578793783925203L;

		@SuppressWarnings("serial")
		private final Set<Entry<K, List<V>>> delegate;

		@SuppressWarnings("unchecked")
		public UnmodifiableEntrySet(Set<? extends Entry<? extends K, ? extends List<? extends V>>> delegate) {
			this.delegate = (Set<Entry<K, List<V>>>) delegate;
		}

		// delegation

		@Override
		public int size() {
			return this.delegate.size();
		}

		@Override
		public boolean isEmpty() {
			return this.delegate.isEmpty();
		}

		@Override
		public boolean contains(Object o) {
			return this.delegate.contains(o);
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			return this.delegate.containsAll(c);
		}

		@Override
		public Iterator<Entry<K, List<V>>> iterator() {
			Iterator<? extends Entry<? extends K, ? extends List<? extends V>>> iterator = this.delegate.iterator();
			return new Iterator<>() {
				@Override
				public boolean hasNext() {
					return iterator.hasNext();
				}

				@Override
				public Entry<K, List<V>> next() {
					return new UnmodifiableEntry<>(iterator.next());
				}
			};
		}

		@Override
		public Object[] toArray() {
			Object[] result = this.delegate.toArray();
			filterArray(result);
			return result;
		}

		@Override
		public <T> T[] toArray(T[] a) {
			T[] result = this.delegate.toArray(a);
			filterArray(result);
			return result;
		}

		@SuppressWarnings("unchecked")
		private void filterArray(Object[] result) {
			for (int i = 0; i < result.length; i++) {
				if (result[i] instanceof Map.Entry<?,?> entry) {
					result[i] = new UnmodifiableEntry<>((Entry<K, List<V>>) entry);
				}
			}
		}

		@Override
		public void forEach(Consumer<? super Entry<K, List<V>>> action) {
			this.delegate.forEach(e -> action.accept(new UnmodifiableEntry<>(e)));
		}

		@Override
		public Stream<Entry<K, List<V>>> stream() {
			return StreamSupport.stream(spliterator(), false);
		}

		@Override
		public Stream<Entry<K, List<V>>> parallelStream() {
			return StreamSupport.stream(spliterator(), true);
		}

		@Override
		public Spliterator<Entry<K, List<V>>> spliterator() {
			return new UnmodifiableEntrySpliterator<>(this.delegate.spliterator());
		}

		@Override
		public boolean equals(@Nullable Object other) {
			return (this == other || other instanceof Set<?> that && size() == that.size() && containsAll(that));
		}

		@Override
		public int hashCode() {
			return this.delegate.hashCode();
		}

		@Override
		public String toString() {
			return this.delegate.toString();
		}

		// unsupported

		@Override
		public boolean add(Entry<K, List<V>> kListEntry) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean remove(Object o) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean removeIf(Predicate<? super Entry<K, List<V>>> filter) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean addAll(Collection<? extends Entry<K, List<V>>> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		}


		private static class UnmodifiableEntrySpliterator<K,V> implements Spliterator<Entry<K,List<V>>> {

			private final Spliterator<Entry<K, List<V>>> delegate;

			@SuppressWarnings("unchecked")
			public UnmodifiableEntrySpliterator(
					Spliterator<? extends Entry<? extends K, ? extends List<? extends V>>> delegate) {

				this.delegate = (Spliterator<Entry<K, List<V>>>) delegate;
			}

			@Override
			public boolean tryAdvance(Consumer<? super Entry<K, List<V>>> action) {
				return this.delegate.tryAdvance(entry -> action.accept(new UnmodifiableEntry<>(entry)));
			}

			@Override
			public void forEachRemaining(Consumer<? super Entry<K, List<V>>> action) {
				this.delegate.forEachRemaining(entry -> action.accept(new UnmodifiableEntry<>(entry)));
			}

			@Override
			@Nullable
			public Spliterator<Entry<K, List<V>>> trySplit() {
				Spliterator<? extends Entry<? extends K, ? extends List<? extends V>>> split = this.delegate.trySplit();
				if (split != null) {
					return new UnmodifiableEntrySpliterator<>(split);
				}
				else {
					return null;
				}
			}

			@Override
			public long estimateSize() {
				return this.delegate.estimateSize();
			}

			@Override
			public long getExactSizeIfKnown() {
				return this.delegate.getExactSizeIfKnown();
			}

			@Override
			public int characteristics() {
				return this.delegate.characteristics();
			}

			@Override
			public boolean hasCharacteristics(int characteristics) {
				return this.delegate.hasCharacteristics(characteristics);
			}

			@Override
			public Comparator<? super Entry<K, List<V>>> getComparator() {
				return this.delegate.getComparator();
			}
		}


		private static class UnmodifiableEntry<K,V> implements Map.Entry<K,List<V>> {

			private final Entry<K, List<V>> delegate;

			@SuppressWarnings("unchecked")
			public UnmodifiableEntry(Entry<? extends K, ? extends List<? extends V>> delegate) {
				Assert.notNull(delegate, "Delegate must not be null");
				this.delegate = (Entry<K, List<V>>) delegate;
			}

			@Override
			public K getKey() {
				return this.delegate.getKey();
			}

			@Override
			public List<V> getValue() {
				return Collections.unmodifiableList(this.delegate.getValue());
			}

			@Override
			public List<V> setValue(List<V> value) {
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean equals(@Nullable Object other) {
				return (this == other || (other instanceof Map.Entry<?, ?> that &&
						getKey().equals(that.getKey()) && getValue().equals(that.getValue())));
			}

			@Override
			public int hashCode() {
				return this.delegate.hashCode();
			}

			@Override
			public String toString() {
				return this.delegate.toString();
			}
		}
	}


	private static class UnmodifiableValueCollection<V> implements Collection<List<V>>, Serializable {

		private static final long serialVersionUID = 5518377583904339588L;

		@SuppressWarnings("serial")
		private final Collection<List<V>> delegate;

		public UnmodifiableValueCollection(Collection<List<V>> delegate) {
			this.delegate = delegate;
		}

		 // delegation

		@Override
		public int size() {
			return this.delegate.size();
		}

		@Override
		public boolean isEmpty() {
			return this.delegate.isEmpty();
		}

		@Override
		public boolean contains(Object o) {
			return this.delegate.contains(o);
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			return this.delegate.containsAll(c);
		}

		@Override
		public Object[] toArray() {
			Object[] result = this.delegate.toArray();
			filterArray(result);
			return result;
		}

		@Override
		public <T> T[] toArray(T[] a) {
			T[] result = this.delegate.toArray(a);
			filterArray(result);
			return result;
		}

		private void filterArray(Object[] array) {
			for (int i = 0; i < array.length; i++) {
				if (array[i] instanceof List<?> list) {
					array[i] = Collections.unmodifiableList(list);
				}
			}
		}

		@Override
		public Iterator<List<V>> iterator() {
			Iterator<List<V>> iterator = this.delegate.iterator();
			return new Iterator<>() {
				@Override
				public boolean hasNext() {
					return iterator.hasNext();
				}

				@Override
				public List<V> next() {
					return Collections.unmodifiableList(iterator.next());
				}
			};
		}

		@Override
		public void forEach(Consumer<? super List<V>> action) {
			this.delegate.forEach(list -> action.accept(Collections.unmodifiableList(list)));
		}

		@Override
		public Spliterator<List<V>> spliterator() {
			return new UnmodifiableValueSpliterator<>(this.delegate.spliterator());
		}

		@Override
		public Stream<List<V>> stream() {
			return StreamSupport.stream(spliterator(), false);
		}

		@Override
		public Stream<List<V>> parallelStream() {
			return StreamSupport.stream(spliterator(), true);
		}

		@Override
		public boolean equals(@Nullable Object other) {
			return (this == other || this.delegate.equals(other));
		}

		@Override
		public int hashCode() {
			return this.delegate.hashCode();
		}

		@Override
		public String toString() {
			return this.delegate.toString();
		}

		// unsupported

		@Override
		public boolean add(List<V> ts) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean remove(Object o) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean addAll(Collection<? extends List<V>> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean removeIf(Predicate<? super List<V>> filter) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		}


		private static class UnmodifiableValueSpliterator<T> implements Spliterator<List<T>> {

			private final Spliterator<List<T>> delegate;

			public UnmodifiableValueSpliterator(Spliterator<List<T>> delegate) {
				this.delegate = delegate;
			}

			@Override
			public boolean tryAdvance(Consumer<? super List<T>> action) {
				return this.delegate.tryAdvance(l -> action.accept(Collections.unmodifiableList(l)));
			}

			@Override
			public void forEachRemaining(Consumer<? super List<T>> action) {
				this.delegate.forEachRemaining(l -> action.accept(Collections.unmodifiableList(l)));
			}

			@Override
			@Nullable
			public Spliterator<List<T>> trySplit() {
				Spliterator<List<T>> split = this.delegate.trySplit();
				if (split != null) {
					return new UnmodifiableValueSpliterator<>(split);
				}
				else {
					return null;
				}
			}

			@Override
			public long estimateSize() {
				return this.delegate.estimateSize();
			}

			@Override
			public long getExactSizeIfKnown() {
				return this.delegate.getExactSizeIfKnown();
			}

			@Override
			public int characteristics() {
				return this.delegate.characteristics();
			}

			@Override
			public boolean hasCharacteristics(int characteristics) {
				return this.delegate.hasCharacteristics(characteristics);
			}

			@Override
			public Comparator<? super List<T>> getComparator() {
				return this.delegate.getComparator();
			}
		}
	}

}
