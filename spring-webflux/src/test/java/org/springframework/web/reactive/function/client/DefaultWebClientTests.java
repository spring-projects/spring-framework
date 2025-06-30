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

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.NamedThreadLocal;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.web.reactive.function.BodyExtractors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultWebClient}.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 */
@MockitoSettings(strictness = Strictness.LENIENT)
public class DefaultWebClientTests {

	@Mock
	private ExchangeFunction exchangeFunction;

	@Captor
	private ArgumentCaptor<ClientRequest> captor;

	private WebClient.Builder builder;


	@BeforeEach
	void setup() {
		ClientResponse mockResponse = mock();
		when(mockResponse.statusCode()).thenReturn(HttpStatus.OK);
		when(mockResponse.bodyToMono(Void.class)).thenReturn(Mono.empty());
		given(this.exchangeFunction.exchange(this.captor.capture())).willReturn(Mono.just(mockResponse));
		this.builder = WebClient.builder().baseUrl("/base").exchangeFunction(this.exchangeFunction);
	}


	@Test
	void basic() {
		this.builder.build().get().uri("/path")
				.retrieve().bodyToMono(Void.class).block(Duration.ofSeconds(10));

		ClientRequest request = verifyAndGetRequest();
		assertThat(request.url().toString()).isEqualTo("/base/path");
		assertThat(request.headers()).isEqualTo(new HttpHeaders());
		assertThat(request.cookies()).isEqualTo(Collections.emptyMap());
	}

	@Test
	void uriBuilder() {
		this.builder.build().get()
				.uri(builder -> builder.path("/path").queryParam("q", "12").build())
				.retrieve().bodyToMono(Void.class).block(Duration.ofSeconds(10));

		ClientRequest request = verifyAndGetRequest();
		assertThat(request.url().toString()).isEqualTo("/base/path?q=12");
	}

	@Test // gh-22705
	public void uriBuilderWithUriTemplate() {
		this.builder.build().get()
				.uri("/path/{id}", builder -> builder.queryParam("q", "12").build("identifier"))
				.retrieve().bodyToMono(Void.class).block(Duration.ofSeconds(10));

		ClientRequest request = verifyAndGetRequest();
		assertThat(request.url().toString()).isEqualTo("/base/path/identifier?q=12");
		assertThat(request.attribute(WebClient.class.getName() + ".uriTemplate")).contains("/base/path/{id}");
	}

	@Test
	void uriBuilderWithPathOverride() {
		this.builder.build().get()
				.uri(builder -> builder.replacePath("/path").build())
				.retrieve().bodyToMono(Void.class).block(Duration.ofSeconds(10));

		ClientRequest request = verifyAndGetRequest();
		assertThat(request.url().toString()).isEqualTo("/path");
	}

	@Test
	void requestHeaderAndCookie() {
		this.builder.build().get().uri("/path").accept(MediaType.APPLICATION_JSON)
				.cookies(cookies -> cookies.add("id", "123"))	// SPR-16178
				.retrieve().bodyToMono(Void.class).block(Duration.ofSeconds(10));

		ClientRequest request = verifyAndGetRequest();
		assertThat(request.headers().getFirst("Accept")).isEqualTo("application/json");
		assertThat(request.cookies().getFirst("id")).isEqualTo("123");
	}

	@Test
	void httpRequest() {
		this.builder.build().get().uri("/path")
				.httpRequest(httpRequest -> {})
				.retrieve().bodyToMono(Void.class).block(Duration.ofSeconds(10));

		ClientRequest request = verifyAndGetRequest();
		assertThat(request.httpRequest()).isNotNull();
	}

	@Test
	void defaultHeaderAndCookie() {
		WebClient client = this.builder
				.defaultHeader("Accept", "application/json")
				.defaultCookie("id", "123")
				.build();

		client.get().uri("/path")
				.retrieve().bodyToMono(Void.class).block(Duration.ofSeconds(10));

		ClientRequest request = verifyAndGetRequest();
		assertThat(request.headers().getFirst("Accept")).isEqualTo("application/json");
		assertThat(request.cookies().getFirst("id")).isEqualTo("123");
	}

	@Test
	void defaultHeaderAndCookieOverrides() {
		WebClient client = this.builder
				.defaultHeader("Accept", "application/json")
				.defaultCookie("id", "123")
				.build();

		client.get().uri("/path")
				.header("Accept", "application/xml")
				.cookie("id", "456")
				.retrieve().bodyToMono(Void.class).block(Duration.ofSeconds(10));

		ClientRequest request = verifyAndGetRequest();
		assertThat(request.headers().getFirst("Accept")).isEqualTo("application/xml");
		assertThat(request.cookies().getFirst("id")).isEqualTo("456");
	}

