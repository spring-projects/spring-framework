/*
 * Copyright 2002-2019 the original author or authors.
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

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import javax.servlet.http.Cookie;

import org.junit.Test;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpSession;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.HttpMediaTypeNotSupportedException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;

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

		assertEquals(HttpMethod.HEAD, request.method());
	}

	@Test
	public void uri() {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/");
		servletRequest.setServerName("example.com");
		servletRequest.setScheme("https");
		servletRequest.setServerPort(443);

		DefaultServerRequest request =
				new DefaultServerRequest(servletRequest, this.messageConverters);

		assertEquals(URI.create("https://example.com/"), request.uri());
	}

	@Test
	public void uriBuilder() {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/path");
		servletRequest.setQueryString("a=1");

		DefaultServerRequest request =
				new DefaultServerRequest(servletRequest, this.messageConverters);

		URI result = request.uriBuilder().build();
		assertEquals("http", result.getScheme());
		assertEquals("localhost", result.getHost());
		assertEquals(-1, result.getPort());
		assertEquals("/path", result.getPath());
		assertEquals("a=1", result.getQuery());
	}

	@Test
	public void attribute() {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/");
		servletRequest.setAttribute("foo", "bar");

		DefaultServerRequest request =
				new DefaultServerRequest(servletRequest, this.messageConverters);

		assertEquals(Optional.of("bar"), request.attribute("foo"));
	}

	@Test
	public void params() {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/");
		servletRequest.setParameter("foo", "bar");

		DefaultServerRequest request =
				new DefaultServerRequest(servletRequest, this.messageConverters);

		assertEquals(Optional.of("bar"), request.param("foo"));
	}

	@Test
	public void emptyQueryParam() {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/");
		servletRequest.setParameter("foo", "");

		DefaultServerRequest request =
				new DefaultServerRequest(servletRequest, this.messageConverters);

		assertEquals(Optional.of(""), request.param("foo"));
	}

	@Test
	public void absentQueryParam() {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/");
		servletRequest.setParameter("foo", "");

		DefaultServerRequest request =
				new DefaultServerRequest(servletRequest, this.messageConverters);

		assertEquals(Optional.empty(), request.param("bar"));
	}

	@Test
	public void pathVariable() {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/");
		Map<String, String> pathVariables = Collections.singletonMap("foo", "bar");
		servletRequest
				.setAttribute(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE, pathVariables);

		DefaultServerRequest request = new DefaultServerRequest(servletRequest,
				this.messageConverters);

		assertEquals("bar", request.pathVariable("foo"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void pathVariableNotFound() {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/");
		Map<String, String> pathVariables = Collections.singletonMap("foo", "bar");
		servletRequest
				.setAttribute(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE, pathVariables);

		DefaultServerRequest request = new DefaultServerRequest(servletRequest,
				this.messageConverters);

		request.pathVariable("baz");
	}

	@Test
	public void pathVariables() {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/");
		Map<String, String> pathVariables = Collections.singletonMap("foo", "bar");
		servletRequest
				.setAttribute(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE, pathVariables);

		DefaultServerRequest request = new DefaultServerRequest(servletRequest,
				this.messageConverters);

		assertEquals(pathVariables, request.pathVariables());
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
		assertEquals(accept, headers.accept());
		assertEquals(acceptCharset, headers.acceptCharset());
		assertEquals(OptionalLong.of(contentLength), headers.contentLength());
		assertEquals(Optional.of(contentType), headers.contentType());
		assertEquals(httpHeaders, headers.asHttpHeaders());
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

		assertEquals(expected, request.cookies());

	}

	@Test
	public void bodyClass() throws Exception {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/");
		servletRequest.setContentType(MediaType.TEXT_PLAIN_VALUE);
		servletRequest.setContent("foo".getBytes(UTF_8));

		DefaultServerRequest request = new DefaultServerRequest(servletRequest,
				this.messageConverters);

		String result = request.body(String.class);
		assertEquals("foo", result);
	}

	@Test
	public void bodyParameterizedTypeReference() throws Exception {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/");
		servletRequest.setContentType(MediaType.APPLICATION_JSON_VALUE);
		servletRequest.setContent("[\"foo\",\"bar\"]".getBytes(UTF_8));

		DefaultServerRequest request = new DefaultServerRequest(servletRequest,
				Collections.singletonList(new MappingJackson2HttpMessageConverter()));

		List<String> result = request.body(new ParameterizedTypeReference<List<String>>() {});
		assertEquals(2, result.size());
		assertEquals("foo", result.get(0));
		assertEquals("bar", result.get(1));
	}

	@Test(expected = HttpMediaTypeNotSupportedException.class)
	public void bodyUnacceptable() throws Exception {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/");
		servletRequest.setContentType(MediaType.TEXT_PLAIN_VALUE);
		servletRequest.setContent("foo".getBytes(UTF_8));

		DefaultServerRequest request =
				new DefaultServerRequest(servletRequest, Collections.emptyList());

		request.body(String.class);
	}

	@Test
	public void session() {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/");
		MockHttpSession session = new MockHttpSession();
		servletRequest.setSession(session);

		DefaultServerRequest request = new DefaultServerRequest(servletRequest,
				this.messageConverters);

		assertEquals(session, request.session());

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

		assertEquals(principal, request.principal().get());

	}

}
