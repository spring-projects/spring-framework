/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.AbstractEncoderTestCase;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.support.DataBufferTestUtils;
import org.springframework.http.codec.Pojo;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.MimeType;

import static org.junit.Assert.*;
import static org.springframework.http.MediaType.APPLICATION_XML;

/**
 * Unit tests for {@link Jackson2SmileEncoder}.
 *
 * @author Sebastien Deleuze
 */
public class Jackson2SmileEncoderTests extends AbstractEncoderTestCase<Object, Jackson2SmileEncoder> {

	private final static MimeType SMILE_MIME_TYPE = new MimeType("application", "x-jackson-smile");
	private final static MimeType STREAM_SMILE_MIME_TYPE = new MimeType("application", "stream+x-jackson-smile");

	private final Jackson2SmileEncoder encoder = new Jackson2SmileEncoder();

	private final ObjectMapper mapper = Jackson2ObjectMapperBuilder.smile().build();

	private Pojo pojo1 = new Pojo("foo", "bar");

	private Pojo pojo2 = new Pojo("foofoo", "barbar");

	private Pojo pojo3 = new Pojo("foofoofoo", "barbarbar");

	public Jackson2SmileEncoderTests() {
		super(new Jackson2SmileEncoder(), ResolvableType.forClass(Pojo.class),
				STREAM_SMILE_MIME_TYPE, null);

	}

	@Override
	protected Flux<Object> input() {
		return Flux.just(this.pojo1, this.pojo2, this.pojo3);
	}

	@Override
	protected Stream<Consumer<DataBuffer>> outputConsumers() {
		return Stream.<Consumer<DataBuffer>>builder()
				.add(pojoConsumer(this.pojo1))
				.add(pojoConsumer(this.pojo2))
				.add(pojoConsumer(this.pojo3))
				.build();
	}

	public Consumer<DataBuffer> pojoConsumer(Pojo expected) {
		return dataBuffer -> {
			try {
				Pojo actual = this.mapper.reader().forType(Pojo.class)
						.readValue(DataBufferTestUtils.dumpBytes(dataBuffer));
				assertEquals(expected, actual);
				DataBufferUtils.release(dataBuffer);
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		};
	}


	@Test
	public void canEncode() {
		ResolvableType pojoType = ResolvableType.forClass(Pojo.class);
		assertTrue(this.encoder.canEncode(pojoType, SMILE_MIME_TYPE));
		assertTrue(this.encoder.canEncode(pojoType, STREAM_SMILE_MIME_TYPE));
		assertTrue(this.encoder.canEncode(pojoType, null));

		// SPR-15464
		assertTrue(this.encoder.canEncode(ResolvableType.NONE, null));
	}

	@Test
	public void canNotEncode() {
		assertFalse(this.encoder.canEncode(ResolvableType.forClass(String.class), null));
		assertFalse(this.encoder.canEncode(ResolvableType.forClass(Pojo.class), APPLICATION_XML));

		ResolvableType sseType = ResolvableType.forClass(ServerSentEvent.class);
		assertFalse(this.encoder.canEncode(sseType, SMILE_MIME_TYPE));
	}

	@Test
	public void encodeNonStream() {
		Flux<DataBuffer> output = this.encoder.encode(input(), this.bufferFactory, elementType,
				null, null);

		ObjectMapper mapper = Jackson2ObjectMapperBuilder.smile().build();
		StepVerifier.create(output)
				.consumeNextWith(dataBuffer -> {
					try {
						CollectionType type = mapper.getTypeFactory()
								.constructCollectionType(List.class, Pojo.class);
						List<Pojo> value = mapper.reader().forType(type)
								.readValue(dataBuffer.asInputStream());
						assertEquals(3, value.size());
						assertEquals(pojo1, value.get(0));
						assertEquals(pojo2, value.get(1));
						assertEquals(pojo3, value.get(2));
					}
					catch (IOException ex) {
						throw new UncheckedIOException(ex);
					}
					finally {
						DataBufferUtils.release(dataBuffer);
					}
				})
				.verifyComplete();
	}



}
