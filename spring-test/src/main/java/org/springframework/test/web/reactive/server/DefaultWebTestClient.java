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
import org.springframework.test.util.JsonExpectationsHelper;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * Default implementation of {@link WebTestClient}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class DefaultWebTestClient implements WebTestClient {

	private final WebClient webClient;

	private final WiretapConnector wiretapConnector;

	private final Duration timeout;

	private final DefaultWebTestClientBuilder builder;

	private final AtomicLong requestIndex = new AtomicLong();


	DefaultWebTestClient(WebClient.Builder clientBuilder, ClientHttpConnector connector,
			@Nullable Duration timeout, DefaultWebTestClientBuilder webTestClientBuilder) {

		Assert.notNull(clientBuilder, "WebClient.Builder is required");
		this.wiretapConnector = new WiretapConnector(connector);
		this.webClient = clientBuilder.clientConnector(this.wiretapConnector).build();
		this.timeout = (timeout != null ? timeout : Duration.ofSeconds(5));
		this.builder = webTestClientBuilder;
	}


	private Duration getTimeout() {
		return this.timeout;
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
	public RequestBodyUriSpec method(HttpMethod method) {
		return methodInternal(method);
	}

	private RequestBodyUriSpec methodInternal(HttpMethod method) {
		return new DefaultRequestBodyUriSpec(this.webClient.method(method));
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

		private final WebClient.RequestBodyUriSpec bodySpec;

		@Nullable
		private String uriTemplate;

		private final String requestId;

		DefaultRequestBodyUriSpec(WebClient.RequestBodyUriSpec spec) {
			this.bodySpec = spec;
			this.requestId = String.valueOf(requestIndex.incrementAndGet());
			this.bodySpec.header(WebTestClient.WEBTESTCLIENT_REQUEST_ID, this.requestId);
		}

		@Override
		public RequestBodySpec uri(String uriTemplate, Object... uriVariables) {
			this.bodySpec.uri(uriTemplate, uriVariables);
			this.uriTemplate = uriTemplate;
			return this;
		}

		@Override
		public RequestBodySpec uri(String uriTemplate, Map<String, ?> uriVariables) {
			this.bodySpec.uri(uriTemplate, uriVariables);
			this.uriTemplate = uriTemplate;
			return this;
		}

		@Override
		public RequestBodySpec uri(Function<UriBuilder, URI> uriFunction) {
			this.bodySpec.uri(uriFunction);
			this.uriTemplate = null;
			return this;
		}

		@Override
		public RequestBodySpec uri(URI uri) {
			this.bodySpec.uri(uri);
			this.uriTemplate = null;
			return this;
		}

		@Override
		public RequestBodySpec header(String headerName, String... headerValues) {
			this.bodySpec.header(headerName, headerValues);
			return this;
		}

		@Override
		public RequestBodySpec headers(Consumer<HttpHeaders> headersConsumer) {
			this.bodySpec.headers(headersConsumer);
			return this;
		}

		@Override
		public RequestBodySpec attribute(String name, Object value) {
			this.bodySpec.attribute(name, value);
			return this;
		}

		@Override
		public RequestBodySpec attributes(
				Consumer<Map<String, Object>> attributesConsumer) {
			this.bodySpec.attributes(attributesConsumer);
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
		public RequestBodySpec cookies(
				Consumer<MultiValueMap<String, String>> cookiesConsumer) {
			this.bodySpec.cookies(cookiesConsumer);
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
		public RequestHeadersSpec<?> body(BodyInserter<?, ? super ClientHttpRequest> inserter) {
			this.bodySpec.body(inserter);
			return this;
		}

		@Override
		public <T, S extends Publisher<T>> RequestHeadersSpec<?> body(S publisher, Class<T> elementClass) {
			this.bodySpec.body(publisher, elementClass);
			return this;
		}

		@Override
		public RequestHeadersSpec<?> syncBody(Object body) {
			this.bodySpec.syncBody(body);
			return this;
		}

		@Override
		public ResponseSpec exchange() {
			ClientResponse clientResponse = this.bodySpec.exchange().block(getTimeout());
			Assert.state(clientResponse != null, "No ClientResponse");
			WiretapConnector.Info info = wiretapConnector.claimRequest(this.requestId);
			return new DefaultResponseSpec(info, clientResponse, this.uriTemplate, getTimeout());
		}
	}


	private static class DefaultResponseSpec implements ResponseSpec {

		private final ExchangeResult exchangeResult;

		private final ClientResponse response;

		private final Duration timeout;


		DefaultResponseSpec(WiretapConnector.Info wiretapInfo, ClientResponse response,
				@Nullable String uriTemplate, Duration timeout) {

			this.exchangeResult = wiretapInfo.createExchangeResult(uriTemplate);
			this.response = response;
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
		public <B> BodySpec<B, ?> expectBody(Class<B> bodyType) {
			B body = this.response.bodyToMono(bodyType).block(this.timeout);
			EntityExchangeResult<B> entityResult = new EntityExchangeResult<>(this.exchangeResult, body);
			return new DefaultBodySpec<>(entityResult);
		}

		@Override
		public <B> BodySpec<B, ?> expectBody(ParameterizedTypeReference<B> bodyType) {
			B body = this.response.bodyToMono(bodyType).block(this.timeout);
			EntityExchangeResult<B> entityResult = new EntityExchangeResult<>(this.exchangeResult, body);
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
			EntityExchangeResult<List<E>> entityResult = new EntityExchangeResult<>(this.exchangeResult, body);
			return new DefaultListBodySpec<>(entityResult);
		}

		@Override
		public BodyContentSpec expectBody() {
			ByteArrayResource resource = this.response.bodyToMono(ByteArrayResource.class).block(this.timeout);
			byte[] body = (resource != null ? resource.getByteArray() : null);
			EntityExchangeResult<byte[]> entityResult = new EntityExchangeResult<>(this.exchangeResult, body);
			return new DefaultBodyContentSpec(entityResult);
		}

		@Override
		public <T> FluxExchangeResult<T> returnResult(Class<T> elementType) {
			Flux<T> body = this.response.bodyToFlux(elementType);
			return new FluxExchangeResult<>(this.exchangeResult, body, this.timeout);
		}

		@Override
		public <T> FluxExchangeResult<T> returnResult(ParameterizedTypeReference<T> elementType) {
			Flux<T> body = this.response.bodyToFlux(elementType);
			return new FluxExchangeResult<>(this.exchangeResult, body, this.timeout);
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
					assertEquals("Response body", expected, this.result.getResponseBody()));
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
			getResult().assertWithDiagnostics(() -> assertEquals(message, size, (actual != null ? actual.size() : 0)));
			return this;
		}

		@Override
		@SuppressWarnings("unchecked")
		public ListBodySpec<E> contains(E... elements) {
			List<E> expected = Arrays.asList(elements);
			List<E> actual = getResult().getResponseBody();
			String message = "Response body does not contain " + expected;
			getResult().assertWithDiagnostics(() -> assertTrue(message, (actual != null && actual.containsAll(expected))));
			return this;
		}

		@Override
		@SuppressWarnings("unchecked")
		public ListBodySpec<E> doesNotContain(E... elements) {
			List<E> expected = Arrays.asList(elements);
			List<E> actual = getResult().getResponseBody();
			String message = "Response body should not have contained " + expected;
			getResult().assertWithDiagnostics(() -> assertTrue(message, (actual == null || !actual.containsAll(expected))));
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
			return new JsonPathAssertions(this, getBodyAsString(), expression, args);
		}

		private String getBodyAsString() {
			byte[] body = this.result.getResponseBody();
			if (body == null || body.length == 0) {
				return "";
			}
			MediaType mediaType = this.result.getResponseHeaders().getContentType();
			Charset charset = Optional.ofNullable(mediaType).map(MimeType::getCharset).orElse(UTF_8);
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
