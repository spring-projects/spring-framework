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
package org.springframework.core.codec;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
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

	private static final ResolvableType ELEMENT_TYPE = ResolvableType.forClass(String.class);

	@Benchmark
	public void parseLines(DecodeState state, Blackhole blackhole) {
		Flux<DataBuffer> input = Flux.fromIterable(state.chunks);
		MimeType mimeType = state.mimeType;
		List<String> lines = state.decoder.decode(input, ELEMENT_TYPE, mimeType, Collections.emptyMap())
				.collectList()
				.block();

		blackhole.consume(lines);
	}

	@State(Scope.Benchmark)
	@SuppressWarnings({"NotNullFieldNotInitialized", "ConstantConditions"})
	public static class DecodeState {

		private static final Charset CHARSET = StandardCharsets.UTF_8;

		byte[][] delimiterBytes;

		List<DataBuffer> chunks;

		StringDecoder decoder = StringDecoder.textPlainOnly();

		MimeType mimeType = new MimeType("text", "plain", CHARSET);

		@Setup(Level.Trial)
		public void setup() {
			this.delimiterBytes = new byte[][] {"\r\n".getBytes(CHARSET), "\n".getBytes(CHARSET)};

			String eventTemplate = "id:$1\n" +
					"event:some-event\n" +
					":some-comment-$1-aa\n" +
					":some-comment-$1-bb\n" +
					"data:abcdefg-$1-hijklmnop-$1-qrstuvw-$1-xyz-$1\n\n";

			int totalSize = 10 * 1024;
			int chunkSize = 2000;

			int eventLength = String.format(eventTemplate, String.format("%05d", 1)).length();
			int eventCount = totalSize / eventLength;
			DataBufferFactory bufferFactory = new DefaultDataBufferFactory();

			this.chunks = Flux.range(1, eventCount)
					.map(index -> String.format(eventTemplate, String.format("%05d", index)))
					.buffer(chunkSize > eventLength ? chunkSize / eventLength : 1)
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
	}

}
