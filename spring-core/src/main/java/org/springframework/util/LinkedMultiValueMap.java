/*
 * Copyright 2002-2020 the original author or authors.
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple implementation of {@link MultiValueMap} that wraps a {@link LinkedHashMap},
 * storing multiple values in an {@link ArrayList}.
 *
 * <p>This Map implementation is generally not thread-safe. It is primarily designed
 * for data structures exposed from request objects, for use in a single thread only.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 * @param <K> the key type
 * @param <V> the value element type
 */
public class LinkedMultiValueMap<K, V> extends MultiValueMapAdapter<K, V>  // new public base class in 5.3
		implements Serializable, Cloneable {

	private static final long serialVersionUID = 3801124242820219131L;


	/**
	 * Create a new LinkedMultiValueMap that wraps a {@link LinkedHashMap}.
	 */
	public LinkedMultiValueMap() {
		super(new LinkedHashMap<>());
	}

	/**
	 * Create a new LinkedMultiValueMap that wraps a {@link LinkedHashMap}
	 * with an initial capacity that can accommodate the specified number of
	 * elements without any immediate resize/rehash operations to be expected.
	 * @param expectedSize the expected number of elements (with a corresponding
	 * capacity to be derived so that no resize/rehash operations are needed)
	 * @see CollectionUtils#newLinkedHashMap(int)
	 */
	public LinkedMultiValueMap(int expectedSize) {
		super(CollectionUtils.newLinkedHashMap(expectedSize));
	}

	/**
	 * Copy constructor: Create a new LinkedMultiValueMap with the same mappings as
	 * the specified Map. Note that this will be a shallow copy; its value-holding
	 * List entries will get reused and therefore cannot get modified independently.
	 * @param otherMap the Map whose mappings are to be placed in this Map
	 * @see #clone()
	 * @see #deepCopy()
	 */
	public LinkedMultiValueMap(Map<K, List<V>> otherMap) {
		super(new LinkedHashMap<>(otherMap));
	}


	/**
	 * Create a deep copy of this Map.
	 * @return a copy of this Map, including a copy of each value-holding List entry
	 * (consistently using an independent modifiable {@link ArrayList} for each entry)
	 * along the lines of {@code MultiValueMap.addAll} semantics
	 * @since 4.2
	 * @see #addAll(MultiValueMap)
	 * @see #clone()
	 */
	public LinkedMultiValueMap<K, V> deepCopy() {
		LinkedMultiValueMap<K, V> copy = new LinkedMultiValueMap<>(size());
		forEach((key, values) -> copy.put(key, new ArrayList<>(values)));
		return copy;
	}

	/**
	 * Create a regular copy of this Map.
	 * @return a shallow copy of this Map, reusing this Map's value-holding List entries
	 * (even if some entries are shared or unmodifiable) along the lines of standard
	 * {@code Map.put} semantics
	 * @since 4.2
	 * @see #put(Object, List)
	 * @see #putAll(Map)
	 * @see LinkedMultiValueMap#LinkedMultiValueMap(Map)
	 * @see #deepCopy()
	 */
	@Override
	public LinkedMultiValueMap<K, V> clone() {
		return new LinkedMultiValueMap<>(this);
	}

}
