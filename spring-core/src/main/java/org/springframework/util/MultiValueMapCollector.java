/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	  https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.util;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * A {@link Collector} for building a {@link MultiValueMap} from a {@link java.util.stream.Stream}.
 * <br/>
 * Moved from {@code org.springframework.data.util.MultiValueMapCollector}.
 *
 * @param <T> – the type of input elements to the reduction operation
 * @param <K> – the type of the key elements
 * @param <V> – the type of the value elements
 *
 * @author Jens Schauder
 */
public class MultiValueMapCollector<T, K, V> implements Collector<T, MultiValueMap<K, V>, MultiValueMap<K, V>> {
	private final Function<T, K> keyFunction;
	private final Function<T, V> valueFunction;

	public MultiValueMapCollector(Function<T, K> keyFunction, Function<T, V> valueFunction) {
		this.keyFunction = keyFunction;
		this.valueFunction = valueFunction;
	}

	public static <K, V> MultiValueMapCollector<V, K, V> indexingBy(Function<V, K> indexer) {
		return new MultiValueMapCollector<>(indexer, Function.identity());
	}

	@Override
	public Supplier<MultiValueMap<K, V>> supplier() {
		return () -> CollectionUtils.toMultiValueMap(new HashMap<K, List<V>>());
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
