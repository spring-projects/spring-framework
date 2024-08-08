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

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.testfixture.codec.AbstractDecoderTests;
import org.springframework.http.MediaType;
import org.springframework.protobuf.Msg;
import org.springframework.protobuf.SecondMsg;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ProtobufJsonDecoder}.
 * @author Brian Clozel
 */
public class ProtobufJsonDecoderTests extends AbstractDecoderTests<ProtobufJsonDecoder> {

	private Msg msg1 = Msg.newBuilder().setFoo("Foo").setBlah(SecondMsg.newBuilder().setBlah(123).build()).build();

	public ProtobufJsonDecoderTests() {
		super(new ProtobufJsonDecoder());
	}

	@Test
	@Override
	protected void canDecode() throws Exception {
		ResolvableType msgType = ResolvableType.forClass(Msg.class);
		assertThat(this.decoder.canDecode(msgType, null)).isFalse();
		assertThat(this.decoder.canDecode(msgType, MediaType.APPLICATION_JSON)).isTrue();
		assertThat(this.decoder.canDecode(msgType, MediaType.APPLICATION_PROTOBUF)).isFalse();
		assertThat(this.decoder.canDecode(ResolvableType.forClass(Object.class), MediaType.APPLICATION_JSON)).isFalse();
	}

	@Test
	@Override
	protected void decode() throws Exception {
		ResolvableType msgType = ResolvableType.forClass(Msg.class);
		Flux<DataBuffer> input = Flux.just(dataBuffer("[{\"foo\":\"Foo\",\"blah\":{\"blah\":123}}"),
				dataBuffer(",{\"foo\":\"Bar\",\"blah\":{\"blah\":456}}"),
				dataBuffer("]"));

		testDecode(input, msgType, step -> step.consumeErrorWith(error -> assertThat(error).isInstanceOf(UnsupportedOperationException.class)),
				MediaType.APPLICATION_JSON, null);
	}

	@Test
	@Override
	protected void decodeToMono() throws Exception {
		DataBuffer dataBuffer = dataBuffer("{\"foo\":\"Foo\",\"blah\":{\"blah\":123}}");
		testDecodeToMonoAll(Mono.just(dataBuffer), Msg.class, step -> step
				.expectNext(this.msg1)
				.verifyComplete());
	}

	@Test
	void exceedMaxSize() {
		this.decoder.setMaxMessageSize(1);
		DataBuffer first = dataBuffer("{\"foo\":\"Foo\",");
		DataBuffer second = dataBuffer("\"blah\":{\"blah\":123}}");

		testDecodeToMono(Flux.just(first, second), Msg.class, step -> step.verifyError(DecodingException.class));
	}

	private DataBuffer dataBuffer(String json) {
		return this.bufferFactory.wrap(json.getBytes());
	}

}
