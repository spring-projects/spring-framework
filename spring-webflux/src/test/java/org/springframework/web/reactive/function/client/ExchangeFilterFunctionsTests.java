/*
 * Copyright 2002-2019 the original author or authors.
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

import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.BodyExtractors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link ExchangeFilterFunctions}.
 *
 * @author Arjen Poutsma
 */
public class ExchangeFilterFunctionsTests {

	private static final URI DEFAULT_URL = URI.create("https://example.com");


	@Test
	public void andThen() {
		ClientRequest request = ClientRequest.create(HttpMethod.GET, DEFAULT_URL).build();
		ClientResponse response = mock(ClientResponse.class);
		ExchangeFunction exchange = r -> Mono.just(response);

		boolean[] filtersInvoked = new boolean[2];
		ExchangeFilterFunction filter1 = (r, n) -> {
			assertThat(filtersInvoked[0]).isFalse();
			assertThat(filtersInvoked[1]).isFalse();
			filtersInvoked[0] = true;
			assertThat(filtersInvoked[1]).isFalse();
			return n.exchange(r);
		};
		ExchangeFilterFunction filter2 = (r, n) -> {
			assertThat(filtersInvoked[0]).isTrue();
			assertThat(filtersInvoked[1]).isFalse();
			filtersInvoked[1] = true;
			return n.exchange(r);
		};
		ExchangeFilterFunction filter = filter1.andThen(filter2);


		ClientResponse result = filter.filter(request, exchange).block();
		assertThat(result).isEqualTo(response);

		assertThat(filtersInvoked[0]).isTrue();
		assertThat(filtersInvoked[1]).isTrue();
	}

	@Test
	public void apply() {
		ClientRequest request = ClientRequest.create(HttpMethod.GET, DEFAULT_URL).build();
		ClientResponse response = mock(ClientResponse.class);
		ExchangeFunction exchange = r -> Mono.just(response);

		boolean[] filterInvoked = new boolean[1];
		ExchangeFilterFunction filter = (r, n) -> {
			assertThat(filterInvoked[0]).isFalse();
			filterInvoked[0] = true;
			return n.exchange(r);
		};

		ExchangeFunction filteredExchange = filter.apply(exchange);
		ClientResponse result = filteredExchange.exchange(request).block();
		assertThat(result).isEqualTo(response);
		assertThat(filterInvoked[0]).isTrue();
	}

	@Test
	public void basicAuthenticationUsernamePassword() {
		ClientRequest request = ClientRequest.create(HttpMethod.GET, DEFAULT_URL).build();
		ClientResponse response = mock(ClientResponse.class);

		ExchangeFunction exchange = r -> {
			assertThat(r.headers().containsKey(HttpHeaders.AUTHORIZATION)).isTrue();
			assertThat(r.headers().getFirst(HttpHeaders.AUTHORIZATION).startsWith("Basic ")).isTrue();
			return Mono.just(response);
		};

		ExchangeFilterFunction auth = ExchangeFilterFunctions.basicAuthentication("foo", "bar");
		assertThat(request.headers().containsKey(HttpHeaders.AUTHORIZATION)).isFalse();
		ClientResponse result = auth.filter(request, exchange).block();
		assertThat(result).isEqualTo(response);
	}

	@Test
	public void basicAuthenticationInvalidCharacters() {
		ClientRequest request = ClientRequest.create(HttpMethod.GET, DEFAULT_URL).build();
		ExchangeFunction exchange = r -> Mono.just(mock(ClientResponse.class));

		assertThatIllegalArgumentException().isThrownBy(() ->
				ExchangeFilterFunctions.basicAuthentication("foo", "\ud83d\udca9").filter(request, exchange));
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
			assertThat(r.headers().containsKey(HttpHeaders.AUTHORIZATION)).isTrue();
			assertThat(r.headers().getFirst(HttpHeaders.AUTHORIZATION).startsWith("Basic ")).isTrue();
			return Mono.just(response);
		};

		ExchangeFilterFunction auth = ExchangeFilterFunctions.basicAuthentication();
		assertThat(request.headers().containsKey(HttpHeaders.AUTHORIZATION)).isFalse();
		ClientResponse result = auth.filter(request, exchange).block();
		assertThat(result).isEqualTo(response);
	}

	@Test
	@SuppressWarnings("deprecation")
	public void basicAuthenticationAbsentAttributes() {
		ClientRequest request = ClientRequest.create(HttpMethod.GET, DEFAULT_URL).build();
		ClientResponse response = mock(ClientResponse.class);

		ExchangeFunction exchange = r -> {
			assertThat(r.headers().containsKey(HttpHeaders.AUTHORIZATION)).isFalse();
			return Mono.just(response);
		};

		ExchangeFilterFunction auth = ExchangeFilterFunctions.basicAuthentication();
		assertThat(request.headers().containsKey(HttpHeaders.AUTHORIZATION)).isFalse();
		ClientResponse result = auth.filter(request, exchange).block();
		assertThat(result).isEqualTo(response);
	}

	@Test
	public void statusHandlerMatch() {
		ClientRequest request = ClientRequest.create(HttpMethod.GET, DEFAULT_URL).build();
		ClientResponse response = mock(ClientResponse.class);
		given(response.statusCode()).willReturn(HttpStatus.NOT_FOUND);

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
		given(response.statusCode()).willReturn(HttpStatus.NOT_FOUND);

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
				.consumeNextWith(buffer -> assertThat(string(buffer)).isEqualTo("foo"))
				.consumeNextWith(buffer -> assertThat(string(buffer)).isEqualTo("ba"))
				.expectComplete()
				.verify();

	}

	private String string(DataBuffer buffer) {
		String value = buffer.toString(UTF_8);
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
