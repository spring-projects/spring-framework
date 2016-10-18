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

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.subscriber.ScriptedSubscriber;

import org.springframework.core.codec.ByteBufferEncoder;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.codec.xml.Jaxb2XmlEncoder;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * @author Arjen Poutsma
 */
public class BodyInsertersTests {

	private BodyInserter.Context context;

	@Before
	public void createContext() {
		final List<HttpMessageWriter<?>> messageWriters = new ArrayList<>();
		messageWriters.add(new EncoderHttpMessageWriter<>(new ByteBufferEncoder()));
		messageWriters.add(new EncoderHttpMessageWriter<>(new CharSequenceEncoder()));
		messageWriters.add(new EncoderHttpMessageWriter<>(new Jaxb2XmlEncoder()));
		messageWriters.add(new EncoderHttpMessageWriter<>(new Jackson2JsonEncoder()));

		this.context = new BodyInserter.Context() {
			@Override
			public Supplier<Stream<HttpMessageWriter<?>>> messageWriters() {
				return messageWriters::stream;
			}
		};

	}


	@Test
	public void ofObject() throws Exception {
		String body = "foo";
		BodyInserter<String, ReactiveHttpOutputMessage> inserter = BodyInserters.fromObject(body);

		assertEquals(body, inserter.t());

		MockServerHttpResponse response = new MockServerHttpResponse();
		Mono<Void> result = inserter.insert(response, this.context);
		ScriptedSubscriber.create().expectComplete().verify(result);

		ByteBuffer byteBuffer = ByteBuffer.wrap(body.getBytes(UTF_8));
		DataBuffer buffer = new DefaultDataBufferFactory().wrap(byteBuffer);
		ScriptedSubscriber.<DataBuffer>create()
				.expectNext(buffer)
				.expectComplete()
				.verify(response.getBody());
	}

	@Test
	public void ofPublisher() throws Exception {
		Flux<String> body = Flux.just("foo");
		BodyInserter<Flux<String>, ReactiveHttpOutputMessage> inserter = BodyInserters.fromPublisher(body, String.class);

		assertEquals(body, inserter.t());

		MockServerHttpResponse response = new MockServerHttpResponse();
		Mono<Void> result = inserter.insert(response, this.context);
		ScriptedSubscriber.create().expectComplete().verify(result);

		ByteBuffer byteBuffer = ByteBuffer.wrap("foo".getBytes(UTF_8));
		DataBuffer buffer = new DefaultDataBufferFactory().wrap(byteBuffer);
		ScriptedSubscriber.<DataBuffer>create()
				.expectNext(buffer)
				.expectComplete()
				.verify(response.getBody());
	}

	@Test
	public void ofResource() throws Exception {
		Resource body = new ClassPathResource("response.txt", getClass());
		BodyInserter<Resource, ReactiveHttpOutputMessage> inserter = BodyInserters.fromResource(body);

		assertEquals(body, inserter.t());

		MockServerHttpResponse response = new MockServerHttpResponse();
		Mono<Void> result = inserter.insert(response, this.context);
		ScriptedSubscriber.create().expectComplete().verify(result);

		byte[] expectedBytes = Files.readAllBytes(body.getFile().toPath());

		ScriptedSubscriber.<DataBuffer>create()
				.consumeNextWith(dataBuffer -> {
					byte[] resultBytes = new byte[dataBuffer.readableByteCount()];
					dataBuffer.read(resultBytes);
					assertArrayEquals(expectedBytes, resultBytes);
				})
				.expectComplete()
				.verify(response.getBody());
	}

	@Test
	public void ofServerSentEventFlux() throws Exception {
		ServerSentEvent<String> event = ServerSentEvent.builder("foo").build();
		Flux<ServerSentEvent<String>> body = Flux.just(event);
		BodyInserter<Flux<ServerSentEvent<String>>, ServerHttpResponse> inserter =
				BodyInserters.fromServerSentEvents(body);

		assertEquals(body, inserter.t());

		MockServerHttpResponse response = new MockServerHttpResponse();
		Mono<Void> result = inserter.insert(response, this.context);
		ScriptedSubscriber.create().expectNextCount(0).expectComplete().verify(result);
	}

	@Test
	public void ofServerSentEventClass() throws Exception {
		Flux<String> body = Flux.just("foo");
		BodyInserter<Flux<String>, ServerHttpResponse> inserter =
				BodyInserters.fromServerSentEvents(body, String.class);

		assertEquals(body, inserter.t());

		MockServerHttpResponse response = new MockServerHttpResponse();
		Mono<Void> result = inserter.insert(response, this.context);
		ScriptedSubscriber.create().expectNextCount(0).expectComplete().verify(result);
	}

}