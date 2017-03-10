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
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriBuilder;

import static java.util.stream.Collectors.toList;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;
import static org.springframework.web.reactive.function.BodyExtractors.toDataBuffers;
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
	public UriSpec get() {
		return toUriSpec(WebClient::get);
	}

	@Override
	public UriSpec head() {
		return toUriSpec(WebClient::head);
	}

	@Override
	public UriSpec post() {
		return toUriSpec(WebClient::post);
	}

	@Override
	public UriSpec put() {
		return toUriSpec(WebClient::put);
	}

	@Override
	public UriSpec patch() {
		return toUriSpec(WebClient::patch);
	}

	@Override
	public UriSpec delete() {
		return toUriSpec(WebClient::delete);
	}

	@Override
	public UriSpec options() {
		return toUriSpec(WebClient::options);
	}

	private UriSpec toUriSpec(Function<WebClient, WebClient.UriSpec> function) {
		return new DefaultUriSpec(function.apply(this.webClient));
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


	private class DefaultUriSpec implements UriSpec {

		private final WebClient.UriSpec uriSpec;


		DefaultUriSpec(WebClient.UriSpec spec) {
			this.uriSpec = spec;
		}

		@Override
		public HeaderSpec uri(URI uri) {
			return new DefaultHeaderSpec(this.uriSpec.uri(uri));
		}

		@Override
		public HeaderSpec uri(String uriTemplate, Object... uriVariables) {
			return new DefaultHeaderSpec(this.uriSpec.uri(uriTemplate, uriVariables));
		}

		@Override
		public HeaderSpec uri(String uriTemplate, Map<String, ?> uriVariables) {
			return new DefaultHeaderSpec(this.uriSpec.uri(uriTemplate, uriVariables));
		}

		@Override
		public HeaderSpec uri(Function<UriBuilder, URI> uriBuilder) {
			return new DefaultHeaderSpec(this.uriSpec.uri(uriBuilder));
		}
	}

	private class DefaultHeaderSpec implements WebTestClient.HeaderSpec {

		private final WebClient.HeaderSpec headerSpec;

		private final String requestId;


		DefaultHeaderSpec(WebClient.HeaderSpec spec) {
			this.headerSpec = spec;
			this.requestId = String.valueOf(requestIndex.incrementAndGet());
			this.headerSpec.header(WiretapConnector.REQUEST_ID_HEADER_NAME, this.requestId);
		}


		@Override
		public DefaultHeaderSpec header(String headerName, String... headerValues) {
			this.headerSpec.header(headerName, headerValues);
			return this;
		}

		@Override
		public DefaultHeaderSpec headers(HttpHeaders headers) {
			this.headerSpec.headers(headers);
			return this;
		}

		@Override
		public DefaultHeaderSpec accept(MediaType... acceptableMediaTypes) {
			this.headerSpec.accept(acceptableMediaTypes);
			return this;
		}

		@Override
		public DefaultHeaderSpec acceptCharset(Charset... acceptableCharsets) {
			this.headerSpec.acceptCharset(acceptableCharsets);
			return this;
		}

		@Override
		public DefaultHeaderSpec contentType(MediaType contentType) {
			this.headerSpec.contentType(contentType);
			return this;
		}

		@Override
		public DefaultHeaderSpec contentLength(long contentLength) {
			this.headerSpec.contentLength(contentLength);
			return this;
		}

		@Override
		public DefaultHeaderSpec cookie(String name, String value) {
			this.headerSpec.cookie(name, value);
			return this;
		}

		@Override
		public DefaultHeaderSpec cookies(MultiValueMap<String, String> cookies) {
			this.headerSpec.cookies(cookies);
			return this;
		}

		@Override
		public DefaultHeaderSpec ifModifiedSince(ZonedDateTime ifModifiedSince) {
			this.headerSpec.ifModifiedSince(ifModifiedSince);
			return this;
		}

		@Override
		public DefaultHeaderSpec ifNoneMatch(String... ifNoneMatches) {
			this.headerSpec.ifNoneMatch(ifNoneMatches);
			return this;
		}

		@Override
		public ResponseSpec exchange() {
			return toResponseSpec(this.headerSpec.exchange());
		}

		@Override
		public <T> ResponseSpec exchange(BodyInserter<T, ? super ClientHttpRequest> inserter) {
			return toResponseSpec(this.headerSpec.exchange(inserter));
		}

		@Override
		public <T, S extends Publisher<T>> ResponseSpec exchange(S publisher, Class<T> elementClass) {
			return toResponseSpec(this.headerSpec.exchange(publisher, elementClass));
		}

		private DefaultResponseSpec toResponseSpec(Mono<ClientResponse> mono) {
			ClientResponse clientResponse = mono.block(getTimeout());
			ExchangeResult exchangeResult = wiretapConnector.claimRequest(this.requestId);
			return new DefaultResponseSpec(exchangeResult, clientResponse);
		}
	}

	/**
	 * The {@code ExchangeResult} with live, undecoded {@link ClientResponse}.
	 */
	private class UndecodedExchangeResult extends ExchangeResult {

		private final ClientResponse response;


		public UndecodedExchangeResult(ExchangeResult result, ClientResponse response) {
			super(result);
			this.response = response;
		}


		public EntityExchangeResult<?> consumeSingle(ResolvableType elementType) {
			Object body = this.response.body(toMono(elementType)).block(getTimeout());
			return new EntityExchangeResult<>(this, body);
		}

		public EntityExchangeResult<List<?>> consumeList(ResolvableType elementType, int count) {
			Flux<?> flux = this.response.body(toFlux(elementType));
			if (count >= 0) {
				flux = flux.take(count);
			}
			List<?> body = flux.collectList().block(getTimeout());
			return new EntityExchangeResult<>(this, body);
		}

		public <T> FluxExchangeResult<T> decodeBody(ResolvableType elementType) {
			Flux<T> body = this.response.body(toFlux(elementType));
			return new FluxExchangeResult<>(this, body, getTimeout());
		}

		@SuppressWarnings("unchecked")
		public EntityExchangeResult<Map<?, ?>> consumeMap(ResolvableType keyType, ResolvableType valueType) {
			ResolvableType mapType = ResolvableType.forClassWithGenerics(Map.class, keyType, valueType);
			return (EntityExchangeResult<Map<?, ?>>) consumeSingle(mapType);
		}

		public EntityExchangeResult<Void> consumeEmpty() {
			DataBuffer buffer = this.response.body(toDataBuffers()).blockFirst(getTimeout());
			assertWithDiagnostics(() -> assertTrue("Expected empty body", buffer == null));
			return new EntityExchangeResult<>(this, null);
		}
	}

	private class DefaultResponseSpec implements ResponseSpec {

		private final UndecodedExchangeResult result;


		public DefaultResponseSpec(ExchangeResult result, ClientResponse response) {
			this.result = new UndecodedExchangeResult(result, response);
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
		public TypeBodySpec expectBody(Class<?> elementType) {
			return expectBody(ResolvableType.forClass(elementType));
		}

		@Override
		public TypeBodySpec expectBody(ResolvableType elementType) {
			return new DefaultTypeBodySpec(this.result, elementType);
		}

		@Override
		public BodySpec expectBody() {
			return new DefaultBodySpec(this.result);
		}
	}

	private class DefaultTypeBodySpec implements TypeBodySpec {

		private final UndecodedExchangeResult result;

		private final ResolvableType elementType;


		public DefaultTypeBodySpec(UndecodedExchangeResult result, ResolvableType elementType) {
			this.result = result;
			this.elementType = elementType;
		}


		@Override
		public SingleValueBodySpec value() {
			return new DefaultSingleValueBodySpec(this.result.consumeSingle(this.elementType));
		}

		@Override
		public ListBodySpec list() {
			return list(-1);
		}

		@Override
		public ListBodySpec list(int count) {
			return new DefaultListBodySpec(this.result.consumeList(this.elementType, count));
		}

		@Override
		public <T> FluxExchangeResult<T> returnResult() {
			return this.result.decodeBody(this.elementType);
		}
	}

	private class DefaultSingleValueBodySpec implements SingleValueBodySpec {

		private final EntityExchangeResult<?> result;


		public DefaultSingleValueBodySpec(EntityExchangeResult<?> result) {
			this.result = result;
		}


		@Override
		public <T> EntityExchangeResult<T> isEqualTo(T expected) {
			Object actual = this.result.getResponseBody();
			this.result.assertWithDiagnostics(() -> assertEquals("Response body", expected, actual));
			return returnResult();
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> EntityExchangeResult<T> returnResult() {
			return new EntityExchangeResult<>(this.result, (T) this.result.getResponseBody());
		}
	}

	private class DefaultListBodySpec implements ListBodySpec {

		private final EntityExchangeResult<List<?>> result;


		public DefaultListBodySpec(EntityExchangeResult<List<?>> result) {
			this.result = result;
		}


		@Override
		public <T> EntityExchangeResult<List<T>> isEqualTo(List<T> expected) {
			List<?> actual = this.result.getResponseBody();
			this.result.assertWithDiagnostics(() -> assertEquals("Response body", expected, actual));
			return returnResult();
		}

		@Override
		public ListBodySpec hasSize(int size) {
			List<?> actual = this.result.getResponseBody();
			String message = "Response body does not contain " + size + " elements";
			this.result.assertWithDiagnostics(() -> assertEquals(message, size, actual.size()));
			return this;
		}

		@Override
		public ListBodySpec contains(Object... elements) {
			List<?> expected = Arrays.asList(elements);
			List<?> actual = this.result.getResponseBody();
			String message = "Response body does not contain " + expected;
			this.result.assertWithDiagnostics(() -> assertTrue(message, actual.containsAll(expected)));
			return this;
		}

		@Override
		public ListBodySpec doesNotContain(Object... elements) {
			List<?> expected = Arrays.asList(elements);
			List<?> actual = this.result.getResponseBody();
			String message = "Response body should have contained " + expected;
			this.result.assertWithDiagnostics(() -> assertTrue(message, !actual.containsAll(expected)));
			return this;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> EntityExchangeResult<List<T>> returnResult() {
			return new EntityExchangeResult<>(this.result,  (List<T>) this.result.getResponseBody());
		}
	}

	private class DefaultBodySpec implements BodySpec {

		private final UndecodedExchangeResult exchangeResult;


		public DefaultBodySpec(UndecodedExchangeResult result) {
			this.exchangeResult = result;
		}


		@Override
		public EntityExchangeResult<Void> isEmpty() {
			return this.exchangeResult.consumeEmpty();
		}

		@Override
		public MapBodySpec map(Class<?> keyType, Class<?> valueType) {
			return map(ResolvableType.forClass(keyType), ResolvableType.forClass(valueType));
		}

		@Override
		public MapBodySpec map(ResolvableType keyType, ResolvableType valueType) {
			return new DefaultMapBodySpec(this.exchangeResult.consumeMap(keyType, valueType));
		}
	}

	private class DefaultMapBodySpec implements MapBodySpec {

		private final EntityExchangeResult<Map<?, ?>> result;


		public DefaultMapBodySpec(EntityExchangeResult<Map<?, ?>> result) {
			this.result = result;
		}


		private Map<?, ?> getBody() {
			return this.result.getResponseBody();
		}

		@Override
		public <K, V> EntityExchangeResult<Map<K, V>> isEqualTo(Map<K, V> expected) {
			String message = "Response body map";
			this.result.assertWithDiagnostics(() -> assertEquals(message, expected, getBody()));
			return returnResult();
		}

		@Override
		public MapBodySpec hasSize(int size) {
			String message = "Response body map size";
			this.result.assertWithDiagnostics(() -> assertEquals(message, size, getBody().size()));
			return this;
		}

		@Override
		public MapBodySpec contains(Object key, Object value) {
			String message = "Response body map value for key " + key;
			this.result.assertWithDiagnostics(() -> assertEquals(message, value, getBody().get(key)));
			return this;
		}

		@Override
		public MapBodySpec containsKeys(Object... keys) {
			List<?> missing = Arrays.stream(keys).filter(k -> !getBody().containsKey(k)).collect(toList());
			String message = "Response body map does not contain keys " + missing;
			this.result.assertWithDiagnostics(() -> assertTrue(message, missing.isEmpty()));
			return this;
		}

		@Override
		public MapBodySpec containsValues(Object... values) {
			List<?> missing = Arrays.stream(values).filter(v -> !getBody().containsValue(v)).collect(toList());
			String message = "Response body map does not contain values " + missing;
			this.result.assertWithDiagnostics(() -> assertTrue(message, missing.isEmpty()));
			return this;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <K, V> EntityExchangeResult<Map<K, V>> returnResult() {
			return new EntityExchangeResult<>(this.result, (Map<K, V>) getBody());
		}
	}

}
