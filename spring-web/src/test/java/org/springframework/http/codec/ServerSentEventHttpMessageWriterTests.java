/*
 * Copyright 2002-2018 the original author or authors.
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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.AbstractDataBufferAllocatingTestCase;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.support.DataBufferTestUtils;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;

import static org.junit.Assert.*;
import static org.springframework.core.ResolvableType.forClass;

/**
 * Unit tests for {@link ServerSentEventHttpMessageWriter}.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 */
@SuppressWarnings("rawtypes")
public class ServerSentEventHttpMessageWriterTests extends AbstractDataBufferAllocatingTestCase {

	private static final Map<String, Object> HINTS = Collections.emptyMap();

	private ServerSentEventHttpMessageWriter messageWriter =
			new ServerSentEventHttpMessageWriter(new Jackson2JsonEncoder());

	private MockServerHttpResponse outputMessage;


	@Before
	public void setUp() {
		this.outputMessage = new MockServerHttpResponse(this.bufferFactory);
	}



	@Test
	public void canWrite() {
		assertTrue(this.messageWriter.canWrite(forClass(Object.class), null));
		assertFalse(this.messageWriter.canWrite(forClass(Object.class), new MediaType("foo", "bar")));

		assertTrue(this.messageWriter.canWrite(null, MediaType.TEXT_EVENT_STREAM));
		assertTrue(this.messageWriter.canWrite(forClass(ServerSentEvent.class), new MediaType("foo", "bar")));

		// SPR-15464
		assertTrue(this.messageWriter.canWrite(ResolvableType.NONE, MediaType.TEXT_EVENT_STREAM));
		assertFalse(this.messageWriter.canWrite(ResolvableType.NONE, new MediaType("foo", "bar")));
	}

	@Test
	public void writeServerSentEvent() {
		ServerSentEvent<?> event = ServerSentEvent.builder().data("bar").id("c42").event("foo")
				.comment("bla\nbla bla\nbla bla bla").retry(Duration.ofMillis(123L)).build();

		Mono<ServerSentEvent> source = Mono.just(event);
		testWrite(source, outputMessage, ServerSentEvent.class);

		StepVerifier.create(outputMessage.getBody())
				.consumeNextWith(stringConsumer("id:c42\nevent:foo\nretry:123\n:bla\n:bla bla\n:bla bla bla\ndata:"))
				.consumeNextWith(stringConsumer("bar\n"))
				.consumeNextWith(stringConsumer("\n"))
				.expectComplete()
				.verify();
	}

	@Test
	public void writeString() {
		Flux<String> source = Flux.just("foo", "bar");
		testWrite(source, outputMessage, String.class);

		StepVerifier.create(outputMessage.getBody())
				.consumeNextWith(stringConsumer("data:"))
				.consumeNextWith(stringConsumer("foo\n"))
				.consumeNextWith(stringConsumer("\n"))
				.consumeNextWith(stringConsumer("data:"))
				.consumeNextWith(stringConsumer("bar\n"))
				.consumeNextWith(stringConsumer("\n"))
				.expectComplete()
				.verify();
	}

	@Test
	public void writeMultiLineString() {
		Flux<String> source = Flux.just("foo\nbar", "foo\nbaz");
		testWrite(source, outputMessage, String.class);

		StepVerifier.create(outputMessage.getBody())
				.consumeNextWith(stringConsumer("data:"))
				.consumeNextWith(stringConsumer("foo\ndata:bar\n"))
				.consumeNextWith(stringConsumer("\n"))
				.consumeNextWith(stringConsumer("data:"))
				.consumeNextWith(stringConsumer("foo\ndata:baz\n"))
				.consumeNextWith(stringConsumer("\n"))
				.expectComplete()
				.verify();
	}

	@Test // SPR-16516
	public void writeStringWithCustomCharset() {
		Flux<String> source = Flux.just("\u00A3");
		Charset charset = StandardCharsets.ISO_8859_1;
		MediaType mediaType = new MediaType("text", "event-stream", charset);
		testWrite(source, mediaType, outputMessage, String.class);

		assertEquals(mediaType, outputMessage.getHeaders().getContentType());
		StepVerifier.create(outputMessage.getBody())
				.consumeNextWith(stringConsumer("data:"))
				.consumeNextWith(dataBuffer -> {
					String value =
							DataBufferTestUtils.dumpString(dataBuffer, charset);
					DataBufferUtils.release(dataBuffer);
					assertEquals("\u00A3\n", value);
				})
				.consumeNextWith(stringConsumer("\n"))
				.expectComplete()
				.verify();
	}

