/*
 * Copyright 2002-2017 the original author or authors.
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
package org.springframework.test.web.reactive.server;

import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.ResponseCookie;
import org.springframework.web.server.WebHandler;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;

/**
 * Mock server integration test scenarios.
 * @author Rossen Stoyanchev
 */
public class MockServerIntegrationTests {


	@Test
	public void sameSessionInstanceAfterMutate() throws Exception {

		WebHandler webHandler = exchange -> {
			if (exchange.getRequest().getURI().getPath().equals("/set")) {
				return exchange.getSession()
						.doOnNext(session -> session.getAttributes().put("foo", "bar"))
						.then();
			}
			else {
				return exchange.getSession()
						.map(session -> session.getAttributeOrDefault("foo", "none"))
						.flatMap(value -> {
							byte[] bytes = value.getBytes(UTF_8);
							DataBuffer buffer = new DefaultDataBufferFactory().wrap(bytes);
							return exchange.getResponse().writeWith(Mono.just(buffer));
						});
			}
		};

		WebTestClient client = new DefaultMockServerSpec(webHandler).configureClient().build();

		// Set the session attribute
		EntityExchangeResult<Void> result = client.get().uri("/set").exchange()
				.expectStatus().isOk().expectBody().isEmpty();

		ResponseCookie session = result.getResponseCookies().getFirst("SESSION");

		// Now get attribute
		client.mutate().build()
				.get().uri("/get")
				.cookie(session.getName(), session.getValue())
				.exchange()
				.expectBody(String.class).isEqualTo("bar");
	}

	@Test
	public void mutateDoesCopy() throws Exception {

		WebTestClient.Builder builder = WebTestClient.bindToWebHandler(exchange -> exchange.getResponse().setComplete()).configureClient();
		builder.filter((request, next) -> next.exchange(request));
		builder.defaultHeader("foo", "bar");
		builder.defaultCookie("foo", "bar");
		WebTestClient client1 = builder.build();

		builder.filter((request, next) -> next.exchange(request));
		builder.defaultHeader("baz", "qux");
		builder.defaultCookie("baz", "qux");
		WebTestClient client2 = builder.build();

		WebTestClient.Builder mutatedBuilder = client1.mutate();

		mutatedBuilder.filter((request, next) -> next.exchange(request));
		mutatedBuilder.defaultHeader("baz", "qux");
		mutatedBuilder.defaultCookie("baz", "qux");
		WebTestClient clientFromMutatedBuilder = mutatedBuilder.build();

		client1.mutate().filters(filters -> assertEquals(1, filters.size()));
		client1.mutate().defaultHeaders(headers -> assertEquals(1, headers.size()));
		client1.mutate().defaultCookies(cookies -> assertEquals(1, cookies.size()));

		client2.mutate().filters(filters -> assertEquals(2, filters.size()));
		client2.mutate().defaultHeaders(headers -> assertEquals(2, headers.size()));
		client2.mutate().defaultCookies(cookies -> assertEquals(2, cookies.size()));

		clientFromMutatedBuilder.mutate().filters(filters -> assertEquals(2, filters.size()));
		clientFromMutatedBuilder.mutate().defaultHeaders(headers -> assertEquals(2, headers.size()));
		clientFromMutatedBuilder.mutate().defaultCookies(cookies -> assertEquals(2, cookies.size()));
	}


}
