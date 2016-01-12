/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.reactive.codec.decoder;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import reactor.Flux;
import reactor.io.buffer.Buffer;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.support.JacksonJsonDecoder;
import org.springframework.http.MediaType;
import org.springframework.reactive.codec.Pojo;

/**
 * @author Sebastien Deleuze
 */
public class JacksonJsonDecoderTests {

	private final JacksonJsonDecoder decoder = new JacksonJsonDecoder();

	@Test
	public void canDecode() {
		assertTrue(decoder.canDecode(null, MediaType.APPLICATION_JSON));
		assertFalse(decoder.canDecode(null, MediaType.APPLICATION_XML));
	}

	@Test
	public void decode() throws InterruptedException {
		Flux<ByteBuffer> source = Flux.just(Buffer.wrap("{\"foo\": \"foofoo\", \"bar\": \"barbar\"}").byteBuffer());
		Flux<Object> output = decoder.decode(source, ResolvableType.forClass(Pojo.class), null);
		List<Object> results = StreamSupport.stream(output.toIterable().spliterator(), false).collect(toList());
		assertEquals(1, results.size());
		assertEquals("foofoo", ((Pojo) results.get(0)).getFoo());
	}

}
