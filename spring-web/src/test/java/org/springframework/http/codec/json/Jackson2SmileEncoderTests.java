/*
 * Copyright 2002-2022 the original author or authors.
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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.testfixture.codec.AbstractEncoderTests;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.MimeType;
import org.springframework.web.testfixture.xml.Pojo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.core.io.buffer.DataBufferUtils.release;
import static org.springframework.http.MediaType.APPLICATION_XML;

/**
 * Unit tests for {@link Jackson2SmileEncoder}.
 *
 * @author Sebastien Deleuze
 */
public class Jackson2SmileEncoderTests extends AbstractEncoderTests<Jackson2SmileEncoder> {

	private final static MimeType SMILE_MIME_TYPE = new MimeType("application", "x-jackson-smile");
	private final static MimeType STREAM_SMILE_MIME_TYPE = new MimeType("application", "stream+x-jackson-smile");

	private final Jackson2SmileEncoder encoder = new Jackson2SmileEncoder();

	private final ObjectMapper mapper = Jackson2ObjectMapperBuilder.smile().build();

	public Jackson2SmileEncoderTests() {
		super(new Jackson2SmileEncoder());

	}

	@Override
	@Test
	public void canEncode() {
		ResolvableType pojoType = ResolvableType.forClass(Pojo.class);
		assertThat(this.encoder.canEncode(pojoType, SMILE_MIME_TYPE)).isTrue();
		assertThat(this.encoder.canEncode(pojoType, STREAM_SMILE_MIME_TYPE)).isTrue();
		assertThat(this.encoder.canEncode(pojoType, null)).isTrue();

		// SPR-15464
		assertThat(this.encoder.canEncode(ResolvableType.NONE, null)).isTrue();
	}

	@Test
	public void canNotEncode() {
		assertThat(this.encoder.canEncode(ResolvableType.forClass(String.class), null)).isFalse();
		assertThat(this.encoder.canEncode(ResolvableType.forClass(Pojo.class), APPLICATION_XML)).isFalse();

		ResolvableType sseType = ResolvableType.forClass(ServerSentEvent.class);
		assertThat(this.encoder.canEncode(sseType, SMILE_MIME_TYPE)).isFalse();
	}

	@Override
	@Test
	public void encode() {
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
					catch (IOException e) {
						throw new UncheckedIOException(e);
					}
					finally {
						release(dataBuffer);
					}
				}));
	}

	@Test
	public void encodeError() {
		Mono<Pojo> input = Mono.error(new InputException());
		testEncode(input, Pojo.class, step -> step.expectError(InputException.class).verify());
	}

	@Test
	public void encodeAsStream() {
		Pojo pojo1 = new Pojo("foo", "bar");
		Pojo pojo2 = new Pojo("foofoo", "barbar");
		Pojo pojo3 = new Pojo("foofoofoo", "barbarbar");
		Flux<Pojo> input = Flux.just(pojo1, pojo2, pojo3);
		ResolvableType type = ResolvableType.forClass(Pojo.class);

		Flux<DataBuffer> result = this.encoder
				.encode(input, bufferFactory, type, STREAM_SMILE_MIME_TYPE, null);

		Mono<MappingIterator<Pojo>> joined = DataBufferUtils.join(result)
				.map(buffer -> {
					try {
						return this.mapper.reader().forType(Pojo.class).readValues(buffer.asInputStream(true));
					}
					catch (IOException ex) {
						throw new UncheckedIOException(ex);
					}
				});

		StepVerifier.create(joined)
				.assertNext(iter -> assertThat(iter).toIterable().contains(pojo1, pojo2, pojo3))
				.verifyComplete();
	}

}
