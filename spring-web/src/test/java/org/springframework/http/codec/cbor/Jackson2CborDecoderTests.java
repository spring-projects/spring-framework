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

package org.springframework.http.codec.cbor;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.testfixture.codec.AbstractDecoderTests;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.MimeType;
import org.springframework.web.testfixture.xml.Pojo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * Tests for {@link Jackson2CborDecoder}.
 *
 * @author Sebastien Deleuze
 */
@SuppressWarnings("removal")
class Jackson2CborDecoderTests extends AbstractDecoderTests<Jackson2CborDecoder> {

	private static final MimeType CBOR_MIME_TYPE = new MimeType("application", "cbor");

	private Pojo pojo1 = new Pojo("f1", "b1");

	private Pojo pojo2 = new Pojo("f2", "b2");

	private ObjectMapper mapper = Jackson2ObjectMapperBuilder.cbor().build();

	public Jackson2CborDecoderTests() {
		super(new Jackson2CborDecoder());
	}

	@Override
	@Test
	protected void canDecode() {
		assertThat(decoder.canDecode(ResolvableType.forClass(Pojo.class), CBOR_MIME_TYPE)).isTrue();
		assertThat(decoder.canDecode(ResolvableType.forClass(Pojo.class), null)).isTrue();

		assertThat(decoder.canDecode(ResolvableType.forClass(String.class), null)).isFalse();
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
		catch (JsonProcessingException e) {
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
