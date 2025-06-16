/*
 * Copyright 2002-2025 the original author or authors.
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.Part;
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
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.servlet.handler.PathPatternsTestUtils;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpSession;
import org.springframework.web.testfixture.servlet.MockPart;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link DefaultServerRequest}.
 *
 * @author Arjen Poutsma
 */
class DefaultServerRequestTests {

	private final List<HttpMessageConverter<?>> messageConverters = List.of(new StringHttpMessageConverter());


	@Test
	void method() {
		MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("HEAD", "/", true);
		DefaultServerRequest request = new DefaultServerRequest(servletRequest, this.messageConverters);

		assertThat(request.method()).isEqualTo(HttpMethod.HEAD);
	}

	@Test
	void uri() {
		MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
		servletRequest.setServerName("example.com");
		servletRequest.setScheme("https");
		servletRequest.setServerPort(443);

		DefaultServerRequest request = new DefaultServerRequest(servletRequest, this.messageConverters);

		assertThat(request.uri()).isEqualTo(URI.create("https://example.com/"));
	}

	@Test
	void uriBuilder() {
		MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("GET", "/path", true);
		servletRequest.setQueryString("a=1");

		DefaultServerRequest request = new DefaultServerRequest(servletRequest, this.messageConverters);

		URI result = request.uriBuilder().build();
		assertThat(result.getScheme()).isEqualTo("http");
		assertThat(result.getHost()).isEqualTo("localhost");
		assertThat(result.getPort()).isEqualTo(-1);
		assertThat(result.getPath()).isEqualTo("/path");
		assertThat(result.getQuery()).isEqualTo("a=1");
	}

	@Test
	void attribute() {
		MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
		servletRequest.setAttribute("foo", "bar");

		DefaultServerRequest request = new DefaultServerRequest(servletRequest, this.messageConverters);

		assertThat(request.attribute("foo")).contains("bar");
	}

	@Test
	void attributes() {
		MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
		servletRequest.setAttribute("foo", "bar");
		servletRequest.setAttribute("baz", "qux");

		DefaultServerRequest request = new DefaultServerRequest(servletRequest, this.messageConverters);

		Map<String, Object> attributesMap = request.attributes();
		assertThat(attributesMap).isNotEmpty();
		assertThat(attributesMap).containsEntry("foo", "bar");
		assertThat(attributesMap).containsEntry("baz", "qux");
		assertThat(attributesMap).doesNotContainEntry("foo", "blah");

		Set<Map.Entry<String, Object>> entrySet = attributesMap.entrySet();
		assertThat(entrySet).isNotEmpty();
		assertThat(entrySet).hasSize(attributesMap.size());
		assertThat(entrySet).contains(Map.entry("foo", "bar"));
		assertThat(entrySet).contains(Map.entry("baz", "qux"));
		assertThat(entrySet).doesNotContain(Map.entry("foo", "blah"));
		assertThat(entrySet).isUnmodifiable();

		assertThat(entrySet.iterator()).toIterable().contains(Map.entry("foo", "bar"), Map.entry("baz", "qux"));
		Iterator<String> attributes = servletRequest.getAttributeNames().asIterator();
		Iterator<Map.Entry<String, Object>> entrySetIterator = entrySet.iterator();
		while (attributes.hasNext()) {
			attributes.next();
			assertThat(entrySetIterator).hasNext();
			entrySetIterator.next();
		}
		assertThat(entrySetIterator).isExhausted();

		attributesMap.clear();
		assertThat(attributesMap).isEmpty();
		assertThat(attributesMap).hasSize(0);
		assertThat(entrySet).isEmpty();
		assertThat(entrySet).hasSize(0);
		assertThat(entrySet.iterator()).isExhausted();
	}

	@Test
	void params() {
		MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
		servletRequest.setParameter("foo", "bar");

		DefaultServerRequest request = new DefaultServerRequest(servletRequest, this.messageConverters);

		assertThat(request.param("foo")).contains("bar");
	}

