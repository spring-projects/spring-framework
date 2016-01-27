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

import java.nio.charset.StandardCharsets;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;

import static org.junit.Assert.assertEquals;

/**
 * @author Sebastien Deleuze
 */
public class JsonObjectEncoderTests extends AbstractAllocatingTestCase {

	private JsonObjectEncoder encoder;

	@Before
	public void createEncoder() {
		encoder = new JsonObjectEncoder(allocator);
	}

	@Test
	public void encodeSingleElementFlux() throws InterruptedException {
		Flux<DataBuffer> source =
				Flux.just(stringBuffer("{\"foo\": \"foofoo\", \"bar\": \"barbar\"}"));
		Iterable<String> results = Flux.from(encoder.encode(source, null, null)).map(chunk -> {
			byte[] b = new byte[chunk.readableByteCount()];
			chunk.read(b);
			return new String(b, StandardCharsets.UTF_8);
		}).toIterable();
		String result = String.join("", results);
		assertEquals("[{\"foo\": \"foofoo\", \"bar\": \"barbar\"}]", result);
	}


	@Test
	public void encodeSingleElementMono() throws InterruptedException {
		Mono<DataBuffer> source =
				Mono.just(stringBuffer("{\"foo\": \"foofoo\", \"bar\": \"barbar\"}"));
		Iterable<String> results = Flux.from(encoder.encode(source, null, null)).map(chunk -> {
			byte[] b = new byte[chunk.readableByteCount()];
			chunk.read(b);
			return new String(b, StandardCharsets.UTF_8);
		}).toIterable();
		String result = String.join("", results);
		assertEquals("{\"foo\": \"foofoo\", \"bar\": \"barbar\"}", result);
	}

	@Test
	public void encodeTwoElementsFlux() throws InterruptedException {
		Flux<DataBuffer> source =
				Flux.just(stringBuffer("{\"foo\": \"foofoo\", \"bar\": \"barbar\"}"),
						stringBuffer("{\"foo\": \"foofoofoo\", \"bar\": \"barbarbar\"}"));
		Iterable<String> results = Flux.from(encoder.encode(source, null, null)).map(chunk -> {
			byte[] b = new byte[chunk.readableByteCount()];
			chunk.read(b);
			return new String(b, StandardCharsets.UTF_8);
		}).toIterable();
		String result = String.join("", results);
		assertEquals("[{\"foo\": \"foofoo\", \"bar\": \"barbar\"},{\"foo\": \"foofoofoo\", \"bar\": \"barbarbar\"}]", result);
	}

	@Test
	public void encodeThreeElementsFlux() throws InterruptedException {
		Flux<DataBuffer> source =
				Flux.just(stringBuffer("{\"foo\": \"foofoo\", \"bar\": \"barbar\"}"),
						stringBuffer("{\"foo\": \"foofoofoo\", \"bar\": \"barbarbar\"}"),
						stringBuffer(
								"{\"foo\": \"foofoofoofoo\", \"bar\": \"barbarbarbar\"}")
		);
		Iterable<String> results = Flux.from(encoder.encode(source, null, null)).map(chunk -> {
			byte[] b = new byte[chunk.readableByteCount()];
			chunk.read(b);
			return new String(b, StandardCharsets.UTF_8);
		}).toIterable();
		String result = String.join("", results);
		assertEquals("[{\"foo\": \"foofoo\", \"bar\": \"barbar\"},{\"foo\": \"foofoofoo\", \"bar\": \"barbarbar\"},{\"foo\": \"foofoofoofoo\", \"bar\": \"barbarbarbar\"}]", result);
	}

}
