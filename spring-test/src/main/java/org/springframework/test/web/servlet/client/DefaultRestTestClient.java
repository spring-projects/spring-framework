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

package org.springframework.test.web.servlet.client;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.jspecify.annotations.Nullable;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonAssert;
import org.springframework.test.json.JsonComparator;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.util.AssertionErrors;
import org.springframework.test.util.ExceptionCollector;
import org.springframework.test.util.XmlExpectationsHelper;
import org.springframework.util.MimeType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

/**
 * Default implementation of {@link RestTestClient}.
 *
 * @author Rob Worsnop
 */
class DefaultRestTestClient implements RestTestClient {

	private final RestClient restClient;

	private final AtomicLong requestIndex = new AtomicLong();

	private final RestClient.Builder restClientBuilder;

	DefaultRestTestClient(RestClient.Builder restClientBuilder) {
		this.restClient = restClientBuilder.build();
		this.restClientBuilder = restClientBuilder;
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

	@Override
	public Builder mutate() {
		return new DefaultRestTestClientBuilder(this.restClientBuilder);
	}

	private RequestBodyUriSpec methodInternal(HttpMethod httpMethod) {
		return new DefaultRequestBodyUriSpec(this.restClient.method(httpMethod));
	}


	private class DefaultRequestBodyUriSpec implements RequestBodyUriSpec {

		private final RestClient.RequestBodyUriSpec requestHeadersUriSpec;
		private RestClient.RequestBodySpec requestBodySpec;
		private final String requestId;


		public DefaultRequestBodyUriSpec(RestClient.RequestBodyUriSpec spec) {
			this.requestHeadersUriSpec = spec;
			this.requestBodySpec = spec;
			this.requestId = String.valueOf(requestIndex.incrementAndGet());
		}

		@Override
		public RequestBodySpec accept(MediaType... acceptableMediaTypes) {
			this.requestBodySpec = this.requestHeadersUriSpec.accept(acceptableMediaTypes);
			return this;
		}

		@Override
		public RequestBodySpec uri(URI uri) {
			this.requestBodySpec = this.requestHeadersUriSpec.uri(uri);
			return this;
		}

		@Override
		public RequestBodySpec uri(String uriTemplate, Object... uriVariables) {
			this.requestBodySpec = this.requestHeadersUriSpec.uri(uriTemplate, uriVariables);
			return this;
		}

		@Override
		public RequestBodySpec uri(String uri, Map<String, ?> uriVariables) {
			this.requestBodySpec = this.requestHeadersUriSpec.uri(uri, uriVariables);
			return this;
		}

		@Override
		public RequestBodySpec uri(Function<UriBuilder, URI> uriFunction) {
			this.requestBodySpec = this.requestHeadersUriSpec.uri(uriFunction);
			return this;
		}

		@Override
		public RequestBodySpec cookie(String name, String value) {
			this.requestBodySpec = this.requestHeadersUriSpec.cookie(name, value);
			return this;
		}

		@Override
		public RequestBodySpec cookies(Consumer<MultiValueMap<String, String>> cookiesConsumer) {
			this.requestBodySpec = this.requestHeadersUriSpec.cookies(cookiesConsumer);
			return this;
		}

		@Override
		public RequestBodySpec header(String headerName, String... headerValues) {
			this.requestBodySpec = this.requestHeadersUriSpec.header(headerName, headerValues);
			return this;
		}

		@Override
		public RequestBodySpec contentType(MediaType contentType) {
			this.requestBodySpec = this.requestHeadersUriSpec.contentType(contentType);
			return this;
		}

		@Override
		public RequestHeadersSpec<?> body(Object body) {
			this.requestHeadersUriSpec.body(body);
			return this;
		}

		@Override
		public RequestBodySpec acceptCharset(Charset... acceptableCharsets) {
			this.requestBodySpec = this.requestHeadersUriSpec.acceptCharset(acceptableCharsets);
			return this;
		}

		@Override
		public RequestBodySpec ifModifiedSince(ZonedDateTime ifModifiedSince) {
			this.requestBodySpec = this.requestHeadersUriSpec.ifModifiedSince(ifModifiedSince);
			return this;
		}

		@Override
		public RequestBodySpec ifNoneMatch(String... ifNoneMatches) {
			this.requestBodySpec = this.requestHeadersUriSpec.ifNoneMatch(ifNoneMatches);
			return this;
		}

		@Override
		public RequestBodySpec headers(Consumer<HttpHeaders> headersConsumer) {
			this.requestBodySpec = this.requestHeadersUriSpec.headers(headersConsumer);
			return this;
		}

		@Override
		public RequestBodySpec attribute(String name, Object value) {
			this.requestBodySpec = this.requestHeadersUriSpec.attribute(name, value);
			return this;
		}

		@Override
		public RequestBodySpec attributes(Consumer<Map<String, Object>> attributesConsumer) {
			this.requestBodySpec = this.requestHeadersUriSpec.attributes(attributesConsumer);
			return this;
		}

		@Override
		public ResponseSpec exchange() {
			this.requestBodySpec = this.requestBodySpec.header(RESTTESTCLIENT_REQUEST_ID, this.requestId);
			ExchangeResult exchangeResult = this.requestBodySpec.exchange(
					(clientRequest, clientResponse) -> new ExchangeResult(clientResponse),
					false);
			return new DefaultResponseSpec(Objects.requireNonNull(exchangeResult));
		}
	}

	private static class DefaultResponseSpec implements ResponseSpec {

		private final ExchangeResult exchangeResult;

		public DefaultResponseSpec(ExchangeResult exchangeResult) {
			this.exchangeResult = exchangeResult;
		}

		@Override
		public StatusAssertions expectStatus() {
			return new StatusAssertions(this.exchangeResult, this);
		}

		@Override
		public BodyContentSpec expectBody() {
			byte[] body = this.exchangeResult.getBody(byte[].class);
			return new DefaultBodyContentSpec( new EntityExchangeResult<>(this.exchangeResult, body));
		}

		@Override
		public <B> BodySpec<B, ?> expectBody(Class<B> bodyType) {
			B body = this.exchangeResult.getBody(bodyType);
			return new DefaultBodySpec<>(new EntityExchangeResult<>(this.exchangeResult, body));
		}

		@Override
		public <B> BodySpec<B, ?> expectBody(ParameterizedTypeReference<B> bodyType) {
			B body = this.exchangeResult.getBody(bodyType);
			return new DefaultBodySpec<>(new EntityExchangeResult<>(this.exchangeResult, body));
		}

		@Override
		public CookieAssertions expectCookie() {
			return new CookieAssertions(this.exchangeResult, this);
		}

		@Override
		public HeaderAssertions expectHeader() {
			return new HeaderAssertions(this.exchangeResult, this);
		}

		@Override
		public ResponseSpec expectAll(ResponseSpecConsumer... consumers) {
			ExceptionCollector exceptionCollector = new ExceptionCollector();
			for (ResponseSpecConsumer consumer : consumers) {
				exceptionCollector.execute(() -> consumer.accept(this));
			}
			try {
				exceptionCollector.assertEmpty();
			}
			catch (RuntimeException ex) {
				throw ex;
			}
			catch (Exception ex) {
				// In theory, a ResponseSpecConsumer should never throw an Exception
				// that is not a RuntimeException, but since ExceptionCollector may
				// throw a checked Exception, we handle this to appease the compiler
				// and in case someone uses a "sneaky throws" technique.
				throw new AssertionError(ex.getMessage(), ex);
			}
			return this;
		}

		@Override
		public <T> EntityExchangeResult<T> returnResult(Class<T> elementClass) {
			return new EntityExchangeResult<>(this.exchangeResult, this.exchangeResult.getBody(elementClass));
		}

		@Override
		public <T> EntityExchangeResult<T> returnResult(ParameterizedTypeReference<T> elementTypeRef) {
			return new EntityExchangeResult<>(this.exchangeResult, this.exchangeResult.getBody(elementTypeRef));
		}
	}

	private static class DefaultBodyContentSpec implements BodyContentSpec {
		private final EntityExchangeResult<byte[]> result;

		public DefaultBodyContentSpec(EntityExchangeResult<byte[]> result) {
			this.result = result;
		}

		@Override
		public EntityExchangeResult<Void> isEmpty() {
			this.result.assertWithDiagnostics(() ->
					AssertionErrors.assertTrue("Expected empty body",
							this.result.getBody(byte[].class) == null));
			return new EntityExchangeResult<>(this.result, null);
		}

		@Override
		public BodyContentSpec json(String expectedJson, JsonCompareMode compareMode) {
			return json(expectedJson, JsonAssert.comparator(compareMode));
		}

		@Override
		public BodyContentSpec json(String expectedJson, JsonComparator comparator) {
			this.result.assertWithDiagnostics(() -> {
				try {
					comparator.assertIsMatch(expectedJson, getBodyAsString());
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
		public JsonPathAssertions jsonPath(String expression) {
			return new JsonPathAssertions(this, getBodyAsString(), expression, null);
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
		public EntityExchangeResult<byte[]> returnResult() {
			return this.result;
		}
	}

	private static class DefaultBodySpec<B, S extends BodySpec<B, S>> implements BodySpec<B, S> {

		private final EntityExchangeResult<B> result;

		public DefaultBodySpec(@Nullable EntityExchangeResult<B> result) {
			this.result = Objects.requireNonNull(result, "exchangeResult must be non-null");
		}

		@Override
		public EntityExchangeResult<B> returnResult() {
			return this.result;
		}

		@Override
		public <T extends S> T isEqualTo(B expected) {
			this.result.assertWithDiagnostics(() ->
					AssertionErrors.assertEquals("Response body", expected, this.result.getResponseBody()));
			return self();
		}

		@Override
		@SuppressWarnings("NullAway") // https://github.com/uber/NullAway/issues/1129
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
	}
}
