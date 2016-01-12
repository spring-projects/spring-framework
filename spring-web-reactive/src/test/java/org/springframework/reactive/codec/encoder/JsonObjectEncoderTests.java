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
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import reactor.Flux;
import reactor.Mono;
import reactor.io.buffer.Buffer;

import org.springframework.core.codec.support.JsonObjectEncoder;

/**
 * @author Sebastien Deleuze
 */
public class JsonObjectEncoderTests {

	@Test
	public void encodeSingleElementFlux() throws InterruptedException {
		JsonObjectEncoder encoder = new JsonObjectEncoder();
		Flux<ByteBuffer> source = Flux.just(Buffer.wrap("{\"foo\": \"foofoo\", \"bar\": \"barbar\"}").byteBuffer());
		Iterable<String> results = Flux.from(encoder.encode(source, null, null)).map(chunk -> {
			byte[] b = new byte[chunk.remaining()];
			chunk.get(b);
			return new String(b, StandardCharsets.UTF_8);
		}).toIterable();
		String result = String.join("", results);
		assertEquals("[{\"foo\": \"foofoo\", \"bar\": \"barbar\"}]", result);
	}

	@Test
	public void encodeSingleElementMono() throws InterruptedException {
		JsonObjectEncoder encoder = new JsonObjectEncoder();
		Mono<ByteBuffer> source = Mono.just(Buffer.wrap("{\"foo\": \"foofoo\", \"bar\": \"barbar\"}").byteBuffer());
		Iterable<String> results = Flux.from(encoder.encode(source, null, null)).map(chunk -> {
			byte[] b = new byte[chunk.remaining()];
			chunk.get(b);
			return new String(b, StandardCharsets.UTF_8);
		}).toIterable();
		String result = String.join("", results);
		assertEquals("{\"foo\": \"foofoo\", \"bar\": \"barbar\"}", result);
	}

	@Test
	public void encodeTwoElementsFlux() throws InterruptedException {
		JsonObjectEncoder encoder = new JsonObjectEncoder();
		Flux<ByteBuffer> source = Flux.just(
				Buffer.wrap("{\"foo\": \"foofoo\", \"bar\": \"barbar\"}").byteBuffer(),
				Buffer.wrap("{\"foo\": \"foofoofoo\", \"bar\": \"barbarbar\"}").byteBuffer());
		Iterable<String> results = Flux.from(encoder.encode(source, null, null)).map(chunk -> {
			byte[] b = new byte[chunk.remaining()];
			chunk.get(b);
			return new String(b, StandardCharsets.UTF_8);
		}).toIterable();
		String result = String.join("", results);
		assertEquals("[{\"foo\": \"foofoo\", \"bar\": \"barbar\"},{\"foo\": \"foofoofoo\", \"bar\": \"barbarbar\"}]", result);
	}

	@Test
	public void encodeThreeElementsFlux() throws InterruptedException {
		JsonObjectEncoder encoder = new JsonObjectEncoder();
		Flux<ByteBuffer> source = Flux.just(
				Buffer.wrap("{\"foo\": \"foofoo\", \"bar\": \"barbar\"}").byteBuffer(),
				Buffer.wrap("{\"foo\": \"foofoofoo\", \"bar\": \"barbarbar\"}").byteBuffer(),
				Buffer.wrap("{\"foo\": \"foofoofoofoo\", \"bar\": \"barbarbarbar\"}").byteBuffer()
		);
		Iterable<String> results = Flux.from(encoder.encode(source, null, null)).map(chunk -> {
			byte[] b = new byte[chunk.remaining()];
			chunk.get(b);
			return new String(b, StandardCharsets.UTF_8);
		}).toIterable();
		String result = String.join("", results);
		assertEquals("[{\"foo\": \"foofoo\", \"bar\": \"barbar\"},{\"foo\": \"foofoofoo\", \"bar\": \"barbarbar\"},{\"foo\": \"foofoofoofoo\", \"bar\": \"barbarbarbar\"}]", result);
	}

}
