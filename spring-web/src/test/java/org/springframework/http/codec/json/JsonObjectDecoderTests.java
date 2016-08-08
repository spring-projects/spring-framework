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

package org.springframework.http.codec.json;

import java.nio.charset.StandardCharsets;

import org.junit.Test;
import reactor.core.publisher.Flux;

import org.springframework.core.io.buffer.AbstractDataBufferAllocatingTestCase;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.tests.TestSubscriber;

/**
 * @author Sebastien Deleuze
 */
public class JsonObjectDecoderTests extends AbstractDataBufferAllocatingTestCase {

	@Test
	public void decodeSingleChunkToJsonObject()  {
		JsonObjectDecoder decoder = new JsonObjectDecoder();
		Flux<DataBuffer> source =
				Flux.just(stringBuffer("{\"foo\": \"foofoo\", \"bar\": \"barbar\"}"));
		Flux<String> output =
				decoder.decode(source, null, null).map(JsonObjectDecoderTests::toString);
		TestSubscriber
				.subscribe(output)
				.assertValues("{\"foo\": \"foofoo\", \"bar\": \"barbar\"}");
	}

	@Test
	public void decodeMultipleChunksToJsonObject() throws InterruptedException {
		JsonObjectDecoder decoder = new JsonObjectDecoder();
		Flux<DataBuffer> source = Flux.just(stringBuffer("{\"foo\": \"foofoo\""),
				stringBuffer(", \"bar\": \"barbar\"}"));
		Flux<String> output =
				decoder.decode(source, null, null).map(JsonObjectDecoderTests::toString);
		TestSubscriber
				.subscribe(output)
				.assertValues("{\"foo\": \"foofoo\", \"bar\": \"barbar\"}");
	}

	@Test
	public void decodeSingleChunkToArray() throws InterruptedException {
		JsonObjectDecoder decoder = new JsonObjectDecoder();
		Flux<DataBuffer> source = Flux.just(stringBuffer(
				"[{\"foo\": \"foofoo\", \"bar\": \"barbar\"},{\"foo\": \"foofoofoo\", \"bar\": \"barbarbar\"}]"));
		Flux<String> output =
				decoder.decode(source, null, null).map(JsonObjectDecoderTests::toString);
		TestSubscriber
				.subscribe(output)
				.assertValues("{\"foo\": \"foofoo\", \"bar\": \"barbar\"}",
							  "{\"foo\": \"foofoofoo\", \"bar\": \"barbarbar\"}");
	}

	@Test
	public void decodeMultipleChunksToArray() throws InterruptedException {
		JsonObjectDecoder decoder = new JsonObjectDecoder();
		Flux<DataBuffer> source =
				Flux.just(stringBuffer("[{\"foo\": \"foofoo\", \"bar\""), stringBuffer(
						": \"barbar\"},{\"foo\": \"foofoofoo\", \"bar\": \"barbarbar\"}]"));
		Flux<String> output =
				decoder.decode(source, null, null).map(JsonObjectDecoderTests::toString);
		TestSubscriber
				.subscribe(output)
				.assertValues("{\"foo\": \"foofoo\", \"bar\": \"barbar\"}",
							  "{\"foo\": \"foofoofoo\", \"bar\": \"barbarbar\"}");
	}


	private static String toString(DataBuffer buffer) {
		byte[] b = new byte[buffer.readableByteCount()];
		buffer.read(b);
		return new String(b, StandardCharsets.UTF_8);
	}

}
