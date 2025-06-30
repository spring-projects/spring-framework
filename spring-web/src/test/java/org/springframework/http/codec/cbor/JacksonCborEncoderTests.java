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

package org.springframework.http.codec.cbor;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
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

		// SPR-15464
		assertThat(this.encoder.canEncode(ResolvableType.NONE, null)).isTrue();
	}

	@Test
	void canNotEncode() {
		assertThat(this.encoder.canEncode(ResolvableType.forClass(String.class), null)).isFalse();
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
