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

package org.springframework.messaging.rsocket.service;

import java.time.Duration;
import java.util.List;

import io.rsocket.util.DefaultPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.messaging.rsocket.TestRSocket;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.util.MimeTypeUtils.TEXT_PLAIN;

/**
 * Tests for {@link RSocketServiceMethod} that create an interface client with
 * an {@link RSocketRequester} delegating to a {@link TestRSocket}.
 *
 * @author Rossen Stoyanchev
 */
class RSocketServiceMethodTests {

	private TestRSocket rsocket;

	private RSocketServiceProxyFactory proxyFactory;


	@BeforeEach
	void setUp() {
		this.rsocket = new TestRSocket();
		RSocketRequester requester = RSocketRequester.wrap(this.rsocket, TEXT_PLAIN, TEXT_PLAIN, RSocketStrategies.create());
		this.proxyFactory = RSocketServiceProxyFactory.builder(requester).build();
	}


	@Test
	void fireAndForget() {
		ReactorService service = this.proxyFactory.createClient(ReactorService.class);

		String payload = "p1";
		service.fireAndForget(Mono.just(payload)).block(Duration.ofSeconds(5));

		assertThat(this.rsocket.getSavedMethodName()).isEqualTo("fireAndForget");
		assertThat(this.rsocket.getSavedPayload().getMetadataUtf8()).isEqualTo("ff");
		assertThat(this.rsocket.getSavedPayload().getDataUtf8()).isEqualTo(payload);
	}

	@Test
	void requestResponse() {
		ReactorService service = this.proxyFactory.createClient(ReactorService.class);

		String payload1 = "p1";
		String payload2 = "p2";
		this.rsocket.setPayloadMonoToReturn(
				Mono.just(DefaultPayload.create(payload2)));

		String response = service.requestResponse(Mono.just(payload1)).block(Duration.ofSeconds(5));

		assertThat(response).isEqualTo(payload2);
		assertThat(this.rsocket.getSavedMethodName()).isEqualTo("requestResponse");
		assertThat(this.rsocket.getSavedPayload().getMetadataUtf8()).isEqualTo("rr");
		assertThat(this.rsocket.getSavedPayload().getDataUtf8()).isEqualTo(payload1);
	}

	@Test
	void requestStream() {
		ReactorService service = this.proxyFactory.createClient(ReactorService.class);

		String payload1 = "p1";
		String payload2 = "p2";
		String payload3 = "p3";
		this.rsocket.setPayloadFluxToReturn(
				Flux.just(DefaultPayload.create(payload2), DefaultPayload.create(payload3)));

		List<String> response = service.requestStream(Mono.just(payload1))
				.collectList()
				.block(Duration.ofSeconds(5));

		assertThat(response).containsExactly(payload2, payload3);
		assertThat(this.rsocket.getSavedMethodName()).isEqualTo("requestStream");
		assertThat(this.rsocket.getSavedPayload().getMetadataUtf8()).isEqualTo("rs");
		assertThat(this.rsocket.getSavedPayload().getDataUtf8()).isEqualTo(payload1);
	}

	@Test
	void requestChannel() {
		ReactorService service = this.proxyFactory.createClient(ReactorService.class);

		String payload1 = "p1";
		String payload2 = "p2";
		String payload3 = "p3";
		String payload4 = "p4";
		this.rsocket.setPayloadFluxToReturn(
				Flux.just(DefaultPayload.create(payload3), DefaultPayload.create(payload4)));

		List<String> response = service.requestChannel(Flux.just(payload1, payload2))
				.collectList()
				.block(Duration.ofSeconds(5));

		assertThat(response).containsExactly(payload3, payload4);
		assertThat(this.rsocket.getSavedMethodName()).isEqualTo("requestChannel");

		List<String> savedPayloads = this.rsocket.getSavedPayloadFlux()
				.map(io.rsocket.Payload::getDataUtf8)
				.collectList()
				.block(Duration.ofSeconds(5));

		assertThat(savedPayloads).containsExactly("p1", "p2");
	}


	@Test
	@Timeout(value = 5, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
	void metadataWithoutRoute() {
		MimeType composite = MimeTypeUtils.parseMimeType("message/x.rsocket.composite-metadata.v0");
		RSocketRequester requester = RSocketRequester.wrap(this.rsocket, TEXT_PLAIN, composite, RSocketStrategies.create());
		MetadataService service = RSocketServiceProxyFactory.builder(requester).build().createClient(MetadataService.class);

		service.execute("m1", TEXT_PLAIN, "m2", TEXT_PLAIN, Mono.just("p1")).block(Duration.ofSeconds(5));

		assertThat(this.rsocket.getSavedMethodName()).isEqualTo("fireAndForget");
		assertThat(this.rsocket.getSavedPayload().getDataUtf8()).isEqualTo("p1");
	}


	private interface MetadataService {

		@RSocketExchange
		Mono<Void> execute(String metadata1, MimeType mimeType1, String metadata2, MimeType mimeType2,
				@Payload Mono<String> input);
	}


	private interface ReactorService {

		@RSocketExchange("ff")
		Mono<Void> fireAndForget(@Payload Mono<String> input);

		@RSocketExchange("rr")
		Mono<String> requestResponse(@Payload Mono<String> input);

		@RSocketExchange("rs")
		Flux<String> requestStream(@Payload Mono<String> input);

		@RSocketExchange("rc")
		Flux<String> requestChannel(@Payload Flux<String> input);

	}

}
