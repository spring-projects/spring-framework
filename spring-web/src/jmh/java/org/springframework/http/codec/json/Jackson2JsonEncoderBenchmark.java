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

package org.springframework.http.codec.json;

import java.util.Collections;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
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
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Benchmarks for encoding POJOs to JSON using Jackson.
 *
 * @author Brian Clozel
 * @see AbstractJackson2Encoder
 */
@BenchmarkMode(Mode.Throughput)
public class Jackson2JsonEncoderBenchmark {


	/**
	 * Benchmark data holding {@link Project} to be serialized by the JSON Encoder.
	 * A {@code projectCount} parameter can be used to grow the size of the object graph to serialize.
	 */
	@State(Scope.Benchmark)
	public static class EncodeSingleData {

		@Param({"0", "50", "500"})
		int projectCount;

		Jackson2JsonEncoder jsonEncoder;

		DataBufferFactory bufferFactory;

		ResolvableType resolvableType;

		Project project;

		@Setup
		public void setup() {
			final Jackson2ObjectMapperBuilder mapperBuilder = new Jackson2ObjectMapperBuilder();
			ObjectMapper objectMapper = mapperBuilder.build();
			this.bufferFactory = new DefaultDataBufferFactory();
			this.jsonEncoder = new Jackson2JsonEncoder(objectMapper);
			this.resolvableType = ResolvableType.forClass(Project.class);
			this.project = new Project("spring", this.projectCount);
		}

	}

	@Benchmark
	public DataBuffer encodeValue(EncodeSingleData data) {
		return data.jsonEncoder.encodeValue(data.project, data.bufferFactory, data.resolvableType, MediaType.APPLICATION_JSON, Collections.emptyMap());
	}

	/**
	 * Benchmark data holding {@link Project} to be serialized by the JSON Encoder.
	 * A {@code projectCount} parameter can be used to grow the size of the object graph to serialize.
	 */
	@State(Scope.Benchmark)
	public static class EncodeData extends EncodeSingleData {

		@Param({"1", "50", "500"})
		int streamSize;

	}

	@Benchmark
	public void encode(Blackhole bh, EncodeData data) {
		Flux<Project> projects = Flux.generate(sink -> sink.next(data.project)).take(data.streamSize).cast(Project.class);
		data.jsonEncoder.encode(projects, data.bufferFactory, data.resolvableType, MediaType.APPLICATION_JSON, Collections.emptyMap())
				.doOnNext(bh::consume)
				.then().block();
	}

}
