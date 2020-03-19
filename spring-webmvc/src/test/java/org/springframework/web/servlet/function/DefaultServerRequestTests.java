/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.servlet.function;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.Principal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

import javax.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.testfixture.server.MockServerWebExchange;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpSession;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest.get;

/**
 * @author Arjen Poutsma
 * @since 5.1
 */
public class DefaultServerRequestTests {

	private final List<HttpMessageConverter<?>> messageConverters = Collections.singletonList(
			new StringHttpMessageConverter());

	@Test
	public void method() {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest("HEAD", "/");
		DefaultServerRequest request =
				new DefaultServerRequest(servletRequest, this.messageConverters);

		assertThat(request.method()).isEqualTo(HttpMethod.HEAD);
	}

	@Test
	public void uri() {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/");
		servletRequest.setServerName("example.com");
		servletRequest.setScheme("https");
		servletRequest.setServerPort(443);

		DefaultServerRequest request =
				new DefaultServerRequest(servletRequest, this.messageConverters);

		assertThat(request.uri()).isEqualTo(URI.create("https://example.com/"));
	}

	@Test
	public void uriBuilder() {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/path");
		servletRequest.setQueryString("a=1");

		DefaultServerRequest request =
				new DefaultServerRequest(servletRequest, this.messageConverters);

		URI result = request.uriBuilder().build();
		assertThat(result.getScheme()).isEqualTo("http");
		assertThat(result.getHost()).isEqualTo("localhost");
		assertThat(result.getPort()).isEqualTo(-1);
		assertThat(result.getPath()).isEqualTo("/path");
		assertThat(result.getQuery()).isEqualTo("a=1");
	}

	@Test
	public void attribute() {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/");
		servletRequest.setAttribute("foo", "bar");

		DefaultServerRequest request =
				new DefaultServerRequest(servletRequest, this.messageConverters);

		assertThat(request.attribute("foo")).isEqualTo(Optional.of("bar"));
	}

	@Test
	public void params() {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/");
		servletRequest.setParameter("foo", "bar");

		DefaultServerRequest request =
				new DefaultServerRequest(servletRequest, this.messageConverters);

		assertThat(request.param("foo")).isEqualTo(Optional.of("bar"));
	}

	@Test
	public void emptyQueryParam() {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/");
		servletRequest.setParameter("foo", "");

		DefaultServerRequest request =
				new DefaultServerRequest(servletRequest, this.messageConverters);

		assertThat(request.param("foo")).isEqualTo(Optional.of(""));
	}

	@Test
	public void absentQueryParam() {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/");
		servletRequest.setParameter("foo", "");

		DefaultServerRequest request =
				new DefaultServerRequest(servletRequest, this.messageConverters);

		assertThat(request.param("bar")).isEqualTo(Optional.empty());
	}

	@Test
	public void pathVariable() {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/");
		Map<String, String> pathVariables = Collections.singletonMap("foo", "bar");
		servletRequest
				.setAttribute(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE, pathVariables);

		DefaultServerRequest request = new DefaultServerRequest(servletRequest,
				this.messageConverters);

		assertThat(request.pathVariable("foo")).isEqualTo("bar");
	}

	@Test
	public void pathVariableNotFound() {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/");
		Map<String, String> pathVariables = Collections.singletonMap("foo", "bar");
		servletRequest
				.setAttribute(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE, pathVariables);

		DefaultServerRequest request = new DefaultServerRequest(servletRequest,
				this.messageConverters);

		assertThatIllegalArgumentException().isThrownBy(() ->
				request.pathVariable("baz"));
	}

	@Test
	public void pathVariables() {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/");
		Map<String, String> pathVariables = Collections.singletonMap("foo", "bar");
		servletRequest
				.setAttribute(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE, pathVariables);

		DefaultServerRequest request = new DefaultServerRequest(servletRequest,
				this.messageConverters);

		assertThat(request.pathVariables()).isEqualTo(pathVariables);
	}

	@Test
	public void header() {
		HttpHeaders httpHeaders = new HttpHeaders();
		List<MediaType> accept =
				Collections.singletonList(MediaType.APPLICATION_JSON);
		httpHeaders.setAccept(accept);
		List<Charset> acceptCharset = Collections.singletonList(UTF_8);
		httpHeaders.setAcceptCharset(acceptCharset);
		long contentLength = 42L;
		httpHeaders.setContentLength(contentLength);
		MediaType contentType = MediaType.TEXT_PLAIN;
		httpHeaders.setContentType(contentType);
		InetSocketAddress host = InetSocketAddress.createUnresolved("localhost", 80);
		httpHeaders.setHost(host);
		List<HttpRange> range = Collections.singletonList(HttpRange.createByteRange(0, 42));
		httpHeaders.setRange(range);

		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/");
		httpHeaders.forEach(servletRequest::addHeader);
		servletRequest.setContentType(MediaType.TEXT_PLAIN_VALUE);

		DefaultServerRequest request = new DefaultServerRequest(servletRequest,
				this.messageConverters);

		ServerRequest.Headers headers = request.headers();
		assertThat(headers.accept()).isEqualTo(accept);
		assertThat(headers.acceptCharset()).isEqualTo(acceptCharset);
		assertThat(headers.contentLength()).isEqualTo(OptionalLong.of(contentLength));
		assertThat(headers.contentType()).isEqualTo(Optional.of(contentType));
		assertThat(headers.header(HttpHeaders.CONTENT_TYPE)).containsExactly(MediaType.TEXT_PLAIN_VALUE);
		assertThat(headers.firstHeader(HttpHeaders.CONTENT_TYPE)).isEqualTo(MediaType.TEXT_PLAIN_VALUE);
		assertThat(headers.asHttpHeaders()).isEqualTo(httpHeaders);
	}