	@Test
	void multipartData() throws Exception {
		MockPart formPart = new MockPart("form", "foo".getBytes(UTF_8));
		MockPart filePart = new MockPart("file", "foo.txt", "foo".getBytes(UTF_8));

		MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("POST", "/", true);
		servletRequest.addPart(formPart);
		servletRequest.addPart(filePart);

		DefaultServerRequest request = new DefaultServerRequest(servletRequest, this.messageConverters);

		MultiValueMap<String, Part> result = request.multipartData();

		assertThat(result).hasSize(2);
		assertThat(result.get("form")).containsExactly(formPart);
		assertThat(result.get("file")).containsExactly(filePart);
	}

	@Test
	void emptyQueryParam() {
		MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
		servletRequest.setParameter("foo", "");

		DefaultServerRequest request = new DefaultServerRequest(servletRequest, this.messageConverters);

		assertThat(request.param("foo")).contains("");
	}

	@Test
	void absentQueryParam() {
		MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
		servletRequest.setParameter("foo", "");

		DefaultServerRequest request = new DefaultServerRequest(servletRequest, this.messageConverters);

		assertThat(request.param("bar")).isNotPresent();
	}

	@Test
	void pathVariable() {
		MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
		Map<String, String> pathVariables = Collections.singletonMap("foo", "bar");
		servletRequest.setAttribute(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE, pathVariables);

		DefaultServerRequest request = new DefaultServerRequest(servletRequest, this.messageConverters);

		assertThat(request.pathVariable("foo")).isEqualTo("bar");
	}

	@Test
	void pathVariableNotFound() {
		MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
		Map<String, String> pathVariables = Collections.singletonMap("foo", "bar");
		servletRequest.setAttribute(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE, pathVariables);

		DefaultServerRequest request = new DefaultServerRequest(servletRequest, this.messageConverters);

		assertThatIllegalArgumentException().isThrownBy(() -> request.pathVariable("baz"));
	}

	@Test
	void pathVariables() {
		MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
		Map<String, String> pathVariables = Collections.singletonMap("foo", "bar");
		servletRequest.setAttribute(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE, pathVariables);

		DefaultServerRequest request = new DefaultServerRequest(servletRequest, this.messageConverters);

		assertThat(request.pathVariables()).isEqualTo(pathVariables);
	}

	@Test
	void header() {
		HttpHeaders httpHeaders = new HttpHeaders();
		List<MediaType> accept = Collections.singletonList(MediaType.APPLICATION_JSON);
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

		MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
		httpHeaders.forEach(servletRequest::addHeader);
		servletRequest.setContentType(MediaType.TEXT_PLAIN_VALUE);

		DefaultServerRequest request = new DefaultServerRequest(servletRequest, this.messageConverters);

		ServerRequest.Headers headers = request.headers();
		assertThat(headers.accept()).isEqualTo(accept);
		assertThat(headers.acceptCharset()).isEqualTo(acceptCharset);
		assertThat(headers.contentLength()).isEqualTo(OptionalLong.of(contentLength));
		assertThat(headers.contentType()).contains(contentType);
		assertThat(headers.header(HttpHeaders.CONTENT_TYPE)).containsExactly(MediaType.TEXT_PLAIN_VALUE);
		assertThat(headers.firstHeader(HttpHeaders.CONTENT_TYPE)).isEqualTo(MediaType.TEXT_PLAIN_VALUE);
		assertThat(headers.asHttpHeaders()).isEqualTo(httpHeaders);
	}

	@Test
	void cookies() {
		Cookie cookie = new Cookie("foo", "bar");

		MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
		servletRequest.setCookies(cookie);

		DefaultServerRequest request = new DefaultServerRequest(servletRequest, this.messageConverters);

		MultiValueMap<String, Cookie> expected = new LinkedMultiValueMap<>();
		expected.add("foo", cookie);

		assertThat(request.cookies()).isEqualTo(expected);

	}

	@Test
	void bodyClass() throws Exception {
		MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
		servletRequest.setContentType(MediaType.TEXT_PLAIN_VALUE);
		servletRequest.setContent("foo".getBytes(UTF_8));

		DefaultServerRequest request = new DefaultServerRequest(servletRequest, this.messageConverters);

		String result = request.body(String.class);
		assertThat(result).isEqualTo("foo");
	}

