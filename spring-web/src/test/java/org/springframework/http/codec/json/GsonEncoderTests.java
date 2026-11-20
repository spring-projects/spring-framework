/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.codec.json;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.testfixture.codec.AbstractEncoderTests;
import org.springframework.http.codec.ServerSentEvent;
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
		assertThat(this.encoder.canEncode(ResolvableType.forClass(ServerSentEvent.class), null)).isFalse();
		assertThat(this.encoder.canEncode(ResolvableType.forClass(String.class), null)).isFalse();
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
