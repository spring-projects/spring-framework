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
import java.util.List;
import java.util.stream.StreamSupport;

import org.junit.Test;
import reactor.core.publisher.Flux;

import org.springframework.core.io.buffer.DataBuffer;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;

/**
 * @author Sebastien Deleuze
 */
public class JsonObjectDecoderTests extends AbstractAllocatingTestCase {


	@Test
	public void decodeSingleChunkToJsonObject() throws InterruptedException {
		JsonObjectDecoder decoder = new JsonObjectDecoder(allocator);
		Flux<DataBuffer> source =
				Flux.just(stringBuffer("{\"foo\": \"foofoo\", \"bar\": \"barbar\"}"));
		Flux<String> output =
				decoder.decode(source, null, null).map(JsonObjectDecoderTests::toString);
		List<Object> results = StreamSupport.stream(output.toIterable().spliterator(), false).collect(toList());
		assertEquals(1, results.size());
		assertEquals("{\"foo\": \"foofoo\", \"bar\": \"barbar\"}", results.get(0));
	}

	@Test
	public void decodeMultipleChunksToJsonObject() throws InterruptedException {
		JsonObjectDecoder decoder = new JsonObjectDecoder(allocator);
		Flux<DataBuffer> source = Flux.just(stringBuffer("{\"foo\": \"foofoo\""),
				stringBuffer(", \"bar\": \"barbar\"}"));
		Flux<String> output =
				decoder.decode(source, null, null).map(JsonObjectDecoderTests::toString);
		List<String> results = StreamSupport.stream(output.toIterable().spliterator(), false).collect(toList());
		assertEquals(1, results.size());
		assertEquals("{\"foo\": \"foofoo\", \"bar\": \"barbar\"}", results.get(0));
	}

	@Test
	public void decodeSingleChunkToArray() throws InterruptedException {
		JsonObjectDecoder decoder = new JsonObjectDecoder(allocator);
		Flux<DataBuffer> source = Flux.just(stringBuffer(
				"[{\"foo\": \"foofoo\", \"bar\": \"barbar\"},{\"foo\": \"foofoofoo\", \"bar\": \"barbarbar\"}]"));
		Flux<String> output =
				decoder.decode(source, null, null).map(JsonObjectDecoderTests::toString);

		List<String> results = StreamSupport.stream(output.toIterable().spliterator(), false).collect(toList());
		assertEquals(2, results.size());
		assertEquals("{\"foo\": \"foofoo\", \"bar\": \"barbar\"}", results.get(0));
		assertEquals("{\"foo\": \"foofoofoo\", \"bar\": \"barbarbar\"}", results.get(1));
	}

	@Test
	public void decodeMultipleChunksToArray() throws InterruptedException {
		JsonObjectDecoder decoder = new JsonObjectDecoder(allocator);
		Flux<DataBuffer> source =
				Flux.just(stringBuffer("[{\"foo\": \"foofoo\", \"bar\""), stringBuffer(
						": \"barbar\"},{\"foo\": \"foofoofoo\", \"bar\": \"barbarbar\"}]"));
		Flux<String> output =
				decoder.decode(source, null, null).map(JsonObjectDecoderTests::toString);
		List<String> results = StreamSupport.stream(output.toIterable().spliterator(), false).collect(toList());
		assertEquals(2, results.size());
		assertEquals("{\"foo\": \"foofoo\", \"bar\": \"barbar\"}", results.get(0));
		assertEquals("{\"foo\": \"foofoofoo\", \"bar\": \"barbarbar\"}", results.get(1));
	}

	private static String toString(DataBuffer buffer) {
		byte[] b = new byte[buffer.readableByteCount()];
		buffer.read(b);
		return new String(b, StandardCharsets.UTF_8);
	}

}
