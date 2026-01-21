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

package org.springframework.http.codec.xml;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import tools.jackson.core.JacksonException;
import tools.jackson.dataformat.xml.XmlMapper;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.testfixture.codec.AbstractDecoderTests;
import org.springframework.http.MediaType;
import org.springframework.web.testfixture.xml.Pojo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link JacksonXmlDecoder}.
 *
 * @author Sebastien Deleuze
 */
class JacksonXmlDecoderTests extends AbstractDecoderTests<JacksonXmlDecoder> {

	private final Pojo pojo1 = new Pojo("f1", "b1");

	private final Pojo pojo2 = new Pojo("f2", "b2");

	private final XmlMapper mapper = XmlMapper.builder().build();


	public JacksonXmlDecoderTests() {
		super(new JacksonXmlDecoder());
	}


	@Override
	@Test
	protected void canDecode() {
		assertThat(decoder.canDecode(ResolvableType.forClass(Pojo.class), MediaType.APPLICATION_XML)).isTrue();
		assertThat(decoder.canDecode(ResolvableType.forClass(Pojo.class), MediaType.TEXT_XML)).isTrue();
		assertThat(decoder.canDecode(ResolvableType.forClass(Pojo.class), new MediaType("application", "soap+xml"))).isTrue();
		assertThat(decoder.canDecode(ResolvableType.forClass(Pojo.class), null)).isTrue();
		assertThat(decoder.canDecode(ResolvableType.forClass(String.class), null)).isTrue();
		assertThat(decoder.canDecode(ResolvableType.forClass(Pojo.class), MediaType.APPLICATION_JSON)).isFalse();
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
