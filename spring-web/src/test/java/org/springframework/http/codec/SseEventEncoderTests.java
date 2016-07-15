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

package org.springframework.http.codec;

import java.util.Arrays;

import static org.junit.Assert.*;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.TestSubscriber;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.AbstractDataBufferAllocatingTestCase;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.FlushingDataBuffer;
import org.springframework.http.codec.json.JacksonJsonEncoder;
import org.springframework.util.MimeType;

/**
 * @author Sebastien Deleuze
 */
public class SseEventEncoderTests extends AbstractDataBufferAllocatingTestCase {

	@Test
	public void nullMimeType() {
		SseEventEncoder encoder = new SseEventEncoder(Arrays.asList(new JacksonJsonEncoder()));
		assertTrue(encoder.canEncode(ResolvableType.forClass(Object.class), null));
	}

	@Test
	public void unsupportedMimeType() {
		SseEventEncoder encoder = new SseEventEncoder(Arrays.asList(new JacksonJsonEncoder()));
		assertFalse(encoder.canEncode(ResolvableType.forClass(Object.class), new MimeType("foo", "bar")));
	}

	@Test
	public void supportedMimeType() {
		SseEventEncoder encoder = new SseEventEncoder(Arrays.asList(new JacksonJsonEncoder()));
		assertTrue(encoder.canEncode(ResolvableType.forClass(Object.class), new MimeType("text", "event-stream")));
	}

	@Test
	public void encodeServerSentEvent() {
		SseEventEncoder encoder = new SseEventEncoder(Arrays.asList(new JacksonJsonEncoder()));
		SseEvent event = new SseEvent();
		event.setId("c42");
		event.setName("foo");
		event.setComment("bla\nbla bla\nbla bla bla");
		event.setReconnectTime(123L);
		Mono<SseEvent> source = Mono.just(event);
		Flux<DataBuffer> output = encoder.encode(source, this.bufferFactory,
						ResolvableType.forClass(SseEvent.class), new MimeType("text", "event-stream"));
		TestSubscriber
				.subscribe(output)
				.assertNoError()
				.assertValuesWith(
						stringConsumer(
								"id:c42\n" +
								"event:foo\n" +
								"retry:123\n" +
								":bla\n:bla bla\n:bla bla bla\n"),
						stringConsumer("\n"),
						b -> assertEquals(FlushingDataBuffer.class, b.getClass())
				);
	}

	@Test
	public void encodeString() {
		SseEventEncoder encoder = new SseEventEncoder(Arrays.asList(new JacksonJsonEncoder()));
		Flux<String> source = Flux.just("foo", "bar");
		Flux<DataBuffer> output = encoder.encode(source, this.bufferFactory,
				ResolvableType.forClass(String.class), new MimeType("text", "event-stream"));
		TestSubscriber
				.subscribe(output)
				.assertNoError()
				.assertValuesWith(
						stringConsumer("data:foo\n"),
						stringConsumer("\n"),
						b -> assertEquals(FlushingDataBuffer.class, b.getClass()),
						stringConsumer("data:bar\n"),
						stringConsumer("\n"),
						b -> assertEquals(FlushingDataBuffer.class, b.getClass())
				);
	}

	@Test
	public void encodeMultilineString() {
		SseEventEncoder encoder = new SseEventEncoder(Arrays.asList(new JacksonJsonEncoder()));
		Flux<String> source = Flux.just("foo\nbar", "foo\nbaz");
		Flux<DataBuffer> output = encoder.encode(source, this.bufferFactory,
				ResolvableType.forClass(String.class), new MimeType("text", "event-stream"));
		TestSubscriber
				.subscribe(output)
				.assertNoError()
				.assertValuesWith(
						stringConsumer("data:foo\ndata:bar\n"),
						stringConsumer("\n"),
						b -> assertEquals(FlushingDataBuffer.class, b.getClass()),
						stringConsumer("data:foo\ndata:baz\n"),
						stringConsumer("\n"),
						b -> assertEquals(FlushingDataBuffer.class, b.getClass())
				);
	}

	@Test
	public void encodePojo() {
		SseEventEncoder encoder = new SseEventEncoder(Arrays.asList(new JacksonJsonEncoder()));
		Flux<Pojo> source = Flux.just(new Pojo("foofoo", "barbar"), new Pojo("foofoofoo", "barbarbar"));
		Flux<DataBuffer> output = encoder.encode(source, this.bufferFactory,
						ResolvableType.forClass(Pojo.class), new MimeType("text", "event-stream"));
		TestSubscriber
				.subscribe(output)
				.assertNoError()
				.assertValuesWith(
						stringConsumer("data:"),
						stringConsumer("{\"foo\":\"foofoo\",\"bar\":\"barbar\"}"),
						stringConsumer("\n"),
						stringConsumer("\n"),
						b -> assertEquals(FlushingDataBuffer.class, b.getClass()),
						stringConsumer("data:"),
						stringConsumer("{\"foo\":\"foofoofoo\",\"bar\":\"barbarbar\"}"),
						stringConsumer("\n"),
						stringConsumer("\n"),
						b -> assertEquals(FlushingDataBuffer.class, b.getClass())
				);
	}

}
