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

package org.springframework.test.web.reactive.server.assertj;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.function.Supplier;

import org.assertj.core.api.AbstractByteArrayAssert;
import org.assertj.core.api.AbstractIntegerAssert;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ByteArrayAssert;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.http.HttpHeadersAssert;
import org.springframework.test.http.MediaTypeAssert;
import org.springframework.test.json.AbstractJsonContentAssert;
import org.springframework.test.json.JsonContent;
import org.springframework.test.json.JsonContentAssert;
import org.springframework.test.web.reactive.server.ExchangeResult;
import org.springframework.util.function.SingletonSupplier;

/**
 * AssertJ {@linkplain org.assertj.core.api.Assert assertions} for the result
 * from a {@link org.springframework.test.web.reactive.server.WebTestClient}
 * exchange.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
@SuppressWarnings({"UnusedReturnValue", "unused"})
public class WebTestClientResponseAssert
		extends AbstractObjectAssert<WebTestClientResponseAssert, WebTestClientResponse> {

	private final Supplier<MediaTypeAssert> contentTypeAssertSupplier;

	private final Supplier<HttpHeadersAssert> headersAssertSupplier;

	private final Supplier<AbstractIntegerAssert<?>> statusAssert;


	WebTestClientResponseAssert(WebTestClientResponse actual) {
		super(actual, WebTestClientResponseAssert.class);

		this.contentTypeAssertSupplier = SingletonSupplier.of(() ->
				new MediaTypeAssert(getExchangeResult().getResponseHeaders().getContentType()));

		this.headersAssertSupplier = SingletonSupplier.of(() ->
				new HttpHeadersAssert(getExchangeResult().getResponseHeaders()));

		this.statusAssert = SingletonSupplier.of(() ->
				Assertions.assertThat(getExchangeResult().getStatus().value()).as("HTTP status code"));
	}


	/**
	 * Verify that the HTTP status is equal to the specified status code.
	 * @param status the expected HTTP status code
	 */
	public WebTestClientResponseAssert hasStatus(int status) {
		status().isEqualTo(status);
		return this.myself;
	}

	/**
	 * Verify that the HTTP status is equal to the specified
	 * {@linkplain HttpStatus status}.
	 * @param status the expected HTTP status code
	 */
	public WebTestClientResponseAssert hasStatus(HttpStatus status) {
		return hasStatus(status.value());
	}

	/**
	 * Verify that the HTTP status is equal to {@link HttpStatus#OK}.
	 * @see #hasStatus(HttpStatus)
	 */
	public WebTestClientResponseAssert hasStatusOk() {
		return hasStatus(HttpStatus.OK);
	}

	/**
	 * Verify that the HTTP status code is in the 1xx range.
	 * @see <a href="https://datatracker.ietf.org/doc/html/rfc2616#section-10.1">RFC 2616</a>
	 */
	public WebTestClientResponseAssert hasStatus1xxInformational() {
		return hasStatusSeries(HttpStatus.Series.INFORMATIONAL);
	}

	/**
	 * Verify that the HTTP status code is in the 2xx range.
	 * @see <a href="https://datatracker.ietf.org/doc/html/rfc2616#section-10.2">RFC 2616</a>
	 */
	public WebTestClientResponseAssert hasStatus2xxSuccessful() {
		return hasStatusSeries(HttpStatus.Series.SUCCESSFUL);
	}

	/**
	 * Verify that the HTTP status code is in the 3xx range.
	 * @see <a href="https://datatracker.ietf.org/doc/html/rfc2616#section-10.3">RFC 2616</a>
	 */
	public WebTestClientResponseAssert hasStatus3xxRedirection() {
		return hasStatusSeries(HttpStatus.Series.REDIRECTION);
	}

	/**
	 * Verify that the HTTP status code is in the 4xx range.
	 * @see <a href="https://datatracker.ietf.org/doc/html/rfc2616#section-10.4">RFC 2616</a>
	 */
	public WebTestClientResponseAssert hasStatus4xxClientError() {
		return hasStatusSeries(HttpStatus.Series.CLIENT_ERROR);
	}

	/**
	 * Verify that the HTTP status code is in the 5xx range.
	 * @see <a href="https://datatracker.ietf.org/doc/html/rfc2616#section-10.5">RFC 2616</a>
	 */
	public WebTestClientResponseAssert hasStatus5xxServerError() {
		return hasStatusSeries(HttpStatus.Series.SERVER_ERROR);
	}

	private WebTestClientResponseAssert hasStatusSeries(HttpStatus.Series series) {
		HttpStatusCode status = getExchangeResult().getStatus();
		Assertions.assertThat(HttpStatus.Series.resolve(status.value())).as("HTTP status series").isEqualTo(series);
		return this.myself;
	}

	private AbstractIntegerAssert<?> status() {
		return this.statusAssert.get();
	}

	/**
	 * Return a new {@linkplain HttpHeadersAssert assertion} object that uses
	 * {@link HttpHeaders} as the object to test. The returned assertion object
	 * provides all the regular {@linkplain org.assertj.core.api.AbstractMapAssert
	 * map assertions}, with headers mapped by header name.
	 * Examples: <pre><code class="java">
	 * // Check for the presence of the Accept header:
	 * assertThat(response).headers().containsHeader(HttpHeaders.ACCEPT);
	 *
	 * // Check for the absence of the Content-Length header:
	 * assertThat(response).headers().doesNotContainsHeader(HttpHeaders.CONTENT_LENGTH);
	 * </code></pre>
	 */
	public HttpHeadersAssert headers() {
		return this.headersAssertSupplier.get();
	}

	/**
	 * Verify that the response contains a header with the given {@code name}.
	 * @param name the name of an expected HTTP header
	 */
	public WebTestClientResponseAssert containsHeader(String name) {
		headers().containsHeader(name);
		return this.myself;
	}

	/**
	 * Verify that the response does not contain a header with the given {@code name}.
	 * @param name the name of an HTTP header that should not be present
	 */
	public WebTestClientResponseAssert doesNotContainHeader(String name) {
		headers().doesNotContainHeader(name);
		return this.myself;
	}

	/**
	 * Verify that the response contains a header with the given {@code name}
	 * and primary {@code value}.
	 * @param name the name of an expected HTTP header
	 * @param value the expected value of the header
	 */
	public WebTestClientResponseAssert hasHeader(String name, String value) {
		headers().hasValue(name, value);
		return this.myself;
	}

	/**
	 * Return a new {@linkplain MediaTypeAssert assertion} object that uses the
	 * response's {@linkplain MediaType content type} as the object to test.
	 */
	public MediaTypeAssert contentType() {
		return this.contentTypeAssertSupplier.get();
	}

	/**
	 * Verify that the response's {@code Content-Type} is equal to the given value.
	 * @param contentType the expected content type
	 */
	public WebTestClientResponseAssert hasContentType(MediaType contentType) {
		contentType().isEqualTo(contentType);
		return this.myself;
	}

	/**
	 * Verify that the response's {@code Content-Type} is equal to the given
	 * string representation.
	 * @param contentType the expected content type
	 */
	public WebTestClientResponseAssert hasContentType(String contentType) {
		contentType().isEqualTo(contentType);
		return this.myself;
	}

	/**
	 * Verify that the response's {@code Content-Type} is
	 * {@linkplain MediaType#isCompatibleWith(MediaType) compatible} with the
	 * given value.
	 * @param contentType the expected compatible content type
	 */
	public WebTestClientResponseAssert hasContentTypeCompatibleWith(MediaType contentType) {
		contentType().isCompatibleWith(contentType);
		return this.myself;
	}

	/**
	 * Verify that the response's {@code Content-Type} is
	 * {@linkplain MediaType#isCompatibleWith(MediaType) compatible} with the
	 * given string representation.
	 * @param contentType the expected compatible content type
	 */
	public WebTestClientResponseAssert hasContentTypeCompatibleWith(String contentType) {
		contentType().isCompatibleWith(contentType);
		return this.myself;
	}

	/**
	 * Return a new {@linkplain ResponseCookieMapAssert assertion} object that uses the
	 * response's {@linkplain ResponseCookie cookies} as the object to test.
	 */
	public ResponseCookieMapAssert cookies() {
		return new ResponseCookieMapAssert(getCookies());
	}

	private ResponseCookie[] getCookies() {
		return getExchangeResult().getResponseCookies().values().stream()
				.flatMap(Collection::stream)
				.toArray(ResponseCookie[]:: new);
	}

	/**
	 * Return a new {@linkplain AbstractByteArrayAssert assertion} object that
	 * uses the response body as the object to test.
	 * @see #bodyText()
	 * @see #bodyJson()
	 */
	public AbstractByteArrayAssert<?> body() {
		return new ByteArrayAssert(getExchangeResult().getResponseBodyContent());
	}

	/**
	 * Return a new {@linkplain AbstractStringAssert assertion} object that uses
	 * the response body converted to text as the object to test.
	 * <p>Examples: <pre><code class="java">
	 * // Check that the response body is equal to "Hello World":
	 * assertThat(response).bodyText().isEqualTo("Hello World");
	 * </code></pre>
	 */
	public AbstractStringAssert<?> bodyText() {
		return Assertions.assertThat(readBody());
	}

	/**
	 * Verify that the response body is equal to the given value.
	 */
	public WebTestClientResponseAssert hasBodyTextEqualTo(String bodyText) {
		bodyText().isEqualTo(bodyText);
		return this.myself;
	}

	/**
	 * Return a new {@linkplain AbstractJsonContentAssert assertion} object that
	 * uses the response body converted to text as the object to test. Compared
	 * to {@link #bodyText()}, the assertion object provides dedicated JSON
	 * support.
	 * <p>Examples: <pre><code class="java">
	 * // Check that the response body is strictly equal to the content of
	 * // "/com/acme/sample/person-created.json":
	 * assertThat(response).bodyJson()
	 *         .isStrictlyEqualToJson("/com/acme/sample/person-created.json");
	 *
	 * // Check that the response is strictly equal to the content of the
	 * // specified file located in the same package as the PersonController:
	 * assertThat(response).bodyJson().withResourceLoadClass(PersonController.class)
	 *         .isStrictlyEqualToJson("person-created.json");
	 * </code></pre>
	 * The returned assert object also supports JSON path expressions.
	 * <p>Examples: <pre><code class="java">
	 * // Check that the JSON document does not have an "error" element
	 * assertThat(response).bodyJson().doesNotHavePath("$.error");
	 *
	 * // Check that the JSON document as a top level "message" element
	 * assertThat(response).bodyJson()
	 *         .extractingPath("$.message").asString().isEqualTo("hello");
	 * </code></pre>
	 */
	public AbstractJsonContentAssert<?> bodyJson() {
		return new JsonContentAssert(new JsonContent(readBody(), getExchangeResult().getJsonConverterDelegate()));
	}

	private String readBody() {
		return new String(getExchangeResult().getResponseBodyContent(), getCharset());
	}

	private Charset getCharset() {
		ExchangeResult result = getExchangeResult();
		MediaType contentType = result.getResponseHeaders().getContentType();
		Charset charset = (contentType != null ? contentType.getCharset() : null);
		return (charset != null ? charset : StandardCharsets.UTF_8);
	}

	private ExchangeResult getExchangeResult() {
		return this.actual.getExchangeResult();
	}

}
