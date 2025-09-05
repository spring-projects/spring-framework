/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.reactive.function.client;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelOption;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.core.testfixture.io.buffer.AbstractDataBufferAllocatingTests;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ReactorResourceFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

/**
 * WebClient integration tests focusing on data buffer management.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
@TestInstance(PER_CLASS)
class WebClientDataBufferAllocatingTests extends AbstractDataBufferAllocatingTests {

	private static final Duration DELAY = Duration.ofSeconds(5);

	private final ReactorResourceFactory factory = new ReactorResourceFactory();
	private MockWebServer server;
	private WebClient webClient;


	@BeforeAll
	void setUpReactorResourceFactory() {
		this.factory.setShutdownQuietPeriod(Duration.ofMillis(100));
		this.factory.afterPropertiesSet();
	}

	@AfterAll
	void destroyReactorResourceFactory() {
		this.factory.destroy();
	}

	private void setUp(DataBufferFactory bufferFactory) throws IOException {
		super.bufferFactory = bufferFactory;
		this.server = new MockWebServer();
		this.server.start();
		this.webClient = WebClient
				.builder()
				.clientConnector(initConnector())
				.baseUrl(this.server.url("/").toString())
				.build();
	}

	private ReactorClientHttpConnector initConnector() {
		assertThat(super.bufferFactory).isNotNull();

		if (super.bufferFactory instanceof NettyDataBufferFactory nettyDataBufferFactory) {
			ByteBufAllocator allocator = nettyDataBufferFactory.getByteBufAllocator();
			return new ReactorClientHttpConnector(this.factory,
					client -> client.option(ChannelOption.ALLOCATOR, allocator));
		}
		else {
			return new ReactorClientHttpConnector();
		}
	}


	@ParameterizedDataBufferAllocatingTest
	void bodyToMonoVoid(DataBufferFactory bufferFactory) throws IOException {
		setUp(bufferFactory);

		this.server.enqueue(new MockResponse.Builder().
				code(201)
				.setHeader("Content-Type", "application/json")
				.chunkedBody("{\"foo\" : {\"bar\" : \"123\", \"baz\" : \"456\"}}", 5)
				.build());

		Mono<Void> mono = this.webClient.get()
				.uri("/json").accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.bodyToMono(Void.class);

		StepVerifier.create(mono).expectComplete().verify(Duration.ofSeconds(3));
		assertThat(this.server.getRequestCount()).isEqualTo(1);
	}

	@ParameterizedDataBufferAllocatingTest // SPR-17482
	void bodyToMonoVoidWithoutContentType(DataBufferFactory bufferFactory) throws IOException {
		setUp(bufferFactory);

		this.server.enqueue(new MockResponse.Builder()
				.code(HttpStatus.ACCEPTED.value())
				.chunkedBody("{\"foo\" : \"123\",  \"baz\" : \"456\", \"baz\" : \"456\"}", 5)
				.build());

		Mono<Map<String, String>> mono = this.webClient.get()
				.uri("/sample").accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.bodyToMono(new ParameterizedTypeReference<>() {});

		StepVerifier.create(mono).expectError(WebClientResponseException.class).verify(Duration.ofSeconds(3));
		assertThat(this.server.getRequestCount()).isEqualTo(1);
	}

	@ParameterizedDataBufferAllocatingTest
	void onStatusWithBodyNotConsumed(DataBufferFactory bufferFactory) throws IOException {
		setUp(bufferFactory);

		RuntimeException ex = new RuntimeException("response error");
		testOnStatus(ex, response -> Mono.just(ex));
	}

	@ParameterizedDataBufferAllocatingTest
	void onStatusWithBodyConsumed(DataBufferFactory bufferFactory) throws IOException {
		setUp(bufferFactory);

		RuntimeException ex = new RuntimeException("response error");
		testOnStatus(ex, response -> response.bodyToMono(Void.class).thenReturn(ex));
	}

	@ParameterizedDataBufferAllocatingTest // SPR-17473
	void onStatusWithMonoErrorAndBodyNotConsumed(DataBufferFactory bufferFactory) throws IOException {
		setUp(bufferFactory);

		RuntimeException ex = new RuntimeException("response error");
		testOnStatus(ex, response -> Mono.error(ex));
	}

	@ParameterizedDataBufferAllocatingTest
	void onStatusWithMonoErrorAndBodyConsumed(DataBufferFactory bufferFactory) throws IOException {
		setUp(bufferFactory);

		RuntimeException ex = new RuntimeException("response error");
		testOnStatus(ex, response -> response.bodyToMono(Void.class).then(Mono.error(ex)));
	}

	@ParameterizedDataBufferAllocatingTest // gh-23230
	void onStatusWithImmediateErrorAndBodyNotConsumed(DataBufferFactory bufferFactory) throws IOException {
		setUp(bufferFactory);

		RuntimeException ex = new RuntimeException("response error");
		testOnStatus(ex, response -> {
			throw ex;
		});
	}

	@ParameterizedDataBufferAllocatingTest
	void releaseBody(DataBufferFactory bufferFactory) throws IOException {
		setUp(bufferFactory);

		this.server.enqueue(new MockResponse.Builder()
				.code(200)
				.setHeader("Content-Type", "text/plain")
				.body("foo bar")
				.build());

		Mono<Void> result = this.webClient.get()
				.exchangeToMono(ClientResponse::releaseBody);

		StepVerifier.create(result)
				.expectComplete()
				.verify(Duration.ofSeconds(3));
	}

	@ParameterizedDataBufferAllocatingTest
	void exchangeToBodilessEntity(DataBufferFactory bufferFactory) throws IOException {
		setUp(bufferFactory);

		this.server.enqueue(new MockResponse.Builder()
				.code(201)
				.setHeader("Foo", "bar")
				.body("foo bar")
				.build());

		Mono<ResponseEntity<Void>> result = this.webClient.get()
				.exchangeToMono(ClientResponse::toBodilessEntity);

		StepVerifier.create(result)
				.assertNext(entity -> {
					assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.CREATED);
					assertThat(entity.getHeaders().hasHeaderValues("Foo", Collections.singletonList("bar"))).isTrue();
					assertThat(entity.getBody()).isNull();
				})
				.expectComplete()
				.verify(Duration.ofSeconds(3));
	}


	private void testOnStatus(Throwable expected,
			Function<ClientResponse, Mono<? extends Throwable>> exceptionFunction) {

		HttpStatus errorStatus = HttpStatus.BAD_GATEWAY;

		this.server.enqueue(new MockResponse.Builder()
				.code(errorStatus.value())
				.setHeader("Content-Type", "application/json")
				.chunkedBody("{\"error\" : {\"status\" : 502, \"message\" : \"Bad gateway.\"}}", 5)
				.build());

		Mono<String> mono = this.webClient.get()
				.uri("/json").accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.onStatus(status -> status.equals(errorStatus), exceptionFunction)
				.bodyToMono(String.class);

		StepVerifier.create(mono).expectErrorSatisfies(actual -> assertThat(actual).isSameAs(expected)).verify(DELAY);
		assertThat(this.server.getRequestCount()).isEqualTo(1);
	}

}
