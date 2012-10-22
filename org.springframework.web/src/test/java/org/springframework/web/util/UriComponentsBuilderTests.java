/*
 * Copyright 2002-2012 the original author or authors.
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
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 */
public class UriComponentsBuilderTests {

	@Test
	public void plain() throws URISyntaxException {
		UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
		UriComponents result = builder.scheme("http").host("example.com").path("foo").queryParam("bar").fragment("baz").build();
		assertEquals("http", result.getScheme());
		assertEquals("example.com", result.getHost());
		assertEquals("foo", result.getPath());
		assertEquals("bar", result.getQuery());
		assertEquals("baz", result.getFragment());

		URI expected = new URI("http://example.com/foo?bar#baz");
		assertEquals("Invalid result URI", expected, result.toUri());
	}

	@Test
	public void fromPath() throws URISyntaxException {
		UriComponents result = UriComponentsBuilder.fromPath("foo").queryParam("bar").fragment("baz").build();
		assertEquals("foo", result.getPath());
		assertEquals("bar", result.getQuery());
		assertEquals("baz", result.getFragment());

		URI expected = new URI("/foo?bar#baz");
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

	// SPR-9317

	@Test
	public void fromUriEncodedQuery() throws URISyntaxException {
		URI uri = new URI("http://www.example.org/?param=aGVsbG9Xb3JsZA%3D%3D");
		String fromUri = UriComponentsBuilder.fromUri(uri).build().getQueryParams().get("param").get(0);
		String fromUriString = UriComponentsBuilder.fromUriString(uri.toString()).build().getQueryParams().get("param").get(0);

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

		result = UriComponentsBuilder.fromUriString(
				"http://arjen:foobar@java.sun.com:80/javase/6/docs/api/java/util/BitSet.html?foo=bar#and(java.util.BitSet)")
				.build();
		assertEquals("http", result.getScheme());
		assertEquals("arjen:foobar", result.getUserInfo());
		assertEquals("java.sun.com", result.getHost());
		assertEquals(80, result.getPort());
		assertEquals("/javase/6/docs/api/java/util/BitSet.html", result.getPath());
		assertEquals("foo=bar", result.getQuery());
		MultiValueMap<String, String> expectedQueryParams = new LinkedMultiValueMap<String, String>(1);
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

	// SPR-9832

	@Test
	public void fromUriStringQueryParamWithReservedCharInValue() throws URISyntaxException {
        String uri = "http://www.google.com/ig/calculator?q=1USD=?EUR";
		UriComponents result = UriComponentsBuilder.fromUriString(uri).build();

		assertEquals("q=1USD=?EUR", result.getQuery());
		assertEquals("1USD=?EUR", result.getQueryParams().getFirst("q"));
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
		assertEquals(Arrays.asList("foo"), result.getPathSegments());
	}

	@Test
	public void pathSegmentsSomeEmpty() {
		UriComponentsBuilder builder = UriComponentsBuilder.newInstance().pathSegment("", "foo", "", "bar");
		UriComponents result = builder.build();

		assertEquals("/foo/bar", result.getPath());
		assertEquals(Arrays.asList("foo", "bar"), result.getPathSegments());
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
		MultiValueMap<String, String> expectedQueryParams = new LinkedMultiValueMap<String, String>(2);
		expectedQueryParams.add("baz", "qux");
		expectedQueryParams.add("baz", "42");
		assertEquals(expectedQueryParams, result.getQueryParams());
	}

	@Test
	public void emptyQueryParam() throws URISyntaxException {
		UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
		UriComponents result = builder.queryParam("baz").build();

		assertEquals("baz", result.getQuery());
		MultiValueMap<String, String> expectedQueryParams = new LinkedMultiValueMap<String, String>(2);
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

		Map<String, String> values = new HashMap<String, String>();
		values.put("foo", "fooValue");
		values.put("bar", "barValue");
		result = UriComponentsBuilder.fromPath("/{foo}/{bar}").buildAndExpand(values);
		assertEquals("/fooValue/barValue", result.toUriString());
	}

	@Test
	public void buildAndExpandOpaque() {
		UriComponents result = UriComponentsBuilder.fromUriString("mailto:{user}@{domain}").buildAndExpand("foo", "example.com");
		assertEquals("mailto:foo@example.com", result.toUriString());

		Map<String, String> values = new HashMap<String, String>();
		values.put("user", "foo");
		values.put("domain", "example.com");
		UriComponentsBuilder.fromUriString("mailto:{user}@{domain}").buildAndExpand(values);
		assertEquals("mailto:foo@example.com", result.toUriString());
	}

}