	@Test
	public void writePojo() {
		Flux<Pojo> source = Flux.just(new Pojo("foofoo", "barbar"), new Pojo("foofoofoo", "barbarbar"));
		testWrite(source, outputMessage, Pojo.class);

		StepVerifier.create(outputMessage.getBody())
				.consumeNextWith(stringConsumer("data:"))
				.consumeNextWith(stringConsumer("{\"foo\":\"foofoo\",\"bar\":\"barbar\"}"))
				.consumeNextWith(stringConsumer("\n"))
				.consumeNextWith(stringConsumer("\n"))
				.consumeNextWith(stringConsumer("data:"))
				.consumeNextWith(stringConsumer("{\"foo\":\"foofoofoo\",\"bar\":\"barbarbar\"}"))
				.consumeNextWith(stringConsumer("\n"))
				.consumeNextWith(stringConsumer("\n"))
				.expectComplete()
				.verify();
	}

	@Test  // SPR-14899
	public void writePojoWithPrettyPrint() {
		ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().indentOutput(true).build();
		this.messageWriter = new ServerSentEventHttpMessageWriter(new Jackson2JsonEncoder(mapper));

		Flux<Pojo> source = Flux.just(new Pojo("foofoo", "barbar"), new Pojo("foofoofoo", "barbarbar"));
		testWrite(source, outputMessage, Pojo.class);

		StepVerifier.create(outputMessage.getBody())
				.consumeNextWith(stringConsumer("data:"))
				.consumeNextWith(stringConsumer("{\n" +
						"data:  \"foo\" : \"foofoo\",\n" +
						"data:  \"bar\" : \"barbar\"\n" + "data:}"))
				.consumeNextWith(stringConsumer("\n"))
				.consumeNextWith(stringConsumer("\n"))
				.consumeNextWith(stringConsumer("data:"))
				.consumeNextWith(stringConsumer("{\n" +
						"data:  \"foo\" : \"foofoofoo\",\n" +
						"data:  \"bar\" : \"barbarbar\"\n" + "data:}"))
				.consumeNextWith(stringConsumer("\n"))
				.consumeNextWith(stringConsumer("\n"))
				.expectComplete()
				.verify();
	}

	@Test // SPR-16516, SPR-16539
	public void writePojoWithCustomEncoding() {
		Flux<Pojo> source = Flux.just(new Pojo("foo\uD834\uDD1E", "bar\uD834\uDD1E"));
		Charset charset = StandardCharsets.UTF_16LE;
		MediaType mediaType = new MediaType("text", "event-stream", charset);
		testWrite(source, mediaType, outputMessage, Pojo.class);

		assertEquals(mediaType, outputMessage.getHeaders().getContentType());
		StepVerifier.create(outputMessage.getBody())
				.consumeNextWith(dataBuffer1 -> {
					String value1 =
							DataBufferTestUtils.dumpString(dataBuffer1, charset);
					DataBufferUtils.release(dataBuffer1);
					assertEquals("data:", value1);
				})
				.consumeNextWith(dataBuffer -> {
					String value = DataBufferTestUtils.dumpString(dataBuffer, charset);
					DataBufferUtils.release(dataBuffer);
					assertEquals("{\"foo\":\"foo\uD834\uDD1E\",\"bar\":\"bar\uD834\uDD1E\"}", value);
				})
				.consumeNextWith(dataBuffer2 -> {
					String value2 =
							DataBufferTestUtils.dumpString(dataBuffer2, charset);
					DataBufferUtils.release(dataBuffer2);
					assertEquals("\n", value2);
				})
				.consumeNextWith(dataBuffer3 -> {
					String value3 =
							DataBufferTestUtils.dumpString(dataBuffer3, charset);
					DataBufferUtils.release(dataBuffer3);
					assertEquals("\n", value3);
				})
				.expectComplete()
				.verify();
	}


	private <T> void testWrite(Publisher<T> source, MockServerHttpResponse response, Class<T> clazz) {
		testWrite(source, MediaType.TEXT_EVENT_STREAM, response, clazz);
	}

	private <T> void testWrite(
			Publisher<T> source, MediaType mediaType, MockServerHttpResponse response, Class<T> clazz) {

		Mono<Void> result =
				this.messageWriter.write(source, forClass(clazz), mediaType, response, HINTS);

		StepVerifier.create(result)
				.verifyComplete();
	}

}
