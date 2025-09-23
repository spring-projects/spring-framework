/*
 * Copyright 2002-present the original author or authors.
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

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.testfixture.codec.AbstractEncoderTests;
import org.springframework.web.testfixture.xml.Pojo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_NDJSON;

class GsonEncoderTests extends AbstractEncoderTests<GsonEncoder> {

	public GsonEncoderTests() {
		super(new GsonEncoder());
	}

	@Test
	@Override
	protected void canEncode() throws Exception {
		ResolvableType pojoType = ResolvableType.forClass(Pojo.class);
		assertThat(this.encoder.canEncode(pojoType, APPLICATION_JSON)).isTrue();
		assertThat(this.encoder.canEncode(pojoType, APPLICATION_NDJSON)).isTrue();
		assertThat(this.encoder.canEncode(pojoType, null)).isTrue();

	}

	@Test
	@Override
	protected void encode() throws Exception {
		Flux<Object> input = Flux.just(new Pojo("foo", "bar"),
				new Pojo("foofoo", "barbar"),
				new Pojo("foofoofoo", "barbarbar"));

		testEncodeAll(input, ResolvableType.forClass(Pojo.class), APPLICATION_NDJSON, null, step -> step
				.consumeNextWith(expectString("{\"foo\":\"foo\",\"bar\":\"bar\"}\n"))
				.consumeNextWith(expectString("{\"foo\":\"foofoo\",\"bar\":\"barbar\"}\n"))
				.consumeNextWith(expectString("{\"foo\":\"foofoofoo\",\"bar\":\"barbarbar\"}\n"))
				.verifyComplete()
		);
	}

	@Test
	void encodeNonStream() {
		Flux<Pojo> input = Flux.just(
				new Pojo("foo", "bar"),
				new Pojo("foofoo", "barbar"),
				new Pojo("foofoofoo", "barbarbar")
		);

		testEncode(input, Pojo.class, step -> step
				.consumeNextWith(expectString("[{\"foo\":\"foo\",\"bar\":\"bar\"}"))
				.consumeNextWith(expectString(",{\"foo\":\"foofoo\",\"bar\":\"barbar\"}"))
				.consumeNextWith(expectString(",{\"foo\":\"foofoofoo\",\"bar\":\"barbarbar\"}"))
				.consumeNextWith(expectString("]"))
				.verifyComplete());
	}

	@Test
	void encodeNonStreamEmpty() {
		testEncode(Flux.empty(), Pojo.class, step -> step
				.consumeNextWith(expectString("["))
				.consumeNextWith(expectString("]"))
				.verifyComplete());
	}

	@Test
	void encodeNonStreamWithErrorAsFirstSignal() {
		String message = "I'm a teapot";
		Flux<Object> input = Flux.error(new IllegalStateException(message));

		Flux<DataBuffer> output = this.encoder.encode(
				input, this.bufferFactory, ResolvableType.forClass(Pojo.class), null, null);

		StepVerifier.create(output).expectErrorMessage(message).verify();
	}
}
