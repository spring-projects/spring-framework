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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.AbstractDataBufferAllocatingTestCase;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.support.DataBufferTestUtils;
import org.springframework.http.codec.Pojo;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.MimeType;

import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_XML;

/**
 * Unit tests for {@link Jackson2SmileEncoder}.
 * 
 * @author Sebastien Deleuze
 */
public class Jackson2SmileEncoderTests extends AbstractDataBufferAllocatingTestCase {

	private final static MimeType SMILE_MIME_TYPE = new MimeType("application", "x-jackson-smile");
	private final static MimeType STREAM_SMILE_MIME_TYPE = new MimeType("application", "stream+x-jackson-smile");
	
	private final Jackson2SmileEncoder encoder = new Jackson2SmileEncoder();


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
	public void encode() throws Exception {
		Flux<Pojo> source = Flux.just(
				new Pojo("foo", "bar"),
				new Pojo("foofoo", "barbar"),
				new Pojo("foofoofoo", "barbarbar")
		);
		ResolvableType type = ResolvableType.forClass(Pojo.class);
		Flux<DataBuffer> output = this.encoder.encode(source, this.bufferFactory, type, null, emptyMap());

		ObjectMapper mapper = Jackson2ObjectMapperBuilder.smile().build();
		StepVerifier.create(output)
				.consumeNextWith(dataBuffer -> readPojo(mapper, List.class, dataBuffer))
				.verifyComplete();
	}

	@Test
	public void encodeAsStream() throws Exception {
		Flux<Pojo> source = Flux.just(
				new Pojo("foo", "bar"),
				new Pojo("foofoo", "barbar"),
				new Pojo("foofoofoo", "barbarbar")
		);
		ResolvableType type = ResolvableType.forClass(Pojo.class);
		Flux<DataBuffer> output = this.encoder.encode(source, this.bufferFactory, type, STREAM_SMILE_MIME_TYPE, emptyMap());

		ObjectMapper mapper = Jackson2ObjectMapperBuilder.smile().build();
		StepVerifier.create(output)
				.consumeNextWith(dataBuffer -> readPojo(mapper, Pojo.class, dataBuffer))
				.consumeNextWith(dataBuffer -> readPojo(mapper, Pojo.class, dataBuffer))
				.consumeNextWith(dataBuffer -> readPojo(mapper, Pojo.class, dataBuffer))
				.verifyComplete();
	}
	
	public <T> T readPojo(ObjectMapper mapper, Class<T> valueType, DataBuffer dataBuffer) {
		try {
			T value = mapper.reader().forType(valueType).readValue(DataBufferTestUtils.dumpBytes(dataBuffer));
			DataBufferUtils.release(dataBuffer);
			return value;
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

}