	@Test
	public void cookies() {
		Cookie cookie = new Cookie("foo", "bar");

		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/");
		servletRequest.setCookies(cookie);

		DefaultServerRequest request = new DefaultServerRequest(servletRequest,
				this.messageConverters);

		MultiValueMap<String, Cookie> expected = new LinkedMultiValueMap<>();
		expected.add("foo", cookie);

		assertThat(request.cookies()).isEqualTo(expected);

	}

	@Test
	public void bodyClass() throws Exception {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/");
		servletRequest.setContentType(MediaType.TEXT_PLAIN_VALUE);
		servletRequest.setContent("foo".getBytes(UTF_8));

		DefaultServerRequest request = new DefaultServerRequest(servletRequest,
				this.messageConverters);

		String result = request.body(String.class);
		assertThat(result).isEqualTo("foo");
	}

	@Test
	public void bodyParameterizedTypeReference() throws Exception {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/");
		servletRequest.setContentType(MediaType.APPLICATION_JSON_VALUE);
		servletRequest.setContent("[\"foo\",\"bar\"]".getBytes(UTF_8));

		DefaultServerRequest request = new DefaultServerRequest(servletRequest,
				Collections.singletonList(new MappingJackson2HttpMessageConverter()));

		List<String> result = request.body(new ParameterizedTypeReference<List<String>>() {});
		assertThat(result.size()).isEqualTo(2);
		assertThat(result.get(0)).isEqualTo("foo");
		assertThat(result.get(1)).isEqualTo("bar");
	}

	@Test
	public void bodyUnacceptable() throws Exception {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/");
		servletRequest.setContentType(MediaType.TEXT_PLAIN_VALUE);
		servletRequest.setContent("foo".getBytes(UTF_8));

		DefaultServerRequest request =
				new DefaultServerRequest(servletRequest, Collections.emptyList());

		assertThatExceptionOfType(HttpMediaTypeNotSupportedException.class).isThrownBy(() ->
				request.body(String.class));
	}

	@Test
	public void session() {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/");
		MockHttpSession session = new MockHttpSession();
		servletRequest.setSession(session);

		DefaultServerRequest request = new DefaultServerRequest(servletRequest,
				this.messageConverters);

		assertThat(request.session()).isEqualTo(session);

	}

	@Test
	public void principal() {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/");
		Principal principal = new Principal() {
			@Override
			public String getName() {
				return "foo";
			}
		};
		servletRequest.setUserPrincipal(principal);

		DefaultServerRequest request = new DefaultServerRequest(servletRequest,
				this.messageConverters);

		assertThat(request.principal().get()).isEqualTo(principal);
	}

	@ParameterizedHttpMethodTest
	void checkNotModifiedTimestamp(String method) throws Exception {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest(method, "/");
		Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		servletRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, now.toEpochMilli());

		DefaultServerRequest request = new DefaultServerRequest(servletRequest, this.messageConverters);

		Optional<ServerResponse> result = request.checkNotModified(now, "");

		assertThat(result).hasValueSatisfying(serverResponse -> {
			assertThat(serverResponse.statusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
			assertThat(serverResponse.headers().getLastModified()).isEqualTo(now.toEpochMilli());
		});
	}

	@ParameterizedHttpMethodTest
	void checkModifiedTimestamp(String method) {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest(method, "/");
		Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		Instant oneMinuteAgo = now.minus(1, ChronoUnit.MINUTES);
		servletRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, oneMinuteAgo.toEpochMilli());

		DefaultServerRequest request = new DefaultServerRequest(servletRequest, this.messageConverters);

		Optional<ServerResponse> result = request.checkNotModified(now, "");

