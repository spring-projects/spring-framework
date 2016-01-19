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

package org.springframework.reactive.codec.encoder;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.stream.StreamSupport;

import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.io.buffer.Buffer;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.support.ByteBufferEncoder;
import org.springframework.http.MediaType;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;

/**
 * @author Sebastien Deleuze
 */
public class ByteBufferEncoderTests {

	private final ByteBufferEncoder encoder = new ByteBufferEncoder();

	@Test
	public void canDecode() {
		assertTrue(encoder.canEncode(ResolvableType.forClass(ByteBuffer.class), MediaType.TEXT_PLAIN));
		assertFalse(encoder.canEncode(ResolvableType.forClass(Integer.class), MediaType.TEXT_PLAIN));
		assertTrue(encoder.canEncode(ResolvableType.forClass(ByteBuffer.class), MediaType.APPLICATION_JSON));
	}

	@Test
	public void decode() throws InterruptedException {
		ByteBuffer fooBuffer = Buffer.wrap("foo").byteBuffer();
		ByteBuffer barBuffer = Buffer.wrap("bar").byteBuffer();
		Flux<ByteBuffer> source = Flux.just(fooBuffer, barBuffer);
		Flux<ByteBuffer> output = encoder.encode(source, ResolvableType.forClassWithGenerics(Publisher.class, ByteBuffer.class), null);
		List<ByteBuffer> results = StreamSupport.stream(output.toIterable().spliterator(), false).collect(toList());
		assertEquals(2, results.size());
		assertEquals(fooBuffer, results.get(0));
		assertEquals(barBuffer, results.get(1));
	}

}
