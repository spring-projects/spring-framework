/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http.codec;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.AbstractLeakCheckingTests;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ServerSentEventHttpMessageReader}.
 *
 * @author Sebastien Deleuze
 */
public class ServerSentEventHttpMessageReaderTests extends AbstractLeakCheckingTests {

	private ServerSentEventHttpMessageReader messageReader =
			new ServerSentEventHttpMessageReader(new Jackson2JsonDecoder());


	@Test
	public void cantRead() {
		assertThat(messageReader.canRead(ResolvableType.forClass(Object.class), new MediaType("foo", "bar"))).isFalse();
		assertThat(messageReader.canRead(ResolvableType.forClass(Object.class), null)).isFalse();
	}

	@Test
	public void canRead() {
		assertThat(messageReader.canRead(ResolvableType.forClass(Object.class), new MediaType("text", "event-stream"))).isTrue();
		assertThat(messageReader.canRead(ResolvableType.forClass(ServerSentEvent.class), new MediaType("foo", "bar"))).isTrue();
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void readServerSentEvents() {
		MockServerHttpRequest request = MockServerHttpRequest.post("/")
				.body(Mono.just(stringBuffer(
						"id:c42\nevent:foo\nretry:123\n:bla\n:bla bla\n:bla bla bla\ndata:bar\n\n" +
						"id:c43\nevent:bar\nretry:456\ndata:baz\n\n")));

		Flux<ServerSentEvent> events = this.messageReader
				.read(ResolvableType.forClassWithGenerics(ServerSentEvent.class, String.class),
						request, Collections.emptyMap()).cast(ServerSentEvent.class);

		StepVerifier.create(events)
				.consumeNextWith(event -> {
					assertThat(event.id()).isEqualTo("c42");
					assertThat(event.event()).isEqualTo("foo");
					assertThat(event.retry()).isEqualTo(Duration.ofMillis(123));
					assertThat(event.comment()).isEqualTo("bla\nbla bla\nbla bla bla");
					assertThat(event.data()).isEqualTo("bar");
				})
				.consumeNextWith(event -> {
					assertThat(event.id()).isEqualTo("c43");
					assertThat(event.event()).isEqualTo("bar");
					assertThat(event.retry()).isEqualTo(Duration.ofMillis(456));
					assertThat(event.comment()).isNull();
					assertThat(event.data()).isEqualTo("baz");
				})
				.expectComplete()
				.verify();
	}

	@Test
	@SuppressWarnings("rawtypes")
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
					assertThat(event.id()).isEqualTo("c42");
					assertThat(event.event()).isEqualTo("foo");
					assertThat(event.retry()).isEqualTo(Duration.ofMillis(123));
					assertThat(event.comment()).isEqualTo("bla\nbla bla\nbla bla bla");
					assertThat(event.data()).isEqualTo("bar");
				})
				.consumeNextWith(event -> {
					assertThat(event.id()).isEqualTo("c43");
					assertThat(event.event()).isEqualTo("bar");
					assertThat(event.retry()).isEqualTo(Duration.ofMillis(456));
					assertThat(event.comment()).isNull();
					assertThat(event.data()).isEqualTo("baz");
				})
				.expectComplete()
				.verify();
	}

	@Test
	public void readString() {
		MockServerHttpRequest request = MockServerHttpRequest.post("/")
				.body(Mono.just(stringBuffer("data:foo\ndata:bar\n\ndata:baz\n\n")));

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
		MockServerHttpRequest request = MockServerHttpRequest.post("/")
				.body(Mono.just(stringBuffer(
						"data:{\"foo\": \"foofoo\", \"bar\": \"barbar\"}\n\n" +
								"data:{\"foo\": \"foofoofoo\", \"bar\": \"barbarbar\"}\n\n")));

		Flux<Pojo> data = messageReader.read(ResolvableType.forClass(Pojo.class), request,
				Collections.emptyMap()).cast(Pojo.class);

		StepVerifier.create(data)
				.consumeNextWith(pojo -> {
					assertThat(pojo.getFoo()).isEqualTo("foofoo");
					assertThat(pojo.getBar()).isEqualTo("barbar");
				})
				.consumeNextWith(pojo -> {
					assertThat(pojo.getFoo()).isEqualTo("foofoofoo");
					assertThat(pojo.getBar()).isEqualTo("barbarbar");
				})
				.expectComplete()
				.verify();
	}

	@Test  // SPR-15331
	public void decodeFullContentAsString() {
		String body = "data:foo\ndata:bar\n\ndata:baz\n\n";
		MockServerHttpRequest request = MockServerHttpRequest.post("/")
				.body(Mono.just(stringBuffer(body)));

		String actual = messageReader
				.readMono(ResolvableType.forClass(String.class), request, Collections.emptyMap())
				.cast(String.class)
				.block(Duration.ZERO);

		assertThat(actual).isEqualTo(body);
	}

	@Test
	public void readError() {
		Flux<DataBuffer> body =
				Flux.just(stringBuffer("data:foo\ndata:bar\n\ndata:baz\n\n"))
						.concatWith(Flux.error(new RuntimeException()));

		MockServerHttpRequest request = MockServerHttpRequest.post("/")
				.body(body);

		Flux<String> data = messageReader.read(ResolvableType.forClass(String.class),
				request, Collections.emptyMap()).cast(String.class);

		StepVerifier.create(data)
				.expectNextMatches(elem -> elem.equals("foo\nbar"))
				.expectNextMatches(elem -> elem.equals("baz"))
				.expectError()
				.verify();
	}

	private DataBuffer stringBuffer(String value) {
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		DataBuffer buffer = this.bufferFactory.allocateBuffer(bytes.length);
		buffer.write(bytes);
		return buffer;
	}

}
