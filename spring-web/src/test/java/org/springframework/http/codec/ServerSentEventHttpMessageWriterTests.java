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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.AbstractDataBufferAllocatingTestCase;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Sebastien Deleuze
 */
public class ServerSentEventHttpMessageWriterTests extends AbstractDataBufferAllocatingTestCase {

	private ServerSentEventHttpMessageWriter messageWriter = new ServerSentEventHttpMessageWriter(
			Collections.singletonList(new Jackson2JsonEncoder()));

	@Test
	public void cantRead() {
		assertFalse(messageWriter.canWrite(ResolvableType.forClass(Object.class),
				new MediaType("foo", "bar")));
	}

	@Test
	public void canRead() {
		assertTrue(messageWriter.canWrite(ResolvableType.forClass(Object.class), null));
		assertTrue(messageWriter.canWrite(ResolvableType.forClass(Object.class),
				new MediaType("text", "event-stream")));
		assertTrue(messageWriter.canWrite(ResolvableType.forClass(ServerSentEvent.class),
				new MediaType("bar", "bar")));
	}

	@Test
	public void writeServerSentEvent() {
		ServerSentEvent<String> event = ServerSentEvent.<String>builder().
				data("bar").id("c42").event("foo").comment("bla\nbla bla\nbla bla bla")
				.retry(Duration.ofMillis(123L)).build();

		Mono<ServerSentEvent<String>> source = Mono.just(event);
		MockServerHttpResponse outputMessage = new MockServerHttpResponse();
		messageWriter.write(source, ResolvableType.forClass(ServerSentEvent.class),
				new MediaType("text", "event-stream"), outputMessage, Collections.emptyMap()).block(Duration.ofMillis(5000));

		StepVerifier.create(outputMessage.getBodyAsString())
				.expectNext("id:c42\n" +
						"event:foo\n" +
						"retry:123\n" +
						":bla\n" +
						":bla bla\n" +
						":bla bla bla\n" +
						"data:bar\n\n")
				.expectComplete()
				.verify();
	}

	@Test
	public void writeString() {
		Flux<String> source = Flux.just("foo", "bar");
		MockServerHttpResponse outputMessage = new MockServerHttpResponse();
		messageWriter.write(source, ResolvableType.forClass(String.class),
				new MediaType("text", "event-stream"), outputMessage, Collections.emptyMap()).block(Duration.ofMillis(5000));

		StepVerifier.create(outputMessage.getBodyAsString())
				.expectNext("data:foo\n\ndata:bar\n\n")
				.expectComplete()
				.verify();
	}

	@Test
	public void writeMultiLineString() {
		Flux<String> source = Flux.just("foo\nbar", "foo\nbaz");
		MockServerHttpResponse outputMessage = new MockServerHttpResponse();
		messageWriter.write(source, ResolvableType.forClass(String.class),
				new MediaType("text", "event-stream"), outputMessage, Collections.emptyMap()).block(Duration.ofMillis(5000));

		StepVerifier.create(outputMessage.getBodyAsString())
				.expectNext("data:foo\n" +
						"data:bar\n\n" +
						"data:foo\n" +
						"data:baz\n\n")
				.expectComplete()
				.verify();
	}

	@Test
	public void writePojo() {
		Flux<Pojo> source = Flux.just(new Pojo("foofoo", "barbar"),
				new Pojo("foofoofoo", "barbarbar"));
		MockServerHttpResponse outputMessage = new MockServerHttpResponse();
		messageWriter.write(source, ResolvableType.forClass(Pojo.class),
				MediaType.TEXT_EVENT_STREAM, outputMessage, Collections.emptyMap()).block(Duration.ofMillis(5000));

		StepVerifier.create(outputMessage.getBodyAsString())
				.expectNext("data:{\"foo\":\"foofoo\",\"bar\":\"barbar\"}\n\n" +
						"data:{\"foo\":\"foofoofoo\",\"bar\":\"barbarbar\"}\n\n")
				.expectComplete()
				.verify();
	}

	@Test  // SPR-14899
	public void writePojoWithPrettyPrint() {
		ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().indentOutput(true).build();
		this.messageWriter = new ServerSentEventHttpMessageWriter(
				Collections.singletonList(new Jackson2JsonEncoder(mapper)));

		Flux<Pojo> source = Flux.just(new Pojo("foofoo", "barbar"),
				new Pojo("foofoofoo", "barbarbar"));
		MockServerHttpResponse outputMessage = new MockServerHttpResponse();
		messageWriter.write(source, ResolvableType.forClass(Pojo.class),
				MediaType.TEXT_EVENT_STREAM, outputMessage, Collections.emptyMap()).block(Duration.ofMillis(5000));

		StepVerifier.create(outputMessage.getBodyAsString())
				.expectNext("data:{\n" +
						"data:  \"foo\" : \"foofoo\",\n" +
						"data:  \"bar\" : \"barbar\"\n" + "data:}\n\n" +
						"data:{\n" +
						"data:  \"foo\" : \"foofoofoo\",\n" +
						"data:  \"bar\" : \"barbarbar\"\n" + "data:}\n\n")
				.expectComplete()
				.verify();
	}

}
