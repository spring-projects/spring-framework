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

package org.springframework.web.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.http.HttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link org.springframework.web.util.UriComponentsBuilder}.
 *
 * @author Arjen Poutsma
 * @author Phillip Webb
 * @author Oliver Gierke
 * @author David Eckel
 * @author Sam Brannen
 */
public class UriComponentsBuilderTests {

	@Test
	public void plain() throws URISyntaxException {
		UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
		UriComponents result = builder.scheme("http").host("example.com")
				.path("foo").queryParam("bar").fragment("baz")
				.build();
		assertEquals("http", result.getScheme());
		assertEquals("example.com", result.getHost());
		assertEquals("foo", result.getPath());
		assertEquals("bar", result.getQuery());
		assertEquals("baz", result.getFragment());

		URI expected = new URI("http://example.com/foo?bar#baz");
		assertEquals("Invalid result URI", expected, result.toUri());
	}

	@Test
	public void multipleFromSameBuilder() throws URISyntaxException {
		UriComponentsBuilder builder = UriComponentsBuilder.newInstance()
				.scheme("http").host("example.com").pathSegment("foo");
		UriComponents result1 = builder.build();
		builder = builder.pathSegment("foo2").queryParam("bar").fragment("baz");
		UriComponents result2 = builder.build();

		assertEquals("http", result1.getScheme());
		assertEquals("example.com", result1.getHost());
		assertEquals("/foo", result1.getPath());
		URI expected = new URI("http://example.com/foo");
		assertEquals("Invalid result URI", expected, result1.toUri());

		assertEquals("http", result2.getScheme());
		assertEquals("example.com", result2.getHost());
		assertEquals("/foo/foo2", result2.getPath());
		assertEquals("bar", result2.getQuery());
		assertEquals("baz", result2.getFragment());
		expected = new URI("http://example.com/foo/foo2?bar#baz");
		assertEquals("Invalid result URI", expected, result2.toUri());
	}

	@Test
	public void fromPath() throws URISyntaxException {
		UriComponents result = UriComponentsBuilder.fromPath("foo").queryParam("bar").fragment("baz").build();
		assertEquals("foo", result.getPath());
		assertEquals("bar", result.getQuery());
		assertEquals("baz", result.getFragment());

		assertEquals("Invalid result URI String", "foo?bar#baz", result.toUriString());

		URI expected = new URI("foo?bar#baz");
		assertEquals("Invalid result URI", expected, result.toUri());

		result = UriComponentsBuilder.fromPath("/foo").build();
		assertEquals("/foo", result.getPath());

		expected = new URI("/foo");
		assertEquals("Invalid result URI", expected, result.toUri());
	}

	@Test
	public void fromHierarchicalUri() throws URISyntaxException {
		URI uri = new URI("http://example.com/foo?bar#baz");
		UriComponents result = UriComponentsBuilder.fromUri(uri).build();
		assertEquals("http", result.getScheme());
		assertEquals("example.com", result.getHost());
		assertEquals("/foo", result.getPath());
		assertEquals("bar", result.getQuery());
		assertEquals("baz", result.getFragment());

		assertEquals("Invalid result URI", uri, result.toUri());
	}

	@Test
	public void fromOpaqueUri() throws URISyntaxException {
		URI uri = new URI("mailto:foo@bar.com#baz");
		UriComponents result = UriComponentsBuilder.fromUri(uri).build();
		assertEquals("mailto", result.getScheme());
		assertEquals("foo@bar.com", result.getSchemeSpecificPart());
		assertEquals("baz", result.getFragment());

		assertEquals("Invalid result URI", uri, result.toUri());
	}

	@Test // SPR-9317
	public void fromUriEncodedQuery() throws URISyntaxException {
		URI uri = new URI("http://www.example.org/?param=aGVsbG9Xb3JsZA%3D%3D");
		String fromUri = UriComponentsBuilder.fromUri(uri).build().getQueryParams().get("param").get(0);
		String fromUriString = UriComponentsBuilder.fromUriString(uri.toString())
				.build().getQueryParams().get("param").get(0);

		assertEquals(fromUri, fromUriString);
	}

