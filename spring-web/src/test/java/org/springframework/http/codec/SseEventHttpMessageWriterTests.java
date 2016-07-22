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

import java.util.Collections;

import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.TestSubscriber;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.AbstractDataBufferAllocatingTestCase;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.codec.Pojo;
import org.springframework.http.codec.SseEvent;
import org.springframework.http.codec.SseEventHttpMessageWriter;
import org.springframework.http.codec.json.JacksonJsonEncoder;
import org.springframework.http.server.reactive.MockServerHttpResponse;

import static org.junit.Assert.*;

/**
 * @author Sebastien Deleuze
 */
public class SseEventHttpMessageWriterTests
		extends AbstractDataBufferAllocatingTestCase {

	private SseEventHttpMessageWriter converter = new SseEventHttpMessageWriter(
			Collections.singletonList(new JacksonJsonEncoder()));

	@Test
	public void nullMimeType() {
		assertTrue(converter.canWrite(ResolvableType.forClass(Object.class), null));
	}

	@Test
	public void unsupportedMimeType() {
		assertFalse(converter.canWrite(ResolvableType.forClass(Object.class),
				new MediaType("foo", "bar")));
	}

	@Test
	public void supportedMimeType() {
		assertTrue(converter.canWrite(ResolvableType.forClass(Object.class),
				new MediaType("text", "event-stream")));
	}

	@Test
	public void encodeServerSentEvent() {
		SseEvent event = new SseEvent();
		event.setId("c42");
		event.setName("foo");
		event.setComment("bla\nbla bla\nbla bla bla");
		event.setReconnectTime(123L);
		Mono<SseEvent> source = Mono.just(event);
		MockServerHttpResponse outputMessage = new MockServerHttpResponse();
		converter.write(source, ResolvableType.forClass(SseEvent.class),
				new MediaType("text", "event-stream"), outputMessage);

		Publisher<Publisher<DataBuffer>> result = outputMessage.getBodyWithFlush();
		TestSubscriber.subscribe(result).
				assertNoError().
				assertValuesWith(publisher -> {
					TestSubscriber.subscribe(publisher).assertNoError().assertValuesWith(
							stringConsumer("id:c42\n" + "event:foo\n" + "retry:123\n" +
									":bla\n:bla bla\n:bla bla bla\n"),
							stringConsumer("\n"));

				});
	}

	@Test
	public void encodeString() {
		Flux<String> source = Flux.just("foo", "bar");
		MockServerHttpResponse outputMessage = new MockServerHttpResponse();
		converter.write(source, ResolvableType.forClass(String.class),
				new MediaType("text", "event-stream"), outputMessage);

		Publisher<Publisher<DataBuffer>> result = outputMessage.getBodyWithFlush();
		TestSubscriber.subscribe(result).
				assertNoError().
				assertValuesWith(publisher -> {
					TestSubscriber.subscribe(publisher).assertNoError()
							.assertValuesWith(stringConsumer("data:foo\n"),
									stringConsumer("\n"));

				}, publisher -> {
					TestSubscriber.subscribe(publisher).assertNoError()
							.assertValuesWith(stringConsumer("data:bar\n"),
									stringConsumer("\n"));

				});
	}

	@Test
	public void encodeMultiLineString() {
		Flux<String> source = Flux.just("foo\nbar", "foo\nbaz");
		MockServerHttpResponse outputMessage = new MockServerHttpResponse();
		converter.write(source, ResolvableType.forClass(String.class),
				new MediaType("text", "event-stream"), outputMessage);

		Publisher<Publisher<DataBuffer>> result = outputMessage.getBodyWithFlush();
		TestSubscriber.subscribe(result).
				assertNoError().
				assertValuesWith(publisher -> {
					TestSubscriber.subscribe(publisher).assertNoError()
							.assertValuesWith(stringConsumer("data:foo\ndata:bar\n"),
									stringConsumer("\n"));

				}, publisher -> {
					TestSubscriber.subscribe(publisher).assertNoError()
							.assertValuesWith(stringConsumer("data:foo\ndata:baz\n"),
									stringConsumer("\n"));

				});
	}

	@Test
	public void encodePojo() {
		Flux<Pojo> source = Flux.just(new Pojo("foofoo", "barbar"),
				new Pojo("foofoofoo", "barbarbar"));
		MockServerHttpResponse outputMessage = new MockServerHttpResponse();
		converter.write(source, ResolvableType.forClass(Pojo.class),
				new MediaType("text", "event-stream"), outputMessage);

		Publisher<Publisher<DataBuffer>> result = outputMessage.getBodyWithFlush();
		TestSubscriber.subscribe(result).
				assertNoError().
				assertValuesWith(publisher -> {
					TestSubscriber.subscribe(publisher).assertNoError()
							.assertValuesWith(stringConsumer("data:"), stringConsumer(
									"{\"foo\":\"foofoo\",\"bar\":\"barbar\"}"),
									stringConsumer("\n"), stringConsumer("\n"));

				}, publisher -> {
					TestSubscriber.subscribe(publisher).assertNoError()
							.assertValuesWith(stringConsumer("data:"), stringConsumer(
									"{\"foo\":\"foofoofoo\",\"bar\":\"barbarbar\"}"),
									stringConsumer("\n"), stringConsumer("\n"));

				});
	}

}
