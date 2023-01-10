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

package org.springframework.core.codec;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import reactor.core.publisher.Flux;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.util.MimeType;

/**
 * Benchmarks for {@link DataBufferUtils}.
 *
 * @author Rossen Stoyanchev
 */
@BenchmarkMode(Mode.Throughput)
public class StringDecoderBenchmark {

	@Benchmark
	public void parseSseLines(SseLinesState state, Blackhole blackhole) {
		blackhole.consume(state.parseLines().blockLast());
	}


	@State(Scope.Benchmark)
	@SuppressWarnings({"NotNullFieldNotInitialized", "ConstantConditions"})
	public static class SseLinesState {

		private static final Charset CHARSET = StandardCharsets.UTF_8;

		private static final ResolvableType ELEMENT_TYPE = ResolvableType.forClass(String.class);


		@Param("10240")
		int totalSize;

		@Param("2000")
		int chunkSize;

		List<DataBuffer> chunks;

		StringDecoder decoder = StringDecoder.textPlainOnly(Arrays.asList("\r\n", "\n"), false);

		MimeType mimeType = new MimeType("text", "plain", CHARSET);


		@Setup(Level.Trial)
		public void setup() {
			String eventTemplate = """
					id:$1
					event:some-event
					:some-comment-$1-aa
					:some-comment-$1-bb
					data:abcdefg-$1-hijklmnop-$1-qrstuvw-$1-xyz-$1

					""";

			int eventLength = String.format(eventTemplate, String.format("%05d", 1)).length();
			int eventCount = this.totalSize / eventLength;
			DataBufferFactory bufferFactory = new DefaultDataBufferFactory();

			this.chunks = Flux.range(1, eventCount)
					.map(index -> String.format(eventTemplate, String.format("%05d", index)))
					.buffer(this.chunkSize > eventLength ? this.chunkSize / eventLength : 1)
					.map(strings -> String.join("", strings))
					.map(chunk -> {
						byte[] bytes = chunk.getBytes(CHARSET);
						DataBuffer buffer = bufferFactory.allocateBuffer(bytes.length);
						buffer.write(bytes);
						return buffer;
					})
					.collectList()
					.block();
		}

		public Flux<String> parseLines() {
			Flux<DataBuffer> input = Flux.fromIterable(this.chunks).doOnNext(DataBufferUtils::retain);
			return this.decoder.decode(input, ELEMENT_TYPE, this.mimeType, Collections.emptyMap());
		}
	}

}