	@Test
	void defaultHeaderAndCookieCopies() {
		WebClient client1 = this.builder
				.defaultHeader("Accept", "application/json")
				.defaultCookie("id", "123")
				.build();
		WebClient client2 = this.builder
				.defaultHeader("Accept", "application/xml")
				.defaultCookies(cookies -> cookies.set("id", "456"))
				.build();

		client1.get().uri("/path")
				.retrieve().bodyToMono(Void.class).block(Duration.ofSeconds(10));

		ClientRequest request = verifyAndGetRequest();
		assertThat(request.headers().getFirst("Accept")).isEqualTo("application/json");
		assertThat(request.cookies().getFirst("id")).isEqualTo("123");


		client2.get().uri("/path")
				.retrieve().bodyToMono(Void.class).block(Duration.ofSeconds(10));

		request = verifyAndGetRequest();
		assertThat(request.headers().getFirst("Accept")).isEqualTo("application/xml");
		assertThat(request.cookies().getFirst("id")).isEqualTo("456");
	}

	@Test
	void defaultRequest() {
		ThreadLocal<String> context = new NamedThreadLocal<>("foo");

		Map<String, Object> actual = new HashMap<>();
		ExchangeFilterFunction filter = (request, next) -> {
			actual.putAll(request.attributes());
			return next.exchange(request);
		};

		WebClient client = this.builder
				.defaultRequest(spec -> spec.attribute("foo", context.get()))
				.filter(filter)
				.build();

		try {
			context.set("bar");
			client.get().uri("/path").attribute("foo", "bar")
					.retrieve().bodyToMono(Void.class).block(Duration.ofSeconds(10));
		}
		finally {
			context.remove();
		}

		assertThat(actual.get("foo")).isEqualTo("bar");
	}

	@Test
	void bodyObjectPublisher() {
		Mono<Void> mono = Mono.empty();
		WebClient client = this.builder.build();

		assertThatIllegalArgumentException().isThrownBy(() ->
				client.post().uri("https://example.com").bodyValue(mono));
	}

	@Test
	void mutateDoesCopy() {
		// First, build the clients

		WebClient.Builder builder = WebClient.builder()
				.filter((request, next) -> next.exchange(request))
				.defaultHeader("foo", "bar")
				.defaultCookie("foo", "bar");

		WebClient client1 = builder.build();

		WebClient client2 = builder.filter((request, next) -> next.exchange(request))
				.defaultHeader("baz", "qux")
				.defaultCookie("baz", "qux")
				.build();

		WebClient client1a = client1.mutate()
				.filter((request, next) -> next.exchange(request))
				.defaultHeader("baz", "qux")
				.defaultCookie("baz", "qux")
				.build();

		// Now, verify what each client has.

		WebClient.Builder builder1 = client1.mutate();
		builder1.filters(filters -> assertThat(filters).hasSize(1));
		builder1.defaultHeaders(headers -> assertThat(headers.size()).isOne());
		builder1.defaultCookies(cookies -> assertThat(cookies).hasSize(1));

		WebClient.Builder builder2 = client2.mutate();
		builder2.filters(filters -> assertThat(filters).hasSize(2));
		builder2.defaultHeaders(headers -> assertThat(headers.size()).isEqualTo(2));
		builder2.defaultCookies(cookies -> assertThat(cookies).hasSize(2));

		WebClient.Builder builder1a = client1a.mutate();
		builder1a.filters(filters -> assertThat(filters).hasSize(2));
		builder1a.defaultHeaders(headers -> assertThat(headers.size()).isEqualTo(2));
		builder1a.defaultCookies(cookies -> assertThat(cookies).hasSize(2));
	}

	@Test
	void cloneBuilder() {
		Consumer<ClientCodecConfigurer> codecsConfig = c -> {};
		ExchangeFunction exchangeFunction = request -> Mono.empty();
		WebClient.Builder builder = WebClient.builder().baseUrl("https://example.org")
				.exchangeFunction(exchangeFunction)
				.filter((request, next) -> Mono.empty())
				.codecs(codecsConfig);

		WebClient.Builder clonedBuilder = builder.clone();

		assertThat(clonedBuilder).extracting("baseUrl").isEqualTo("https://example.org");
		assertThat(clonedBuilder).extracting("filters").isNotNull();
		assertThat(clonedBuilder).extracting("strategiesConfigurers").isNotNull();
		assertThat(clonedBuilder).extracting("exchangeFunction").isEqualTo(exchangeFunction);
	}

