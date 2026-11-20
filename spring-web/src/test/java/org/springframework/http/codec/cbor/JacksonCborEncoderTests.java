/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.codec.cbor;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tools.jackson.dataformat.cbor.CBORMapper;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.testfixture.io.buffer.AbstractLeakCheckingTests;
import org.springframework.core.testfixture.io.buffer.DataBufferTestUtils;
import org.springframework.http.MediaType;
import org.springframework.web.testfixture.xml.Pojo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.core.io.buffer.DataBufferUtils.release;
import static org.springframework.http.MediaType.APPLICATION_XML;

/**
 * Tests for {@link JacksonCborEncoder}.
 *
 * @author Sebastien Deleuze
 */
class JacksonCborEncoderTests extends AbstractLeakCheckingTests {

	private final CBORMapper mapper = CBORMapper.builder().build();

	private final JacksonCborEncoder encoder = new JacksonCborEncoder();

	private Consumer<DataBuffer> pojoConsumer(Pojo expected) {
		return dataBuffer -> {
			Pojo actual = this.mapper.reader().forType(Pojo.class)
					.readValue(DataBufferTestUtils.dumpBytes(dataBuffer));
			assertThat(actual).isEqualTo(expected);
			release(dataBuffer);
		};
	}

	@Test
	void canEncode() {
		ResolvableType pojoType = ResolvableType.forClass(Pojo.class);
		assertThat(this.encoder.canEncode(pojoType, MediaType.APPLICATION_CBOR)).isTrue();
		assertThat(this.encoder.canEncode(pojoType, null)).isTrue();
		assertThat(this.encoder.canEncode(ResolvableType.forClass(String.class), null)).isTrue();

		// SPR-15464
		assertThat(this.encoder.canEncode(ResolvableType.NONE, null)).isTrue();
	}

	@Test
	void canNotEncode() {
		assertThat(this.encoder.canEncode(ResolvableType.forClass(Pojo.class), APPLICATION_XML)).isFalse();
	}

	@Test
	void encode() {
		Pojo value = new Pojo("foo", "bar");
		DataBuffer result = encoder.encodeValue(value, this.bufferFactory, ResolvableType.forClass(Pojo.class),
				MediaType.APPLICATION_CBOR, null);
		pojoConsumer(value).accept(result);
	}

	@Test
	void encodeMono() {
		Pojo value = new Pojo("foo", "bar");
		Flux<DataBuffer> result = this.encoder.encode(Mono.just(value), this.bufferFactory,
				ResolvableType.forClass(Pojo.class), MediaType.APPLICATION_CBOR, null);
		StepVerifier.create(result)
				.consumeNextWith(pojoConsumer(value))
				.verifyComplete();
	}

	@Test
	void encodeStream() {
		Pojo pojo1 = new Pojo("foo", "bar");
		Pojo pojo2 = new Pojo("foofoo", "barbar");
		Pojo pojo3 = new Pojo("foofoofoo", "barbarbar");
		Flux<Pojo> input = Flux.just(pojo1, pojo2, pojo3);
		ResolvableType type = ResolvableType.forClass(Pojo.class);
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
				encoder.encode(input, this.bufferFactory, type, MediaType.APPLICATION_CBOR, null));
	}
}
