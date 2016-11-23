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
import java.util.function.Consumer;

import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.AbstractDataBufferAllocatingTestCase;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;

import static org.junit.Assert.*;

/**
 * @author Sebastien Deleuze
 */
public class ServerSentEventHttpMessageWriterTests extends AbstractDataBufferAllocatingTestCase {

	private ServerSentEventHttpMessageWriter messageWriter = new ServerSentEventHttpMessageWriter(
			Collections.singletonList(new Jackson2JsonEncoder()));


	@Test
	public void nullMimeType() {
		assertTrue(messageWriter.canWrite(ResolvableType.forClass(Object.class), null));
	}

	@Test
	public void unsupportedMimeType() {
		assertFalse(messageWriter.canWrite(ResolvableType.forClass(Object.class),
				new MediaType("foo", "bar")));
	}

	@Test
	public void supportedMimeType() {
		assertTrue(messageWriter.canWrite(ResolvableType.forClass(Object.class),
				new MediaType("text", "event-stream")));
	}

	@Test
	public void encodeServerSentEvent() {
		ServerSentEvent<String> event = ServerSentEvent.<String>builder().
				data("bar").id("c42").event("foo").comment("bla\nbla bla\nbla bla bla")
				.retry(Duration.ofMillis(123L)).build();

		Mono<ServerSentEvent<String>> source = Mono.just(event);
		MockServerHttpResponse outputMessage = new MockServerHttpResponse();
		messageWriter.write(source, ResolvableType.forClass(ServerSentEvent.class),
				new MediaType("text", "event-stream"), outputMessage, Collections.emptyMap());

		Publisher<Publisher<DataBuffer>> result = Flux.from(outputMessage.getBodyWithFlush());
		StepVerifier.create(result)
				.consumeNextWith(sseConsumer("id:c42\n" + "event:foo\n" + "retry:123\n" +
						":bla\n:bla bla\n:bla bla bla\n" + "data:bar\n"))
				.expectComplete()
				.verify();
	}

	@Test
	public void encodeString() {
		Flux<String> source = Flux.just("foo", "bar");
		MockServerHttpResponse outputMessage = new MockServerHttpResponse();
		messageWriter.write(source, ResolvableType.forClass(String.class),
				new MediaType("text", "event-stream"), outputMessage, Collections.emptyMap());

		Publisher<Publisher<DataBuffer>> result = outputMessage.getBodyWithFlush();
		StepVerifier.create(result)
				.consumeNextWith(sseConsumer("data:foo\n"))
				.consumeNextWith(sseConsumer("data:bar\n"))
				.expectComplete()
				.verify();
	}

	@Test
	public void encodeMultiLineString() {
		Flux<String> source = Flux.just("foo\nbar", "foo\nbaz");
		MockServerHttpResponse outputMessage = new MockServerHttpResponse();
		messageWriter.write(source, ResolvableType.forClass(String.class),
				new MediaType("text", "event-stream"), outputMessage, Collections.emptyMap());

		Publisher<Publisher<DataBuffer>> result = outputMessage.getBodyWithFlush();
		StepVerifier.create(result)
				.consumeNextWith(sseConsumer("data:foo\ndata:bar\n"))
				.consumeNextWith(sseConsumer("data:foo\ndata:baz\n"))
				.expectComplete()
				.verify();
	}

	@Test
	public void encodePojo() {
		Flux<Pojo> source = Flux.just(new Pojo("foofoo", "barbar"),
				new Pojo("foofoofoo", "barbarbar"));
		MockServerHttpResponse outputMessage = new MockServerHttpResponse();
		messageWriter.write(source, ResolvableType.forClass(Pojo.class),
				new MediaType("text", "event-stream"), outputMessage, Collections.emptyMap());

		Publisher<Publisher<DataBuffer>> result = outputMessage.getBodyWithFlush();
		StepVerifier.create(result)
				.consumeNextWith(sseConsumer("data:", "{\"foo\":\"foofoo\",\"bar\":\"barbar\"}", "\n"))
				.consumeNextWith(sseConsumer("data:", "{\"foo\":\"foofoofoo\",\"bar\":\"barbarbar\"}", "\n"))
				.expectComplete()
				.verify();
	}


	private Consumer<Publisher<DataBuffer>> sseConsumer(String... expected) {
		return publisher -> {
			StepVerifier.Step<DataBuffer> builder = StepVerifier.create(publisher);
			for (String value : expected) {
				builder = builder.consumeNextWith(stringConsumer(value));
			}
			builder.consumeNextWith(stringConsumer("\n")).expectComplete().verify();
		};
	}

}
