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

import java.util.List;
import java.util.Map;

import org.springframework.lang.Nullable;

/**
 * Extension of the {@code Map} interface that stores multiple values.
 *
 * @author Arjen Poutsma
 * @since 3.0
 * @param <K> the key type
 * @param <V> the value element type
 */
public interface MultiValueMap<K, V> extends Map<K, List<V>> {

	/**
	 * Return the first value for the given key.
	 * @param key the key
	 * @return the first value for the specified key, or {@code null} if none
	 */
	@Nullable
	V getFirst(K key);

	/**
	 * Add the given single value to the current list of values for the given key.
	 * @param key the key
	 * @param value the value to be added
	 */
	void add(K key, @Nullable V value);

	/**
	 * Add all the values of the given list to the current list of values for the given key.
	 * @param key they key
	 * @param values the values to be added
	 * @since 5.0
	 */
	void addAll(K key, List<? extends V> values);

	/**
	 * Add all the values of the given {@code MultiValueMap} to the current values.
	 * @param values the values to be added
	 * @since 5.0
	 */
	void addAll(MultiValueMap<K, V> values);

	/**
	 * {@link #add(Object, Object) Add} the given value, only when the map does not
	 * {@link #containsKey(Object) contain} the given key.
	 * @param key the key
	 * @param value the value to be added
	 * @since 5.2
	 */
	default void addIfAbsent(K key, @Nullable V value) {
		if (!containsKey(key)) {
			add(key, value);
		}
	}

	/**
	 * Set the given single value under the given key.
	 * @param key the key
	 * @param value the value to set
	 */
	void set(K key, @Nullable V value);

	/**
	 * Set the given values under.
	 * @param values the values.
	 */
	void setAll(Map<K, V> values);

	/**
	 * Return a {@code Map} with the first values contained in this {@code MultiValueMap}.
	 * The difference between this method and {@link #asSingleValueMap()} is
	 * that this method returns a copy of the entries of this map, whereas
	 * the latter returns a view.
	 * @return a single value representation of this map
	 */
	Map<K, V> toSingleValueMap();

	/**
	 * Return this map as a {@code Map} with the first values contained in this {@code MultiValueMap}.
	 * The difference between this method and {@link #toSingleValueMap()} is
	 * that this method returns a view of the entries of this map, whereas
	 * the latter returns a copy.
	 * @return a single value representation of this map
	 * @since 6.2
	 */
	default Map<K, V> asSingleValueMap() {
		return new MultiToSingleValueMapAdapter<>(this);
	}


	/**
	 * Return a {@code MultiValueMap<K, V>} that adapts the given single-value
	 * {@code Map<K, V>}.
	 * The returned map cannot map multiple values to the same key, and doing so
	 * results in an {@link UnsupportedOperationException}. Use
	 * {@link #fromMultiValue(Map)} to support multiple values.
	 * @param map the map to be adapted
	 * @param <K> the key type
	 * @param <V> the value element type
	 * @return a multi-value-map that delegates to {@code map}
	 * @since 6.2
	 * @see #fromMultiValue(Map)
	 */
	static <K, V> MultiValueMap<K, V> fromSingleValue(Map<K, V> map) {
		Assert.notNull(map, "Map must not be null");
		return new SingleToMultiValueMapAdapter<>(map);
	}

	/**
	 * Return a {@code MultiValueMap<K, V>} that adapts the given multi-value
	 * {@code Map<K, List<V>>}.
	 * @param map the map to be adapted
	 * @param <K> the key type
	 * @param <V> the value element type
	 * @return a multi-value-map that delegates to {@code map}
	 * @since 6.2
	 * @see #fromSingleValue(Map)
	 */
	static <K, V> MultiValueMap<K, V> fromMultiValue(Map<K, List<V>> map) {
		Assert.notNull(map, "Map must not be null");
		return new MultiValueMapAdapter<>(map);
	}

}
