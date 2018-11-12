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
package org.springframework.web.reactive.function.client;

import java.time.Duration;
import java.util.Map;
import java.util.function.Function;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelOption;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.buffer.AbstractDataBufferAllocatingTestCase;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.client.reactive.ReactorResourceFactory;
import org.springframework.web.reactive.function.UnsupportedMediaTypeException;

import static org.junit.Assert.*;

/**
 * WebClient integration tests focusing on data buffer management.
 *
 * @author Rossen Stoyanchev
 */
public class WebClientDataBufferAllocatingTests extends AbstractDataBufferAllocatingTestCase {

	private static final Duration DELAY = Duration.ofSeconds(5);


	private MockWebServer server;

	private WebClient webClient;

	private ReactorResourceFactory factory;


	@Before
	public void setUp() {

		this.factory = new ReactorResourceFactory();
		this.factory.afterPropertiesSet();

		this.server = new MockWebServer();
		this.webClient = WebClient
				.builder()
				.clientConnector(initConnector())
				.baseUrl(this.server.url("/").toString())
				.build();
	}

	private ReactorClientHttpConnector initConnector() {
		if (bufferFactory instanceof NettyDataBufferFactory) {
			ByteBufAllocator allocator = ((NettyDataBufferFactory) bufferFactory).getByteBufAllocator();
			return new ReactorClientHttpConnector(this.factory, httpClient ->
					httpClient.tcpConfiguration(tcpClient -> tcpClient.option(ChannelOption.ALLOCATOR, allocator)));
		}
		else {
			return new ReactorClientHttpConnector();
		}
	}

	@After
	public void shutDown() throws InterruptedException {
		waitForDataBufferRelease(Duration.ofSeconds(2));
		this.factory.destroy();
	}



	@Test
	public void bodyToMonoVoid() {

		this.server.enqueue(new MockResponse()
				.setResponseCode(201)
				.setHeader("Content-Type", "application/json")
				.setChunkedBody("{\"foo\" : {\"bar\" : \"123\", \"baz\" : \"456\"}}", 5));

		Mono<Void> mono = this.webClient.get()
				.uri("/json").accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.bodyToMono(Void.class);

		StepVerifier.create(mono).expectComplete().verify(Duration.ofSeconds(3));
		assertEquals(1, this.server.getRequestCount());
	}

	@Test // SPR-17482
	public void bodyToMonoVoidWithoutContentType() {

		this.server.enqueue(new MockResponse()
				.setResponseCode(HttpStatus.ACCEPTED.value())
				.setChunkedBody("{\"foo\" : \"123\",  \"baz\" : \"456\", \"baz\" : \"456\"}", 5));

		Mono<Map<String, String>> mono = this.webClient.get()
				.uri("/sample").accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.bodyToMono(new ParameterizedTypeReference<Map<String, String>>() {});

		StepVerifier.create(mono).expectError(UnsupportedMediaTypeException.class).verify(Duration.ofSeconds(3));
		assertEquals(1, this.server.getRequestCount());
	}

	@Test
	public void onStatusWithBodyNotConsumed() {
		RuntimeException ex = new RuntimeException("response error");
		testOnStatus(ex, response -> Mono.just(ex));
	}

	@Test
	public void onStatusWithBodyConsumed() {
		RuntimeException ex = new RuntimeException("response error");
		testOnStatus(ex, response -> response.bodyToMono(Void.class).thenReturn(ex));
	}

	@Test // SPR-17473
	public void onStatusWithMonoErrorAndBodyNotConsumed() {
		RuntimeException ex = new RuntimeException("response error");
		testOnStatus(ex, response -> Mono.error(ex));
	}

	@Test
	public void onStatusWithMonoErrorAndBodyConsumed() {
		RuntimeException ex = new RuntimeException("response error");
		testOnStatus(ex, response -> response.bodyToMono(Void.class).then(Mono.error(ex)));
	}

	private void testOnStatus(Throwable expected,
			Function<ClientResponse, Mono<? extends Throwable>> exceptionFunction) {

		HttpStatus errorStatus = HttpStatus.BAD_GATEWAY;

		this.server.enqueue(new MockResponse()
				.setResponseCode(errorStatus.value())
				.setHeader("Content-Type", "application/json")
				.setChunkedBody("{\"error\" : {\"status\" : 502, \"message\" : \"Bad gateway.\"}}", 5));

		Mono<String> mono = this.webClient.get()
				.uri("/json").accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.onStatus(status -> status.equals(errorStatus), exceptionFunction)
				.bodyToMono(String.class);

		StepVerifier.create(mono).expectErrorSatisfies(actual -> assertSame(expected, actual)).verify(DELAY);
		assertEquals(1, this.server.getRequestCount());
	}

}
