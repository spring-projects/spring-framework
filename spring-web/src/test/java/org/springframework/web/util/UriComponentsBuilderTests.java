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

package org.springframework.web.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link UriComponentsBuilder}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @author Oliver Gierke
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author David Eckel
 */
class UriComponentsBuilderTests {

	@Test
	void plain() throws URISyntaxException {
		UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
		UriComponents result = builder.scheme("https").host("example.com")
				.path("foo").queryParam("bar").fragment("baz")
				.build();
		assertThat(result.getScheme()).isEqualTo("https");
		assertThat(result.getHost()).isEqualTo("example.com");
		assertThat(result.getPath()).isEqualTo("foo");
		assertThat(result.getQuery()).isEqualTo("bar");
		assertThat(result.getFragment()).isEqualTo("baz");

		URI expected = new URI("https://example.com/foo?bar#baz");
		assertThat(result.toUri()).as("Invalid result URI").isEqualTo(expected);
	}

	@Test
	void multipleFromSameBuilder() throws URISyntaxException {
		UriComponentsBuilder builder = UriComponentsBuilder.newInstance()
				.scheme("https").host("example.com").pathSegment("foo");
		UriComponents result1 = builder.build();
		builder = builder.pathSegment("foo2").queryParam("bar").fragment("baz");
		UriComponents result2 = builder.build();

		assertThat(result1.getScheme()).isEqualTo("https");
		assertThat(result1.getHost()).isEqualTo("example.com");
		assertThat(result1.getPath()).isEqualTo("/foo");
		URI expected = new URI("https://example.com/foo");
		assertThat(result1.toUri()).as("Invalid result URI").isEqualTo(expected);

		assertThat(result2.getScheme()).isEqualTo("https");
		assertThat(result2.getHost()).isEqualTo("example.com");
		assertThat(result2.getPath()).isEqualTo("/foo/foo2");
		assertThat(result2.getQuery()).isEqualTo("bar");
		assertThat(result2.getFragment()).isEqualTo("baz");
		expected = new URI("https://example.com/foo/foo2?bar#baz");
		assertThat(result2.toUri()).as("Invalid result URI").isEqualTo(expected);
	}

	@Test
	void fromPath() throws URISyntaxException {
		UriComponents result = UriComponentsBuilder.fromPath("foo").queryParam("bar").fragment("baz").build();
		assertThat(result.getPath()).isEqualTo("foo");
		assertThat(result.getQuery()).isEqualTo("bar");
		assertThat(result.getFragment()).isEqualTo("baz");

		assertThat(result.toUriString()).as("Invalid result URI String").isEqualTo("foo?bar#baz");

		URI expected = new URI("foo?bar#baz");
		assertThat(result.toUri()).as("Invalid result URI").isEqualTo(expected);

		result = UriComponentsBuilder.fromPath("/foo").build();
		assertThat(result.getPath()).isEqualTo("/foo");

		expected = new URI("/foo");
		assertThat(result.toUri()).as("Invalid result URI").isEqualTo(expected);
	}

	@Test
	void fromHierarchicalUri() throws URISyntaxException {
		URI uri = new URI("https://example.com/foo?bar#baz");
		UriComponents result = UriComponentsBuilder.fromUri(uri).build();
		assertThat(result.getScheme()).isEqualTo("https");
		assertThat(result.getHost()).isEqualTo("example.com");
		assertThat(result.getPath()).isEqualTo("/foo");
		assertThat(result.getQuery()).isEqualTo("bar");
		assertThat(result.getFragment()).isEqualTo("baz");

		assertThat(result.toUri()).as("Invalid result URI").isEqualTo(uri);
	}

	@Test
	void fromOpaqueUri() throws URISyntaxException {
		URI uri = new URI("mailto:foo@bar.com#baz");
		UriComponents result = UriComponentsBuilder.fromUri(uri).build();
		assertThat(result.getScheme()).isEqualTo("mailto");
		assertThat(result.getSchemeSpecificPart()).isEqualTo("foo@bar.com");
		assertThat(result.getFragment()).isEqualTo("baz");

		assertThat(result.toUri()).as("Invalid result URI").isEqualTo(uri);
	}

	@Test  // SPR-9317
	void fromUriEncodedQuery() throws URISyntaxException {
		URI uri = new URI("https://www.example.org/?param=aGVsbG9Xb3JsZA%3D%3D");
		String fromUri = UriComponentsBuilder.fromUri(uri).build().getQueryParams().get("param").get(0);
		String fromUriString = UriComponentsBuilder.fromUriString(uri.toString())
				.build().getQueryParams().get("param").get(0);

		assertThat(fromUriString).isEqualTo(fromUri);
	}