	@Test
	public void fromUriString() {
		UriComponents result = UriComponentsBuilder.fromUriString("http://www.ietf.org/rfc/rfc3986.txt").build();
		assertEquals("http", result.getScheme());
		assertNull(result.getUserInfo());
		assertEquals("www.ietf.org", result.getHost());
		assertEquals(-1, result.getPort());
		assertEquals("/rfc/rfc3986.txt", result.getPath());
		assertEquals(Arrays.asList("rfc", "rfc3986.txt"), result.getPathSegments());
		assertNull(result.getQuery());
		assertNull(result.getFragment());

		String url = "http://arjen:foobar@java.sun.com:80" +
				"/javase/6/docs/api/java/util/BitSet.html?foo=bar#and(java.util.BitSet)";
		result = UriComponentsBuilder.fromUriString(url).build();
		assertEquals("http", result.getScheme());
		assertEquals("arjen:foobar", result.getUserInfo());
		assertEquals("java.sun.com", result.getHost());
		assertEquals(80, result.getPort());
		assertEquals("/javase/6/docs/api/java/util/BitSet.html", result.getPath());
		assertEquals("foo=bar", result.getQuery());
		MultiValueMap<String, String> expectedQueryParams = new LinkedMultiValueMap<>(1);
		expectedQueryParams.add("foo", "bar");
		assertEquals(expectedQueryParams, result.getQueryParams());
		assertEquals("and(java.util.BitSet)", result.getFragment());

		result = UriComponentsBuilder.fromUriString("mailto:java-net@java.sun.com#baz").build();
		assertEquals("mailto", result.getScheme());
		assertNull(result.getUserInfo());
		assertNull(result.getHost());
		assertEquals(-1, result.getPort());
		assertEquals("java-net@java.sun.com", result.getSchemeSpecificPart());
		assertNull(result.getPath());
		assertNull(result.getQuery());
		assertEquals("baz", result.getFragment());

		result = UriComponentsBuilder.fromUriString("docs/guide/collections/designfaq.html#28").build();
		assertNull(result.getScheme());
		assertNull(result.getUserInfo());
		assertNull(result.getHost());
		assertEquals(-1, result.getPort());
		assertEquals("docs/guide/collections/designfaq.html", result.getPath());
		assertNull(result.getQuery());
		assertEquals("28", result.getFragment());
	}

	@Test // SPR-9832
	public void fromUriStringQueryParamWithReservedCharInValue() throws URISyntaxException {
		String uri = "http://www.google.com/ig/calculator?q=1USD=?EUR";
		UriComponents result = UriComponentsBuilder.fromUriString(uri).build();

		assertEquals("q=1USD=?EUR", result.getQuery());
		assertEquals("1USD=?EUR", result.getQueryParams().getFirst("q"));
	}

	@Test // SPR-14828
	public void fromUriStringQueryParamEncodedAndContainingPlus() throws Exception {
		String httpUrl = "http://localhost:8080/test/print?value=%EA%B0%80+%EB%82%98";
		URI uri = UriComponentsBuilder.fromHttpUrl(httpUrl).build(true).toUri();

		assertEquals(httpUrl, uri.toString());
	}

	@Test // SPR-10779
	public void fromHttpUrlStringCaseInsesitiveScheme() {
		assertEquals("http", UriComponentsBuilder.fromHttpUrl("HTTP://www.google.com").build().getScheme());
		assertEquals("https", UriComponentsBuilder.fromHttpUrl("HTTPS://www.google.com").build().getScheme());
	}



