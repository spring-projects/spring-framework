/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.util;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link UriComponentsBuilder}.
 *
 * @author Rossen Stoyanchev
 */
class ForwardedHeaderUtilsTests {

	@Test
	void fromHttpRequest() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(-1);
		request.setRequestURI("/path");
		request.setQueryString("a=1");

		ServletServerHttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();
		assertThat(result.getScheme()).isEqualTo("http");
		assertThat(result.getHost()).isEqualTo("localhost");
		assertThat(result.getPort()).isEqualTo(-1);
		assertThat(result.getPath()).isEqualTo("/path");
		assertThat(result.getQuery()).isEqualTo("a=1");
	}

	@ParameterizedTest // gh-17368, gh-27097
	@ValueSource(strings = {"https", "wss"})
	void fromHttpRequestResetsPort443(String protocol) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("X-Forwarded-Proto", protocol);
		request.addHeader("X-Forwarded-Host", "84.198.58.199");
		request.addHeader("X-Forwarded-Port", 443);
		request.setScheme("http");
		request.setServerName("example.com");
		request.setServerPort(80);
		request.setRequestURI("/rest/mobile/users/1");

		HttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

		assertThat(result.getScheme()).isEqualTo(protocol);
		assertThat(result.getHost()).isEqualTo("84.198.58.199");
		assertThat(result.getPort()).isEqualTo(-1);
		assertThat(result.getPath()).isEqualTo("/rest/mobile/users/1");
	}

	@ParameterizedTest // gh-27097
	@ValueSource(strings = {"http", "ws"})
	void fromHttpRequestResetsPort80(String protocol) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("X-Forwarded-Proto", protocol);
		request.addHeader("X-Forwarded-Host", "84.198.58.199");
		request.addHeader("X-Forwarded-Port", 80);
		request.setScheme("http");
		request.setServerName("example.com");
		request.setServerPort(80);
		request.setRequestURI("/path");

		HttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

		assertThat(result.getScheme()).isEqualTo(protocol);
		assertThat(result.getHost()).isEqualTo("84.198.58.199");
		assertThat(result.getPort()).isEqualTo(-1);
		assertThat(result.getPath()).isEqualTo("/path");
	}

	@Test  // SPR-14761
	void fromHttpRequestWithForwardedIPv4Host() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("https");
		request.setServerName("localhost");
		request.setServerPort(-1);
		request.setRequestURI("/mvc-showcase");
		request.addHeader("Forwarded", "host=192.168.0.1");

		HttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

		assertThat(result.toString()).isEqualTo("https://192.168.0.1/mvc-showcase");
	}

	@Test  // SPR-14761
	void fromHttpRequestWithForwardedIPv6() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(-1);
		request.setRequestURI("/mvc-showcase");
		request.addHeader("Forwarded", "host=[1abc:2abc:3abc::5ABC:6abc]");

		HttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

		assertThat(result.toString()).isEqualTo("http://[1abc:2abc:3abc::5ABC:6abc]/mvc-showcase");
	}

	@Test  // SPR-14761
	void fromHttpRequestWithForwardedIPv6Host() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(-1);
		request.setRequestURI("/mvc-showcase");
		request.addHeader("X-Forwarded-Host", "[1abc:2abc:3abc::5ABC:6abc]");

		HttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

		assertThat(result.toString()).isEqualTo("http://[1abc:2abc:3abc::5ABC:6abc]/mvc-showcase");
	}

	@Test  // SPR-14761
	void fromHttpRequestWithForwardedIPv6HostAndPort() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(-1);
		request.setRequestURI("/mvc-showcase");
		request.addHeader("X-Forwarded-Host", "[1abc:2abc:3abc::5ABC:6abc]:8080");

		HttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

		assertThat(result.toString()).isEqualTo("http://[1abc:2abc:3abc::5ABC:6abc]:8080/mvc-showcase");
	}

	@Test  // gh-26748
	void fromHttpRequestWithForwardedInvalidIPv6Address() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(-1);
		request.setRequestURI("/mvc-showcase");
		request.addHeader("X-Forwarded-Host", "2a02:918:175:ab60:45ee:c12c:dac1:808b");

		HttpRequest httpRequest = new ServletServerHttpRequest(request);

		assertThatIllegalArgumentException().isThrownBy(() ->
				ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build());
	}

	@Test
	void fromHttpRequestWithForwardedHost() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("https");
		request.setServerName("localhost");
		request.setServerPort(-1);
		request.setRequestURI("/mvc-showcase");
		request.addHeader("X-Forwarded-Host", "anotherHost");

		HttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

		assertThat(result.toString()).isEqualTo("https://anotherHost/mvc-showcase");
	}

	@Test  // SPR-10701
	void fromHttpRequestWithForwardedHostIncludingPort() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(-1);
		request.setRequestURI("/mvc-showcase");
		request.addHeader("X-Forwarded-Host", "webtest.foo.bar.com:443");

		HttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

		assertThat(result.getHost()).isEqualTo("webtest.foo.bar.com");
		assertThat(result.getPort()).isEqualTo(443);
	}

	@Test  // SPR-11140
	void fromHttpRequestWithForwardedHostMultiValuedHeader() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(-1);
		request.addHeader("X-Forwarded-Host", "a.example.org, b.example.org, c.example.org");

		HttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

		assertThat(result.getHost()).isEqualTo("a.example.org");
		assertThat(result.getPort()).isEqualTo(-1);
	}

	@Test  // SPR-11855
	void fromHttpRequestWithForwardedHostAndPort() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8080);
		request.addHeader("X-Forwarded-Host", "foobarhost");
		request.addHeader("X-Forwarded-Port", "9090");

		HttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

		assertThat(result.getHost()).isEqualTo("foobarhost");
		assertThat(result.getPort()).isEqualTo(9090);
	}

	@Test  // SPR-11872
	void fromHttpRequestWithForwardedHostWithDefaultPort() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(10080);
		request.addHeader("X-Forwarded-Host", "example.org");

		HttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

		assertThat(result.getHost()).isEqualTo("example.org");
		assertThat(result.getPort()).isEqualTo(-1);
	}

	@Test  // SPR-16262
	void fromHttpRequestWithForwardedProtoWithDefaultPort() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("example.org");
		request.setServerPort(10080);
		request.addHeader("X-Forwarded-Proto", "https");

		HttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

		assertThat(result.getScheme()).isEqualTo("https");
		assertThat(result.getHost()).isEqualTo("example.org");
		assertThat(result.getPort()).isEqualTo(-1);
	}

	@Test  // SPR-16863
	void fromHttpRequestWithForwardedSsl() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("example.org");
		request.setServerPort(10080);
		request.addHeader("X-Forwarded-Ssl", "on");

		HttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

		assertThat(result.getScheme()).isEqualTo("https");
		assertThat(result.getHost()).isEqualTo("example.org");
		assertThat(result.getPort()).isEqualTo(-1);
	}

	@Test
	void fromHttpRequestWithForwardedHostWithForwardedScheme() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(10080);
		request.addHeader("X-Forwarded-Host", "example.org");
		request.addHeader("X-Forwarded-Proto", "https");

		HttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

		assertThat(result.getHost()).isEqualTo("example.org");
		assertThat(result.getScheme()).isEqualTo("https");
		assertThat(result.getPort()).isEqualTo(-1);
	}

	@Test  // SPR-12771
	void fromHttpRequestWithForwardedProtoAndDefaultPort() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(80);
		request.setRequestURI("/mvc-showcase");
		request.addHeader("X-Forwarded-Proto", "https");
		request.addHeader("X-Forwarded-Host", "84.198.58.199");
		request.addHeader("X-Forwarded-Port", "443");

		HttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

		assertThat(result.toString()).isEqualTo("https://84.198.58.199/mvc-showcase");
	}

	@Test  // SPR-12813
	void fromHttpRequestWithForwardedPortMultiValueHeader() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(9090);
		request.setRequestURI("/mvc-showcase");
		request.addHeader("X-Forwarded-Host", "a.example.org");
		request.addHeader("X-Forwarded-Port", "80,52022");

		HttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

		assertThat(result.toString()).isEqualTo("http://a.example.org/mvc-showcase");
	}

	@Test  // SPR-12816
	void fromHttpRequestWithForwardedProtoMultiValueHeader() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8080);
		request.setRequestURI("/mvc-showcase");
		request.addHeader("X-Forwarded-Host", "a.example.org");
		request.addHeader("X-Forwarded-Port", "443");
		request.addHeader("X-Forwarded-Proto", "https,https");

		HttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

		assertThat(result.toString()).isEqualTo("https://a.example.org/mvc-showcase");
	}

	@Test  // gh-19890
	void fromHttpRequestWithEmptyScheme() {
		HttpRequest request = new HttpRequest() {
			@Override
			public HttpMethod getMethod() {
				return HttpMethod.GET;
			}

			@Override
			public URI getURI() {
				return UriComponentsBuilder.fromUriString("/").build().toUri();
			}

			@Override
			public HttpHeaders getHeaders() {
				return new HttpHeaders();
			}
		};
		UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(request.getURI(), request.getHeaders()).build();

		assertThat(result.toString()).isEqualTo("/");
	}

	@Test  // SPR-11856
	void fromHttpRequestForwardedHeader()  {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Forwarded", "proto=https; host=84.198.58.199");
		request.setScheme("http");
		request.setServerName("example.com");
		request.setRequestURI("/rest/mobile/users/1");

		HttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

		assertThat(result.getScheme()).isEqualTo("https");
		assertThat(result.getHost()).isEqualTo("84.198.58.199");
		assertThat(result.getPath()).isEqualTo("/rest/mobile/users/1");
	}

	@Test
	void fromHttpRequestForwardedHeaderQuoted()  {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Forwarded", "proto=\"https\"; host=\"84.198.58.199\"");
		request.setScheme("http");
		request.setServerName("example.com");
		request.setRequestURI("/rest/mobile/users/1");

		HttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

		assertThat(result.getScheme()).isEqualTo("https");
		assertThat(result.getHost()).isEqualTo("84.198.58.199");
		assertThat(result.getPath()).isEqualTo("/rest/mobile/users/1");
	}

	@Test
	void fromHttpRequestMultipleForwardedHeader()  {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Forwarded", "host=84.198.58.199;proto=https");
		request.addHeader("Forwarded", "proto=ftp; host=1.2.3.4");
		request.setScheme("http");
		request.setServerName("example.com");
		request.setRequestURI("/rest/mobile/users/1");

		HttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

		assertThat(result.getScheme()).isEqualTo("https");
		assertThat(result.getHost()).isEqualTo("84.198.58.199");
		assertThat(result.getPath()).isEqualTo("/rest/mobile/users/1");
	}

	@Test
	void fromHttpRequestMultipleForwardedHeaderComma()  {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Forwarded", "host=84.198.58.199 ;proto=https, proto=ftp; host=1.2.3.4");
		request.setScheme("http");
		request.setServerName("example.com");
		request.setRequestURI("/rest/mobile/users/1");

		HttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

		assertThat(result.getScheme()).isEqualTo("https");
		assertThat(result.getHost()).isEqualTo("84.198.58.199");
		assertThat(result.getPath()).isEqualTo("/rest/mobile/users/1");
	}

	@Test
	void fromHttpRequestForwardedHeaderWithHostPortAndWithoutServerPort()  {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Forwarded", "proto=https; host=84.198.58.199:9090");
		request.setScheme("http");
		request.setServerName("example.com");
		request.setRequestURI("/rest/mobile/users/1");

		HttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

		assertThat(result.getScheme()).isEqualTo("https");
		assertThat(result.getHost()).isEqualTo("84.198.58.199");
		assertThat(result.getPath()).isEqualTo("/rest/mobile/users/1");
		assertThat(result.getPort()).isEqualTo(9090);
		assertThat(result.toUriString()).isEqualTo("https://84.198.58.199:9090/rest/mobile/users/1");
	}

	@Test
	void fromHttpRequestForwardedHeaderWithHostPortAndServerPort()  {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Forwarded", "proto=https; host=84.198.58.199:9090");
		request.setScheme("http");
		request.setServerPort(8080);
		request.setServerName("example.com");
		request.setRequestURI("/rest/mobile/users/1");

		HttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

		assertThat(result.getScheme()).isEqualTo("https");
		assertThat(result.getHost()).isEqualTo("84.198.58.199");
		assertThat(result.getPath()).isEqualTo("/rest/mobile/users/1");
		assertThat(result.getPort()).isEqualTo(9090);
		assertThat(result.toUriString()).isEqualTo("https://84.198.58.199:9090/rest/mobile/users/1");
	}

	@Test
	void fromHttpRequestForwardedHeaderWithoutHostPortAndWithServerPort()  {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Forwarded", "proto=https; host=84.198.58.199");
		request.setScheme("http");
		request.setServerPort(8080);
		request.setServerName("example.com");
		request.setRequestURI("/rest/mobile/users/1");

		HttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

		assertThat(result.getScheme()).isEqualTo("https");
		assertThat(result.getHost()).isEqualTo("84.198.58.199");
		assertThat(result.getPath()).isEqualTo("/rest/mobile/users/1");
		assertThat(result.getPort()).isEqualTo(-1);
		assertThat(result.toUriString()).isEqualTo("https://84.198.58.199/rest/mobile/users/1");
	}

	@Test  // SPR-16262
	void fromHttpRequestForwardedHeaderWithProtoAndServerPort()  {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Forwarded", "proto=https");
		request.setScheme("http");
		request.setServerPort(8080);
		request.setServerName("example.com");
		request.setRequestURI("/rest/mobile/users/1");

		HttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

		assertThat(result.getScheme()).isEqualTo("https");
		assertThat(result.getHost()).isEqualTo("example.com");
		assertThat(result.getPath()).isEqualTo("/rest/mobile/users/1");
		assertThat(result.getPort()).isEqualTo(-1);
		assertThat(result.toUriString()).isEqualTo("https://example.com/rest/mobile/users/1");
	}

	@Test  // gh-25737
	void fromHttpRequestForwardedHeaderComma() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Forwarded", "for=192.0.2.0,for=192.0.2.1;proto=https;host=192.0.2.3:9090");
		request.setScheme("http");
		request.setServerPort(8080);
		request.setServerName("example.com");
		request.setRequestURI("/rest/mobile/users/1");

		HttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents result = ForwardedHeaderUtils.adaptFromForwardedHeaders(httpRequest.getURI(), httpRequest.getHeaders()).build();

		assertThat(result.getScheme()).isEqualTo("https");
		assertThat(result.getHost()).isEqualTo("192.0.2.3");
		assertThat(result.getPath()).isEqualTo("/rest/mobile/users/1");
		assertThat(result.getPort()).isEqualTo(9090);
		assertThat(result.toUriString()).isEqualTo("https://192.0.2.3:9090/rest/mobile/users/1");
	}

}