	@Test
	void withStringAttribute() {
		Map<String, Object> actual = new HashMap<>();
		ExchangeFilterFunction filter = (request, next) -> {
			actual.putAll(request.attributes());
			return next.exchange(request);
		};

		this.builder.filter(filter).build()
				.get().uri("/path")
				.attribute("foo", "bar")
				.retrieve().bodyToMono(Void.class).block(Duration.ofSeconds(10));

		assertThat(actual.get("foo")).isEqualTo("bar");

		ClientRequest request = verifyAndGetRequest();
		assertThat(request.attribute("foo")).contains("bar");
	}

	@Test
	void withNullAttribute() {
		Map<String, Object> actual = new HashMap<>();
		ExchangeFilterFunction filter = (request, next) -> {
			actual.putAll(request.attributes());
			return next.exchange(request);
		};

		this.builder.filter(filter).build()
				.get().uri("/path")
				.attribute("foo", null)
				.retrieve().bodyToMono(Void.class).block(Duration.ofSeconds(10));

		assertThat(actual.get("foo")).isNull();

		ClientRequest request = verifyAndGetRequest();
		assertThat(request.attribute("foo")).isNotPresent();
	}

	@Test
	void uriTemplateAttribute() {
		testUriTemplateAttribute(client -> client.get().uri("/{id}", 1), "/base/{id}");
		testUriTemplateAttribute(client -> client.get().uri("/{id}", Map.of("id", 1)), "/base/{id}");
		testUriTemplateAttribute(client -> client.get().uri("/{id}", builder -> builder.build(1)), "/base/{id}");
	}

	private void testUriTemplateAttribute(
			Function<WebClient, WebClient.RequestHeadersSpec<?>> requestFn, String expectedPath) {

		Map<String, Object> actual = new HashMap<>();
		ExchangeFilterFunction filter = (request, next) -> {
			actual.putAll(request.attributes());
			return next.exchange(request);
		};

		requestFn.apply(this.builder.filter(filter).build())
				.retrieve().bodyToMono(Void.class)
				.block(Duration.ofSeconds(10));

		String key = WebClient.class.getName() + ".uriTemplate";
		assertThat(actual.get(key)).isEqualTo(expectedPath);
	}

	@Test
	void apply() {
		WebClient client = this.builder
				.apply(builder -> builder
						.defaultHeader("Accept", "application/json")
						.defaultCookie("id", "123"))
				.build();

		client.get().uri("/path").retrieve().bodyToMono(Void.class).block(Duration.ofSeconds(10));

		ClientRequest request = verifyAndGetRequest();
		assertThat(request.headers().getFirst("Accept")).isEqualTo("application/json");
		assertThat(request.cookies().getFirst("id")).isEqualTo("123");
	}

	@Test
	void switchToErrorOnEmptyClientResponseMono() {
		ExchangeFunction exchangeFunction = mock();
		given(exchangeFunction.exchange(any())).willReturn(Mono.empty());
		WebClient client = WebClient.builder().baseUrl("/base").exchangeFunction(exchangeFunction).build();
		StepVerifier.create(client.get().uri("/path").retrieve().bodyToMono(Void.class))
				.expectErrorMessage("The underlying HTTP client completed without emitting a response.")
				.verify(Duration.ofSeconds(5));
	}

	@Test
	void shouldApplyFiltersAtSubscription() {
		WebClient client = this.builder
				.filter((request, next) ->
					next.exchange(ClientRequest
							.from(request)
							.header("Custom", "value")
							.build())
				)
				.build();

		Mono<Void> result = client.get().uri("/path").retrieve().bodyToMono(Void.class);

		verifyNoInteractions(this.exchangeFunction);
		result.block(Duration.ofSeconds(10));
		ClientRequest request = verifyAndGetRequest();
		assertThat(request.headers().getFirst("Custom")).isEqualTo("value");
	}

	@Test // gh-23880
	public void onStatusHandlersOrderIsPreserved() {

		ClientResponse response = ClientResponse.create(HttpStatus.BAD_REQUEST).build();
		given(exchangeFunction.exchange(any())).willReturn(Mono.just(response));

		Mono<Void> result = this.builder.build().get()
				.uri("/path")
				.retrieve()
				.onStatus(HttpStatusCode::is4xxClientError, resp -> Mono.error(new IllegalStateException("1")))
				.onStatus(HttpStatusCode::is4xxClientError, resp -> Mono.error(new IllegalStateException("2")))
				.bodyToMono(Void.class);

		StepVerifier.create(result).expectErrorMessage("1").verify();
	}