	@Test
	void fromUriString() {
		UriComponents result = UriComponentsBuilder.fromUriString("https://www.ietf.org/rfc/rfc3986.txt").build();
		assertThat(result.getScheme()).isEqualTo("https");
		assertThat(result.getUserInfo()).isNull();
		assertThat(result.getHost()).isEqualTo("www.ietf.org");
		assertThat(result.getPort()).isEqualTo(-1);
		assertThat(result.getPath()).isEqualTo("/rfc/rfc3986.txt");
		assertThat(result.getPathSegments()).isEqualTo(Arrays.asList("rfc", "rfc3986.txt"));
		assertThat(result.getQuery()).isNull();
		assertThat(result.getFragment()).isNull();

		String url = "https://arjen:foobar@java.sun.com:80" +
				"/javase/6/docs/api/java/util/BitSet.html?foo=bar#and(java.util.BitSet)";
		result = UriComponentsBuilder.fromUriString(url).build();
		assertThat(result.getScheme()).isEqualTo("https");
		assertThat(result.getUserInfo()).isEqualTo("arjen:foobar");
		assertThat(result.getHost()).isEqualTo("java.sun.com");
		assertThat(result.getPort()).isEqualTo(80);
		assertThat(result.getPath()).isEqualTo("/javase/6/docs/api/java/util/BitSet.html");
		assertThat(result.getQuery()).isEqualTo("foo=bar");
		MultiValueMap<String, String> expectedQueryParams = new LinkedMultiValueMap<>(1);
		expectedQueryParams.add("foo", "bar");
		assertThat(result.getQueryParams()).isEqualTo(expectedQueryParams);
		assertThat(result.getFragment()).isEqualTo("and(java.util.BitSet)");

		result = UriComponentsBuilder.fromUriString("mailto:java-net@java.sun.com#baz").build();
		assertThat(result.getScheme()).isEqualTo("mailto");
		assertThat(result.getUserInfo()).isNull();
		assertThat(result.getHost()).isNull();
		assertThat(result.getPort()).isEqualTo(-1);
		assertThat(result.getSchemeSpecificPart()).isEqualTo("java-net@java.sun.com");
		assertThat(result.getPath()).isNull();
		assertThat(result.getQuery()).isNull();
		assertThat(result.getFragment()).isEqualTo("baz");

		result = UriComponentsBuilder.fromUriString("docs/guide/collections/designfaq.html#28").build();
		assertThat(result.getScheme()).isNull();
		assertThat(result.getUserInfo()).isNull();
		assertThat(result.getHost()).isNull();
		assertThat(result.getPort()).isEqualTo(-1);
		assertThat(result.getPath()).isEqualTo("docs/guide/collections/designfaq.html");
		assertThat(result.getQuery()).isNull();
		assertThat(result.getFragment()).isEqualTo("28");
	}

	@Test  // SPR-9832
	void fromUriStringQueryParamWithReservedCharInValue() {
		String uri = "https://www.google.com/ig/calculator?q=1USD=?EUR";
		UriComponents result = UriComponentsBuilder.fromUriString(uri).build();

		assertThat(result.getQuery()).isEqualTo("q=1USD=?EUR");
		assertThat(result.getQueryParams().getFirst("q")).isEqualTo("1USD=?EUR");
	}

	@Test  // SPR-14828
	void fromUriStringQueryParamEncodedAndContainingPlus() {
		String httpUrl = "http://localhost:8080/test/print?value=%EA%B0%80+%EB%82%98";
		URI uri = UriComponentsBuilder.fromUriString(httpUrl).build(true).toUri();

		assertThat(uri.toString()).isEqualTo(httpUrl);
	}

	@Test  // SPR-10539
	void fromUriStringIPv6Host() {
		UriComponents result = UriComponentsBuilder
				.fromUriString("http://[1abc:2abc:3abc::5ABC:6abc]:8080/resource").build().encode();
		assertThat(result.getHost()).isEqualTo("[1abc:2abc:3abc::5ABC:6abc]");

		UriComponents resultWithScopeId = UriComponentsBuilder
				.fromUriString("http://[1abc:2abc:3abc::5ABC:6abc%eth0]:8080/resource").build().encode();
		assertThat(resultWithScopeId.getHost()).isEqualTo("[1abc:2abc:3abc::5ABC:6abc%25eth0]");

		UriComponents resultIPv4compatible = UriComponentsBuilder
				.fromUriString("http://[::192.168.1.1]:8080/resource").build().encode();
		assertThat(resultIPv4compatible.getHost()).isEqualTo("[::192.168.1.1]");
	}

	@Test  // SPR-11970
	void fromUriStringNoPathWithReservedCharInQuery() {
		UriComponents result = UriComponentsBuilder.fromUriString("https://example.com?foo=bar@baz").build();
		assertThat(result.getUserInfo()).isNull();
		assertThat(result.getHost()).isEqualTo("example.com");
		assertThat(result.getQueryParams()).containsKey("foo");
		assertThat(result.getQueryParams().getFirst("foo")).isEqualTo("bar@baz");
	}