	@Test
	void bodyParameterizedTypeReference() throws Exception {
		MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
		servletRequest.setContentType(MediaType.APPLICATION_JSON_VALUE);
		servletRequest.setContent("[\"foo\",\"bar\"]".getBytes(UTF_8));

		DefaultServerRequest request = new DefaultServerRequest(servletRequest,
				List.of(new JacksonJsonHttpMessageConverter()));

		List<String> result = request.body(new ParameterizedTypeReference<>() {});
		assertThat(result).containsExactly("foo", "bar");
	}

	@Test
	void bodyUnacceptable() {
		MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
		servletRequest.setContentType(MediaType.TEXT_PLAIN_VALUE);
		servletRequest.setContent("foo".getBytes(UTF_8));

		DefaultServerRequest request = new DefaultServerRequest(servletRequest, Collections.emptyList());

		assertThatExceptionOfType(HttpMediaTypeNotSupportedException.class)
				.isThrownBy(() -> request.body(String.class));
	}

	@Test
	void session() {
		MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
		MockHttpSession session = new MockHttpSession();
		servletRequest.setSession(session);

		DefaultServerRequest request = new DefaultServerRequest(servletRequest, this.messageConverters);

		assertThat(request.session()).isEqualTo(session);
	}

	@Test
	void principal() {
		MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
		Principal principal = () -> "foo";
		servletRequest.setUserPrincipal(principal);

		DefaultServerRequest request = new DefaultServerRequest(servletRequest, this.messageConverters);

		assertThat(request.principal()).contains(principal);
	}

	@Test
	void bindToConstructor() throws BindException {
		MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
		servletRequest.addParameter("foo", "FOO");
		servletRequest.addParameter("bar", "BAR");

		DefaultServerRequest request = new DefaultServerRequest(servletRequest, this.messageConverters);

		ConstructorInjection result = request.bind(ConstructorInjection.class);
		assertThat(result.getFoo()).isEqualTo("FOO");
		assertThat(result.getBar()).isEqualTo("BAR");
	}

	@Test
	void bindToProperties() throws BindException {
		MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
		servletRequest.addParameter("foo", "FOO");
		servletRequest.addParameter("bar", "BAR");

		DefaultServerRequest request = new DefaultServerRequest(servletRequest, this.messageConverters);

		PropertyInjection result = request.bind(PropertyInjection.class);
		assertThat(result.getFoo()).isEqualTo("FOO");
		assertThat(result.getBar()).isEqualTo("BAR");
	}

	@Test
	void bindToMixed() throws BindException {
		MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
		servletRequest.addParameter("foo", "FOO");
		servletRequest.addParameter("bar", "BAR");

		DefaultServerRequest request = new DefaultServerRequest(servletRequest, this.messageConverters);

		MixedInjection result = request.bind(MixedInjection.class);
		assertThat(result.getFoo()).isEqualTo("FOO");
		assertThat(result.getBar()).isEqualTo("BAR");
	}

	@Test
	void bindCustomizer() throws BindException {
		MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
		servletRequest.addParameter("foo", "FOO");
		servletRequest.addParameter("bar", "BAR");

		DefaultServerRequest request = new DefaultServerRequest(servletRequest, this.messageConverters);

		PropertyInjection result = request.bind(PropertyInjection.class, dataBinder -> dataBinder.setAllowedFields("foo"));
		assertThat(result.getFoo()).isEqualTo("FOO");
		assertThat(result.getBar()).isNull();
	}

	@Test
	void bindError() {
		MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
		servletRequest.addParameter("foo", "FOO");

		DefaultServerRequest request = new DefaultServerRequest(servletRequest, this.messageConverters);

		assertThatExceptionOfType(BindException.class).isThrownBy(() ->
			request.bind(ErrorInjection.class)
		);
	}


