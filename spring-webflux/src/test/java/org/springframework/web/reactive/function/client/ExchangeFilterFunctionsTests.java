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

import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.core.io.buffer.support.DataBufferTestUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.BodyExtractors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Arjen Poutsma
 */
public class ExchangeFilterFunctionsTests {

	private static final URI DEFAULT_URL = URI.create("http://example.com");


	@Test
	public void andThen() {
		ClientRequest request = ClientRequest.create(HttpMethod.GET, DEFAULT_URL).build();
		ClientResponse response = mock(ClientResponse.class);
		ExchangeFunction exchange = r -> Mono.just(response);

		boolean[] filtersInvoked = new boolean[2];
		ExchangeFilterFunction filter1 = (r, n) -> {
			assertFalse(filtersInvoked[0]);
			assertFalse(filtersInvoked[1]);
			filtersInvoked[0] = true;
			assertFalse(filtersInvoked[1]);
			return n.exchange(r);
		};
		ExchangeFilterFunction filter2 = (r, n) -> {
			assertTrue(filtersInvoked[0]);
			assertFalse(filtersInvoked[1]);
			filtersInvoked[1] = true;
			return n.exchange(r);
		};
		ExchangeFilterFunction filter = filter1.andThen(filter2);


		ClientResponse result = filter.filter(request, exchange).block();
		assertEquals(response, result);

		assertTrue(filtersInvoked[0]);
		assertTrue(filtersInvoked[1]);
	}

	@Test
	public void apply() {
		ClientRequest request = ClientRequest.create(HttpMethod.GET, DEFAULT_URL).build();
		ClientResponse response = mock(ClientResponse.class);
		ExchangeFunction exchange = r -> Mono.just(response);

		boolean[] filterInvoked = new boolean[1];
		ExchangeFilterFunction filter = (r, n) -> {
			assertFalse(filterInvoked[0]);
			filterInvoked[0] = true;
			return n.exchange(r);
		};

		ExchangeFunction filteredExchange = filter.apply(exchange);
		ClientResponse result = filteredExchange.exchange(request).block();
		assertEquals(response, result);
		assertTrue(filterInvoked[0]);
	}

	@Test
	@SuppressWarnings("deprecation")
	public void basicAuthenticationUsernamePassword() {
		ClientRequest request = ClientRequest.create(HttpMethod.GET, DEFAULT_URL).build();
		ClientResponse response = mock(ClientResponse.class);

		ExchangeFunction exchange = r -> {
			assertTrue(r.headers().containsKey(HttpHeaders.AUTHORIZATION));
			assertTrue(r.headers().getFirst(HttpHeaders.AUTHORIZATION).startsWith("Basic "));
			return Mono.just(response);
		};

		ExchangeFilterFunction auth = ExchangeFilterFunctions.basicAuthentication("foo", "bar");
		assertFalse(request.headers().containsKey(HttpHeaders.AUTHORIZATION));
		ClientResponse result = auth.filter(request, exchange).block();
		assertEquals(response, result);
	}

	@Test(expected = IllegalArgumentException.class)
	public void basicAuthenticationInvalidCharacters() {

		ExchangeFilterFunctions.basicAuthentication("foo", "\ud83d\udca9");
	}

	@Test
	@SuppressWarnings("deprecation")
	public void basicAuthenticationAttributes() {
		ClientRequest request = ClientRequest.create(HttpMethod.GET, DEFAULT_URL)
				.attributes(org.springframework.web.reactive.function.client.ExchangeFilterFunctions
						.Credentials.basicAuthenticationCredentials("foo", "bar"))
				.build();
		ClientResponse response = mock(ClientResponse.class);

		ExchangeFunction exchange = r -> {
			assertTrue(r.headers().containsKey(HttpHeaders.AUTHORIZATION));
			assertTrue(r.headers().getFirst(HttpHeaders.AUTHORIZATION).startsWith("Basic "));
			return Mono.just(response);
		};

		ExchangeFilterFunction auth = ExchangeFilterFunctions.basicAuthentication();
		assertFalse(request.headers().containsKey(HttpHeaders.AUTHORIZATION));
		ClientResponse result = auth.filter(request, exchange).block();
		assertEquals(response, result);
	}

	@Test
	@SuppressWarnings("deprecation")
	public void basicAuthenticationAbsentAttributes() {
		ClientRequest request = ClientRequest.create(HttpMethod.GET, DEFAULT_URL).build();
		ClientResponse response = mock(ClientResponse.class);

		ExchangeFunction exchange = r -> {
			assertFalse(r.headers().containsKey(HttpHeaders.AUTHORIZATION));
			return Mono.just(response);
		};

		ExchangeFilterFunction auth = ExchangeFilterFunctions.basicAuthentication();
		assertFalse(request.headers().containsKey(HttpHeaders.AUTHORIZATION));
		ClientResponse result = auth.filter(request, exchange).block();
		assertEquals(response, result);
	}

	@Test
	public void statusHandlerMatch() {
		ClientRequest request = ClientRequest.create(HttpMethod.GET, DEFAULT_URL).build();
		ClientResponse response = mock(ClientResponse.class);
		when(response.statusCode()).thenReturn(HttpStatus.NOT_FOUND);

		ExchangeFunction exchange = r -> Mono.just(response);

		ExchangeFilterFunction errorHandler = ExchangeFilterFunctions.statusError(
				HttpStatus::is4xxClientError, r -> new MyException());

		Mono<ClientResponse> result = errorHandler.filter(request, exchange);

		StepVerifier.create(result)
				.expectError(MyException.class)
				.verify();
	}

	@Test
	public void statusHandlerNoMatch() {
		ClientRequest request = ClientRequest.create(HttpMethod.GET, DEFAULT_URL).build();
		ClientResponse response = mock(ClientResponse.class);
		when(response.statusCode()).thenReturn(HttpStatus.NOT_FOUND);

		Mono<ClientResponse> result = ExchangeFilterFunctions
				.statusError(HttpStatus::is5xxServerError, req -> new MyException())
				.filter(request, req -> Mono.just(response));

		StepVerifier.create(result)
				.expectNext(response)
				.expectComplete()
				.verify();
	}

	@Test
	public void limitResponseSize() {
		DefaultDataBufferFactory bufferFactory = new DefaultDataBufferFactory();
		DataBuffer b1 = dataBuffer("foo", bufferFactory);
		DataBuffer b2 = dataBuffer("bar", bufferFactory);
		DataBuffer b3 = dataBuffer("baz", bufferFactory);

		ClientRequest request = ClientRequest.create(HttpMethod.GET, DEFAULT_URL).build();
		ClientResponse response = ClientResponse.create(HttpStatus.OK).body(Flux.just(b1, b2, b3)).build();

		Mono<ClientResponse> result = ExchangeFilterFunctions.limitResponseSize(5)
				.filter(request, req -> Mono.just(response));

		StepVerifier.create(result.flatMapMany(res -> res.body(BodyExtractors.toDataBuffers())))
				.consumeNextWith(buffer -> assertEquals("foo", string(buffer)))
				.consumeNextWith(buffer -> assertEquals("ba", string(buffer)))
				.expectComplete()
				.verify();

	}

	private String string(DataBuffer buffer) {
		String value = DataBufferTestUtils.dumpString(buffer, StandardCharsets.UTF_8);
		DataBufferUtils.release(buffer);
		return value;
	}

	private DataBuffer dataBuffer(String foo, DefaultDataBufferFactory bufferFactory) {
		return bufferFactory.wrap(foo.getBytes(StandardCharsets.UTF_8));
	}


	@SuppressWarnings("serial")
	private static class MyException extends Exception {

	}

}