	@Test  // SPR-14828
	void fromHttpUrlQueryParamEncodedAndContainingPlus() {
		String httpUrl = "http://localhost:8080/test/print?value=%EA%B0%80+%EB%82%98";
		URI uri = UriComponentsBuilder.fromHttpUrl(httpUrl).build(true).toUri();

		assertThat(uri.toString()).isEqualTo(httpUrl);
	}

	@Test  // SPR-10779
	void fromHttpUrlCaseInsensitiveScheme() {
		assertThat(UriComponentsBuilder.fromHttpUrl("HTTP://www.google.com").build().getScheme()).isEqualTo("http");
		assertThat(UriComponentsBuilder.fromHttpUrl("HTTPS://www.google.com").build().getScheme()).isEqualTo("https");
	}

	@Test  // SPR-10539
	void fromHttpUrlInvalidIPv6Host() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				UriComponentsBuilder.fromHttpUrl("http://[1abc:2abc:3abc::5ABC:6abc:8080/resource"));
	}

	@Test
	void fromHttpUrlWithoutFragment() {
		String httpUrl = "http://localhost:8080/test/print";
		UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(httpUrl).build();
		assertThat(uriComponents.getScheme()).isEqualTo("http");
		assertThat(uriComponents.getUserInfo()).isNull();
		assertThat(uriComponents.getHost()).isEqualTo("localhost");
		assertThat(uriComponents.getPort()).isEqualTo(8080);
		assertThat(uriComponents.getPath()).isEqualTo("/test/print");
		assertThat(uriComponents.getPathSegments()).isEqualTo(Arrays.asList("test", "print"));
		assertThat(uriComponents.getQuery()).isNull();
		assertThat(uriComponents.getFragment()).isNull();
		assertThat(uriComponents.toUri().toString()).isEqualTo(httpUrl);

		httpUrl = "http://user:test@localhost:8080/test/print?foo=bar";
		uriComponents = UriComponentsBuilder.fromHttpUrl(httpUrl).build();
		assertThat(uriComponents.getScheme()).isEqualTo("http");
		assertThat(uriComponents.getUserInfo()).isEqualTo("user:test");
		assertThat(uriComponents.getHost()).isEqualTo("localhost");
		assertThat(uriComponents.getPort()).isEqualTo(8080);
		assertThat(uriComponents.getPath()).isEqualTo("/test/print");
		assertThat(uriComponents.getPathSegments()).isEqualTo(Arrays.asList("test", "print"));
		assertThat(uriComponents.getQuery()).isEqualTo("foo=bar");
		assertThat(uriComponents.getFragment()).isNull();
		assertThat(uriComponents.toUri().toString()).isEqualTo(httpUrl);

		httpUrl = "http://localhost:8080/test/print?foo=bar";
		uriComponents = UriComponentsBuilder.fromHttpUrl(httpUrl).build();
		assertThat(uriComponents.getScheme()).isEqualTo("http");
		assertThat(uriComponents.getUserInfo()).isNull();
		assertThat(uriComponents.getHost()).isEqualTo("localhost");
		assertThat(uriComponents.getPort()).isEqualTo(8080);
		assertThat(uriComponents.getPath()).isEqualTo("/test/print");
		assertThat(uriComponents.getPathSegments()).isEqualTo(Arrays.asList("test", "print"));
		assertThat(uriComponents.getQuery()).isEqualTo("foo=bar");
		assertThat(uriComponents.getFragment()).isNull();
		assertThat(uriComponents.toUri().toString()).isEqualTo(httpUrl);
	}

	@Test  // gh-25300
	void fromHttpUrlWithFragment() {
		String httpUrl = "https://example.com#baz";
		UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(httpUrl).build();
		assertThat(uriComponents.getScheme()).isEqualTo("https");
		assertThat(uriComponents.getUserInfo()).isNull();
		assertThat(uriComponents.getHost()).isEqualTo("example.com");
		assertThat(uriComponents.getPort()).isEqualTo(-1);
		assertThat(uriComponents.getPath()).isNullOrEmpty();
		assertThat(uriComponents.getPathSegments()).isEmpty();
		assertThat(uriComponents.getQuery()).isNull();
		assertThat(uriComponents.getFragment()).isEqualTo("baz");
		assertThat(uriComponents.toUri().toString()).isEqualTo(httpUrl);

		httpUrl = "http://localhost:8080/test/print#baz";
		uriComponents = UriComponentsBuilder.fromHttpUrl(httpUrl).build();
		assertThat(uriComponents.getScheme()).isEqualTo("http");
		assertThat(uriComponents.getUserInfo()).isNull();
		assertThat(uriComponents.getHost()).isEqualTo("localhost");
		assertThat(uriComponents.getPort()).isEqualTo(8080);
		assertThat(uriComponents.getPath()).isEqualTo("/test/print");
		assertThat(uriComponents.getPathSegments()).isEqualTo(Arrays.asList("test", "print"));
		assertThat(uriComponents.getQuery()).isNull();
		assertThat(uriComponents.getFragment()).isEqualTo("baz");
		assertThat(uriComponents.toUri().toString()).isEqualTo(httpUrl);

		httpUrl = "http://localhost:8080/test/print?foo=bar#baz";
		uriComponents = UriComponentsBuilder.fromHttpUrl(httpUrl).build();
		assertThat(uriComponents.getScheme()).isEqualTo("http");
		assertThat(uriComponents.getUserInfo()).isNull();
		assertThat(uriComponents.getHost()).isEqualTo("localhost");
		assertThat(uriComponents.getPort()).isEqualTo(8080);
		assertThat(uriComponents.getPath()).isEqualTo("/test/print");
		assertThat(uriComponents.getPathSegments()).isEqualTo(Arrays.asList("test", "print"));
		assertThat(uriComponents.getQuery()).isEqualTo("foo=bar");
		assertThat(uriComponents.getFragment()).isEqualTo("baz");
		assertThat(uriComponents.toUri().toString()).isEqualTo(httpUrl);
	}

	@Test
	void fromHttpRequest() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(-1);
		request.setRequestURI("/path");
		request.setQueryString("a=1");

		UriComponents result = UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request)).build();
		assertThat(result.getScheme()).isEqualTo("http");
		assertThat(result.getHost()).isEqualTo("localhost");
		assertThat(result.getPort()).isEqualTo(-1);
		assertThat(result.getPath()).isEqualTo("/path");
		assertThat(result.getQuery()).isEqualTo("a=1");
	}

	@Test  // SPR-12771
	void fromHttpRequestResetsPortBeforeSettingIt() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("X-Forwarded-Proto", "https");
		request.addHeader("X-Forwarded-Host", "84.198.58.199");
		request.addHeader("X-Forwarded-Port", 443);
		request.setScheme("http");
		request.setServerName("example.com");
		request.setServerPort(80);
		request.setRequestURI("/rest/mobile/users/1");

		HttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents result = UriComponentsBuilder.fromHttpRequest(httpRequest).build();

		assertThat(result.getScheme()).isEqualTo("https");
		assertThat(result.getHost()).isEqualTo("84.198.58.199");
		assertThat(result.getPort()).isEqualTo(-1);
		assertThat(result.getPath()).isEqualTo("/rest/mobile/users/1");
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
		UriComponents result = UriComponentsBuilder.fromHttpRequest(httpRequest).build();

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
		UriComponents result = UriComponentsBuilder.fromHttpRequest(httpRequest).build();

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
		UriComponents result = UriComponentsBuilder.fromHttpRequest(httpRequest).build();

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
		UriComponents result = UriComponentsBuilder.fromHttpRequest(httpRequest).build();

		assertThat(result.toString()).isEqualTo("http://[1abc:2abc:3abc::5ABC:6abc]:8080/mvc-showcase");
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
		UriComponents result = UriComponentsBuilder.fromHttpRequest(httpRequest).build();

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
		UriComponents result = UriComponentsBuilder.fromHttpRequest(httpRequest).build();

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
		UriComponents result = UriComponentsBuilder.fromHttpRequest(httpRequest).build();

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
		UriComponents result = UriComponentsBuilder.fromHttpRequest(httpRequest).build();

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
		UriComponents result = UriComponentsBuilder.fromHttpRequest(httpRequest).build();

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
		UriComponents result = UriComponentsBuilder.fromHttpRequest(httpRequest).build();

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
		UriComponents result = UriComponentsBuilder.fromHttpRequest(httpRequest).build();

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
		UriComponents result = UriComponentsBuilder.fromHttpRequest(httpRequest).build();

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
		UriComponents result = UriComponentsBuilder.fromHttpRequest(httpRequest).build();

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
		UriComponents result = UriComponentsBuilder.fromHttpRequest(httpRequest).build();

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
		UriComponents result = UriComponentsBuilder.fromHttpRequest(httpRequest).build();

		assertThat(result.toString()).isEqualTo("https://a.example.org/mvc-showcase");
	}

	@Test  // SPR-12742
	void fromHttpRequestWithTrailingSlash() {
		UriComponents before = UriComponentsBuilder.fromPath("/foo/").build();
		UriComponents after = UriComponentsBuilder.newInstance().uriComponents(before).build();
		assertThat(after.getPath()).isEqualTo("/foo/");
	}

	@Test // gh-19890
	void fromHttpRequestWithEmptyScheme() {
		HttpRequest request = new HttpRequest() {
			@Override
			public String getMethodValue() {
				return "GET";
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
		UriComponents result = UriComponentsBuilder.fromHttpRequest(request).build();

		assertThat(result.toString()).isEqualTo("/");
	}

	@Test
	void path() {
		UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/foo/bar");
		UriComponents result = builder.build();

		assertThat(result.getPath()).isEqualTo("/foo/bar");
		assertThat(result.getPathSegments()).isEqualTo(Arrays.asList("foo", "bar"));
	}

	@Test
	void pathSegments() {
		UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
		UriComponents result = builder.pathSegment("foo").pathSegment("bar").build();

		assertThat(result.getPath()).isEqualTo("/foo/bar");
		assertThat(result.getPathSegments()).isEqualTo(Arrays.asList("foo", "bar"));
	}

	@Test
	void pathThenPath() {
		UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/foo/bar").path("ba/z");
		UriComponents result = builder.build().encode();

		assertThat(result.getPath()).isEqualTo("/foo/barba/z");
		assertThat(result.getPathSegments()).isEqualTo(Arrays.asList("foo", "barba", "z"));
	}

	@Test
	void pathThenPathSegments() {
		UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/foo/bar").pathSegment("ba/z");
		UriComponents result = builder.build().encode();

		assertThat(result.getPath()).isEqualTo("/foo/bar/ba%2Fz");
		assertThat(result.getPathSegments()).isEqualTo(Arrays.asList("foo", "bar", "ba%2Fz"));
	}

	@Test
	void pathSegmentsThenPathSegments() {
		UriComponentsBuilder builder = UriComponentsBuilder.newInstance().pathSegment("foo").pathSegment("bar");
		UriComponents result = builder.build();

		assertThat(result.getPath()).isEqualTo("/foo/bar");
		assertThat(result.getPathSegments()).isEqualTo(Arrays.asList("foo", "bar"));
	}

	@Test
	void pathSegmentsThenPath() {
		UriComponentsBuilder builder = UriComponentsBuilder.newInstance().pathSegment("foo").path("/");
		UriComponents result = builder.build();

		assertThat(result.getPath()).isEqualTo("/foo/");
		assertThat(result.getPathSegments()).isEqualTo(Collections.singletonList("foo"));
	}

	@Test
	void pathSegmentsSomeEmpty() {
		UriComponentsBuilder builder = UriComponentsBuilder.newInstance().pathSegment("", "foo", "", "bar");
		UriComponents result = builder.build();

		assertThat(result.getPath()).isEqualTo("/foo/bar");
		assertThat(result.getPathSegments()).isEqualTo(Arrays.asList("foo", "bar"));
	}

	@Test  // SPR-12398
	void pathWithDuplicateSlashes() {
		UriComponents uriComponents = UriComponentsBuilder.fromPath("/foo/////////bar").build();
		assertThat(uriComponents.getPath()).isEqualTo("/foo/bar");
	}

	@Test
	void replacePath() {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString("https://www.ietf.org/rfc/rfc2396.txt");
		builder.replacePath("/rfc/rfc3986.txt");
		UriComponents result = builder.build();

		assertThat(result.toUriString()).isEqualTo("https://www.ietf.org/rfc/rfc3986.txt");

		builder = UriComponentsBuilder.fromUriString("https://www.ietf.org/rfc/rfc2396.txt");
		builder.replacePath(null);
		result = builder.build();

		assertThat(result.toUriString()).isEqualTo("https://www.ietf.org");
	}

	@Test
	void replaceQuery() {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString("https://example.com/foo?foo=bar&baz=qux");
		builder.replaceQuery("baz=42");
		UriComponents result = builder.build();

		assertThat(result.toUriString()).isEqualTo("https://example.com/foo?baz=42");

		builder = UriComponentsBuilder.fromUriString("https://example.com/foo?foo=bar&baz=qux");
		builder.replaceQuery(null);
		result = builder.build();

		assertThat(result.toUriString()).isEqualTo("https://example.com/foo");
	}

	@Test
	void queryParam() {
		UriComponents result = UriComponentsBuilder.newInstance().queryParam("baz", "qux", 42).build();

		assertThat(result.getQuery()).isEqualTo("baz=qux&baz=42");
		MultiValueMap<String, String> expectedQueryParams = new LinkedMultiValueMap<>(2);
		expectedQueryParams.add("baz", "qux");
		expectedQueryParams.add("baz", "42");
		assertThat(result.getQueryParams()).isEqualTo(expectedQueryParams);
	}

	@Test
	void queryParamWithList() {
		UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
		UriComponents result = builder.queryParam("baz", Arrays.asList("qux", 42)).build();

		assertThat(result.getQuery()).isEqualTo("baz=qux&baz=42");
		MultiValueMap<String, String> expectedQueryParams = new LinkedMultiValueMap<>(2);
		expectedQueryParams.add("baz", "qux");
		expectedQueryParams.add("baz", "42");
		assertThat(result.getQueryParams()).isEqualTo(expectedQueryParams);
	}

	@Test
	void emptyQueryParam() {
		UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
		UriComponents result = builder.queryParam("baz").build();

		assertThat(result.getQuery()).isEqualTo("baz");
		MultiValueMap<String, String> expectedQueryParams = new LinkedMultiValueMap<>(2);
		expectedQueryParams.add("baz", null);
		assertThat(result.getQueryParams()).isEqualTo(expectedQueryParams);
	}

	@Test
	void emptyQueryParams() {
		UriComponents result = UriComponentsBuilder.newInstance()
				.queryParam("baz", Collections.emptyList())
				.queryParam("foo", (Collection<?>) null)
				.build();

		assertThat(result.getQuery()).isEqualTo("baz&foo");
		MultiValueMap<String, String> expectedQueryParams = new LinkedMultiValueMap<>(2);
		expectedQueryParams.add("baz", null);
		expectedQueryParams.add("foo", null);
		assertThat(result.getQueryParams()).isEqualTo(expectedQueryParams);
	}

	@Test
	void replaceQueryParam() {
		UriComponentsBuilder builder = UriComponentsBuilder.newInstance().queryParam("baz", "qux", 42);
		builder.replaceQueryParam("baz", "xuq", 24);
		UriComponents result = builder.build();

		assertThat(result.getQuery()).isEqualTo("baz=xuq&baz=24");

		builder = UriComponentsBuilder.newInstance().queryParam("baz", "qux", 42);
		builder.replaceQueryParam("baz");
		result = builder.build();

		assertThat(result.getQuery()).as("Query param should have been deleted").isNull();
	}

	@Test
	void replaceQueryParams() {
		UriComponentsBuilder builder = UriComponentsBuilder.newInstance().queryParam("baz", Arrays.asList("qux", 42));
		builder.replaceQueryParam("baz", Arrays.asList("xuq", 24));
		UriComponents result = builder.build();

		assertThat(result.getQuery()).isEqualTo("baz=xuq&baz=24");

		builder = UriComponentsBuilder.newInstance().queryParam("baz", Arrays.asList("qux", 42));
		builder.replaceQueryParam("baz", Collections.emptyList());
		result = builder.build();

		assertThat(result.getQuery()).as("Query param should have been deleted").isNull();
	}

	@Test
	void buildAndExpandHierarchical() {
		UriComponents result = UriComponentsBuilder.fromPath("/{foo}").buildAndExpand("fooValue");
		assertThat(result.toUriString()).isEqualTo("/fooValue");

		Map<String, String> values = new HashMap<>();
		values.put("foo", "fooValue");
		values.put("bar", "barValue");
		result = UriComponentsBuilder.fromPath("/{foo}/{bar}").buildAndExpand(values);
		assertThat(result.toUriString()).isEqualTo("/fooValue/barValue");
	}

	@Test
	void buildAndExpandOpaque() {
		UriComponents result = UriComponentsBuilder.fromUriString("mailto:{user}@{domain}")
				.buildAndExpand("foo", "example.com");
		assertThat(result.toUriString()).isEqualTo("mailto:foo@example.com");

		Map<String, String> values = new HashMap<>();
		values.put("user", "foo");
		values.put("domain", "example.com");
		UriComponentsBuilder.fromUriString("mailto:{user}@{domain}").buildAndExpand(values);
		assertThat(result.toUriString()).isEqualTo("mailto:foo@example.com");
	}

	@Test
	void queryParamWithValueWithEquals() {
		UriComponents uriComponents = UriComponentsBuilder.fromUriString("https://example.com/foo?bar=baz").build();
		assertThat(uriComponents.toUriString()).isEqualTo("https://example.com/foo?bar=baz");
		assertThat(uriComponents.getQueryParams().get("bar").get(0)).isEqualTo("baz");
	}

	@Test
	void queryParamWithoutValueWithEquals()  {
		UriComponents uriComponents = UriComponentsBuilder.fromUriString("https://example.com/foo?bar=").build();
		assertThat(uriComponents.toUriString()).isEqualTo("https://example.com/foo?bar=");
		assertThat(uriComponents.getQueryParams().get("bar").get(0)).isEqualTo("");
	}

	@Test
	void queryParamWithoutValueWithoutEquals() {
		UriComponents uriComponents = UriComponentsBuilder.fromUriString("https://example.com/foo?bar").build();
		assertThat(uriComponents.toUriString()).isEqualTo("https://example.com/foo?bar");

		// TODO [SPR-13537] Change equalTo(null) to equalTo("").
		assertThat(uriComponents.getQueryParams().get("bar").get(0)).isNull();
	}

	@Test // gh-24444
	void opaqueUriDoesNotResetOnNullInput() throws URISyntaxException {
		URI uri = new URI("urn:ietf:wg:oauth:2.0:oob");
		UriComponents result = UriComponentsBuilder.fromUri(uri)
				.host(null)
				.port(-1)
				.port(null)
				.queryParams(null)
				.replaceQuery(null)
				.query(null)
				.build();

		assertThat(result.toUri()).isEqualTo(uri);
	}

	@Test
	void relativeUrls() {
		String baseUrl = "https://example.com";
		assertThat(UriComponentsBuilder.fromUriString(baseUrl + "/foo/../bar").build().toString())
				.isEqualTo(baseUrl + "/foo/../bar");
		assertThat(UriComponentsBuilder.fromUriString(baseUrl + "/foo/../bar").build().toUriString())
				.isEqualTo(baseUrl + "/foo/../bar");
		assertThat(UriComponentsBuilder.fromUriString(baseUrl + "/foo/../bar").build().toUri().getPath())
				.isEqualTo("/foo/../bar");
		assertThat(UriComponentsBuilder.fromUriString("../../").build().toString())
				.isEqualTo("../../");
		assertThat(UriComponentsBuilder.fromUriString("../../").build().toUriString())
				.isEqualTo("../../");
		assertThat(UriComponentsBuilder.fromUriString("../../").build().toUri().getPath())
				.isEqualTo("../../");
		assertThat(UriComponentsBuilder.fromUriString(baseUrl).path("foo/../bar").build().toString())
				.isEqualTo(baseUrl + "/foo/../bar");
		assertThat(UriComponentsBuilder.fromUriString(baseUrl).path("foo/../bar").build().toUriString())
				.isEqualTo(baseUrl + "/foo/../bar");
		assertThat(UriComponentsBuilder.fromUriString(baseUrl).path("foo/../bar").build().toUri().getPath())
				.isEqualTo("/foo/../bar");
	}

	@Test
	void emptySegments() {
		String baseUrl = "https://example.com/abc/";
		assertThat(UriComponentsBuilder.fromUriString(baseUrl).path("/x/y/z").build().toString())
				.isEqualTo("https://example.com/abc/x/y/z");
		assertThat(UriComponentsBuilder.fromUriString(baseUrl).pathSegment("x", "y", "z").build().toString())
				.isEqualTo("https://example.com/abc/x/y/z");
		assertThat(UriComponentsBuilder.fromUriString(baseUrl).path("/x/").path("/y/z").build().toString())
				.isEqualTo("https://example.com/abc/x/y/z");
		assertThat(UriComponentsBuilder.fromUriString(baseUrl).pathSegment("x").path("y").build().toString())
				.isEqualTo("https://example.com/abc/x/y");
	}

	@Test
	void parsesEmptyFragment() {
		UriComponents components = UriComponentsBuilder.fromUriString("/example#").build();
		assertThat(components.getFragment()).isNull();
		assertThat(components.toString()).isEqualTo("/example");
	}

	@Test  // SPR-13257
	void parsesEmptyUri() {
		UriComponents components = UriComponentsBuilder.fromUriString("").build();
		assertThat(components.toString()).isEqualTo("");
	}

	@Test  // gh-25243
	void testCloneAndMerge() {
		UriComponentsBuilder builder1 = UriComponentsBuilder.newInstance();
		builder1.scheme("http").host("e1.com").path("/p1").pathSegment("ps1").queryParam("q1", "x").fragment("f1").encode();

		UriComponentsBuilder builder2 = builder1.cloneBuilder();
		builder2.scheme("https").host("e2.com").path("p2").pathSegment("{ps2}").queryParam("q2").fragment("f2");

		builder1.queryParam("q1", "y");  // one more entry for an existing parameter

		UriComponents result1 = builder1.build();
		assertThat(result1.getScheme()).isEqualTo("http");
		assertThat(result1.getHost()).isEqualTo("e1.com");
		assertThat(result1.getPath()).isEqualTo("/p1/ps1");
		assertThat(result1.getQuery()).isEqualTo("q1=x&q1=y");
		assertThat(result1.getFragment()).isEqualTo("f1");

		UriComponents result2 = builder2.buildAndExpand("ps2;a");
		assertThat(result2.getScheme()).isEqualTo("https");
		assertThat(result2.getHost()).isEqualTo("e2.com");
		assertThat(result2.getPath()).isEqualTo("/p1/ps1/p2/ps2%3Ba");
		assertThat(result2.getQuery()).isEqualTo("q1=x&q2");
		assertThat(result2.getFragment()).isEqualTo("f2");
	}

	@Test  // gh-24772
	void testDeepClone() {
		HashMap<String, Object> vars = new HashMap<>();
		vars.put("ps1", "foo");
		vars.put("ps2", "bar");

		UriComponentsBuilder builder1 = UriComponentsBuilder.newInstance();
		builder1.scheme("http").host("e1.com").userInfo("user:pwd").path("/p1").pathSegment("{ps1}")
				.pathSegment("{ps2}").queryParam("q1").fragment("f1").uriVariables(vars).encode();

		UriComponentsBuilder builder2 = builder1.cloneBuilder();

		UriComponents result1 = builder1.build();
		assertThat(result1.getScheme()).isEqualTo("http");
		assertThat(result1.getUserInfo()).isEqualTo("user:pwd");
		assertThat(result1.getHost()).isEqualTo("e1.com");
		assertThat(result1.getPath()).isEqualTo("/p1/%s/%s", vars.get("ps1"), vars.get("ps2"));
		assertThat(result1.getQuery()).isEqualTo("q1");
		assertThat(result1.getFragment()).isEqualTo("f1");
		assertThat(result1.getSchemeSpecificPart()).isEqualTo(null);

		UriComponents result2 = builder2.build();
		assertThat(result2.getScheme()).isEqualTo("http");
		assertThat(result2.getUserInfo()).isEqualTo("user:pwd");
		assertThat(result2.getHost()).isEqualTo("e1.com");
		assertThat(result2.getPath()).isEqualTo("/p1/%s/%s", vars.get("ps1"), vars.get("ps2"));
		assertThat(result2.getQuery()).isEqualTo("q1");
		assertThat(result2.getFragment()).isEqualTo("f1");
		assertThat(result1.getSchemeSpecificPart()).isEqualTo(null);
	}

	@Test  // SPR-11856
	void fromHttpRequestForwardedHeader()  {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Forwarded", "proto=https; host=84.198.58.199");
		request.setScheme("http");
		request.setServerName("example.com");
		request.setRequestURI("/rest/mobile/users/1");

		HttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents result = UriComponentsBuilder.fromHttpRequest(httpRequest).build();

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
		UriComponents result = UriComponentsBuilder.fromHttpRequest(httpRequest).build();

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
		UriComponents result = UriComponentsBuilder.fromHttpRequest(httpRequest).build();

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
		UriComponents result = UriComponentsBuilder.fromHttpRequest(httpRequest).build();

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
		UriComponents result = UriComponentsBuilder.fromHttpRequest(httpRequest).build();

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
		UriComponents result = UriComponentsBuilder.fromHttpRequest(httpRequest).build();

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
		UriComponents result = UriComponentsBuilder.fromHttpRequest(httpRequest).build();

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
		UriComponents result = UriComponentsBuilder.fromHttpRequest(httpRequest).build();

		assertThat(result.getScheme()).isEqualTo("https");
		assertThat(result.getHost()).isEqualTo("example.com");
		assertThat(result.getPath()).isEqualTo("/rest/mobile/users/1");
		assertThat(result.getPort()).isEqualTo(-1);
		assertThat(result.toUriString()).isEqualTo("https://example.com/rest/mobile/users/1");
	}

	@Test // gh-25737
	void fromHttpRequestForwardedHeaderComma() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Forwarded", "for=192.0.2.0,for=192.0.2.1;proto=https;host=192.0.2.3:9090");
		request.setScheme("http");
		request.setServerPort(8080);
		request.setServerName("example.com");
		request.setRequestURI("/rest/mobile/users/1");

		HttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents result = UriComponentsBuilder.fromHttpRequest(httpRequest).build();

		assertThat(result.getScheme()).isEqualTo("https");
		assertThat(result.getHost()).isEqualTo("192.0.2.3");
		assertThat(result.getPath()).isEqualTo("/rest/mobile/users/1");
		assertThat(result.getPort()).isEqualTo(9090);
		assertThat(result.toUriString()).isEqualTo("https://192.0.2.3:9090/rest/mobile/users/1");
	}


	@Test  // SPR-16364
	void uriComponentsNotEqualAfterNormalization() {
		UriComponents uri1 = UriComponentsBuilder.fromUriString("http://test.com").build().normalize();
		UriComponents uri2 = UriComponentsBuilder.fromUriString("http://test.com/").build();

		assertThat(uri1.getPathSegments().isEmpty()).isTrue();
		assertThat(uri2.getPathSegments().isEmpty()).isTrue();
		assertThat(uri2).isNotEqualTo(uri1);
	}

	@Test  // SPR-17256
	void uriComponentsWithMergedQueryParams() {
		String uri = UriComponentsBuilder.fromUriString("http://localhost:8081")
				.uriComponents(UriComponentsBuilder.fromUriString("/{path}?sort={sort}").build())
				.queryParam("sort", "another_value").build().toString();

		assertThat(uri).isEqualTo("http://localhost:8081/{path}?sort={sort}&sort=another_value");
	}

	@Test // SPR-17630
	void toUriStringWithCurlyBraces() {
		assertThat(UriComponentsBuilder.fromUriString("/path?q={asa}asa").toUriString()).isEqualTo("/path?q=%7Basa%7Dasa");
	}

}
