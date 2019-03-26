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

package org.springframework.web.reactive.function.client;

import java.time.Duration;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DefaultWebClient}.
 * @author Rossen Stoyanchev
 */
public class DefaultWebClientTests {

	private ExchangeFunction exchangeFunction;

	@Captor
	private ArgumentCaptor<ClientRequest> captor;


	@Before
	public void setup() throws Exception {
		MockitoAnnotations.initMocks(this);
		this.exchangeFunction = mock(ExchangeFunction.class);
		when(this.exchangeFunction.exchange(captor.capture())).thenReturn(Mono.empty());
	}


	@Test
	public void basic() throws Exception {
		WebClient client = builder().build();
		client.get().uri("/path").exchange();

		ClientRequest request = verifyExchange();
		assertEquals("/base/path", request.url().toString());
		assertEquals(new HttpHeaders(), request.headers());
		assertEquals(Collections.emptyMap(), request.cookies());
	}

	@Test
	public void uriBuilder() throws Exception {
		WebClient client = builder().build();
		client.get().uri(builder -> builder.path("/path").queryParam("q", "12").build()).exchange();

		ClientRequest request = verifyExchange();
		assertEquals("/base/path?q=12", request.url().toString());
		verifyNoMoreInteractions(this.exchangeFunction);
	}

	@Test
	public void uriBuilderWithPathOverride() throws Exception {
		WebClient client = builder().build();
		client.get().uri(builder -> builder.replacePath("/path").build()).exchange();

		ClientRequest request = verifyExchange();
		assertEquals("/path", request.url().toString());
		verifyNoMoreInteractions(this.exchangeFunction);
	}

	@Test
	public void requestHeaderAndCookie() throws Exception {
		WebClient client = builder().build();
		client.get().uri("/path").accept(MediaType.APPLICATION_JSON)
				.cookies(cookies -> cookies.add("id", "123"))	// SPR-16178
				.exchange();

		ClientRequest request = verifyExchange();
		assertEquals("application/json", request.headers().getFirst("Accept"));
		assertEquals("123", request.cookies().getFirst("id"));
		verifyNoMoreInteractions(this.exchangeFunction);
	}

	@Test
	public void defaultHeaderAndCookie() throws Exception {
		WebClient client = builder().defaultHeader("Accept", "application/json").defaultCookie("id", "123").build();
		client.get().uri("/path").exchange();

		ClientRequest request = verifyExchange();
		assertEquals("application/json", request.headers().getFirst("Accept"));
		assertEquals("123", request.cookies().getFirst("id"));
		verifyNoMoreInteractions(this.exchangeFunction);
	}

	@Test
	public void defaultHeaderAndCookieOverrides() throws Exception {
		WebClient client = builder().defaultHeader("Accept", "application/json").defaultCookie("id", "123").build();
		client.get().uri("/path").header("Accept", "application/xml").cookie("id", "456").exchange();

		ClientRequest request = verifyExchange();
		assertEquals("application/xml", request.headers().getFirst("Accept"));
		assertEquals("456", request.cookies().getFirst("id"));
		verifyNoMoreInteractions(this.exchangeFunction);
	}

	@Test(expected = IllegalArgumentException.class)
	public void bodyObjectPublisher() throws Exception {
		Mono<Void> mono = Mono.empty();
		WebClient client = builder().build();

		client.post().uri("https://example.com").syncBody(mono);
	}

	@Test
	public void mutateDoesCopy() throws Exception {

		WebClient.Builder builder = WebClient.builder();
		builder.filter((request, next) -> next.exchange(request));
		builder.defaultHeader("foo", "bar");
		builder.defaultCookie("foo", "bar");
		WebClient client1 = builder.build();

		builder.filter((request, next) -> next.exchange(request));
		builder.defaultHeader("baz", "qux");
		builder.defaultCookie("baz", "qux");
		WebClient client2 = builder.build();

		WebClient.Builder mutatedBuilder = client1.mutate();

		mutatedBuilder.filter((request, next) -> next.exchange(request));
		mutatedBuilder.defaultHeader("baz", "qux");
		mutatedBuilder.defaultCookie("baz", "qux");
		WebClient clientFromMutatedBuilder = mutatedBuilder.build();

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

	@Test
	public void attributes() {
		ExchangeFilterFunction filter = (request, next) -> {
			assertEquals("bar", request.attributes().get("foo"));
			return next.exchange(request);
		};

		WebClient client = builder().filter(filter).build();

		client.get().uri("/path").attribute("foo", "bar").exchange();
	}

	@Test
	public void apply() {
		WebClient client = builder()
				.apply(builder -> builder.defaultHeader("Accept", "application/json").defaultCookie("id", "123"))
				.build();
		client.get().uri("/path").exchange();

		ClientRequest request = verifyExchange();
		assertEquals("application/json", request.headers().getFirst("Accept"));
		assertEquals("123", request.cookies().getFirst("id"));
		verifyNoMoreInteractions(this.exchangeFunction);
	}

	@Test
	public void switchToErrorOnEmptyClientResponseMono() throws Exception {
		StepVerifier.create(builder().build().get().uri("/path").exchange())
				.expectErrorMessage("The underlying HTTP client completed without emitting a response.")
				.verify(Duration.ofSeconds(5));
	}


	private WebClient.Builder builder() {
		return WebClient.builder().baseUrl("/base").exchangeFunction(this.exchangeFunction);
	}

	private ClientRequest verifyExchange() {
		ClientRequest request = this.captor.getValue();
		Mockito.verify(this.exchangeFunction).exchange(request);
		verifyNoMoreInteractions(this.exchangeFunction);
		return request;
	}

}