	@ParameterizedHttpMethodTest
	void checkNotModifiedTimestamp(String method) {
		MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest(method, "/", true);
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
		MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest(method, "/", true);
		Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		Instant oneMinuteAgo = now.minus(1, ChronoUnit.MINUTES);
		servletRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, oneMinuteAgo.toEpochMilli());

		DefaultServerRequest request = new DefaultServerRequest(servletRequest, this.messageConverters);

		Optional<ServerResponse> result = request.checkNotModified(now, "");

		assertThat(result).isEmpty();
	}

	@ParameterizedHttpMethodTest
	void checkNotModifiedETag(String method) {
		MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest(method, "/", true);
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
		MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest(method, "/", true);
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
		MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest(method, "/", true);
		String currentETag = "\"Foo\"";
		String oldEtag = "Bar";
		servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, oldEtag);

		DefaultServerRequest request = new DefaultServerRequest(servletRequest, this.messageConverters);

		Optional<ServerResponse> result = request.checkNotModified(currentETag);

		assertThat(result).isEmpty();
	}

	@ParameterizedHttpMethodTest
	void checkNotModifiedUnpaddedETag(String method) {
		MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest(method, "/", true);
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
		MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest(method, "/", true);
		String currentETag = "Foo";
		String oldEtag = "Bar";
		servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, oldEtag);

		DefaultServerRequest request = new DefaultServerRequest(servletRequest, this.messageConverters);

		Optional<ServerResponse> result = request.checkNotModified(currentETag);

		assertThat(result).isEmpty();
	}

	@ParameterizedHttpMethodTest
	void checkNotModifiedWildcardIsIgnored(String method) {
		MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest(method, "/", true);
		String eTag = "\"Foo\"";
		servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, "*");
		DefaultServerRequest request = new DefaultServerRequest(servletRequest, this.messageConverters);

		Optional<ServerResponse> result = request.checkNotModified(eTag);

		assertThat(result).isEmpty();
	}

	@ParameterizedHttpMethodTest
	void checkNotModifiedETagAndTimestamp(String method) {
		MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest(method, "/", true);
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
		MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest(method, "/", true);
		String eTag = "\"Foo\"";
		Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		Instant oneMinuteAgo = now.minus(1, ChronoUnit.MINUTES);
		servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, eTag);
		servletRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, oneMinuteAgo.toEpochMilli());

		DefaultServerRequest request = new DefaultServerRequest(servletRequest, this.messageConverters);

		Optional<ServerResponse> result = request.checkNotModified(now, eTag);

		assertThat(result).hasValueSatisfying(serverResponse -> {
			assertThat(serverResponse.statusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
			assertThat(serverResponse.headers().getETag()).isEqualTo(eTag);
			assertThat(serverResponse.headers().getLastModified()).isEqualTo(now.toEpochMilli());
		});
	}

	@ParameterizedHttpMethodTest
	void checkModifiedETagAndNotModifiedTimestamp(String method) {
		MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest(method, "/", true);
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


	@SuppressWarnings("unused")
	private static final class ConstructorInjection {

		private final String foo;

		private final String bar;

		public ConstructorInjection(String foo, String bar) {
			this.foo = foo;
			this.bar = bar;
		}

		public String getFoo() {
			return this.foo;
		}

		public String getBar() {
			return this.bar;
		}
	}

	@SuppressWarnings("unused")
	private static final class PropertyInjection {

		private String foo;

		private String bar;

		public String getFoo() {
			return this.foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}

		public String getBar() {
			return this.bar;
		}

		public void setBar(String bar) {
			this.bar = bar;
		}
	}

	@SuppressWarnings("unused")
	private static final class MixedInjection {

		private final String foo;

		private String bar;

		public MixedInjection(String foo) {
			this.foo = foo;
		}

		public String getFoo() {
			return this.foo;
		}

		public String getBar() {
			return this.bar;
		}

		public void setBar(String bar) {
			this.bar = bar;
		}
	}

	@SuppressWarnings("unused")
	private static final class ErrorInjection {

		private int foo;

		public int getFoo() {
			return this.foo;
		}

		public void setFoo(int foo) {
			this.foo = foo;
		}
	}

}
