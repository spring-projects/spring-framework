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

package org.springframework.http.codec.cbor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import reactor.core.publisher.Flux;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.AbstractLeakCheckingTestCase;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.support.DataBufferTestUtils;
import org.springframework.http.codec.Pojo;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.MimeType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.core.io.buffer.DataBufferUtils.release;
import static org.springframework.http.MediaType.APPLICATION_XML;

/**
 * Unit tests for {@link Jackson2CborEncoder}.
 *
 * @author Sebastien Deleuze
 */
public class Jackson2CborEncoderTests extends AbstractLeakCheckingTestCase {

	private final static MimeType CBOR_MIME_TYPE = new MimeType("application", "cbor");

	private final ObjectMapper mapper = Jackson2ObjectMapperBuilder.cbor().build();

	private final Jackson2CborEncoder encoder = new Jackson2CborEncoder();

	private Consumer<DataBuffer> pojoConsumer(Pojo expected) {
		return dataBuffer -> {
			try {
				Pojo actual = this.mapper.reader().forType(Pojo.class)
						.readValue(DataBufferTestUtils.dumpBytes(dataBuffer));
				assertEquals(expected, actual);
				release(dataBuffer);
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		};
	}

	@Test
	public void canEncode() {
		ResolvableType pojoType = ResolvableType.forClass(Pojo.class);
		assertTrue(this.encoder.canEncode(pojoType, CBOR_MIME_TYPE));
		assertTrue(this.encoder.canEncode(pojoType, null));

		// SPR-15464
		assertTrue(this.encoder.canEncode(ResolvableType.NONE, null));
	}

	@Test
	public void canNotEncode() {
		assertFalse(this.encoder.canEncode(ResolvableType.forClass(String.class), null));
		assertFalse(this.encoder.canEncode(ResolvableType.forClass(Pojo.class), APPLICATION_XML));

		ResolvableType sseType = ResolvableType.forClass(ServerSentEvent.class);
		assertFalse(this.encoder.canEncode(sseType, CBOR_MIME_TYPE));
	}

	@Test
	public void encode() {
		Pojo value = new Pojo("foo", "bar");
		DataBuffer result = encoder.encodeValue(value, this.bufferFactory, ResolvableType.forClass(Pojo.class), CBOR_MIME_TYPE, null);
		pojoConsumer(value).accept(result);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void encodeStream() {
		Pojo pojo1 = new Pojo("foo", "bar");
		Pojo pojo2 = new Pojo("foofoo", "barbar");
		Pojo pojo3 = new Pojo("foofoofoo", "barbarbar");
		Flux<Pojo> input = Flux.just(pojo1, pojo2, pojo3);
		ResolvableType type = ResolvableType.forClass(Pojo.class);
		encoder.encode(input, this.bufferFactory, type, CBOR_MIME_TYPE, null);
	}
}
