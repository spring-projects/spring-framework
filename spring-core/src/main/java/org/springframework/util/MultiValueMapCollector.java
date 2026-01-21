/*
 * Copyright 2002-present the original author or authors.
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

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * A {@link Collector} for building a {@link MultiValueMap} from a
 * {@link java.util.stream.Stream Stream}.
 *
 * <p>Copied from the Spring Data Commons project.
 *
 * @author Jens Schauder
 * @author Florian Hof
 * @author Sam Brannen
 * @since 7.0.2
 * @param <T> the type of input elements to the reduction operation
 * @param <K> the key type
 * @param <V> the value element type
 */
public final class MultiValueMapCollector<T, K, V> implements Collector<T, MultiValueMap<K, V>, MultiValueMap<K, V>> {

	private final Function<T, K> keyFunction;

	private final Function<T, V> valueFunction;


	private MultiValueMapCollector(Function<T, K> keyFunction, Function<T, V> valueFunction) {
		this.keyFunction = keyFunction;
		this.valueFunction = valueFunction;
	}


	/**
	 * Create a new {@code MultiValueMapCollector} from the given key and value
	 * functions.
	 * @param <T> the type of input elements to the reduction operation
	 * @param <K> the key type
	 * @param <V> the value element type
	 * @param keyFunction a {@code Function} which converts an element of type
	 * {@code T} to a key of type {@code K}
	 * @param valueFunction a {@code Function} which converts an element of type
	 * {@code T} to an element of type {@code V}; supply {@link Function#identity()}
	 * if no conversion should be performed
	 * @return a new {@code MultiValueMapCollector}
	 * @see #indexingBy(Function)
	 */
	public static <T, K, V> MultiValueMapCollector<T, K, V> of(Function<T, K> keyFunction, Function<T, V> valueFunction) {
		return new MultiValueMapCollector<>(keyFunction, valueFunction);
	}

	/**
	 * Create a new {@code MultiValueMapCollector} using the given {@code indexer}.
	 * <p>Delegates to {@link #of(Function, Function)}, supplying the given
	 * {@code indexer} as the key function and {@link Function#identity()}
	 * as the value function.
	 * <p>For example, if you would like to collect the elements of a {@code Stream}
	 * of strings into a {@link MultiValueMap} keyed by the lengths of the strings,
	 * you could create such a {@link Collector} via
	 * {@code MultiValueMapCollector.indexingBy(String::length)}.
	 * @param <K> the key type
	 * @param <V> the value element type
	 * @param indexer a {@code Function} which converts a value of type {@code V}
	 * to a key of type {@code K}
	 * @return a new {@code MultiValueMapCollector} based on an {@code indexer}
	 * @see #of(Function, Function)
	 */
	public static <K, V> MultiValueMapCollector<V, K, V> indexingBy(Function<V, K> indexer) {
		return new MultiValueMapCollector<>(indexer, Function.identity());
	}


	@Override
	public Supplier<MultiValueMap<K, V>> supplier() {
		return () -> CollectionUtils.toMultiValueMap(new LinkedHashMap<K, List<V>>());
	}

	@Override
	public BiConsumer<MultiValueMap<K, V>, T> accumulator() {
		return (map, t) -> map.add(this.keyFunction.apply(t), this.valueFunction.apply(t));
	}

	@Override
	public BinaryOperator<MultiValueMap<K, V>> combiner() {
		return (map1, map2) -> {
			for (Entry<K, List<V>> entry : map2.entrySet()) {
				map1.addAll(entry.getKey(), entry.getValue());
			}
			return map1;
		};
	}

	@Override
	public Function<MultiValueMap<K, V>, MultiValueMap<K, V>> finisher() {
		return Function.identity();
	}

	@Override
	public Set<Characteristics> characteristics() {
		return EnumSet.of(Characteristics.IDENTITY_FINISH, Characteristics.UNORDERED);
	}

}
