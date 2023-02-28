/*
 * Copyright 2002-2023 the original author or authors.
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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.testfixture.io.buffer.AbstractDataBufferAllocatingTests;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpResponse;
import org.springframework.web.testfixture.xml.Pojo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.core.ResolvableType.forClass;

/**
 * Unit tests for {@link ServerSentEventHttpMessageWriter}.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
@SuppressWarnings("rawtypes")
class ServerSentEventHttpMessageWriterTests extends AbstractDataBufferAllocatingTests {

	private static final Map<String, Object> HINTS = Collections.emptyMap();

	private ServerSentEventHttpMessageWriter messageWriter =
			new ServerSentEventHttpMessageWriter(new Jackson2JsonEncoder());


	@ParameterizedDataBufferAllocatingTest
	void canWrite(DataBufferFactory bufferFactory) {
		super.bufferFactory = bufferFactory;

		assertThat(this.messageWriter.canWrite(forClass(Object.class), null)).isTrue();
		assertThat(this.messageWriter.canWrite(forClass(Object.class), new MediaType("foo", "bar"))).isFalse();

		assertThat(this.messageWriter.canWrite(null, MediaType.TEXT_EVENT_STREAM)).isTrue();
		assertThat(this.messageWriter.canWrite(forClass(ServerSentEvent.class), new MediaType("foo", "bar"))).isTrue();

		// SPR-15464
		assertThat(this.messageWriter.canWrite(ResolvableType.NONE, MediaType.TEXT_EVENT_STREAM)).isTrue();
		assertThat(this.messageWriter.canWrite(ResolvableType.NONE, new MediaType("foo", "bar"))).isFalse();
	}

	@ParameterizedDataBufferAllocatingTest
	void writeServerSentEvent(DataBufferFactory bufferFactory) {
		super.bufferFactory = bufferFactory;

		ServerSentEvent<?> event = ServerSentEvent.builder().data("bar").id("c42").event("foo")
				.comment("bla\nbla bla\nbla bla bla").retry(Duration.ofMillis(123L)).build();

		MockServerHttpResponse outputMessage = new MockServerHttpResponse(super.bufferFactory);
		Mono<ServerSentEvent> source = Mono.just(event);
		testWrite(source, outputMessage, ServerSentEvent.class);

		StepVerifier.create(outputMessage.getBody())
				.consumeNextWith(stringConsumer(
						"id:c42\nevent:foo\nretry:123\n:bla\n:bla bla\n:bla bla bla\ndata:bar\n\n"))
				.expectComplete()
				.verify();
	}

	@ParameterizedDataBufferAllocatingTest
	void writeString(DataBufferFactory bufferFactory) {
		super.bufferFactory = bufferFactory;

		MockServerHttpResponse outputMessage = new MockServerHttpResponse(super.bufferFactory);
		Flux<String> source = Flux.just("foo", "bar");
		testWrite(source, outputMessage, String.class);

		StepVerifier.create(outputMessage.getBody())
				.consumeNextWith(stringConsumer("data:foo\n\n"))
				.consumeNextWith(stringConsumer("data:bar\n\n"))
				.expectComplete()
				.verify();
	}

	@ParameterizedDataBufferAllocatingTest
	void writeMultiLineString(DataBufferFactory bufferFactory) {
		super.bufferFactory = bufferFactory;

		MockServerHttpResponse outputMessage = new MockServerHttpResponse(super.bufferFactory);
		Flux<String> source = Flux.just("foo\nbar", "foo\nbaz");
		testWrite(source, outputMessage, String.class);

		StepVerifier.create(outputMessage.getBody())
				.consumeNextWith(stringConsumer("data:foo\ndata:bar\n\n"))
				.consumeNextWith(stringConsumer("data:foo\ndata:baz\n\n"))
				.expectComplete()
				.verify();
	}

	@ParameterizedDataBufferAllocatingTest // SPR-16516
	void writeStringWithCustomCharset(DataBufferFactory bufferFactory) {
		super.bufferFactory = bufferFactory;

		MockServerHttpResponse outputMessage = new MockServerHttpResponse(super.bufferFactory);
		Flux<String> source = Flux.just("\u00A3");
		Charset charset = StandardCharsets.ISO_8859_1;
		MediaType mediaType = new MediaType("text", "event-stream", charset);
		testWrite(source, mediaType, outputMessage, String.class);

		assertThat(outputMessage.getHeaders().getContentType()).isEqualTo(mediaType);
		StepVerifier.create(outputMessage.getBody())
				.consumeNextWith(dataBuffer -> {
					String value = dataBuffer.toString(charset);
					DataBufferUtils.release(dataBuffer);
					assertThat(value).isEqualTo("data:\u00A3\n\n");
				})
				.expectComplete()
				.verify();
	}

	@ParameterizedDataBufferAllocatingTest
	void writePojo(DataBufferFactory bufferFactory) {
		super.bufferFactory = bufferFactory;

		MockServerHttpResponse outputMessage = new MockServerHttpResponse(super.bufferFactory);
		Flux<Pojo> source = Flux.just(new Pojo("foofoo", "barbar"), new Pojo("foofoofoo", "barbarbar"));
		testWrite(source, outputMessage, Pojo.class);

		StepVerifier.create(outputMessage.getBody())
				.consumeNextWith(stringConsumer("data:"))
				.consumeNextWith(stringConsumer("{\"foo\":\"foofoo\",\"bar\":\"barbar\"}"))
				.consumeNextWith(stringConsumer("\n\n"))
				.consumeNextWith(stringConsumer("data:"))
				.consumeNextWith(stringConsumer("{\"foo\":\"foofoofoo\",\"bar\":\"barbarbar\"}"))
				.consumeNextWith(stringConsumer("\n\n"))
				.expectComplete()
				.verify();
	}

	@ParameterizedDataBufferAllocatingTest  // SPR-14899
	void writePojoWithPrettyPrint(DataBufferFactory bufferFactory) {
		super.bufferFactory = bufferFactory;

		ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().indentOutput(true).build();
		this.messageWriter = new ServerSentEventHttpMessageWriter(new Jackson2JsonEncoder(mapper));

		MockServerHttpResponse outputMessage = new MockServerHttpResponse(super.bufferFactory);
		Flux<Pojo> source = Flux.just(new Pojo("foofoo", "barbar"), new Pojo("foofoofoo", "barbarbar"));
		testWrite(source, outputMessage, Pojo.class);

		StepVerifier.create(outputMessage.getBody())
				.consumeNextWith(stringConsumer("data:"))
				.consumeNextWith(stringConsumer("""
						{
						data:  "foo" : "foofoo",
						data:  "bar" : "barbar"
						data:}"""))
				.consumeNextWith(stringConsumer("\n\n"))
				.consumeNextWith(stringConsumer("data:"))
				.consumeNextWith(stringConsumer("""
						{
						data:  "foo" : "foofoofoo",
						data:  "bar" : "barbarbar"
						data:}"""))
				.consumeNextWith(stringConsumer("\n\n"))
				.expectComplete()
				.verify();
	}

	@ParameterizedDataBufferAllocatingTest // SPR-16516, SPR-16539
	void writePojoWithCustomEncoding(DataBufferFactory bufferFactory) {
		super.bufferFactory = bufferFactory;

		MockServerHttpResponse outputMessage = new MockServerHttpResponse(super.bufferFactory);
		Flux<Pojo> source = Flux.just(new Pojo("foo\uD834\uDD1E", "bar\uD834\uDD1E"));
		Charset charset = StandardCharsets.UTF_16LE;
		MediaType mediaType = new MediaType("text", "event-stream", charset);
		testWrite(source, mediaType, outputMessage, Pojo.class);

		assertThat(outputMessage.getHeaders().getContentType()).isEqualTo(mediaType);
		StepVerifier.create(outputMessage.getBody())
				.consumeNextWith(stringConsumer("data:", charset))
				.consumeNextWith(stringConsumer("{\"foo\":\"foo\uD834\uDD1E\",\"bar\":\"bar\uD834\uDD1E\"}", charset))
				.consumeNextWith(stringConsumer("\n\n", charset))
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

		StepVerifier.create(result).verifyComplete();
	}

}
