/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.core.codec.support;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.test.TestSubscriber;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.AbstractDataBufferAllocatingTestCase;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link JacksonJsonDecoder}.
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 */
public class JacksonJsonDecoderTests extends AbstractDataBufferAllocatingTestCase {

	@Test
	public void canDecode() {
		JacksonJsonDecoder decoder = new JacksonJsonDecoder();

		assertTrue(decoder.canDecode(null, MediaType.APPLICATION_JSON));
		assertFalse(decoder.canDecode(null, MediaType.APPLICATION_XML));
	}

	@Test
	public void decodePojo() {
		Flux<DataBuffer> source = Flux.just(stringBuffer("{\"foo\": \"foofoo\", \"bar\": \"barbar\"}"));
		ResolvableType elementType = ResolvableType.forClass(Pojo.class);
		Flux<Object> flux = new JacksonJsonDecoder().decode(source, elementType, null);

		TestSubscriber.subscribe(flux).assertNoError().assertComplete().
				assertValues(new Pojo("foofoo", "barbar"));
	}

	@Test
	@Ignore // Issues 112 (no generic type), otherwise works
	public void decodeToListWithoutObjectDecoder() throws Exception {
		Flux<DataBuffer> source = Flux.just(stringBuffer(
				"[{\"bar\":\"b1\",\"foo\":\"f1\"},{\"bar\":\"b2\",\"foo\":\"f2\"}]"));

		Method method = getClass().getDeclaredMethod("handle", List.class);
		ResolvableType elementType = ResolvableType.forMethodParameter(method, 0);
		Flux<Object> flux = new JacksonJsonDecoder().decode(source, elementType, null);

		TestSubscriber.subscribe(flux).assertNoError().assertComplete().
				assertValues(Arrays.asList(new Pojo("f1", "b1"), new Pojo("f2", "b2")));
	}

	@Test
	@Ignore // Issue 109
	public void decodeToFluxWithoutObjectDecoder() throws Exception {
		Flux<DataBuffer> source = Flux.just(stringBuffer(
				"[{\"bar\":\"b1\",\"foo\":\"f1\"},{\"bar\":\"b2\",\"foo\":\"f2\"}]"));

		ResolvableType elementType = ResolvableType.forClass(Pojo.class);
		Flux<Object> flux = new JacksonJsonDecoder().decode(source, elementType, null);

		TestSubscriber.subscribe(flux).assertNoError().assertComplete().
				assertValues(new Pojo("f1", "b1"), new Pojo("f2", "b2"));
	}

	@Test
	@Ignore // Issue 109
	public void decodeToListWithObjectDecoder() throws Exception {
		Flux<DataBuffer> source = Flux.just(stringBuffer(
				"[{\"bar\":\"b1\",\"foo\":\"f1\"},{\"bar\":\"b2\",\"foo\":\"f2\"}]"));

		Method method = getClass().getDeclaredMethod("handle", List.class);
		ResolvableType elementType = ResolvableType.forMethodParameter(method, 0);
		Flux<Object> flux = new JacksonJsonDecoder(new JsonObjectDecoder()).decode(source, elementType, null);

		TestSubscriber.subscribe(flux).assertNoError().assertComplete().
				assertValues(Arrays.asList(new Pojo("f1", "b1"), new Pojo("f2", "b2")));
	}

	@Test
	public void decodeToFluxWithObjectDecoder() throws Exception {
		Flux<DataBuffer> source = Flux.just(stringBuffer(
				"[{\"bar\":\"b1\",\"foo\":\"f1\"},{\"bar\":\"b2\",\"foo\":\"f2\"}]"));

		ResolvableType elementType = ResolvableType.forClass(Pojo.class);
		Flux<Object> flux = new JacksonJsonDecoder(new JsonObjectDecoder()).decode(source, elementType, null);

		TestSubscriber.subscribe(flux).assertNoError().assertComplete().
				assertValues(new Pojo("f1", "b1"), new Pojo("f2", "b2"));
	}

	@SuppressWarnings("unused")
	void handle(List<Pojo> list) {
	}


}
