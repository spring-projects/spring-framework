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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.AbstractDataBufferAllocatingTestCase;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Sebastien Deleuze
 */
public class ServerSentEventHttpMessageReaderTests extends AbstractDataBufferAllocatingTestCase {

	private ServerSentEventHttpMessageReader messageReader = new ServerSentEventHttpMessageReader(
			Collections.singletonList(new Jackson2JsonDecoder()));

	@Test
	public void cantRead() {
		assertFalse(messageReader.canRead(ResolvableType.forClass(Object.class),
				new MediaType("foo", "bar")));
		assertFalse(messageReader.canRead(ResolvableType.forClass(Object.class), null));
	}

	@Test
	public void canRead() {
		assertTrue(messageReader.canRead(ResolvableType.forClass(Object.class),
				new MediaType("text", "event-stream")));
		assertTrue(messageReader.canRead(ResolvableType.forClass(ServerSentEvent.class),
				new MediaType("foo", "bar")));
	}

	@Test
	public void readServerSentEvents() {

		MockServerHttpRequest request = MockServerHttpRequest.post("/").body(
				"id:c42\nevent:foo\nretry:123\n:bla\n:bla bla\n:bla bla bla\ndata:bar\n\n" +
			 	"id:c43\nevent:bar\nretry:456\ndata:baz\n\n");

		Flux<ServerSentEvent> events = this.messageReader
				.read(ResolvableType.forClassWithGenerics(ServerSentEvent.class, String.class),
						request, Collections.emptyMap()).cast(ServerSentEvent.class);

		StepVerifier.create(events)
				.consumeNextWith(event -> {
					assertEquals("c42", event.id().get());
					assertEquals("foo", event.event().get());
					assertEquals(Duration.ofMillis(123), event.retry().get());
					assertEquals("bla\nbla bla\nbla bla bla", event.comment().get());
					assertEquals("bar", event.data().get());
				})
				.consumeNextWith(event -> {
					assertEquals("c43", event.id().get());
					assertEquals("bar", event.event().get());
					assertEquals(Duration.ofMillis(456), event.retry().get());
					assertFalse(event.comment().isPresent());
					assertEquals("baz", event.data().get());
				})
				.expectComplete()
				.verify();
	}

	@Test
	public void readServerSentEventsWithMultipleChunks() {

		MockServerHttpRequest request = MockServerHttpRequest.post("/")
				.body(Flux.just(
						stringBuffer("id:c42\nev"),
						stringBuffer("ent:foo\nretry:123\n:bla\n:bla bla\n:bla bla bla\ndata:"),
						stringBuffer("bar\n\nid:c43\nevent:bar\nretry:456\ndata:baz\n\n")));

		Flux<ServerSentEvent> events = messageReader
				.read(ResolvableType.forClassWithGenerics(ServerSentEvent.class, String.class),
						request, Collections.emptyMap()).cast(ServerSentEvent.class);

		StepVerifier.create(events)
				.consumeNextWith(event -> {
					assertEquals("c42", event.id().get());
					assertEquals("foo", event.event().get());
					assertEquals(Duration.ofMillis(123), event.retry().get());
					assertEquals("bla\nbla bla\nbla bla bla", event.comment().get());
					assertEquals("bar", event.data().get());
				})
				.consumeNextWith(event -> {
					assertEquals("c43", event.id().get());
					assertEquals("bar", event.event().get());
					assertEquals(Duration.ofMillis(456), event.retry().get());
					assertFalse(event.comment().isPresent());
					assertEquals("baz", event.data().get());
				})
				.expectComplete()
				.verify();
	}

	@Test
	public void readString() {

		String body = "data:foo\ndata:bar\n\ndata:baz\n\n";
		MockServerHttpRequest request = MockServerHttpRequest.post("/").body(body);

		Flux<String> data = messageReader.read(ResolvableType.forClass(String.class),
				request, Collections.emptyMap()).cast(String.class);

		StepVerifier.create(data)
				.expectNextMatches(elem -> elem.equals("foo\nbar"))
				.expectNextMatches(elem -> elem.equals("baz"))
				.expectComplete()
				.verify();
	}

	@Test
	public void readPojo() {

		MockServerHttpRequest request = MockServerHttpRequest.post("/").body(
				"data:{\"foo\": \"foofoo\", \"bar\": \"barbar\"}\n\n" +
				"data:{\"foo\": \"foofoofoo\", \"bar\": \"barbarbar\"}\n\n");

		Flux<Pojo> data = messageReader.read(ResolvableType.forClass(Pojo.class), request,
				Collections.emptyMap()).cast(Pojo.class);

		StepVerifier.create(data)
				.consumeNextWith(pojo -> {
					assertEquals("foofoo", pojo.getFoo());
					assertEquals("barbar", pojo.getBar());
				})
				.consumeNextWith(pojo -> {
					assertEquals("foofoofoo", pojo.getFoo());
					assertEquals("barbarbar", pojo.getBar());
				})
				.expectComplete()
				.verify();
	}

	@Test // SPR-15331
	public void decodeFullContentAsString() {

		String body = "data:foo\ndata:bar\n\ndata:baz\n\n";
		MockServerHttpRequest request = MockServerHttpRequest.post("/").body(body);

		String actual = messageReader
				.readMono(ResolvableType.forClass(String.class), request, Collections.emptyMap())
				.cast(String.class)
				.block(Duration.ZERO);

		assertEquals(body, actual);
	}

}
