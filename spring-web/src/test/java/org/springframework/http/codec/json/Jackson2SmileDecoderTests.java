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

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.CodecException;
import org.springframework.core.io.buffer.AbstractDataBufferAllocatingTestCase;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.codec.Pojo;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.MimeType;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.core.ResolvableType.forClass;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * Unit tests for {@link Jackson2SmileDecoder}.
 *
 * @author Sebastien Deleuze
 */
public class Jackson2SmileDecoderTests extends AbstractDataBufferAllocatingTestCase {

	private final static MimeType SMILE_MIME_TYPE = new MimeType("application", "x-jackson-smile");
	private final static MimeType STREAM_SMILE_MIME_TYPE = new MimeType("application", "stream+x-jackson-smile");

	private final Jackson2SmileDecoder decoder = new Jackson2SmileDecoder();

	@Test
	public void canDecode() {
		assertTrue(decoder.canDecode(forClass(Pojo.class), SMILE_MIME_TYPE));
		assertTrue(decoder.canDecode(forClass(Pojo.class), STREAM_SMILE_MIME_TYPE));
		assertTrue(decoder.canDecode(forClass(Pojo.class), null));

		assertFalse(decoder.canDecode(forClass(String.class), null));
		assertFalse(decoder.canDecode(forClass(Pojo.class), APPLICATION_JSON));
	}

	@Test
	public void decodePojo() throws Exception {
		ObjectMapper mapper = Jackson2ObjectMapperBuilder.smile().build();
		Pojo pojo = new Pojo("foo", "bar");
		byte[] serializedPojo = mapper.writer().writeValueAsBytes(pojo);
		
		Flux<DataBuffer> source = Flux.just(this.bufferFactory.wrap(serializedPojo));
		ResolvableType elementType = forClass(Pojo.class);
		Flux<Object> flux = decoder.decode(source, elementType, null, emptyMap());

		StepVerifier.create(flux)
				.expectNext(pojo)
				.verifyComplete();
	}

	@Test
	public void decodePojoWithError() throws Exception {
		Flux<DataBuffer> source = Flux.just(stringBuffer("123"));
		ResolvableType elementType = forClass(Pojo.class);
		Flux<Object> flux = decoder.decode(source, elementType, null, emptyMap());

		StepVerifier.create(flux).verifyError(CodecException.class);
	}

	@Test
	public void decodeToList() throws Exception {
		ObjectMapper mapper = Jackson2ObjectMapperBuilder.smile().build();
		List<Pojo> list = asList(new Pojo("f1", "b1"), new Pojo("f2", "b2"));
		byte[] serializedList = mapper.writer().writeValueAsBytes(list);
		Flux<DataBuffer> source = Flux.just(this.bufferFactory.wrap(serializedList));

		ResolvableType elementType = ResolvableType.forClassWithGenerics(List.class, Pojo.class);
		Mono<Object> mono = decoder.decodeToMono(source, elementType, null, emptyMap());

		StepVerifier.create(mono)
				.expectNext(list)
				.expectComplete()
				.verify();
	}

	@Test
	public void decodeListToFlux() throws Exception {
		ObjectMapper mapper = Jackson2ObjectMapperBuilder.smile().build();
		List<Pojo> list = asList(new Pojo("f1", "b1"), new Pojo("f2", "b2"));
		byte[] serializedList = mapper.writer().writeValueAsBytes(list);
		Flux<DataBuffer> source = Flux.just(this.bufferFactory.wrap(serializedList));

		ResolvableType elementType = forClass(Pojo.class);
		Flux<Object> flux = decoder.decode(source, elementType, null, emptyMap());

		StepVerifier.create(flux)
				.expectNext(new Pojo("f1", "b1"))
				.expectNext(new Pojo("f2", "b2"))
				.verifyComplete();
	}

	@Test
	public void decodeStreamToFlux() throws Exception {
		ObjectMapper mapper = Jackson2ObjectMapperBuilder.smile().build();
		List<Pojo> list = asList(new Pojo("f1", "b1"), new Pojo("f2", "b2"));
		byte[] serializedList = mapper.writer().writeValueAsBytes(list);
		Flux<DataBuffer> source = Flux.just(this.bufferFactory.wrap(serializedList));

		ResolvableType elementType = forClass(Pojo.class);
		Flux<Object> flux = decoder.decode(source, elementType, STREAM_SMILE_MIME_TYPE, emptyMap());

		StepVerifier.create(flux)
				.expectNext(new Pojo("f1", "b1"))
				.expectNext(new Pojo("f2", "b2"))
				.verifyComplete();
	}

}
