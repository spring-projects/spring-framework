/*
 * Copyright 2002-2017 the original author or authors.
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
package org.springframework.test.web.reactive.server;

import java.util.Arrays;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpResponse;

import static java.nio.charset.StandardCharsets.*;
import static org.junit.Assert.*;

/**
 * Test scenarios involving a mock server.
 * @author Rossen Stoyanchev
 */
public class MockServerTests {


	@Test // SPR-15674 (in comments)
	public void mutateDoesNotCreateNewSession() {

		WebTestClient client = WebTestClient
				.bindToWebHandler(exchange -> {
					if (exchange.getRequest().getURI().getPath().equals("/set")) {
						return exchange.getSession()
								.doOnNext(session -> session.getAttributes().put("foo", "bar"))
								.then();
					}
					else {
						return exchange.getSession()
								.map(session -> session.getAttributeOrDefault("foo", "none"))
								.flatMap(value -> {
									DataBuffer buffer = toDataBuffer(value);
									return exchange.getResponse().writeWith(Mono.just(buffer));
								});
					}
				})
				.build();

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

	@Test // SPR-16059
	public void mutateDoesCopy() {

		WebTestClient.Builder builder = WebTestClient
				.bindToWebHandler(exchange -> exchange.getResponse().setComplete())
				.configureClient();

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

	@Test // SPR-16124
	public void exchangeResultHasCookieHeaders() {

		ExchangeResult result = WebTestClient
				.bindToWebHandler(exchange -> {
					ServerHttpResponse response = exchange.getResponse();
					if (exchange.getRequest().getURI().getPath().equals("/cookie")) {
						response.addCookie(ResponseCookie.from("a", "alpha").path("/pathA").build());
						response.addCookie(ResponseCookie.from("b", "beta").path("/pathB").build());
					}
					else {
						response.setStatusCode(HttpStatus.NOT_FOUND);
					}
					return response.setComplete();
				})
				.build()
				.get().uri("/cookie").cookie("a", "alpha").cookie("b", "beta")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().valueEquals(HttpHeaders.SET_COOKIE, "a=alpha; Path=/pathA", "b=beta; Path=/pathB")
				.expectBody().isEmpty();

		assertEquals(Arrays.asList("a=alpha", "b=beta"),
				result.getRequestHeaders().get(HttpHeaders.COOKIE));
	}

	@Test
	public void responseBodyContentWithFluxExchangeResult() {

		FluxExchangeResult<String> result = WebTestClient
				.bindToWebHandler(exchange -> {
					ServerHttpResponse response = exchange.getResponse();
					response.getHeaders().setContentType(MediaType.TEXT_PLAIN);
					return response.writeWith(Flux.just(toDataBuffer("body")));
				})
				.build()
				.get().uri("/")
				.exchange()
				.expectStatus().isOk()
				.returnResult(String.class);

		// Get the raw content without consuming the response body flux..
		byte[] bytes = result.getResponseBodyContent();

		assertNotNull(bytes);
		assertEquals("body", new String(bytes, UTF_8));
	}


	private DataBuffer toDataBuffer(String value) {
		byte[] bytes = value.getBytes(UTF_8);
		return new DefaultDataBufferFactory().wrap(bytes);
	}

}
