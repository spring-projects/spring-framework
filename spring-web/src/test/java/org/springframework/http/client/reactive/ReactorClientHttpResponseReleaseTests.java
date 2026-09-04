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

package org.springframework.http.client.reactive;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.function.LongSupplier;
import java.util.stream.Stream;

import io.netty.buffer.PoolArenaMetric;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocatorMetric;
import io.netty.channel.ChannelOption;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.client.ReactorResourceFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

/**
 * Tests for the release of unconsumed content by {@link ReactorClientHttpResponse}.
 *
 * @author Seungbin Ko
 * @since 7.1
 */
@TestInstance(PER_CLASS)
class ReactorClientHttpResponseReleaseTests {

	private static final Duration TIMEOUT = Duration.ofSeconds(3);

	private final ReactorResourceFactory factory = new ReactorResourceFactory();

	private MockWebServer server;

	private PooledByteBufAllocator allocator;

	private ReactorClientHttpConnector connector;


	@BeforeAll
	void setUpReactorResourceFactory() {
		this.factory.setShutdownQuietPeriod(Duration.ofMillis(100));
		this.factory.afterPropertiesSet();
	}

	@AfterAll
	void destroyReactorResourceFactory() {
		this.factory.destroy();
	}

	@AfterEach
	void stopServer() {
		if (this.server != null) {
			this.server.close();
		}
	}

	private void setUp(boolean preferDirect, boolean followRedirect) throws IOException {
		this.server = new MockWebServer();
		this.server.start();
		this.allocator = new PooledByteBufAllocator(preferDirect, 1, 1, 4096, 4, 0, 0, true);
		this.connector = new ReactorClientHttpConnector(this.factory, client ->
				client.option(ChannelOption.ALLOCATOR, this.allocator).followRedirect(followRedirect));
	}


	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void bodyConsumed(boolean preferDirect) throws Exception {
		setUp(preferDirect, false);
		this.server.enqueue(new MockResponse.Builder().code(200).body("{\"foo\":\"bar\"}").build());

		StepVerifier.create(connect(HttpMethod.GET)
						.flatMapMany(ClientHttpResponse::getBody)
						.map(DataBufferUtils::release))
				.expectNextCount(1)
				.expectComplete()
				.verify(TIMEOUT);

		awaitBodyRelease();
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void cancelBeforeBodySubscribed(boolean preferDirect) throws Exception {
		setUp(preferDirect, false);
		this.server.enqueue(new MockResponse.Builder().code(200).body("{\"foo\":\"bar\"}").build());

		Sinks.Empty<Void> responseReceived = Sinks.empty();
		StepVerifier.create(connect(HttpMethod.GET)
						.doFinally(signal -> responseReceived.tryEmitEmpty())
						.flatMap(response -> Mono.never())
						.takeUntilOther(responseReceived.asMono()))
				.expectComplete()
				.verify(TIMEOUT);

		awaitBodyRelease();
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void responseDroppedWithoutSubscribingBody(boolean preferDirect) throws Exception {
		setUp(preferDirect, false);
		this.server.enqueue(new MockResponse.Builder().code(200).body("{\"foo\":\"bar\"}").build());

		StepVerifier.create(connect(HttpMethod.GET).map(ClientHttpResponse::getStatusCode))
				.expectNextCount(1)
				.expectComplete()
				.verify(TIMEOUT);

		awaitBodyRelease();
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void responseDiscardedByCancelAfterEmission(boolean preferDirect) throws Exception {
		setUp(preferDirect, false);
		this.server.enqueue(new MockResponse.Builder().code(200).body("{\"foo\":\"bar\"}").build());

		Sinks.Empty<Void> responseEmitted = Sinks.empty();
		StepVerifier.create(Mono.zip(
								connect(HttpMethod.GET)
										.doFinally(signal -> responseEmitted.tryEmitEmpty()),
								Mono.never())
						.takeUntilOther(responseEmitted.asMono()))
				.expectComplete()
				.verify(TIMEOUT);

		awaitBodyRelease();
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void headRedirectedToGetResponseDroppedWithoutSubscribingBody(boolean preferDirect) throws Exception {
		setUp(preferDirect, true);
		this.server.enqueue(new MockResponse.Builder().code(303)
				.setHeader("Location", this.server.url("/target").toString()).build());
		this.server.enqueue(new MockResponse.Builder().code(200).body("{\"foo\":\"bar\"}").build());

		StepVerifier.create(connect(HttpMethod.HEAD).map(ClientHttpResponse::getStatusCode))
				.expectNextCount(1)
				.expectComplete()
				.verify(TIMEOUT);

		awaitBodyRelease();
	}


	private Mono<ClientHttpResponse> connect(HttpMethod method) {
		return this.connector.connect(method, this.server.url("/").uri(),
				ReactiveHttpOutputMessage::setComplete);
	}

	private void awaitBodyRelease() throws InterruptedException {
		PooledByteBufAllocatorMetric metric = this.allocator.metric();
		LongSupplier activeAllocations = () ->
				Stream.concat(metric.directArenas().stream(), metric.heapArenas().stream())
						.mapToLong(PoolArenaMetric::numActiveAllocations).sum();
		Instant deadline = Instant.now().plus(TIMEOUT);
		while (activeAllocations.getAsLong() > 0 && Instant.now().isBefore(deadline)) {
			System.gc();
			Thread.sleep(50);
		}
		assertThat(activeAllocations.getAsLong()).as("ByteBuf Leak: unreleased allocations").isZero();
	}

}