	@Test(expected = IllegalArgumentException.class) // SPR-10539
	public void fromHttpUrlStringInvalidIPv6Host() throws URISyntaxException {
		UriComponentsBuilder.fromHttpUrl("http://[1abc:2abc:3abc::5ABC:6abc:8080/resource").build().encode();
	}

	@Test // SPR-10539
	public void fromUriStringIPv6Host() throws URISyntaxException {
		UriComponents result = UriComponentsBuilder
				.fromUriString("http://[1abc:2abc:3abc::5ABC:6abc]:8080/resource").build().encode();
		assertEquals("[1abc:2abc:3abc::5ABC:6abc]", result.getHost());

		UriComponents resultWithScopeId = UriComponentsBuilder
				.fromUriString("http://[1abc:2abc:3abc::5ABC:6abc%eth0]:8080/resource").build().encode();
		assertEquals("[1abc:2abc:3abc::5ABC:6abc%25eth0]", resultWithScopeId.getHost());

		UriComponents resultIPv4compatible = UriComponentsBuilder
				.fromUriString("http://[::192.168.1.1]:8080/resource").build().encode();
		assertEquals("[::192.168.1.1]", resultIPv4compatible.getHost());
	}

	@Test // SPR-11970
	public void fromUriStringNoPathWithReservedCharInQuery() {
		UriComponents result = UriComponentsBuilder.fromUriString("http://example.com?foo=bar@baz").build();
		assertTrue(StringUtils.isEmpty(result.getUserInfo()));
		assertEquals("example.com", result.getHost());
		assertTrue(result.getQueryParams().containsKey("foo"));
		assertEquals("bar@baz", result.getQueryParams().getFirst("foo"));
	}

	@Test
	public void fromHttpRequest() throws URISyntaxException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(-1);
		request.setRequestURI("/path");
		request.setQueryString("a=1");

