/*
 * Copyright 2002-2022 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
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
 * Benchmarks for {@link ConcurrentLruCache}.
 * @author Brian Clozel
 */
@BenchmarkMode(Mode.Throughput)
public class ConcurrentLruCacheBenchmark {

	@Benchmark
	public void lruCache(BenchmarkData data, Blackhole bh) {
		for (String element : data.elements) {
			String value = data.lruCache.get(element);
			bh.consume(value);
		}
	}

	@State(Scope.Benchmark)
	public static class BenchmarkData {

		ConcurrentLruCache<String, String> lruCache;

		@Param({"100"})
		public int capacity;

		@Param({"0.1"})
		public float cacheMissRate;

		public List<String> elements;

		public Function<String, String> generator;

		@Setup(Level.Iteration)
		public void setup() {
			this.generator = key -> key + "value";
			this.lruCache = new ConcurrentLruCache<>(this.capacity, this.generator);
			Assert.isTrue(this.cacheMissRate < 1, "cache miss rate should be < 1");
			Random random = new Random();
			int elementsCount = Math.round(this.capacity * (1 + this.cacheMissRate));
			this.elements = new ArrayList<>(elementsCount);
			random.ints(elementsCount).forEach(value -> this.elements.add(String.valueOf(value)));
			this.elements.sort(String::compareTo);
		}
	}
}
