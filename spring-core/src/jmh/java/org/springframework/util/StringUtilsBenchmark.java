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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;

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
 * Benchmarks for {@link StringUtils}.
 *
 * @author Brian Clozel
 */
@BenchmarkMode(Mode.Throughput)
public class StringUtilsBenchmark {

	@Benchmark
	public void collectionToDelimitedString(DelimitedStringState state, Blackhole bh) {
		bh.consume(StringUtils.collectionToCommaDelimitedString(state.elements));
	}

	@State(Scope.Benchmark)
	public static class DelimitedStringState {

		@Param("10")
		int elementMinSize;

		@Param("20")
		int elementMaxSize;

		@Param("10")
		int elementCount;

		Collection<String> elements;

		@Setup(Level.Iteration)
		public void setup() {
			Random random = new Random();
			this.elements = new ArrayList<>(this.elementCount);
			int bound = this.elementMaxSize - this.elementMinSize;
			for (int i = 0; i < this.elementCount; i++) {
				this.elements.add(String.format("%0" + (random.nextInt(bound) + this.elementMinSize) + "d", 1));
			}
		}
	}

	@Benchmark
	public void cleanPath(CleanPathState state, Blackhole bh) {
		for (String path : state.paths) {
			bh.consume(StringUtils.cleanPath(path));
		}
	}

	@State(Scope.Benchmark)
	public static class CleanPathState {

		private static final List<String> SEGMENTS = Arrays.asList("some", "path", ".", "..", "springspring");

		@Param("10")
		int segmentCount;

		@Param("20")
		int pathsCount;

		Collection<String> paths;

		@Setup(Level.Iteration)
		public void setup() {
			this.paths = new ArrayList<>(this.pathsCount);
			Random random = new Random();
			for (int i = 0; i < this.pathsCount; i++) {
				this.paths.add(createSamplePath(random));
			}
		}

		private String createSamplePath(Random random) {
			String separator = (random.nextBoolean() ? "/" : "\\");
			StringBuilder sb = new StringBuilder();
			sb.append("jar:file:///c:");
			for (int i = 0; i < this.segmentCount; i++) {
				sb.append(separator);
				sb.append(SEGMENTS.get(random.nextInt(SEGMENTS.size())));
			}
			sb.append(separator);
			sb.append("the%20file.txt");
			return sb.toString();
		}

	}
}
