/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.codec.cbor;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import tools.jackson.core.JacksonException;
import tools.jackson.dataformat.cbor.CBORMapper;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.testfixture.codec.AbstractDecoderTests;
import org.springframework.http.MediaType;
import org.springframework.web.testfixture.xml.Pojo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * Tests for {@link JacksonCborDecoder}.
 *
 * @author Sebastien Deleuze
 */
class JacksonCborDecoderTests extends AbstractDecoderTests<JacksonCborDecoder> {

	private Pojo pojo1 = new Pojo("f1", "b1");

	private Pojo pojo2 = new Pojo("f2", "b2");

	private CBORMapper mapper = CBORMapper.builder().build();

	public JacksonCborDecoderTests() {
		super(new JacksonCborDecoder());
	}

	@Override
	@Test
	protected void canDecode() {
		assertThat(decoder.canDecode(ResolvableType.forClass(Pojo.class), MediaType.APPLICATION_CBOR)).isTrue();
		assertThat(decoder.canDecode(ResolvableType.forClass(Pojo.class), null)).isTrue();

		assertThat(decoder.canDecode(ResolvableType.forClass(String.class), null)).isTrue();
		assertThat(decoder.canDecode(ResolvableType.forClass(Pojo.class), APPLICATION_JSON)).isFalse();
	}

	@Override
	@Test
	protected void decode() {
		Flux<DataBuffer> input = Flux.just(this.pojo1, this.pojo2)
				.map(this::writeObject)
				.flatMap(this::dataBuffer);
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
				testDecodeAll(input, Pojo.class, step -> step
						.expectNext(pojo1)
						.expectNext(pojo2)
						.verifyComplete()));

	}

	private byte[] writeObject(Object o) {
		try {
			return this.mapper.writer().writeValueAsBytes(o);
		}
		catch (JacksonException e) {
			throw new AssertionError(e);
		}

	}

	@Override
	@Test
	protected void decodeToMono() {
		List<Pojo> expected = Arrays.asList(pojo1, pojo2);

		Flux<DataBuffer> input = Flux.just(expected)
				.map(this::writeObject)
				.flatMap(this::dataBuffer);

		ResolvableType elementType = ResolvableType.forClassWithGenerics(List.class, Pojo.class);
		testDecodeToMono(input, elementType, step -> step
				.expectNext(expected)
				.expectComplete()
				.verify(), null, null);
	}
}
