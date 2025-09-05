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

package org.springframework.http.codec.smile;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tools.jackson.databind.MappingIterator;
import tools.jackson.dataformat.smile.SmileMapper;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.testfixture.codec.AbstractEncoderTests;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.util.MimeType;
import org.springframework.web.testfixture.xml.Pojo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.core.io.buffer.DataBufferUtils.release;
import static org.springframework.http.MediaType.APPLICATION_XML;

/**
 * Tests for {@link JacksonSmileEncoder}.
 *
 * @author Sebastien Deleuze
 * @since 7.0
 */
class JacksonSmileEncoderTests extends AbstractEncoderTests<JacksonSmileEncoder> {

	private static final MimeType SMILE_MIME_TYPE = new MimeType("application", "x-jackson-smile");
	private static final MimeType STREAM_SMILE_MIME_TYPE = new MimeType("application", "stream+x-jackson-smile");

	private final SmileMapper mapper = SmileMapper.builder().build();


	JacksonSmileEncoderTests() {
		super(new JacksonSmileEncoder());
	}


	@Test
	@Override
	protected void canEncode() {
		ResolvableType pojoType = ResolvableType.forClass(Pojo.class);
		assertThat(this.encoder.canEncode(pojoType, SMILE_MIME_TYPE)).isTrue();
		assertThat(this.encoder.canEncode(pojoType, STREAM_SMILE_MIME_TYPE)).isTrue();
		assertThat(this.encoder.canEncode(pojoType, null)).isTrue();

		// SPR-15464
		assertThat(this.encoder.canEncode(ResolvableType.NONE, null)).isTrue();
	}

	@Test
	void cannotEncode() {
		assertThat(this.encoder.canEncode(ResolvableType.forClass(String.class), null)).isFalse();
		assertThat(this.encoder.canEncode(ResolvableType.forClass(Pojo.class), APPLICATION_XML)).isFalse();
	}

	@Test
	@Disabled("Determine why this fails with JacksonSmileEncoder but passes with Jackson2SmileEncoder")
	void cannotEncodeServerSentEvent() {
		ResolvableType sseType = ResolvableType.forClass(ServerSentEvent.class);
		assertThat(this.encoder.canEncode(sseType, SMILE_MIME_TYPE)).isFalse();
	}

	@Test
	@Override
	protected void encode() {
		List<Pojo> list = Arrays.asList(
				new Pojo("foo", "bar"),
				new Pojo("foofoo", "barbar"),
				new Pojo("foofoofoo", "barbarbar"));

		Flux<Pojo> input = Flux.fromIterable(list);

		testEncode(input, Pojo.class, step -> step
				.consumeNextWith(dataBuffer -> {
					try {
						Object actual = this.mapper.reader().forType(List.class)
								.readValue(dataBuffer.asInputStream());
						assertThat(actual).isEqualTo(list);
					}
					finally {
						release(dataBuffer);
					}
				}));
	}

	@Test
	void encodeError() {
		Mono<Pojo> input = Mono.error(new InputException());
		testEncode(input, Pojo.class, step -> step.expectError(InputException.class).verify());
	}

	@Test
	void encodeAsStream() {
		Pojo pojo1 = new Pojo("foo", "bar");
		Pojo pojo2 = new Pojo("foofoo", "barbar");
		Pojo pojo3 = new Pojo("foofoofoo", "barbarbar");
		Flux<Pojo> input = Flux.just(pojo1, pojo2, pojo3);
		ResolvableType type = ResolvableType.forClass(Pojo.class);

		Flux<DataBuffer> result = this.encoder
				.encode(input, bufferFactory, type, STREAM_SMILE_MIME_TYPE, null);

		Mono<MappingIterator<Pojo>> joined = DataBufferUtils.join(result)
				.map(buffer -> this.mapper.reader().forType(Pojo.class).readValues(buffer.asInputStream(true)));

		StepVerifier.create(joined)
				.assertNext(iter -> assertThat(iter).toIterable().contains(pojo1, pojo2, pojo3))
				.verifyComplete();
	}

}
