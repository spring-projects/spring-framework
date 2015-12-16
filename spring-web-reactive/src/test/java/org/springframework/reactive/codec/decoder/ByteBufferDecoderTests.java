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

import static org.junit.Assert.*;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.io.buffer.Buffer;
import reactor.rx.Stream;
import reactor.rx.Streams;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.support.ByteBufferDecoder;
import org.springframework.http.MediaType;

/**
 * @author Sebastien Deleuze
 */
public class ByteBufferDecoderTests {

	private final ByteBufferDecoder decoder = new ByteBufferDecoder();

	@Test
	public void canDecode() {
		assertTrue(decoder.canDecode(ResolvableType.forClass(ByteBuffer.class), MediaType.TEXT_PLAIN));
		assertFalse(decoder.canDecode(ResolvableType.forClass(Integer.class), MediaType.TEXT_PLAIN));
		assertTrue(decoder.canDecode(ResolvableType.forClass(ByteBuffer.class), MediaType.APPLICATION_JSON));
	}

	@Test
	public void decode() throws InterruptedException {
		ByteBuffer fooBuffer = Buffer.wrap("foo").byteBuffer();
		ByteBuffer barBuffer = Buffer.wrap("bar").byteBuffer();
		Stream<ByteBuffer> source = Streams.just(fooBuffer, barBuffer);
		List<ByteBuffer> results = Streams.from(decoder.decode(source,
				ResolvableType.forClassWithGenerics(Publisher.class, ByteBuffer.class), null)).toList().get();
		assertEquals(2, results.size());
		assertEquals(fooBuffer, results.get(0));
		assertEquals(barBuffer, results.get(1));
	}

}
