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


import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;
import java.util.function.Function;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Benchmarks for {@link ConcurrentReferenceHashMap}.
 * <p>This benchmark ensures that {@link ConcurrentReferenceHashMap} performs
 * better than {@link java.util.Collections#synchronizedMap(Map)} with
 * concurrent read operations.
 * <p>Typically this can be run with {@code "java -jar spring-core-jmh.jar -t 30 -f 2 ConcurrentReferenceHashMapBenchmark"}.
 * @author Brian Clozel
 */
@BenchmarkMode(Mode.Throughput)
public class ConcurrentReferenceHashMapBenchmark {

	@Benchmark
	public void concurrentMap(ConcurrentMapBenchmarkData data, Blackhole bh) {
		for (String element : data.elements) {
			WeakReference<String> value = data.map.get(element);
			bh.consume(value);
		}
	}

	@State(Scope.Benchmark)
	public static class ConcurrentMapBenchmarkData {

		@Param({"500"})
		public int capacity;
		private final Function<String, String> generator = key -> key + "value";

		public List<String> elements;

		public Map<String, WeakReference<String>> map;

		@Setup(Level.Iteration)
		public void setup() {
			this.elements = new ArrayList<>(this.capacity);
			this.map = new ConcurrentReferenceHashMap<>();
			Random random = new Random();
			random.ints(this.capacity).forEach(value -> {
				String element = String.valueOf(value);
				this.elements.add(element);
				this.map.put(element, new WeakReference<>(this.generator.apply(element)));
			});
			this.elements.sort(String::compareTo);
		}
	}

	@Benchmark
	public void synchronizedMap(SynchronizedMapBenchmarkData data, Blackhole bh) {
		for (String element : data.elements) {
			WeakReference<String> value = data.map.get(element);
			bh.consume(value);
		}
	}

	@State(Scope.Benchmark)
	public static class SynchronizedMapBenchmarkData {

		@Param({"500"})
		public int capacity;

		private Function<String, String> generator = key -> key + "value";

		public List<String> elements;

		public Map<String, WeakReference<String>> map;


		@Setup(Level.Iteration)
		public void setup() {
			this.elements = new ArrayList<>(this.capacity);
			this.map = Collections.synchronizedMap(new WeakHashMap<>());
			Random random = new Random();
			random.ints(this.capacity).forEach(value -> {
				String element = String.valueOf(value);
				this.elements.add(element);
				this.map.put(element, new WeakReference<>(this.generator.apply(element)));
			});
			this.elements.sort(String::compareTo);
		}
	}

}
