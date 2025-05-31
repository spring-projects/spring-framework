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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import guru.mocker.annotation.mixin.Mixin;
import org.jspecify.annotations.Nullable;

/**
 * Adapts a given {@link Map} to the {@link MultiValueMap} contract.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 5.3
 * @param <K> the key type
 * @param <V> the value element type
 * @see CollectionUtils#toMultiValueMap
 * @see LinkedMultiValueMap
 */
@SuppressWarnings("serial")
@Mixin
public class MultiValueMapAdapter<K, V> extends MapForwarder<K,V> implements MultiValueMap<K, V>, Serializable {

	/**
	 * Wrap the given target {@link Map} as a {@link MultiValueMap} adapter.
	 * @param mapForwarder the plain target {@code Map}
	 */
	public MultiValueMapAdapter(Map<K, List<V>> mapForwarder) {
		super(mapForwarder);
		Assert.notNull(mapForwarder, "'targetMap' must not be null");
	}


	// MultiValueMap implementation

	@Override
	public @Nullable V getFirst(K key) {
		List<V> values = this.mapForwarder.get(key);
		return (!CollectionUtils.isEmpty(values) ? values.get(0) : null);
	}

	@Override
	public void add(K key, @Nullable V value) {
		List<V> values = this.mapForwarder.computeIfAbsent(key, k -> new ArrayList<>(1));
		values.add(value);
	}

	@Override
	public void addAll(K key, List<? extends V> values) {
		List<V> currentValues = this.mapForwarder.computeIfAbsent(key, k -> new ArrayList<>(values.size()));
		currentValues.addAll(values);
	}

	@Override
	public void addAll(MultiValueMap<K, V> values) {
		values.forEach(this::addAll);
	}

	@Override
	public void set(K key, @Nullable V value) {
		List<V> values = new ArrayList<>(1);
		values.add(value);
		this.mapForwarder.put(key, values);
	}

	@Override
	public void setAll(Map<K, V> values) {
		values.forEach(this::set);
	}

	@Override
	public Map<K, V> toSingleValueMap() {
		Map<K, V> singleValueMap = CollectionUtils.newLinkedHashMap(this.mapForwarder.size());
		this.mapForwarder.forEach((key, values) -> {
			if (!CollectionUtils.isEmpty(values)) {
				singleValueMap.put(key, values.get(0));
			}
		});
		return singleValueMap;
	}


	// Map implementation

	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || this.mapForwarder.equals(other));
	}

	@Override
	public int hashCode() {
		return this.mapForwarder.hashCode();
	}
}
