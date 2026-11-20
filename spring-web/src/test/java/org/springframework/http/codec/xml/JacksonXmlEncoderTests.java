/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.codec.xml;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tools.jackson.dataformat.xml.XmlMapper;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.testfixture.io.buffer.AbstractLeakCheckingTests;
import org.springframework.core.testfixture.io.buffer.DataBufferTestUtils;
import org.springframework.http.MediaType;
import org.springframework.web.testfixture.xml.Pojo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.core.io.buffer.DataBufferUtils.release;

/**
 * Tests for {@link JacksonXmlEncoder}.
 *
 * @author Sebastien Deleuze
 */
class JacksonXmlEncoderTests extends AbstractLeakCheckingTests {

	private final XmlMapper mapper = XmlMapper.builder().build();

	private final JacksonXmlEncoder encoder = new JacksonXmlEncoder();


	@Test
	void canEncode() {
		ResolvableType pojoType = ResolvableType.forClass(Pojo.class);
		assertThat(this.encoder.canEncode(pojoType, MediaType.APPLICATION_XML)).isTrue();
		assertThat(this.encoder.canEncode(pojoType, MediaType.TEXT_XML)).isTrue();
		assertThat(this.encoder.canEncode(pojoType, new MediaType("application", "soap+xml"))).isTrue();
		assertThat(this.encoder.canEncode(pojoType, null)).isTrue();
		assertThat(this.encoder.canEncode(ResolvableType.forClass(String.class), null)).isFalse();
		assertThat(this.encoder.canEncode(ResolvableType.NONE, null)).isTrue();
		assertThat(this.encoder.canEncode(ResolvableType.forClass(Pojo.class), MediaType.APPLICATION_JSON)).isFalse();
	}

	@Test
	void encode() {
		Pojo value = new Pojo("foo", "bar");
		DataBuffer result = encoder.encodeValue(value, this.bufferFactory, ResolvableType.forClass(Pojo.class),
				MediaType.APPLICATION_XML, null);
		pojoConsumer(value).accept(result);
	}

	private Consumer<DataBuffer> pojoConsumer(Pojo expected) {
		return dataBuffer -> {
			Pojo actual = this.mapper.reader().forType(Pojo.class)
					.readValue(DataBufferTestUtils.dumpBytes(dataBuffer));
			assertThat(actual).isEqualTo(expected);
			release(dataBuffer);
		};
	}

	@Test
	void encodeMono() {
		Pojo value = new Pojo("foo", "bar");
		Flux<DataBuffer> result = this.encoder.encode(Mono.just(value), this.bufferFactory,
				ResolvableType.forClass(Pojo.class), MediaType.APPLICATION_XML, null);
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
				encoder.encode(input, this.bufferFactory, type, MediaType.APPLICATION_XML, null));
	}

}
