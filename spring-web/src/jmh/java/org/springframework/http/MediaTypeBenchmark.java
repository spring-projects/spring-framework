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

package org.springframework.http;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import org.springframework.util.MimeTypeUtils;

/**
 * Benchmarks for parsing Media Types using {@link MediaType}.
 * <p>{@code MediaType is using }{@link MimeTypeUtils} has an internal parser only accessible through a package private method.
 * The publicly accessible method is backed by a LRUCache for better performance.
 *
 * @author Brian Clozel
 * @see MimeTypeUtils
 */
@BenchmarkMode(Mode.Throughput)
public class MediaTypeBenchmark {

	@Benchmark
	public void parseAllMediaTypes(BenchmarkData data, Blackhole bh) {
		for (String type : data.mediaTypes) {
			bh.consume(MediaType.parseMediaType(type));
		}
	}

	@Benchmark
	public void parseSomeMediaTypes(BenchmarkData data, Blackhole bh) {
		for (String type : data.requestedMediaTypes) {
			bh.consume(MediaType.parseMediaType(type));
		}
	}

	/**
	 * Benchmark data holding typical raw Media Types.
	 * A {@code customTypesCount} parameter can be used to pad the list with artificial types.
	 * The {@param requestedTypeCount} parameter allows to choose the number of requested types at runtime,
	 * since we don't want to use all available types in the cache in some benchmarks.
	 */
	@State(Scope.Benchmark)
	public static class BenchmarkData {

		@Param({"40"})
		public int customTypesCount;

		@Param({"10"})
		public int requestedTypeCount;

		public List<String> mediaTypes;

		public List<String> requestedMediaTypes;

		@Setup(Level.Trial)
		public void fillCache() {
			this.mediaTypes = new ArrayList<>();
			// Add 25 common MIME types
			this.mediaTypes.addAll(Arrays.asList(
					"application/json",
					"application/octet-stream",
					"application/pdf",
					"application/problem+json",
					"application/xhtml+xml",
					"application/rss+xml",
					"application/x-ndjson",
					"application/xml;q=0.9",
					"application/atom+xml",
					"application/cbor",
					"application/x-www-form-urlencoded",
					"*/*",
					"image/gif",
					"image/jpeg",
					"image/webp",
					"image/png",
					"image/apng",
					"text/plain",
					"text/html",
					"text/xml",
					"text/event-stream",
					"text/markdown",
					"*/*;q=0.8",
					"multipart/form-data",
					"multipart/mixed"
			));
			// Add custom types, allowing to fill the LRU cache (which has a default size of 64)
			IntStream.range(0, this.customTypesCount).forEach(i -> this.mediaTypes.add("custom/type" + i));
			this.requestedMediaTypes = this.mediaTypes.subList(0, this.requestedTypeCount);

			// ensure that all known MIME types are parsed once and cached
			this.mediaTypes.forEach(MediaType::parseMediaType);
		}

	}

}