	@Test
	void onStatusHandlerRegisteredGlobally() {

		ClientResponse response = ClientResponse.create(HttpStatus.BAD_REQUEST).build();
		given(exchangeFunction.exchange(any())).willReturn(Mono.just(response));

		Mono<Void> result = this.builder
				.defaultStatusHandler(HttpStatusCode::is4xxClientError, resp -> Mono.error(new IllegalStateException("1")))
				.defaultStatusHandler(HttpStatusCode::is4xxClientError, resp -> Mono.error(new IllegalStateException("2")))
				.build().get()
				.uri("/path")
				.retrieve()
				.bodyToMono(Void.class);

		StepVerifier.create(result).expectErrorMessage("1").verify();
	}

	@Test
	void onStatusHandlerRegisteredGloballyHaveLowerPrecedence() {

		ClientResponse response = ClientResponse.create(HttpStatus.BAD_REQUEST).build();
		given(exchangeFunction.exchange(any())).willReturn(Mono.just(response));

		Mono<Void> result = this.builder
				.defaultStatusHandler(HttpStatusCode::is4xxClientError, resp -> Mono.error(new IllegalStateException("1")))
				.build().get()
				.uri("/path")
				.retrieve()
				.onStatus(HttpStatusCode::is4xxClientError, resp -> Mono.error(new IllegalStateException("2")))
				.bodyToMono(Void.class);

		StepVerifier.create(result).expectErrorMessage("2").verify();
	}

	@Test // gh-23880
	public void onStatusHandlersDefaultHandlerIsLast() {

		ClientResponse response = ClientResponse.create(HttpStatus.BAD_REQUEST).build();
		given(exchangeFunction.exchange(any())).willReturn(Mono.just(response));

		Predicate<HttpStatusCode> predicate1 = mock();
		Predicate<HttpStatusCode> predicate2 = mock();

		given(predicate1.test(HttpStatus.BAD_REQUEST)).willReturn(false);
		given(predicate2.test(HttpStatus.BAD_REQUEST)).willReturn(false);

		Mono<Void> result = this.builder.build().get()
				.uri("/path")
				.retrieve()
				.onStatus(predicate1, resp -> Mono.error(new IllegalStateException()))
				.onStatus(predicate2, resp -> Mono.error(new IllegalStateException()))
				.bodyToMono(Void.class);

		StepVerifier.create(result).expectError(WebClientResponseException.class).verify();

		verify(predicate1).test(HttpStatus.BAD_REQUEST);
		verify(predicate2).test(HttpStatus.BAD_REQUEST);
	}

	@Test // gh-26069
	public void onStatusHandlersApplyForToEntityMethods() {

		ClientResponse response = ClientResponse.create(HttpStatus.BAD_REQUEST).build();
		given(exchangeFunction.exchange(any())).willReturn(Mono.just(response));

		WebClient.ResponseSpec spec = this.builder.build().get().uri("/path").retrieve();

		testStatusHandlerForToEntity(spec.toEntity(String.class));
		testStatusHandlerForToEntity(spec.toEntity(new ParameterizedTypeReference<String>() {}));
		testStatusHandlerForToEntity(spec.toEntityList(String.class));
		testStatusHandlerForToEntity(spec.toEntityList(new ParameterizedTypeReference<String>() {}));
		testStatusHandlerForToEntity(spec.toEntityFlux(String.class));
		testStatusHandlerForToEntity(spec.toEntityFlux(new ParameterizedTypeReference<String>() {}));
		testStatusHandlerForToEntity(spec.toEntityFlux(BodyExtractors.toFlux(String.class)));
	}

	private void testStatusHandlerForToEntity(Publisher<?> responsePublisher) {
		StepVerifier.create(responsePublisher).expectError(WebClientResponseException.class).verify();
	}

	@Test // gh-32053
	void defaultRequestOverride() {
		WebClient client = this.builder
				.defaultRequest(spec -> spec.accept(MediaType.APPLICATION_JSON))
				.build();

		client.get().uri("/path")
				.accept(MediaType.IMAGE_PNG)
				.retrieve()
				.bodyToMono(Void.class)
				.block(Duration.ofSeconds(3));

		ClientRequest request = verifyAndGetRequest();
		assertThat(request.headers().getAccept()).containsExactly(MediaType.IMAGE_PNG);
	}



	private ClientRequest verifyAndGetRequest() {
		ClientRequest request = this.captor.getValue();
		verify(this.exchangeFunction).exchange(request);
		verifyNoMoreInteractions(this.exchangeFunction);
		return request;
	}

}
