/*
 * Copyright 2002-2019 the original author or authors.
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
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.AbstractEncoderTests;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.support.DataBufferTestUtils;
import org.springframework.http.codec.Pojo;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.MimeType;

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

	public Consumer<DataBuffer> pojoConsumer(Pojo expected) {
		return dataBuffer -> {
			try {
				Pojo actual = this.mapper.reader().forType(Pojo.class)
						.readValue(DataBufferTestUtils.dumpBytes(dataBuffer));
				assertThat(actual).isEqualTo(expected);
				release(dataBuffer);
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		};
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
				.consumeNextWith(expect(list, List.class)));
	}

	@Test
	public void encodeError() throws Exception {
		Mono<Pojo> input = Mono.error(new InputException());

		testEncode(input, Pojo.class, step -> step
				.expectError(InputException.class)
				.verify());

	}

	@Test
	public void encodeAsStream() throws Exception {
		Pojo pojo1 = new Pojo("foo", "bar");
		Pojo pojo2 = new Pojo("foofoo", "barbar");
		Pojo pojo3 = new Pojo("foofoofoo", "barbarbar");
		Flux<Pojo> input = Flux.just(pojo1, pojo2, pojo3);
		ResolvableType type = ResolvableType.forClass(Pojo.class);

		testEncodeAll(input, type, step -> step
				.consumeNextWith(expect(pojo1, Pojo.class))
				.consumeNextWith(expect(pojo2, Pojo.class))
				.consumeNextWith(expect(pojo3, Pojo.class))
				.verifyComplete(),
		STREAM_SMILE_MIME_TYPE, null);
	}


	private <T> Consumer<DataBuffer> expect(T expected, Class<T> expectedType) {
		return dataBuffer -> {
			try {
				Object actual = this.mapper.reader().forType(expectedType)
						.readValue(dataBuffer.asInputStream());
				assertThat(actual).isEqualTo(expected);
			}
			catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			finally {
				release(dataBuffer);
			}
		};

	}



}