		assertThat(result).isEmpty();
	}

	@ParameterizedHttpMethodTest
	void checkNotModifiedETag(String method) {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest(method, "/");
		String eTag = "\"Foo\"";
		servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, eTag);

		DefaultServerRequest request = new DefaultServerRequest(servletRequest, this.messageConverters);

		Optional<ServerResponse> result = request.checkNotModified(eTag);

		assertThat(result).hasValueSatisfying(serverResponse -> {
			assertThat(serverResponse.statusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
			assertThat(serverResponse.headers().getETag()).isEqualTo(eTag);
		});
	}

	@ParameterizedHttpMethodTest
	void checkNotModifiedETagWithSeparatorChars(String method) {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest(method, "/");
		String eTag = "\"Foo, Bar\"";
		servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, eTag);

		DefaultServerRequest request = new DefaultServerRequest(servletRequest, this.messageConverters);

		Optional<ServerResponse> result = request.checkNotModified(eTag);

		assertThat(result).hasValueSatisfying(serverResponse -> {
			assertThat(serverResponse.statusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
			assertThat(serverResponse.headers().getETag()).isEqualTo(eTag);
		});
	}

	@ParameterizedHttpMethodTest
	void checkModifiedETag(String method) {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest(method, "/");
		String currentETag = "\"Foo\"";
		String oldEtag = "Bar";
		servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, oldEtag);

		DefaultServerRequest request = new DefaultServerRequest(servletRequest, this.messageConverters);

		Optional<ServerResponse> result = request.checkNotModified(currentETag);

		assertThat(result).isEmpty();
	}

	@ParameterizedHttpMethodTest
	void checkNotModifiedUnpaddedETag(String method) {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest(method, "/");
		String eTag = "Foo";
		String paddedEtag = String.format("\"%s\"", eTag);
		servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, paddedEtag);

		DefaultServerRequest request = new DefaultServerRequest(servletRequest, this.messageConverters);

		Optional<ServerResponse> result = request.checkNotModified(eTag);

		assertThat(result).hasValueSatisfying(serverResponse -> {
			assertThat(serverResponse.statusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
			assertThat(serverResponse.headers().getETag()).isEqualTo(paddedEtag);
		});
	}

	@ParameterizedHttpMethodTest
	void checkModifiedUnpaddedETag(String method) {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest(method, "/");
		String currentETag = "Foo";
		String oldEtag = "Bar";
		servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, oldEtag);

		DefaultServerRequest request = new DefaultServerRequest(servletRequest, this.messageConverters);

		Optional<ServerResponse> result = request.checkNotModified(currentETag);

		assertThat(result).isEmpty();
	}

	@ParameterizedHttpMethodTest
	void checkNotModifiedWildcardIsIgnored(String method) {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest(method, "/");
		String eTag = "\"Foo\"";
		servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, "*");
		DefaultServerRequest request = new DefaultServerRequest(servletRequest, this.messageConverters);

		Optional<ServerResponse> result = request.checkNotModified(eTag);

		assertThat(result).isEmpty();
	}

	@ParameterizedHttpMethodTest
	void checkNotModifiedETagAndTimestamp(String method) {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest(method, "/");
		String eTag = "\"Foo\"";
		Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, eTag);
		servletRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, now.toEpochMilli());

		DefaultServerRequest request = new DefaultServerRequest(servletRequest, this.messageConverters);

		Optional<ServerResponse> result = request.checkNotModified(now, eTag);

		assertThat(result).hasValueSatisfying(serverResponse -> {
			assertThat(serverResponse.statusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
			assertThat(serverResponse.headers().getETag()).isEqualTo(eTag);
			assertThat(serverResponse.headers().getLastModified()).isEqualTo(now.toEpochMilli());
		});

	}

	@ParameterizedHttpMethodTest
	void checkNotModifiedETagAndModifiedTimestamp(String method) {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest(method, "/");
		String eTag = "\"Foo\"";
		Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		Instant oneMinuteAgo = now.minus(1, ChronoUnit.MINUTES);
		servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, eTag);
		servletRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, oneMinuteAgo.toEpochMilli());
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/")
				.ifNoneMatch(eTag)
				.ifModifiedSince(oneMinuteAgo.toEpochMilli())
				);

		DefaultServerRequest request = new DefaultServerRequest(servletRequest, this.messageConverters);

		Optional<ServerResponse> result = request.checkNotModified(now, eTag);

		assertThat(result).hasValueSatisfying(serverResponse -> {
			assertThat(serverResponse.statusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
			assertThat(serverResponse.headers().getETag()).isEqualTo(eTag);
			assertThat(serverResponse.headers().getLastModified()).isEqualTo(now.toEpochMilli());
		});
	}

	@ParameterizedHttpMethodTest
	void checkModifiedETagAndNotModifiedTimestamp(String method) throws Exception {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest(method, "/");
		String currentETag = "\"Foo\"";
		String oldEtag = "\"Bar\"";
		Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, oldEtag);
		servletRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, now.toEpochMilli());

		DefaultServerRequest request = new DefaultServerRequest(servletRequest, this.messageConverters);

		Optional<ServerResponse> result = request.checkNotModified(now, currentETag);

		assertThat(result).isEmpty();
	}


	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@ParameterizedTest(name = "[{index}] {0}")
	@ValueSource(strings = { "GET", "HEAD" })
	@interface ParameterizedHttpMethodTest {
	}

}
