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

package org.springframework.web.reactive.function;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.function.BiFunction;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.tests.TestSubscriber;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * @author Arjen Poutsma
 */
public class BodyPopulatorsTests {

	@Test
	public void ofObject() throws Exception {
		String body = "foo";
		BodyPopulator<String> populator = BodyPopulators.fromObject(body);

		assertEquals(body, populator.supplier().get());

		BiFunction<ServerHttpResponse, Configuration, Mono<Void>> writer = populator.writer();
		MockServerHttpResponse response = new MockServerHttpResponse();
		Mono<Void> result = writer.apply(response, Configuration.builder().build());
		TestSubscriber.subscribe(result)
				.assertComplete();

		ByteBuffer byteBuffer = ByteBuffer.wrap(body.getBytes(UTF_8));
		DataBuffer buffer = new DefaultDataBufferFactory().wrap(byteBuffer);
		TestSubscriber.subscribe(response.getBody())
				.assertComplete()
				.assertValues(buffer);
	}

	@Test
	public void ofPublisher() throws Exception {
		Flux<String> body = Flux.just("foo");
		BodyPopulator<Flux<String>> populator = BodyPopulators.fromPublisher(body, String.class);

		assertEquals(body, populator.supplier().get());

		BiFunction<ServerHttpResponse, Configuration, Mono<Void>> writer = populator.writer();
		MockServerHttpResponse response = new MockServerHttpResponse();
		Mono<Void> result = writer.apply(response, Configuration.builder().build());
		TestSubscriber.subscribe(result)
				.assertComplete();

		ByteBuffer byteBuffer = ByteBuffer.wrap("foo".getBytes(UTF_8));
		DataBuffer buffer = new DefaultDataBufferFactory().wrap(byteBuffer);
		TestSubscriber.subscribe(response.getBody())
				.assertComplete()
				.assertValues(buffer);
	}

	@Test
	public void ofResource() throws Exception {
		Resource body = new ClassPathResource("response.txt", getClass());
		BodyPopulator<Resource> populator = BodyPopulators.fromResource(body);

		assertEquals(body, populator.supplier().get());

		BiFunction<ServerHttpResponse, Configuration, Mono<Void>> writer = populator.writer();
		MockServerHttpResponse response = new MockServerHttpResponse();
		Mono<Void> result = writer.apply(response, Configuration.builder().build());
		TestSubscriber.subscribe(result)
				.assertComplete();

		byte[] expectedBytes = Files.readAllBytes(body.getFile().toPath());

		TestSubscriber.subscribe(response.getBody())
				.assertComplete()
				.assertValuesWith(dataBuffer -> {
					byte[] resultBytes = new byte[dataBuffer.readableByteCount()];
					dataBuffer.read(resultBytes);
					assertArrayEquals(expectedBytes, resultBytes);
				});
	}

	@Test
	public void ofServerSentEventFlux() throws Exception {
		ServerSentEvent<String> event = ServerSentEvent.builder("foo").build();
		Flux<ServerSentEvent<String>> body = Flux.just(event);
		BodyPopulator<Flux<ServerSentEvent<String>>> populator =
				BodyPopulators.fromServerSentEvents(body);

		assertEquals(body, populator.supplier().get());

		BiFunction<ServerHttpResponse, Configuration, Mono<Void>> writer = populator.writer();
		MockServerHttpResponse response = new MockServerHttpResponse();
		Mono<Void> result = writer.apply(response, Configuration.builder().build());
		TestSubscriber.subscribe(result)
				.assertComplete();

	}

	@Test
	public void ofServerSentEventClass() throws Exception {
		Flux<String> body = Flux.just("foo");
		BodyPopulator<Flux<String>> populator =
				BodyPopulators.fromServerSentEvents(body, String.class);

		assertEquals(body, populator.supplier().get());

		BiFunction<ServerHttpResponse, Configuration, Mono<Void>> writer = populator.writer();
		MockServerHttpResponse response = new MockServerHttpResponse();
		Mono<Void> result = writer.apply(response, Configuration.builder().build());
		TestSubscriber.subscribe(result)
				.assertComplete();

	}

}