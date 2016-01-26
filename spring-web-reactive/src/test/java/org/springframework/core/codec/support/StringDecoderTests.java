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

import java.util.List;
import java.util.stream.StreamSupport;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.converter.RxJava1SingleConverter;
import reactor.io.buffer.Buffer;
import rx.Single;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;

/**
 * @author Sebastien Deleuze
 * @author Brian Clozel
 */
public class StringDecoderTests extends AbstractAllocatingTestCase {

	private StringDecoder decoder;

	@Before
	public void createEncoder() {
		decoder = new StringDecoder(allocator);
	}


	@Test
	public void canDecode() {
		assertTrue(decoder.canDecode(ResolvableType.forClass(String.class), MediaType.TEXT_PLAIN));
		assertFalse(decoder.canDecode(ResolvableType.forClass(Integer.class), MediaType.TEXT_PLAIN));
		assertFalse(decoder.canDecode(ResolvableType.forClass(String.class), MediaType.APPLICATION_JSON));
	}

	@Test
	public void decode() throws InterruptedException {
		Flux<DataBuffer> source = Flux.just(stringBuffer("foo"), stringBuffer("bar"));
		Flux<String> output = this.decoder.decode(source, ResolvableType.forClassWithGenerics(Flux.class, String.class), null);
		List<String> results = StreamSupport.stream(output.toIterable().spliterator(), false).collect(toList());
		assertEquals(1, results.size());
		assertEquals("foobar", results.get(0));
	}

	@Test
	public void decodeDoNotBuffer() throws InterruptedException {
		StringDecoder decoder = new StringDecoder(allocator, false);
		Flux<DataBuffer> source = Flux.just(stringBuffer("foo"), stringBuffer("bar"));
		Flux<String> output = decoder.decode(source, ResolvableType.forClassWithGenerics(Flux.class, String.class), null);
		List<String> results = StreamSupport.stream(output.toIterable().spliterator(), false).collect(toList());
		assertEquals(2, results.size());
		assertEquals("foo", results.get(0));
		assertEquals("bar", results.get(1));
	}

	@Test
	public void decodeMono() throws InterruptedException {
		Flux<DataBuffer> source = Flux.just(stringBuffer("foo"), stringBuffer("bar"));
		Mono<String> mono = Mono.from(this.decoder.decode(source,
				ResolvableType.forClassWithGenerics(Mono.class, String.class),
				MediaType.TEXT_PLAIN));
		String result = mono.get();
		assertEquals("foobar", result);
	}

	@Test
	public void decodeSingle() throws InterruptedException {
		Flux<DataBuffer> source = Flux.just(stringBuffer("foo"), stringBuffer("bar"));
		Single<String> single = RxJava1SingleConverter.from(this.decoder.decode(source,
				ResolvableType.forClassWithGenerics(Single.class, String.class),
				MediaType.TEXT_PLAIN));
		String result = single.toBlocking().value();
		assertEquals("foobar", result);
	}

}
