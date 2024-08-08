/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.http.codec.protobuf;

import java.nio.charset.StandardCharsets;

import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.testfixture.codec.AbstractEncoderTests;
import org.springframework.core.testfixture.io.buffer.DataBufferTestUtils;
import org.springframework.http.MediaType;
import org.springframework.protobuf.Msg;
import org.springframework.protobuf.SecondMsg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.core.ResolvableType.forClass;

/**
 * Tests for {@link ProtobufJsonEncoder}.
 * @author Brian Clozel
 */
class ProtobufJsonEncoderTests extends AbstractEncoderTests<ProtobufJsonEncoder> {

	private Msg msg1 =
			Msg.newBuilder().setFoo("Foo").setBlah(SecondMsg.newBuilder().setBlah(123).build()).build();

	private Msg msg2 =
			Msg.newBuilder().setFoo("Bar").setBlah(SecondMsg.newBuilder().setBlah(456).build()).build();

	public ProtobufJsonEncoderTests() {
		super(new ProtobufJsonEncoder(JsonFormat.printer().omittingInsignificantWhitespace()));
	}

	@Override
	@Test
	protected void canEncode() throws Exception {
		assertThat(this.encoder.canEncode(forClass(Msg.class), null)).isFalse();
		assertThat(this.encoder.canEncode(forClass(Msg.class), MediaType.APPLICATION_JSON)).isTrue();
		assertThat(this.encoder.canEncode(forClass(Msg.class), MediaType.APPLICATION_NDJSON)).isFalse();
		assertThat(this.encoder.canEncode(forClass(Object.class), MediaType.APPLICATION_JSON)).isFalse();
	}

	@Override
	@Test
	protected void encode() throws Exception {
		Mono<Message> input = Mono.just(this.msg1);
		ResolvableType inputType = forClass(Msg.class);

		testEncode(input, inputType, MediaType.APPLICATION_JSON, null, step -> step
				.assertNext(dataBuffer -> assertBufferEqualsJson(dataBuffer, "{\"foo\":\"Foo\",\"blah\":{\"blah\":123}}"))
				.verifyComplete());
		testEncodeError(input, inputType, MediaType.APPLICATION_JSON, null);
		testEncodeCancel(input, inputType, MediaType.APPLICATION_JSON, null);
	}

	@Test
	void encodeEmptyMono() {
		Mono<Message> input = Mono.empty();
		ResolvableType inputType = forClass(Msg.class);
		Flux<DataBuffer> result = this.encoder.encode(input, this.bufferFactory, inputType,
				MediaType.APPLICATION_JSON, null);
		StepVerifier.create(result)
				.verifyComplete();
	}

	@Test
	void encodeStream() {
		Flux<Message> input = Flux.just(this.msg1, this.msg2);
		ResolvableType inputType = forClass(Msg.class);

		testEncode(input, inputType, MediaType.APPLICATION_JSON, null, step -> step
				.assertNext(dataBuffer -> assertBufferEqualsJson(dataBuffer, "[{\"foo\":\"Foo\",\"blah\":{\"blah\":123}}"))
				.assertNext(dataBuffer -> assertBufferEqualsJson(dataBuffer, ",{\"foo\":\"Bar\",\"blah\":{\"blah\":456}}"))
				.assertNext(dataBuffer -> assertBufferEqualsJson(dataBuffer, "]"))
				.verifyComplete());
	}

	@Test
	void encodeEmptyFlux() {
		Flux<Message> input = Flux.empty();
		ResolvableType inputType = forClass(Msg.class);
		Flux<DataBuffer> result = this.encoder.encode(input, this.bufferFactory, inputType,
				MediaType.APPLICATION_JSON, null);
		StepVerifier.create(result)
				.assertNext(buffer -> assertBufferEqualsJson(buffer, "["))
				.assertNext(buffer -> assertBufferEqualsJson(buffer, "]"))
				.verifyComplete();
	}


	private void assertBufferEqualsJson(DataBuffer actual, String expected) {
		byte[] bytes = DataBufferTestUtils.dumpBytes(actual);
		String json = new String(bytes, StandardCharsets.UTF_8);
		assertThat(json).isEqualTo(expected);
		DataBufferUtils.release(actual);
	}

}
