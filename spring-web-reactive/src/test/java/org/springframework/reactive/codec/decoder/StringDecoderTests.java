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

package org.springframework.reactive.codec.decoder;

import static java.util.stream.Collectors.*;
import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.stream.StreamSupport;

import org.junit.Test;
import reactor.Flux;
import reactor.Mono;
import reactor.core.publisher.convert.RxJava1SingleConverter;
import reactor.io.buffer.Buffer;
import rx.Single;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.support.StringDecoder;
import org.springframework.http.MediaType;

/**
 * @author Sebastien Deleuze
 * @author Brian Clozel
 */
public class StringDecoderTests {

	private final StringDecoder decoder = new StringDecoder();

	@Test
	public void canDecode() {
		assertTrue(decoder.canDecode(ResolvableType.forClass(String.class), MediaType.TEXT_PLAIN));
		assertFalse(decoder.canDecode(ResolvableType.forClass(Integer.class), MediaType.TEXT_PLAIN));
		assertFalse(decoder.canDecode(ResolvableType.forClass(String.class), MediaType.APPLICATION_JSON));
	}

	@Test
	public void decode() throws InterruptedException {
		Flux<ByteBuffer> source = Flux.just(Buffer.wrap("foo").byteBuffer(), Buffer.wrap("bar").byteBuffer());
		Flux<String> output = this.decoder.decode(source, ResolvableType.forClassWithGenerics(Flux.class, String.class), null);
		List<String> results = StreamSupport.stream(output.toIterable().spliterator(), false).collect(toList());
		assertEquals(1, results.size());
		assertEquals("foobar", results.get(0));
	}

	@Test
	public void decodeDoNotBuffer() throws InterruptedException {
		StringDecoder decoder = new StringDecoder(false);
		Flux<ByteBuffer> source = Flux.just(Buffer.wrap("foo").byteBuffer(), Buffer.wrap("bar").byteBuffer());
		Flux<String> output = decoder.decode(source, ResolvableType.forClassWithGenerics(Flux.class, String.class), null);
		List<String> results = StreamSupport.stream(output.toIterable().spliterator(), false).collect(toList());
		assertEquals(2, results.size());
		assertEquals("foo", results.get(0));
		assertEquals("bar", results.get(1));
	}

	@Test
	public void decodeMono() throws InterruptedException {
		Flux<ByteBuffer> source = Flux.just(Buffer.wrap("foo").byteBuffer(), Buffer.wrap("bar").byteBuffer());
		Mono<String> mono = Mono.from(this.decoder.decode(source,
				ResolvableType.forClassWithGenerics(Mono.class, String.class),
				MediaType.TEXT_PLAIN));
		String result = mono.get();
		assertEquals("foobar", result);
	}

	@Test
	public void decodeSingle() throws InterruptedException {
		Flux<ByteBuffer> source = Flux.just(Buffer.wrap("foo").byteBuffer(), Buffer.wrap("bar").byteBuffer());
		Single<String> single = RxJava1SingleConverter.from(this.decoder.decode(source,
				ResolvableType.forClassWithGenerics(Single.class, String.class),
				MediaType.TEXT_PLAIN));
		String result = single.toBlocking().value();
		assertEquals("foobar", result);
	}

}
