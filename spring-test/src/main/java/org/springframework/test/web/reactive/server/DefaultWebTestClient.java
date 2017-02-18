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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;

import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;
import static org.springframework.test.util.AssertionErrors.fail;
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

	private final Duration responseTimeout;

	private final WiretapConnectorListener connectorListener;


	DefaultWebTestClient(WebClient.Builder webClientBuilder, ClientHttpConnector connector, Duration timeout) {
		Assert.notNull(webClientBuilder, "WebClient.Builder is required");

		WiretapConnector wiretapConnector = new WiretapConnector(connector);
		webClientBuilder.clientConnector(wiretapConnector);

		this.connectorListener = new WiretapConnectorListener();
		wiretapConnector.addListener(this.connectorListener);

		this.webClient = webClientBuilder.build();
		this.responseTimeout = (timeout != null ? timeout : Duration.ofSeconds(5));
	}

	private DefaultWebTestClient(DefaultWebTestClient webTestClient, ExchangeFilterFunction filter) {
		this.webClient = webTestClient.webClient.filter(filter);
		this.connectorListener = webTestClient.connectorListener;
		this.responseTimeout = webTestClient.responseTimeout;
	}


	private Duration getTimeout() {
		return this.responseTimeout;
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
			this.requestId = connectorListener.registerRequestId(spec);
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
			return createResponseSpec(this.headerSpec.exchange());
		}

		@Override
		public <T> ResponseSpec exchange(BodyInserter<T, ? super ClientHttpRequest> inserter) {
			return createResponseSpec(this.headerSpec.exchange(inserter));
		}

		@Override
		public <T, S extends Publisher<T>> ResponseSpec exchange(S publisher, Class<T> elementClass) {
			return createResponseSpec(this.headerSpec.exchange(publisher, elementClass));
		}

		protected DefaultResponseSpec createResponseSpec(Mono<ClientResponse> responseMono) {
			ClientResponse response = responseMono.block(getTimeout());
			WiretapConnector.Info info = connectorListener.retrieveRequest(this.requestId);
			HttpMethod method = info.getMethod();
			URI url = info.getUrl();
			HttpHeaders headers = info.getRequestHeaders();
			ExchangeResult<Flux<DataBuffer>> result = ExchangeResult.fromResponse(method, url, headers, response);
			return new DefaultResponseSpec(result, response);
		}

	}

	private abstract class ResponseSpecSupport {

		private final ExchangeResult<Flux<DataBuffer>> exchangeResult;

		private final ClientResponse response;


		public ResponseSpecSupport(ExchangeResult<Flux<DataBuffer>> result, ClientResponse response) {
			this.exchangeResult = result;
			this.response = response;
		}

		public ResponseSpecSupport(ResponseSpecSupport responseSpec) {
			this.exchangeResult = responseSpec.getExchangeResult();
			this.response = responseSpec.getResponse();
		}


		protected ExchangeResult<Flux<DataBuffer>> getExchangeResult() {
			return this.exchangeResult;
		}

		protected ClientResponse getResponse() {
			return this.response;
		}

		protected HttpHeaders getResponseHeaders() {
			return getExchangeResult().getResponseHeaders();
		}

		protected <T> ExchangeResult<T> createResultWithDecodedBody(T body) {
			return ExchangeResult.withDecodedBody(this.exchangeResult, body);
		}

	}

	private class DefaultResponseSpec extends ResponseSpecSupport implements ResponseSpec {


		public DefaultResponseSpec(ExchangeResult<Flux<DataBuffer>> result, ClientResponse response) {
			super(result, response);
		}


		@Override
		public StatusAssertions expectStatus() {
			return new StatusAssertions(getResponse().statusCode(), this);
		}

		@Override
		public HeaderAssertions expectHeader() {
			return new HeaderAssertions(getResponseHeaders(), this);
		}

		@Override
		public BodySpec expectBody() {
			return new DefaultBodySpec(this);
		}

		@Override
		public ElementBodySpec expectBody(Class<?> elementType) {
			return expectBody(ResolvableType.forClass(elementType));
		}

		@Override
		public ElementBodySpec expectBody(ResolvableType elementType) {
			return new DefaultElementBodySpec(this, elementType);
		}

		@Override
		public ResponseSpec consumeWith(Consumer<ExchangeResult<Flux<DataBuffer>>> consumer) {
			consumer.accept(getExchangeResult());
			return this;
		}

		@Override
		public ExchangeResult<Flux<DataBuffer>> returnResult() {
			return getExchangeResult();
		}
	}

	private class DefaultBodySpec extends ResponseSpecSupport implements BodySpec {


		public DefaultBodySpec(ResponseSpecSupport responseSpec) {
			super(responseSpec);
		}


		@Override
		public ExchangeResult<Void> isEmpty() {
			DataBuffer buffer = getResponse().body(toDataBuffers()).blockFirst(getTimeout());
			assertTrue("Expected empty body", buffer == null);
			return createResultWithDecodedBody(null);
		}

		@Override
		public MapBodySpec map(Class<?> keyType, Class<?> valueType) {
			return map(ResolvableType.forClass(keyType), ResolvableType.forClass(valueType));
		}

		@Override
		public MapBodySpec map(ResolvableType keyType, ResolvableType valueType) {
			return new DefaultMapBodySpec(this, keyType, valueType);
		}
	}

	private class DefaultMapBodySpec extends ResponseSpecSupport implements MapBodySpec {

		private final Map<?, ?> body;


		public DefaultMapBodySpec(ResponseSpecSupport spec, ResolvableType keyType, ResolvableType valueType) {
			super(spec);
			ResolvableType mapType = ResolvableType.forClassWithGenerics(Map.class, keyType, valueType);
			this.body = (Map<?, ?>) spec.getResponse().body(toMono(mapType)).block(getTimeout());
		}


		@Override
		public <K, V> ExchangeResult<Map<K, V>> isEqualTo(Map<K, V> expected) {
			return returnResult();
		}

		@Override
		public MapBodySpec hasSize(int size) {
			assertEquals("Response body map size", size, this.body.size());
			return this;
		}

		@Override
		public MapBodySpec contains(Object key, Object value) {
			assertEquals("Response body map value for key " + key, value, this.body.get(key));
			return this;
		}

		@Override
		public MapBodySpec containsKeys(Object... keys) {
			List<Object> missing = Arrays.stream(keys)
					.filter(key -> !this.body.containsKey(key))
					.collect(Collectors.toList());
			if (!missing.isEmpty()) {
				fail("Response body map does not contain keys " + Arrays.toString(keys));
			}
			return this;
		}

		@Override
		public MapBodySpec containsValues(Object... values) {
			List<Object> missing = Arrays.stream(values)
					.filter(value -> !this.body.containsValue(value))
					.collect(Collectors.toList());
			if (!missing.isEmpty()) {
				fail("Response body map does not contain values " + Arrays.toString(values));
			}
			return this;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <K, V> ExchangeResult<Map<K, V>> returnResult() {
			return createResultWithDecodedBody((Map<K, V>) this.body);
		}
	}

	private class DefaultElementBodySpec extends ResponseSpecSupport implements ElementBodySpec {

		private final ResolvableType elementType;


		public DefaultElementBodySpec(ResponseSpecSupport spec, ResolvableType elementType) {
			super(spec);
			this.elementType = elementType;
		}


		@Override
		public SingleValueBodySpec value() {
			return new DefaultSingleValueBodySpec(this, this.elementType);
		}

		@Override
		public ListBodySpec list() {
			return new DefaultListBodySpec(this, this.elementType, -1);
		}

		@Override
		public ListBodySpec list(int elementCount) {
			return new DefaultListBodySpec(this, this.elementType, elementCount);
		}

		@Override
		public <T> ExchangeResult<Flux<T>> returnResult() {
			Flux<T> flux = getResponse().body(toFlux(this.elementType));
			return createResultWithDecodedBody(flux);
		}
	}

	private class DefaultSingleValueBodySpec extends ResponseSpecSupport
			implements SingleValueBodySpec {

		private final Object body;


		public DefaultSingleValueBodySpec(ResponseSpecSupport spec, ResolvableType elementType) {
			super(spec);
			this.body = getResponse().body(toMono(elementType)).block(getTimeout());
		}


		@Override
		public <T> ExchangeResult<T> isEqualTo(Object expected) {
			assertEquals("Response body", expected, this.body);
			return returnResult();
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> ExchangeResult<T> returnResult() {
			return createResultWithDecodedBody((T) this.body);
		}
	}

	private class DefaultListBodySpec extends ResponseSpecSupport
			implements ListBodySpec {

		private final List<?> body;


		public DefaultListBodySpec(ResponseSpecSupport spec, ResolvableType elementType, int elementCount) {
			super(spec);
			Flux<?> flux = getResponse().body(toFlux(elementType));
			if (elementCount >= 0) {
				flux = flux.take(elementCount);
			}
			this.body = flux.collectList().block(getTimeout());
		}


		@Override
		public <T> ExchangeResult<List<T>> isEqualTo(List<T> expected) {
			assertEquals("Response body", expected, this.body);
			return returnResult();
		}

		@Override
		public ListBodySpec hasSize(int size) {
			return this;
		}

		@Override
		public ListBodySpec contains(Object... elements) {
			List<Object> elementList = Arrays.asList(elements);
			String message = "Response body does not contain " + elementList;
			assertTrue(message, this.body.containsAll(elementList));
			return this;
		}

		@Override
		public ListBodySpec doesNotContain(Object... elements) {
			List<Object> elementList = Arrays.asList(elements);
			String message = "Response body should have contained " + elementList;
			assertTrue(message, !this.body.containsAll(Arrays.asList(elements)));
			return this;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> ExchangeResult<List<T>> returnResult() {
			return createResultWithDecodedBody((List<T>) this.body);
		}
	}


	private static class WiretapConnectorListener implements Consumer<WiretapConnector.Info> {

		private static final String REQUEST_ID_HEADER_NAME = "request-id";


		private final AtomicLong index = new AtomicLong();

		private final Map<String, WiretapConnector.Info> exchanges = new ConcurrentHashMap<>();


		public String registerRequestId(WebClient.HeaderSpec headerSpec) {
			String requestId = String.valueOf(this.index.incrementAndGet());
			headerSpec.header(REQUEST_ID_HEADER_NAME, requestId);
			return requestId;
		}

		@Override
		public void accept(WiretapConnector.Info info) {
			Optional.ofNullable(info.getRequestHeaders().getFirst(REQUEST_ID_HEADER_NAME))
					.ifPresent(id -> this.exchanges.put(id, info));
		}

		public WiretapConnector.Info retrieveRequest(String requestId) {
			WiretapConnector.Info info = this.exchanges.remove(requestId);
			Assert.notNull(info, "No match for request-id=" + requestId);
			return info;
		}
	}

}
