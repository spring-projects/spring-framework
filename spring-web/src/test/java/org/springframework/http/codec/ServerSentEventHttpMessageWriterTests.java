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

import java.time.Duration;
import java.util.Collections;

import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.AbstractDataBufferAllocatingTestCase;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.tests.TestSubscriber;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Sebastien Deleuze
 */
public class ServerSentEventHttpMessageWriterTests extends AbstractDataBufferAllocatingTestCase {

	private ServerSentEventHttpMessageWriter messageWriter = new ServerSentEventHttpMessageWriter(
			Collections.singletonList(new Jackson2JsonEncoder()));


	@Test
	public void nullMimeType() {
		assertTrue(messageWriter.canWrite(ResolvableType.forClass(Object.class), null,
				Collections.emptyMap()));
	}

	@Test
	public void unsupportedMimeType() {
		assertFalse(messageWriter.canWrite(ResolvableType.forClass(Object.class),
				new MediaType("foo", "bar"), Collections.emptyMap()));
	}

	@Test
	public void supportedMimeType() {
		assertTrue(messageWriter.canWrite(ResolvableType.forClass(Object.class),
				new MediaType("text", "event-stream"), Collections.emptyMap()));
	}

	@Test
	public void encodeServerSentEvent() {
		ServerSentEvent<String>
				event = ServerSentEvent.<String>builder().data("bar").id("c42").event("foo").comment("bla\nbla bla\nbla bla bla")
				.retry(Duration.ofMillis(123L)).build();
		Mono<ServerSentEvent<String>> source = Mono.just(event);
		MockServerHttpResponse outputMessage = new MockServerHttpResponse();
		messageWriter.write(source, ResolvableType.forClass(ServerSentEvent.class),
				new MediaType("text", "event-stream"), outputMessage, Collections.emptyMap());

		Publisher<Publisher<DataBuffer>> result = outputMessage.getBodyWithFlush();
		TestSubscriber.subscribe(result).
				assertNoError().
				assertValuesWith(publisher -> {
					TestSubscriber.subscribe(publisher).assertNoError().assertValuesWith(
							stringConsumer("id:c42\n" + "event:foo\n" + "retry:123\n" +
									":bla\n:bla bla\n:bla bla bla\n" +
									"data:bar\n"),
							stringConsumer("\n"));

				});
	}

	@Test
	public void encodeString() {
		Flux<String> source = Flux.just("foo", "bar");
		MockServerHttpResponse outputMessage = new MockServerHttpResponse();
		messageWriter.write(source, ResolvableType.forClass(String.class),
				new MediaType("text", "event-stream"), outputMessage, Collections.emptyMap());

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
		messageWriter.write(source, ResolvableType.forClass(String.class),
				new MediaType("text", "event-stream"), outputMessage, Collections.emptyMap());

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
		messageWriter.write(source, ResolvableType.forClass(Pojo.class),
				new MediaType("text", "event-stream"), outputMessage, Collections.emptyMap());

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