		UriComponents result = UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request)).build();
		assertEquals("http", result.getScheme());
		assertEquals("localhost", result.getHost());
		assertEquals(-1, result.getPort());
		assertEquals("/path", result.getPath());
		assertEquals("a=1", result.getQuery());
	}

	@Test // SPR-12771
	public void fromHttpRequestResetsPortBeforeSettingIt() throws Exception {
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

		assertEquals("https", result.getScheme());
		assertEquals("84.198.58.199", result.getHost());
		assertEquals(-1, result.getPort());
		assertEquals("/rest/mobile/users/1", result.getPath());
	}

	@Test //SPR-14761
	public void fromHttpRequestWithForwardedIPv4Host() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(-1);
		request.setRequestURI("/mvc-showcase");
		request.addHeader("Forwarded", "host=192.168.0.1");

		HttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents result = UriComponentsBuilder.fromHttpRequest(httpRequest).build();

		assertEquals("http://192.168.0.1/mvc-showcase", result.toString());
	}

	@Test //SPR-14761
	public void fromHttpRequestWithForwardedIPv6() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(-1);
		request.setRequestURI("/mvc-showcase");
		request.addHeader("Forwarded", "host=[1abc:2abc:3abc::5ABC:6abc]");

		HttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents result = UriComponentsBuilder.fromHttpRequest(httpRequest).build();

		assertEquals("http://[1abc:2abc:3abc::5ABC:6abc]/mvc-showcase", result.toString());
	}

	@Test //SPR-14761
	public void fromHttpRequestWithForwardedIPv6Host() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(-1);
		request.setRequestURI("/mvc-showcase");
		request.addHeader("X-Forwarded-Host", "[1abc:2abc:3abc::5ABC:6abc]");

		HttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents result = UriComponentsBuilder.fromHttpRequest(httpRequest).build();

		assertEquals("http://[1abc:2abc:3abc::5ABC:6abc]/mvc-showcase", result.toString());
	}

	@Test //SPR-14761
	public void fromHttpRequestWithForwardedIPv6HostAndPort() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(-1);
		request.setRequestURI("/mvc-showcase");
		request.addHeader("X-Forwarded-Host", "[1abc:2abc:3abc::5ABC:6abc]:8080");

		HttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents result = UriComponentsBuilder.fromHttpRequest(httpRequest).build();

		assertEquals("http://[1abc:2abc:3abc::5ABC:6abc]:8080/mvc-showcase", result.toString());
	}


	@Test
	public void fromHttpRequestWithForwardedHost() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(-1);
		request.setRequestURI("/mvc-showcase");
		request.addHeader("X-Forwarded-Host", "anotherHost");

		HttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents result = UriComponentsBuilder.fromHttpRequest(httpRequest).build();

		assertEquals("http://anotherHost/mvc-showcase", result.toString());
	}

	@Test // SPR-10701
	public void fromHttpRequestWithForwardedHostIncludingPort() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(-1);
		request.setRequestURI("/mvc-showcase");
		request.addHeader("X-Forwarded-Host", "webtest.foo.bar.com:443");

		HttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents result = UriComponentsBuilder.fromHttpRequest(httpRequest).build();

		assertEquals("webtest.foo.bar.com", result.getHost());
		assertEquals(443, result.getPort());
	}

	@Test // SPR-11140
	public void fromHttpRequestWithForwardedHostMultiValuedHeader() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(-1);
		request.addHeader("X-Forwarded-Host", "a.example.org, b.example.org, c.example.org");

		HttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents result = UriComponentsBuilder.fromHttpRequest(httpRequest).build();

		assertEquals("a.example.org", result.getHost());
		assertEquals(-1, result.getPort());
	}

	@Test // SPR-11855
	public void fromHttpRequestWithForwardedHostAndPort() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8080);
		request.addHeader("X-Forwarded-Host", "foobarhost");
		request.addHeader("X-Forwarded-Port", "9090");

		HttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents result = UriComponentsBuilder.fromHttpRequest(httpRequest).build();

		assertEquals("foobarhost", result.getHost());
		assertEquals(9090, result.getPort());
	}

	@Test // SPR-11872
	public void fromHttpRequestWithForwardedHostWithDefaultPort() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(10080);
		request.addHeader("X-Forwarded-Host", "example.org");

		HttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents result = UriComponentsBuilder.fromHttpRequest(httpRequest).build();

		assertEquals("example.org", result.getHost());
		assertEquals(-1, result.getPort());
	}


	@Test
	public void fromHttpRequestWithForwardedHostWithForwardedScheme() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(10080);
		request.addHeader("X-Forwarded-Host", "example.org");
		request.addHeader("X-Forwarded-Proto", "https");

		HttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents result = UriComponentsBuilder.fromHttpRequest(httpRequest).build();

		assertEquals("example.org", result.getHost());
		assertEquals("https", result.getScheme());
		assertEquals(-1, result.getPort());
	}

	@Test // SPR-12771
	public void fromHttpRequestWithForwardedProtoAndDefaultPort() {
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

		assertEquals("https://84.198.58.199/mvc-showcase", result.toString());
	}

	@Test // SPR-12813
	public void fromHttpRequestWithForwardedPortMultiValueHeader() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(9090);
		request.setRequestURI("/mvc-showcase");
		request.addHeader("X-Forwarded-Host", "a.example.org");
		request.addHeader("X-Forwarded-Port", "80,52022");

		HttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents result = UriComponentsBuilder.fromHttpRequest(httpRequest).build();

		assertEquals("http://a.example.org/mvc-showcase", result.toString());
	}

	@Test // SPR-12816
	public void fromHttpRequestWithForwardedProtoMultiValueHeader() {
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

		assertEquals("https://a.example.org/mvc-showcase", result.toString());
	}

	@Test // SPR-12742
	public void fromHttpRequestWithTrailingSlash() throws Exception {
		UriComponents before = UriComponentsBuilder.fromPath("/foo/").build();
		UriComponents after = UriComponentsBuilder.newInstance().uriComponents(before).build();
		assertEquals("/foo/", after.getPath());
	}

	@Test
	public void path() throws URISyntaxException {
		UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/foo/bar");
		UriComponents result = builder.build();

		assertEquals("/foo/bar", result.getPath());
		assertEquals(Arrays.asList("foo", "bar"), result.getPathSegments());
	}

	@Test
	public void pathSegments() throws URISyntaxException {
		UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
		UriComponents result = builder.pathSegment("foo").pathSegment("bar").build();

		assertEquals("/foo/bar", result.getPath());
		assertEquals(Arrays.asList("foo", "bar"), result.getPathSegments());
	}

	@Test
	public void pathThenPath() {
		UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/foo/bar").path("ba/z");
		UriComponents result = builder.build().encode();

		assertEquals("/foo/barba/z", result.getPath());
		assertEquals(Arrays.asList("foo", "barba", "z"), result.getPathSegments());
	}

	@Test
	public void pathThenPathSegments() {
		UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/foo/bar").pathSegment("ba/z");
		UriComponents result = builder.build().encode();

		assertEquals("/foo/bar/ba%2Fz", result.getPath());
		assertEquals(Arrays.asList("foo", "bar", "ba%2Fz"), result.getPathSegments());
	}

	@Test
	public void pathSegmentsThenPathSegments() {
		UriComponentsBuilder builder = UriComponentsBuilder.newInstance().pathSegment("foo").pathSegment("bar");
		UriComponents result = builder.build();

		assertEquals("/foo/bar", result.getPath());
		assertEquals(Arrays.asList("foo", "bar"), result.getPathSegments());
	}

	@Test
	public void pathSegmentsThenPath() {
		UriComponentsBuilder builder = UriComponentsBuilder.newInstance().pathSegment("foo").path("/");
		UriComponents result = builder.build();

		assertEquals("/foo/", result.getPath());
		assertEquals(Collections.singletonList("foo"), result.getPathSegments());
	}

	@Test
	public void pathSegmentsSomeEmpty() {
		UriComponentsBuilder builder = UriComponentsBuilder.newInstance().pathSegment("", "foo", "", "bar");
		UriComponents result = builder.build();

		assertEquals("/foo/bar", result.getPath());
		assertEquals(Arrays.asList("foo", "bar"), result.getPathSegments());
	}

	@Test // SPR-12398
	public void pathWithDuplicateSlashes() throws URISyntaxException {
		UriComponents uriComponents = UriComponentsBuilder.fromPath("/foo/////////bar").build();
		assertEquals("/foo/bar", uriComponents.getPath());
	}

	@Test
	public void replacePath() {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString("http://www.ietf.org/rfc/rfc2396.txt");
		builder.replacePath("/rfc/rfc3986.txt");
		UriComponents result = builder.build();

		assertEquals("http://www.ietf.org/rfc/rfc3986.txt", result.toUriString());

		builder = UriComponentsBuilder.fromUriString("http://www.ietf.org/rfc/rfc2396.txt");
		builder.replacePath(null);
		result = builder.build();

		assertEquals("http://www.ietf.org", result.toUriString());
	}

	@Test
	public void replaceQuery() {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString("http://example.com/foo?foo=bar&baz=qux");
		builder.replaceQuery("baz=42");
		UriComponents result = builder.build();

		assertEquals("http://example.com/foo?baz=42", result.toUriString());

		builder = UriComponentsBuilder.fromUriString("http://example.com/foo?foo=bar&baz=qux");
		builder.replaceQuery(null);
		result = builder.build();

		assertEquals("http://example.com/foo", result.toUriString());
	}

	@Test
	public void queryParams() throws URISyntaxException {
		UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
		UriComponents result = builder.queryParam("baz", "qux", 42).build();

		assertEquals("baz=qux&baz=42", result.getQuery());
		MultiValueMap<String, String> expectedQueryParams = new LinkedMultiValueMap<>(2);
		expectedQueryParams.add("baz", "qux");
		expectedQueryParams.add("baz", "42");
		assertEquals(expectedQueryParams, result.getQueryParams());
	}

	@Test
	public void emptyQueryParam() throws URISyntaxException {
		UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
		UriComponents result = builder.queryParam("baz").build();

		assertEquals("baz", result.getQuery());
		MultiValueMap<String, String> expectedQueryParams = new LinkedMultiValueMap<>(2);
		expectedQueryParams.add("baz", null);
		assertEquals(expectedQueryParams, result.getQueryParams());
	}

	@Test
	public void replaceQueryParam() {
		UriComponentsBuilder builder = UriComponentsBuilder.newInstance().queryParam("baz", "qux", 42);
		builder.replaceQueryParam("baz", "xuq", 24);
		UriComponents result = builder.build();

		assertEquals("baz=xuq&baz=24", result.getQuery());

		builder = UriComponentsBuilder.newInstance().queryParam("baz", "qux", 42);
		builder.replaceQueryParam("baz");
		result = builder.build();

		assertNull("Query param should have been deleted", result.getQuery());
	}

	@Test
	public void buildAndExpandHierarchical() {
		UriComponents result = UriComponentsBuilder.fromPath("/{foo}").buildAndExpand("fooValue");
		assertEquals("/fooValue", result.toUriString());

		Map<String, String> values = new HashMap<>();
		values.put("foo", "fooValue");
		values.put("bar", "barValue");
		result = UriComponentsBuilder.fromPath("/{foo}/{bar}").buildAndExpand(values);
		assertEquals("/fooValue/barValue", result.toUriString());
	}

	@Test
	public void buildAndExpandOpaque() {
		UriComponents result = UriComponentsBuilder.fromUriString("mailto:{user}@{domain}")
				.buildAndExpand("foo", "example.com");
		assertEquals("mailto:foo@example.com", result.toUriString());

		Map<String, String> values = new HashMap<>();
		values.put("user", "foo");
		values.put("domain", "example.com");
		UriComponentsBuilder.fromUriString("mailto:{user}@{domain}").buildAndExpand(values);
		assertEquals("mailto:foo@example.com", result.toUriString());
	}

	@Test
	public void queryParamWithValueWithEquals() throws Exception {
		UriComponents uriComponents = UriComponentsBuilder.fromUriString("http://example.com/foo?bar=baz").build();
		assertThat(uriComponents.toUriString(), equalTo("http://example.com/foo?bar=baz"));
		assertThat(uriComponents.getQueryParams().get("bar").get(0), equalTo("baz"));
	}

	@Test
	public void queryParamWithoutValueWithEquals() throws Exception {
		UriComponents uriComponents = UriComponentsBuilder.fromUriString("http://example.com/foo?bar=").build();
		assertThat(uriComponents.toUriString(), equalTo("http://example.com/foo?bar="));
		assertThat(uriComponents.getQueryParams().get("bar").get(0), equalTo(""));
	}

	@Test
	public void queryParamWithoutValueWithoutEquals() throws Exception {
		UriComponents uriComponents = UriComponentsBuilder.fromUriString("http://example.com/foo?bar").build();
		assertThat(uriComponents.toUriString(), equalTo("http://example.com/foo?bar"));

		// TODO [SPR-13537] Change equalTo(null) to equalTo("").
		assertThat(uriComponents.getQueryParams().get("bar").get(0), equalTo(null));
	}

	@Test
	public void relativeUrls() throws Exception {
		String baseUrl = "http://example.com";
		assertThat(UriComponentsBuilder.fromUriString(baseUrl + "/foo/../bar").build().toString(),
				equalTo(baseUrl + "/foo/../bar"));
		assertThat(UriComponentsBuilder.fromUriString(baseUrl + "/foo/../bar").build().toUriString(),
				equalTo(baseUrl + "/foo/../bar"));
		assertThat(UriComponentsBuilder.fromUriString(baseUrl + "/foo/../bar").build().toUri().getPath(),
				equalTo("/foo/../bar"));
		assertThat(UriComponentsBuilder.fromUriString("../../").build().toString(),
				equalTo("../../"));
		assertThat(UriComponentsBuilder.fromUriString("../../").build().toUriString(),
				equalTo("../../"));
		assertThat(UriComponentsBuilder.fromUriString("../../").build().toUri().getPath(),
				equalTo("../../"));
		assertThat(UriComponentsBuilder.fromUriString(baseUrl).path("foo/../bar").build().toString(),
				equalTo(baseUrl + "/foo/../bar"));
		assertThat(UriComponentsBuilder.fromUriString(baseUrl).path("foo/../bar").build().toUriString(),
				equalTo(baseUrl + "/foo/../bar"));
		assertThat(UriComponentsBuilder.fromUriString(baseUrl).path("foo/../bar").build().toUri().getPath(),
				equalTo("/foo/../bar"));
	}

	@Test
	public void emptySegments() throws Exception {
		String baseUrl = "http://example.com/abc/";
		assertThat(UriComponentsBuilder.fromUriString(baseUrl).path("/x/y/z").build().toString(),
				equalTo("http://example.com/abc/x/y/z"));
		assertThat(UriComponentsBuilder.fromUriString(baseUrl).pathSegment("x", "y", "z").build().toString(),
				equalTo("http://example.com/abc/x/y/z"));
		assertThat(UriComponentsBuilder.fromUriString(baseUrl).path("/x/").path("/y/z").build().toString(),
				equalTo("http://example.com/abc/x/y/z"));
		assertThat(UriComponentsBuilder.fromUriString(baseUrl).pathSegment("x").path("y").build().toString(),
				equalTo("http://example.com/abc/x/y"));
	}

	@Test
	public void parsesEmptyFragment() {
		UriComponents components = UriComponentsBuilder.fromUriString("/example#").build();
		assertThat(components.getFragment(), is(nullValue()));
		assertThat(components.toString(), equalTo("/example"));
	}

	@Test  // SPR-13257
	public void parsesEmptyUri() {
		UriComponents components = UriComponentsBuilder.fromUriString("").build();
		assertThat(components.toString(), equalTo(""));
	}

	@Test
	public void testClone() throws URISyntaxException {
		UriComponentsBuilder builder1 = UriComponentsBuilder.newInstance();
		builder1.scheme("http").host("e1.com").path("/p1").pathSegment("ps1").queryParam("q1").fragment("f1");

		UriComponentsBuilder builder2 = (UriComponentsBuilder) builder1.clone();
		builder2.scheme("https").host("e2.com").path("p2").pathSegment("ps2").queryParam("q2").fragment("f2");

		UriComponents result1 = builder1.build();
		assertEquals("http", result1.getScheme());
		assertEquals("e1.com", result1.getHost());
		assertEquals("/p1/ps1", result1.getPath());
		assertEquals("q1", result1.getQuery());
		assertEquals("f1", result1.getFragment());

		UriComponents result2 = builder2.build();
		assertEquals("https", result2.getScheme());
		assertEquals("e2.com", result2.getHost());
		assertEquals("/p1/ps1/p2/ps2", result2.getPath());
		assertEquals("q1&q2", result2.getQuery());
		assertEquals("f2", result2.getFragment());
	}

	@Test // SPR-11856
	public void fromHttpRequestForwardedHeader() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Forwarded", "proto=https; host=84.198.58.199");
		request.setScheme("http");
		request.setServerName("example.com");
		request.setRequestURI("/rest/mobile/users/1");

		HttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents result = UriComponentsBuilder.fromHttpRequest(httpRequest).build();

		assertEquals("https", result.getScheme());
		assertEquals("84.198.58.199", result.getHost());
		assertEquals("/rest/mobile/users/1", result.getPath());
	}

	@Test
	public void fromHttpRequestForwardedHeaderQuoted() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Forwarded", "proto=\"https\"; host=\"84.198.58.199\"");
		request.setScheme("http");
		request.setServerName("example.com");
		request.setRequestURI("/rest/mobile/users/1");

		HttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents result = UriComponentsBuilder.fromHttpRequest(httpRequest).build();

		assertEquals("https", result.getScheme());
		assertEquals("84.198.58.199", result.getHost());
		assertEquals("/rest/mobile/users/1", result.getPath());
	}

	@Test
	public void fromHttpRequestMultipleForwardedHeader() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Forwarded", "host=84.198.58.199;proto=https");
		request.addHeader("Forwarded", "proto=ftp; host=1.2.3.4");
		request.setScheme("http");
		request.setServerName("example.com");
		request.setRequestURI("/rest/mobile/users/1");

		HttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents result = UriComponentsBuilder.fromHttpRequest(httpRequest).build();

		assertEquals("https", result.getScheme());
		assertEquals("84.198.58.199", result.getHost());
		assertEquals("/rest/mobile/users/1", result.getPath());
	}

	@Test
	public void fromHttpRequestMultipleForwardedHeaderComma() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Forwarded", "host=84.198.58.199 ;proto=https, proto=ftp; host=1.2.3.4");
		request.setScheme("http");
		request.setServerName("example.com");
		request.setRequestURI("/rest/mobile/users/1");

		HttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents result = UriComponentsBuilder.fromHttpRequest(httpRequest).build();

		assertEquals("https", result.getScheme());
		assertEquals("84.198.58.199", result.getHost());
		assertEquals("/rest/mobile/users/1", result.getPath());
	}

	@Test
	public void fromHttpRequestForwardedHeaderWithHostPortAndWithoutServerPort() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Forwarded", "proto=https; host=84.198.58.199:9090");
		request.setScheme("http");
		request.setServerName("example.com");
		request.setRequestURI("/rest/mobile/users/1");

		HttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents result = UriComponentsBuilder.fromHttpRequest(httpRequest).build();

		assertEquals("https", result.getScheme());
		assertEquals("84.198.58.199", result.getHost());
		assertEquals("/rest/mobile/users/1", result.getPath());
		assertEquals(9090, result.getPort());
		assertEquals("https://84.198.58.199:9090/rest/mobile/users/1", result.toUriString());
	}

	@Test
	public void fromHttpRequestForwardedHeaderWithHostPortAndServerPort() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Forwarded", "proto=https; host=84.198.58.199:9090");
		request.setScheme("http");
		request.setServerPort(8080);
		request.setServerName("example.com");
		request.setRequestURI("/rest/mobile/users/1");

		HttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents result = UriComponentsBuilder.fromHttpRequest(httpRequest).build();

		assertEquals("https", result.getScheme());
		assertEquals("84.198.58.199", result.getHost());
		assertEquals("/rest/mobile/users/1", result.getPath());
		assertEquals(9090, result.getPort());
		assertEquals("https://84.198.58.199:9090/rest/mobile/users/1", result.toUriString());
	}

	@Test
	public void fromHttpRequestForwardedHeaderWithoutHostPortAndWithServerPort() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Forwarded", "proto=https; host=84.198.58.199");
		request.setScheme("http");
		request.setServerPort(8080);
		request.setServerName("example.com");
		request.setRequestURI("/rest/mobile/users/1");

		HttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents result = UriComponentsBuilder.fromHttpRequest(httpRequest).build();

		assertEquals("https", result.getScheme());
		assertEquals("84.198.58.199", result.getHost());
		assertEquals("/rest/mobile/users/1", result.getPath());
		assertEquals(-1, result.getPort());
		assertEquals("https://84.198.58.199/rest/mobile/users/1", result.toUriString());
	}
}
