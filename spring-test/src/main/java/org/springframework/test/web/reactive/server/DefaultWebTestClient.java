/*
 * Copyright 2002-2021 the original author or authors.
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

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.test.util.AssertionErrors;
import org.springframework.test.util.JsonExpectationsHelper;
import org.springframework.test.util.XmlExpectationsHelper;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MimeType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriBuilderFactory;

/**
 * Default implementation of {@link WebTestClient}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class DefaultWebTestClient implements WebTestClient {

	private final WiretapConnector wiretapConnector;

	private final ExchangeFunction exchangeFunction;

	private final UriBuilderFactory uriBuilderFactory;

	@Nullable
	private final HttpHeaders defaultHeaders;

	@Nullable
	private final MultiValueMap<String, String> defaultCookies;

	private final Consumer<EntityExchangeResult<?>> entityResultConsumer;

	private final Duration responseTimeout;

	private final DefaultWebTestClientBuilder builder;

	private final AtomicLong requestIndex = new AtomicLong();


	DefaultWebTestClient(ClientHttpConnector connector,
			Function<ClientHttpConnector, ExchangeFunction> exchangeFactory, UriBuilderFactory uriBuilderFactory,
			@Nullable HttpHeaders headers, @Nullable MultiValueMap<String, String> cookies,
			Consumer<EntityExchangeResult<?>> entityResultConsumer,
			@Nullable Duration responseTimeout, DefaultWebTestClientBuilder clientBuilder) {

		this.wiretapConnector = new WiretapConnector(connector);
		this.exchangeFunction = exchangeFactory.apply(this.wiretapConnector);
		this.uriBuilderFactory = uriBuilderFactory;
		this.defaultHeaders = headers;
		this.defaultCookies = cookies;
		this.entityResultConsumer = entityResultConsumer;
		this.responseTimeout = (responseTimeout != null ? responseTimeout : Duration.ofSeconds(5));
		this.builder = clientBuilder;
	}


	private Duration getResponseTimeout() {
		return this.responseTimeout;
	}


	@Override
	public RequestHeadersUriSpec<?> get() {
		return methodInternal(HttpMethod.GET);
	}

	@Override
	public RequestHeadersUriSpec<?> head() {
		return methodInternal(HttpMethod.HEAD);
	}

	@Override
	public RequestBodyUriSpec post() {
		return methodInternal(HttpMethod.POST);
	}

	@Override
	public RequestBodyUriSpec put() {
		return methodInternal(HttpMethod.PUT);
	}

	@Override
	public RequestBodyUriSpec patch() {
		return methodInternal(HttpMethod.PATCH);
	}

	@Override
	public RequestHeadersUriSpec<?> delete() {
		return methodInternal(HttpMethod.DELETE);
	}

	@Override
	public RequestHeadersUriSpec<?> options() {
		return methodInternal(HttpMethod.OPTIONS);
	}

	@Override
	public RequestBodyUriSpec method(HttpMethod httpMethod) {
		return methodInternal(httpMethod);
	}

	private RequestBodyUriSpec methodInternal(HttpMethod httpMethod) {
		return new DefaultRequestBodyUriSpec(httpMethod);
	}

	@Override
	public Builder mutate() {
		return new DefaultWebTestClientBuilder(this.builder);
	}

	@Override
	public WebTestClient mutateWith(WebTestClientConfigurer configurer) {
		return mutate().apply(configurer).build();
	}


	private class DefaultRequestBodyUriSpec implements RequestBodyUriSpec {

		private final HttpMethod httpMethod;

		@Nullable
		private URI uri;

		private final HttpHeaders headers;

		@Nullable
		private MultiValueMap<String, String> cookies;

		@Nullable
		private BodyInserter<?, ? super ClientHttpRequest> inserter;

		private final Map<String, Object> attributes = new LinkedHashMap<>(4);

		@Nullable
		private Consumer<ClientHttpRequest> httpRequestConsumer;

		@Nullable
		private String uriTemplate;

		private final String requestId;

		DefaultRequestBodyUriSpec(HttpMethod httpMethod) {
			this.httpMethod = httpMethod;
			this.requestId = String.valueOf(requestIndex.incrementAndGet());
			this.headers = new HttpHeaders();
			this.headers.add(WebTestClient.WEBTESTCLIENT_REQUEST_ID, this.requestId);
		}

		@Override
		public RequestBodySpec uri(String uriTemplate, Object... uriVariables) {
			this.uriTemplate = uriTemplate;
			return uri(uriBuilderFactory.expand(uriTemplate, uriVariables));
		}

		@Override
		public RequestBodySpec uri(String uriTemplate, Map<String, ?> uriVariables) {
			this.uriTemplate = uriTemplate;
			return uri(uriBuilderFactory.expand(uriTemplate, uriVariables));
		}

		@Override
		public RequestBodySpec uri(Function<UriBuilder, URI> uriFunction) {
			this.uriTemplate = null;
			return uri(uriFunction.apply(uriBuilderFactory.builder()));
		}

		@Override
		public RequestBodySpec uri(URI uri) {
			this.uri = uri;
			return this;
		}

		private HttpHeaders getHeaders() {
			return this.headers;
		}

		private MultiValueMap<String, String> getCookies() {
			if (this.cookies == null) {
				this.cookies = new LinkedMultiValueMap<>(3);
			}
			return this.cookies;
		}

		@Override
		public RequestBodySpec header(String headerName, String... headerValues) {
			for (String headerValue : headerValues) {
				getHeaders().add(headerName, headerValue);
			}
			return this;
		}

		@Override
		public RequestBodySpec headers(Consumer<HttpHeaders> headersConsumer) {
			headersConsumer.accept(getHeaders());
			return this;
		}

		@Override
		public RequestBodySpec attribute(String name, Object value) {
			this.attributes.put(name, value);
			return this;
		}

		@Override
		public RequestBodySpec attributes(Consumer<Map<String, Object>> attributesConsumer) {
			attributesConsumer.accept(this.attributes);
			return this;
		}

		@Override
		public RequestBodySpec accept(MediaType... acceptableMediaTypes) {
			getHeaders().setAccept(Arrays.asList(acceptableMediaTypes));
			return this;
		}

		@Override
		public RequestBodySpec acceptCharset(Charset... acceptableCharsets) {
			getHeaders().setAcceptCharset(Arrays.asList(acceptableCharsets));
			return this;
		}

		@Override
		public RequestBodySpec contentType(MediaType contentType) {
			getHeaders().setContentType(contentType);
			return this;
		}

		@Override
		public RequestBodySpec contentLength(long contentLength) {
			getHeaders().setContentLength(contentLength);
			return this;
		}

		@Override
		public RequestBodySpec cookie(String name, String value) {
			getCookies().add(name, value);
			return this;
		}

		@Override
		public RequestBodySpec cookies(Consumer<MultiValueMap<String, String>> cookiesConsumer) {
			cookiesConsumer.accept(getCookies());
			return this;
		}

		@Override
		public RequestBodySpec ifModifiedSince(ZonedDateTime ifModifiedSince) {
			getHeaders().setIfModifiedSince(ifModifiedSince);
			return this;
		}

		@Override
		public RequestBodySpec ifNoneMatch(String... ifNoneMatches) {
			getHeaders().setIfNoneMatch(Arrays.asList(ifNoneMatches));
			return this;
		}

		@Override
		public RequestHeadersSpec<?> bodyValue(Object body) {
			this.inserter = BodyInserters.fromValue(body);
			return this;
		}

		@Override
		public <T, P extends Publisher<T>> RequestHeadersSpec<?> body(
				P publisher, ParameterizedTypeReference<T> elementTypeRef) {
			this.inserter = BodyInserters.fromPublisher(publisher, elementTypeRef);
			return this;
		}

		@Override
		public <T, P extends Publisher<T>> RequestHeadersSpec<?> body(P publisher, Class<T> elementClass) {
			this.inserter = BodyInserters.fromPublisher(publisher, elementClass);
			return this;
		}

		@Override
		public RequestHeadersSpec<?> body(Object producer, Class<?> elementClass) {
			this.inserter = BodyInserters.fromProducer(producer, elementClass);
			return this;
		}

		@Override
		public RequestHeadersSpec<?> body(Object producer, ParameterizedTypeReference<?> elementTypeRef) {
			this.inserter = BodyInserters.fromProducer(producer, elementTypeRef);
			return this;
		}

		@Override
		public RequestHeadersSpec<?> body(BodyInserter<?, ? super ClientHttpRequest> inserter) {
			this.inserter = inserter;
			return this;
		}

		@Override
		@Deprecated
		public RequestHeadersSpec<?> syncBody(Object body) {
			return bodyValue(body);
		}

		@Override
		public ResponseSpec exchange() {
			ClientRequest request = (this.inserter != null ?
					initRequestBuilder().body(this.inserter).build() :
					initRequestBuilder().build());

			ClientResponse response = exchangeFunction.exchange(request).block(getResponseTimeout());
			Assert.state(response != null, "No ClientResponse");

			ExchangeResult result = wiretapConnector.getExchangeResult(
					this.requestId, this.uriTemplate, getResponseTimeout());

			return new DefaultResponseSpec(result, response,
					DefaultWebTestClient.this.entityResultConsumer, getResponseTimeout());
		}

		private ClientRequest.Builder initRequestBuilder() {
			ClientRequest.Builder builder = ClientRequest.create(this.httpMethod, initUri())
					.headers(headers -> headers.addAll(initHeaders()))
					.cookies(cookies -> cookies.addAll(initCookies()))
					.attributes(attributes -> attributes.putAll(this.attributes));
			if (this.httpRequestConsumer != null) {
				builder.httpRequest(this.httpRequestConsumer);
			}
			return builder;
		}

		private URI initUri() {
			return (this.uri != null ? this.uri : uriBuilderFactory.expand(""));
		}

		private HttpHeaders initHeaders() {
			if (CollectionUtils.isEmpty(defaultHeaders)) {
				return this.headers;
			}
			HttpHeaders result = new HttpHeaders();
			result.putAll(defaultHeaders);
			result.putAll(this.headers);
			return result;
		}

		private MultiValueMap<String, String> initCookies() {
			if (CollectionUtils.isEmpty(this.cookies)) {
				return (defaultCookies != null ? defaultCookies : new LinkedMultiValueMap<>());
			}
			else if (CollectionUtils.isEmpty(defaultCookies)) {
				return this.cookies;
			}
			else {
				MultiValueMap<String, String> result = new LinkedMultiValueMap<>();
				result.putAll(defaultCookies);
				result.putAll(this.cookies);
				return result;
			}
		}
	}


	private static class DefaultResponseSpec implements ResponseSpec {

		private final ExchangeResult exchangeResult;

		private final ClientResponse response;

		private final Consumer<EntityExchangeResult<?>> entityResultConsumer;

		private final Duration timeout;


		DefaultResponseSpec(
				ExchangeResult exchangeResult, ClientResponse response,
				Consumer<EntityExchangeResult<?>> entityResultConsumer,
				Duration timeout) {

			this.exchangeResult = exchangeResult;
			this.response = response;
			this.entityResultConsumer = entityResultConsumer;
			this.timeout = timeout;
		}

		@Override
		public StatusAssertions expectStatus() {
			return new StatusAssertions(this.exchangeResult, this);
		}

		@Override
		public HeaderAssertions expectHeader() {
			return new HeaderAssertions(this.exchangeResult, this);
		}

		@Override
		public CookieAssertions expectCookie() {
			return new CookieAssertions(this.exchangeResult, this);
		}

		@Override
		public <B> BodySpec<B, ?> expectBody(Class<B> bodyType) {
			B body = this.response.bodyToMono(bodyType).block(this.timeout);
			EntityExchangeResult<B> entityResult = initEntityExchangeResult(body);
			return new DefaultBodySpec<>(entityResult);
		}

		@Override
		public <B> BodySpec<B, ?> expectBody(ParameterizedTypeReference<B> bodyType) {
			B body = this.response.bodyToMono(bodyType).block(this.timeout);
			EntityExchangeResult<B> entityResult = initEntityExchangeResult(body);
			return new DefaultBodySpec<>(entityResult);
		}

		@Override
		public <E> ListBodySpec<E> expectBodyList(Class<E> elementType) {
			return getListBodySpec(this.response.bodyToFlux(elementType));
		}

		@Override
		public <E> ListBodySpec<E> expectBodyList(ParameterizedTypeReference<E> elementType) {
			Flux<E> flux = this.response.bodyToFlux(elementType);
			return getListBodySpec(flux);
		}

		private <E> ListBodySpec<E> getListBodySpec(Flux<E> flux) {
			List<E> body = flux.collectList().block(this.timeout);
			EntityExchangeResult<List<E>> entityResult = initEntityExchangeResult(body);
			return new DefaultListBodySpec<>(entityResult);
		}

		@Override
		public BodyContentSpec expectBody() {
			ByteArrayResource resource = this.response.bodyToMono(ByteArrayResource.class).block(this.timeout);
			byte[] body = (resource != null ? resource.getByteArray() : null);
			EntityExchangeResult<byte[]> entityResult = initEntityExchangeResult(body);
			return new DefaultBodyContentSpec(entityResult);
		}

		private  <B> EntityExchangeResult<B> initEntityExchangeResult(@Nullable B body) {
			EntityExchangeResult<B> result = new EntityExchangeResult<>(this.exchangeResult, body);
			result.assertWithDiagnostics(() -> this.entityResultConsumer.accept(result));
			return result;
		}

		@Override
		public <T> FluxExchangeResult<T> returnResult(Class<T> elementClass) {
			Flux<T> body;
			if (elementClass.equals(Void.class)) {
				this.response.releaseBody().block();
				body = Flux.empty();
			}
			else {
				body = this.response.bodyToFlux(elementClass);
			}
			return new FluxExchangeResult<>(this.exchangeResult, body);
		}

		@Override
		public <T> FluxExchangeResult<T> returnResult(ParameterizedTypeReference<T> elementTypeRef) {
			Flux<T> body = this.response.bodyToFlux(elementTypeRef);
			return new FluxExchangeResult<>(this.exchangeResult, body);
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
			this.result.assertWithDiagnostics(() ->
					AssertionErrors.assertEquals("Response body", expected, this.result.getResponseBody()));
			return self();
		}

		@Override
		public <T extends S> T value(Matcher<? super B> matcher) {
			this.result.assertWithDiagnostics(() -> MatcherAssert.assertThat(this.result.getResponseBody(), matcher));
			return self();
		}

		@Override
		public <T extends S, R> T value(Function<B, R> bodyMapper, Matcher<? super R> matcher) {
			this.result.assertWithDiagnostics(() -> {
				B body = this.result.getResponseBody();
				MatcherAssert.assertThat(bodyMapper.apply(body), matcher);
			});
			return self();
		}

		@Override
		public <T extends S> T value(Consumer<B> consumer) {
			this.result.assertWithDiagnostics(() -> consumer.accept(this.result.getResponseBody()));
			return self();
		}

		@Override
		public <T extends S> T consumeWith(Consumer<EntityExchangeResult<B>> consumer) {
			this.result.assertWithDiagnostics(() -> consumer.accept(this.result));
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
			getResult().assertWithDiagnostics(() ->
					AssertionErrors.assertEquals(message, size, (actual != null ? actual.size() : 0)));
			return this;
		}

		@Override
		@SuppressWarnings("unchecked")
		public ListBodySpec<E> contains(E... elements) {
			List<E> expected = Arrays.asList(elements);
			List<E> actual = getResult().getResponseBody();
			String message = "Response body does not contain " + expected;
			getResult().assertWithDiagnostics(() ->
					AssertionErrors.assertTrue(message, (actual != null && actual.containsAll(expected))));
			return this;
		}

		@Override
		@SuppressWarnings("unchecked")
		public ListBodySpec<E> doesNotContain(E... elements) {
			List<E> expected = Arrays.asList(elements);
			List<E> actual = getResult().getResponseBody();
			String message = "Response body should not have contained " + expected;
			getResult().assertWithDiagnostics(() ->
					AssertionErrors.assertTrue(message, (actual == null || !actual.containsAll(expected))));
			return this;
		}

		@Override
		public EntityExchangeResult<List<E>> returnResult() {
			return getResult();
		}
	}


	private static class DefaultBodyContentSpec implements BodyContentSpec {

		private final EntityExchangeResult<byte[]> result;

		private final boolean isEmpty;

		DefaultBodyContentSpec(EntityExchangeResult<byte[]> result) {
			this.result = result;
			this.isEmpty = (result.getResponseBody() == null || result.getResponseBody().length == 0);
		}

		@Override
		public EntityExchangeResult<Void> isEmpty() {
			this.result.assertWithDiagnostics(() ->
					AssertionErrors.assertTrue("Expected empty body", this.isEmpty));
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
		public BodyContentSpec xml(String expectedXml) {
			this.result.assertWithDiagnostics(() -> {
				try {
					new XmlExpectationsHelper().assertXmlEqual(expectedXml, getBodyAsString());
				}
				catch (Exception ex) {
					throw new AssertionError("XML parsing error", ex);
				}
			});
			return this;
		}

		@Override
		public JsonPathAssertions jsonPath(String expression, Object... args) {
			return new JsonPathAssertions(this, getBodyAsString(), expression, args);
		}

		@Override
		public XpathAssertions xpath(String expression, @Nullable Map<String, String> namespaces, Object... args) {
			return new XpathAssertions(this, expression, namespaces, args);
		}

		private String getBodyAsString() {
			byte[] body = this.result.getResponseBody();
			if (body == null || body.length == 0) {
				return "";
			}
			Charset charset = Optional.ofNullable(this.result.getResponseHeaders().getContentType())
					.map(MimeType::getCharset).orElse(StandardCharsets.UTF_8);
			return new String(body, charset);
		}

		@Override
		public BodyContentSpec consumeWith(Consumer<EntityExchangeResult<byte[]>> consumer) {
			this.result.assertWithDiagnostics(() -> consumer.accept(this.result));
			return this;
		}

		@Override
		public EntityExchangeResult<byte[]> returnResult() {
			return this.result;
		}
	}

}
