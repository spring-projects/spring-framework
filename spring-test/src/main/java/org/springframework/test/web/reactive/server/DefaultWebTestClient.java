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

import java.net.URI;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.test.util.JsonExpectationsHelper;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriBuilder;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;
import static org.springframework.web.reactive.function.BodyExtractors.toFlux;
import static org.springframework.web.reactive.function.BodyExtractors.toMono;


/**
 * Default implementation of {@link WebTestClient}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class DefaultWebTestClient implements WebTestClient {

	private final WebClient webClient;

	private final WiretapConnector wiretapConnector;

	private final ExchangeMutatorWebFilter exchangeMutatorWebFilter;

	private final Duration timeout;

	private final AtomicLong requestIndex = new AtomicLong();


	DefaultWebTestClient(WebClient.Builder webClientBuilder, ClientHttpConnector connector,
			ExchangeMutatorWebFilter webFilter, Duration timeout) {

		Assert.notNull(webClientBuilder, "WebClient.Builder is required");

		this.wiretapConnector = new WiretapConnector(connector);
		this.webClient = webClientBuilder.clientConnector(this.wiretapConnector).build();
		this.exchangeMutatorWebFilter = webFilter;
		this.timeout = (timeout != null ? timeout : Duration.ofSeconds(5));
	}

	private DefaultWebTestClient(DefaultWebTestClient webTestClient, ExchangeFilterFunction filter) {
		this.webClient = webTestClient.webClient.filter(filter);
		this.wiretapConnector = webTestClient.wiretapConnector;
		this.exchangeMutatorWebFilter = webTestClient.exchangeMutatorWebFilter;
		this.timeout = webTestClient.timeout;
	}


	private Duration getTimeout() {
		return this.timeout;
	}


	@Override
	public UriSpec<RequestHeadersSpec<?>> get() {
		return toUriSpec(wc -> wc.method(HttpMethod.GET));
	}

	@Override
	public UriSpec<RequestHeadersSpec<?>> head() {
		return toUriSpec(wc -> wc.method(HttpMethod.HEAD));
	}

	@Override
	public UriSpec<RequestBodySpec> post() {
		return toUriSpec(wc -> wc.method(HttpMethod.POST));
	}

	@Override
	public UriSpec<RequestBodySpec> put() {
		return toUriSpec(wc -> wc.method(HttpMethod.PUT));
	}

	@Override
	public UriSpec<RequestBodySpec> patch() {
		return toUriSpec(wc -> wc.method(HttpMethod.PATCH));
	}

	@Override
	public UriSpec<RequestHeadersSpec<?>> delete() {
		return toUriSpec(wc -> wc.method(HttpMethod.DELETE));
	}

	@Override
	public UriSpec<RequestHeadersSpec<?>> options() {
		return toUriSpec(wc -> wc.method(HttpMethod.OPTIONS));
	}

	private <S extends RequestHeadersSpec<?>> UriSpec<S> toUriSpec(
			Function<WebClient, WebClient.UriSpec<WebClient.RequestBodySpec>> function) {

		return new DefaultUriSpec<>(function.apply(this.webClient));
	}


	@Override
	public WebTestClient filter(ExchangeFilterFunction filter) {
		return new DefaultWebTestClient(this, filter);
	}

	@Override
	public WebTestClient exchangeMutator(UnaryOperator<ServerWebExchange> mutator) {

		Assert.notNull(this.exchangeMutatorWebFilter,
				"This option is applicable only for tests without an actual running server");

		return filter((request, next) -> {
			String requestId = request.headers().getFirst(WiretapConnector.REQUEST_ID_HEADER_NAME);
			Assert.notNull(requestId, "No request-id header");
			this.exchangeMutatorWebFilter.register(requestId, mutator);
			return next.exchange(request);
		});
	}


	@SuppressWarnings("unchecked")
	private class DefaultUriSpec<S extends RequestHeadersSpec<?>> implements UriSpec<S> {

		private final WebClient.UriSpec<WebClient.RequestBodySpec> uriSpec;


		DefaultUriSpec(WebClient.UriSpec<WebClient.RequestBodySpec> spec) {
			this.uriSpec = spec;
		}

		@Override
		public S uri(URI uri) {
			return (S) new DefaultRequestBodySpec(this.uriSpec.uri(uri));
		}

		@Override
		public S uri(String uriTemplate, Object... uriVariables) {
			return (S) new DefaultRequestBodySpec(this.uriSpec.uri(uriTemplate, uriVariables));
		}

		@Override
		public S uri(String uriTemplate, Map<String, ?> uriVariables) {
			return (S) new DefaultRequestBodySpec(this.uriSpec.uri(uriTemplate, uriVariables));
		}

		@Override
		public S uri(Function<UriBuilder, URI> uriBuilder) {
			return (S) new DefaultRequestBodySpec(this.uriSpec.uri(uriBuilder));
		}
	}

	private class DefaultRequestBodySpec implements RequestBodySpec {

		private final WebClient.RequestBodySpec bodySpec;

		private final String requestId;


		DefaultRequestBodySpec(WebClient.RequestBodySpec spec) {
			this.bodySpec = spec;
			this.requestId = String.valueOf(requestIndex.incrementAndGet());
			this.bodySpec.header(WiretapConnector.REQUEST_ID_HEADER_NAME, this.requestId);
		}


		@Override
		public RequestBodySpec header(String headerName, String... headerValues) {
			this.bodySpec.header(headerName, headerValues);
			return this;
		}

		@Override
		public RequestBodySpec headers(HttpHeaders headers) {
			this.bodySpec.headers(headers);
			return this;
		}

		@Override
		public RequestBodySpec accept(MediaType... acceptableMediaTypes) {
			this.bodySpec.accept(acceptableMediaTypes);
			return this;
		}

		@Override
		public RequestBodySpec acceptCharset(Charset... acceptableCharsets) {
			this.bodySpec.acceptCharset(acceptableCharsets);
			return this;
		}

		@Override
		public RequestBodySpec contentType(MediaType contentType) {
			this.bodySpec.contentType(contentType);
			return this;
		}

		@Override
		public RequestBodySpec contentLength(long contentLength) {
			this.bodySpec.contentLength(contentLength);
			return this;
		}

		@Override
		public RequestBodySpec cookie(String name, String value) {
			this.bodySpec.cookie(name, value);
			return this;
		}

		@Override
		public RequestBodySpec cookies(MultiValueMap<String, String> cookies) {
			this.bodySpec.cookies(cookies);
			return this;
		}

		@Override
		public RequestBodySpec ifModifiedSince(ZonedDateTime ifModifiedSince) {
			this.bodySpec.ifModifiedSince(ifModifiedSince);
			return this;
		}

		@Override
		public RequestBodySpec ifNoneMatch(String... ifNoneMatches) {
			this.bodySpec.ifNoneMatch(ifNoneMatches);
			return this;
		}

		@Override
		public ResponseSpec exchange() {
			return toResponseSpec(this.bodySpec.exchange());
		}

		@Override
		public <T> RequestHeadersSpec<?> body(BodyInserter<T, ? super ClientHttpRequest> inserter) {
			this.bodySpec.body(inserter);
			return this;
		}

		@Override
		public <T, S extends Publisher<T>> RequestHeadersSpec<?> body(S publisher, Class<T> elementClass) {
			this.bodySpec.body(publisher, elementClass);
			return this;
		}

		@Override
		public <T> RequestHeadersSpec<?> body(T body) {
			this.bodySpec.body(body);
			return this;
		}

		private DefaultResponseSpec toResponseSpec(Mono<ClientResponse> mono) {
			ClientResponse clientResponse = mono.block(getTimeout());
			ExchangeResult exchangeResult = wiretapConnector.claimRequest(this.requestId);
			return new DefaultResponseSpec(exchangeResult, clientResponse, getTimeout());
		}

	}


	private static class UndecodedExchangeResult extends ExchangeResult {

		private final ClientResponse response;

		private final Duration timeout;


		UndecodedExchangeResult(ExchangeResult result, ClientResponse response, Duration timeout) {
			super(result);
			this.response = response;
			this.timeout = timeout;
		}


		@SuppressWarnings("unchecked")
		public <T> EntityExchangeResult<T> decode(ResolvableType bodyType) {
			T body = (T) this.response.body(toMono(bodyType)).block(this.timeout);
			return new EntityExchangeResult<>(this, body);
		}

		public <T> EntityExchangeResult<List<T>> decodeToList(ResolvableType elementType) {
			Flux<T> flux = this.response.body(toFlux(elementType));
			List<T> body = flux.collectList().block(this.timeout);
			return new EntityExchangeResult<>(this, body);
		}

		public <T> FluxExchangeResult<T> decodeToFlux(ResolvableType elementType) {
			Flux<T> body = this.response.body(toFlux(elementType));
			return new FluxExchangeResult<>(this, body, this.timeout);
		}

		public EntityExchangeResult<byte[]> decodeToByteArray() {
			ByteArrayResource resource = this.response.body(toMono(ByteArrayResource.class)).block(this.timeout);
			byte[] body = (resource != null ? resource.getByteArray() : null);
			return new EntityExchangeResult<>(this, body);
		}

	}


	private static class DefaultResponseSpec implements ResponseSpec {

		private final UndecodedExchangeResult result;


		DefaultResponseSpec(ExchangeResult result, ClientResponse response, Duration timeout) {
			this.result = new UndecodedExchangeResult(result, response, timeout);
		}

		@Override
		public StatusAssertions expectStatus() {
			return new StatusAssertions(this.result, this);
		}

		@Override
		public HeaderAssertions expectHeader() {
			return new HeaderAssertions(this.result, this);
		}

		@Override
		public <B> BodySpec<B, ?> expectBody(Class<B> bodyType) {
			return expectBody(ResolvableType.forClass(bodyType));
		}

		@Override
		public <B> BodySpec<B, ?> expectBody(ResolvableType bodyType) {
			return new DefaultBodySpec<>(this.result.decode(bodyType));
		}

		@Override
		public <E> ListBodySpec<E> expectBodyList(Class<E> elementType) {
			return expectBodyList(ResolvableType.forClass(elementType));
		}

		@Override
		public <E> ListBodySpec<E> expectBodyList(ResolvableType elementType) {
			return new DefaultListBodySpec<>(this.result.decodeToList(elementType));
		}

		@Override
		public BodyContentSpec expectBody() {
			return new DefaultBodyContentSpec(this.result.decodeToByteArray());
		}

		@Override
		public <T> FluxExchangeResult<T> returnResult(Class<T> elementType) {
			return returnResult(ResolvableType.forClass(elementType));
		}

		@Override
		public <T> FluxExchangeResult<T> returnResult(ResolvableType elementType) {
			return this.result.decodeToFlux(elementType);
		}

	}


	private static class DefaultBodySpec<B, S extends BodySpec<B, S>> implements BodySpec<B, S> {

		private final EntityExchangeResult<B> result;


		DefaultBodySpec(EntityExchangeResult<B> result) {
			this.result = result;
		}


		protected EntityExchangeResult<B> getResult() {
			return this.result;
		}

		@Override
		public <T extends S> T isEqualTo(B expected) {
			B actual = this.result.getResponseBody();
			this.result.assertWithDiagnostics(() -> assertEquals("Response body", expected, actual));
			return self();
		}

		@Override
		public <T extends S> T consumeWith(Consumer<B> consumer) {
			B actual = this.result.getResponseBody();
			this.result.assertWithDiagnostics(() -> consumer.accept(actual));
			return self();
		}

		@SuppressWarnings("unchecked")
		private <T extends S> T self() {
			return (T) this;
		}

		@Override
		public EntityExchangeResult<B> returnResult() {
			return this.result;
		}

	}


	private static class DefaultListBodySpec<E> extends DefaultBodySpec<List<E>, ListBodySpec<E>>
			implements ListBodySpec<E> {


		DefaultListBodySpec(EntityExchangeResult<List<E>> result) {
			super(result);
		}


		@Override
		public ListBodySpec<E> hasSize(int size) {
			List<E> actual = getResult().getResponseBody();
			String message = "Response body does not contain " + size + " elements";
			getResult().assertWithDiagnostics(() -> assertEquals(message, size, actual.size()));
			return this;
		}

		@Override
		@SuppressWarnings("unchecked")
		public ListBodySpec<E> contains(E... elements) {
			List<E> expected = Arrays.asList(elements);
			List<E> actual = getResult().getResponseBody();
			String message = "Response body does not contain " + expected;
			getResult().assertWithDiagnostics(() -> assertTrue(message, actual.containsAll(expected)));
			return this;
		}

		@Override
		@SuppressWarnings("unchecked")
		public ListBodySpec<E> doesNotContain(E... elements) {
			List<E> expected = Arrays.asList(elements);
			List<E> actual = getResult().getResponseBody();
			String message = "Response body should have contained " + expected;
			getResult().assertWithDiagnostics(() -> assertTrue(message, !actual.containsAll(expected)));
			return this;
		}

		@Override
		@SuppressWarnings("unchecked")
		public EntityExchangeResult<List<E>> returnResult() {
			return getResult();
		}

	}


	private static class DefaultBodyContentSpec implements BodyContentSpec {

		private final EntityExchangeResult<byte[]> result;

		private final boolean isEmpty;


		DefaultBodyContentSpec(EntityExchangeResult<byte[]> result) {
			this.result = result;
			this.isEmpty = (result.getResponseBody() == null);
		}


		@Override
		public EntityExchangeResult<Void> isEmpty() {
			this.result.assertWithDiagnostics(() -> assertTrue("Expected empty body", this.isEmpty));
			return new EntityExchangeResult<>(this.result, null);
		}

		@Override
		public BodyContentSpec json(String json) {
			this.result.assertWithDiagnostics(() -> {
				try {
					new JsonExpectationsHelper().assertJsonEqual(json, getBodyAsString());
				}
				catch (Exception ex) {
					throw new AssertionError("JSON parsing error", ex);
				}
			});
			return this;
		}

		@Override
		public JsonPathAssertions jsonPath(String expression, Object... args) {
			return new JsonPathAssertions(this, expression, args);
		}

		@Override
		public BodyContentSpec consumeAsStringWith(Consumer<String> consumer) {
			this.result.assertWithDiagnostics(() -> consumer.accept(getBodyAsString()));
			return this;
		}

		private String getBodyAsString() {
			if (this.isEmpty) {
				return null;
			}
			MediaType mediaType = this.result.getResponseHeaders().getContentType();
			Charset charset = Optional.ofNullable(mediaType).map(MimeType::getCharset).orElse(UTF_8);
			return new String(this.result.getResponseBody(), charset);
		}

		@Override
		public BodyContentSpec consumeWith(Consumer<byte[]> consumer) {
			this.result.assertWithDiagnostics(() -> consumer.accept(this.result.getResponseBody()));
			return this;
		}

		@Override
		public EntityExchangeResult<byte[]> returnResult() {
			return this.result;
		}

	}

}
