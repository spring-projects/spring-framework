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

package org.springframework.http.converter.protobuf;


import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import org.springframework.protobuf.Msg;
import org.springframework.protobuf.SecondMsg;
import org.springframework.web.testfixture.http.MockHttpInputMessage;
import org.springframework.web.testfixture.http.MockHttpOutputMessage;

/**
 * Benchmarks for the {@link ProtobufHttpMessageConverter}.
 *
 * @author Brian Clozel
 */
@BenchmarkMode(Mode.Throughput)
public class ProtobufHttpMessageConverterBenchmark {

	@Benchmark
	public void writeMessages(BenchmarkWriteData data, Blackhole bh) throws IOException {
		for (Msg message : data.messages) {
			MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
			data.converter.write(message, ProtobufHttpMessageConverter.PROTOBUF, outputMessage);
			bh.consume(outputMessage);
		}
	}

	/**
	 * Benchmark data holding typical Protobuf messages to be converted to bytes.
	 */
	@State(Scope.Benchmark)
	public static class BenchmarkWriteData {

		@Param({"40"})
		public int messageCount;

		public List<Msg> messages;

		public ProtobufHttpMessageConverter converter = new ProtobufHttpMessageConverter();


		@Setup(Level.Trial)
		public void createMessages() {
			Random random = new Random();
			this.messages = Stream.generate(() -> createMessage(random.nextInt())).limit(this.messageCount).toList();
		}

		private Msg createMessage(int randomValue) {
			return Msg.newBuilder().setFoo(String.valueOf(randomValue)).setBlah(SecondMsg.newBuilder().setBlah(randomValue).build()).build();
		}

	}


	@Benchmark
	public void readMessages(BenchmarkReadData data, Blackhole bh) throws IOException {
		for (byte[] message : data.messages) {
			MockHttpInputMessage inputMessage = new MockHttpInputMessage(message);
			bh.consume(data.converter.read(Msg.class, inputMessage));
		}
	}

	/**
	 * Benchmark data holding typical Protobuf messages to be converted to bytes.
	 */
	@State(Scope.Benchmark)
	public static class BenchmarkReadData {

		@Param({"40"})
		public int messageCount;

		public List<byte[]> messages;

		public ProtobufHttpMessageConverter converter = new ProtobufHttpMessageConverter();

		@Setup(Level.Trial)
		public void createMessages() {
			Random random = new Random();
			this.messages = Stream.generate(() -> createMessage(random.nextInt())).limit(this.messageCount).toList();
		}

		private byte[] createMessage(int randomValue) {
			return Msg.newBuilder().setFoo(String.valueOf(randomValue)).setBlah(SecondMsg.newBuilder().setBlah(randomValue).build()).build().toByteArray();
		}

	}
}
