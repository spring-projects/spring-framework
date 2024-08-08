/*
 * Copyright 2002-2024 the original author or authors.
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.Test;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link UriComponentsBuilder}.
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

	@Test  // see gh-26453
	void examplesInReferenceManual() {
		final String expected = "/hotel%20list/New%20York?q=foo%2Bbar";

		URI uri = UriComponentsBuilder.fromPath("/hotel list/{city}")
			.queryParam("q", "{q}")
			.encode()
			.buildAndExpand("New York", "foo+bar")
			.toUri();
		assertThat(uri).asString().isEqualTo(expected);

		uri = UriComponentsBuilder.fromPath("/hotel list/{city}")
			.queryParam("q", "{q}")
			.build("New York", "foo+bar");
		assertThat(uri).asString().isEqualTo(expected);

		uri = UriComponentsBuilder.fromUriString("/hotel list/{city}?q={q}")
			.build("New York", "foo+bar");
		assertThat(uri).asString().isEqualTo(expected);
	}

	@Test
	void plain() {
		UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
		UriComponents result = builder.scheme("https").host("example.com")
				.path("foo").queryParam("bar").fragment("baz").build();

		assertThat(result.getScheme()).isEqualTo("https");
		assertThat(result.getHost()).isEqualTo("example.com");
		assertThat(result.getPath()).isEqualTo("foo");
		assertThat(result.getQuery()).isEqualTo("bar");
		assertThat(result.getFragment()).isEqualTo("baz");

		URI expected = URI.create("https://example.com/foo?bar#baz");
		assertThat(result.toUri()).as("Invalid result URI").isEqualTo(expected);
	}

	@Test
	void multipleFromSameBuilder() {
		UriComponentsBuilder builder = UriComponentsBuilder.newInstance()
				.scheme("https").host("example.com").pathSegment("foo");
		UriComponents result1 = builder.build();
		builder = builder.pathSegment("foo2").queryParam("bar").fragment("baz");
		UriComponents result2 = builder.build();

		assertThat(result1.getScheme()).isEqualTo("https");
		assertThat(result1.getHost()).isEqualTo("example.com");
		assertThat(result1.getPath()).isEqualTo("/foo");
		URI expected = URI.create("https://example.com/foo");
		assertThat(result1.toUri()).as("Invalid result URI").isEqualTo(expected);

		assertThat(result2.getScheme()).isEqualTo("https");
		assertThat(result2.getHost()).isEqualTo("example.com");
		assertThat(result2.getPath()).isEqualTo("/foo/foo2");
		assertThat(result2.getQuery()).isEqualTo("bar");
		assertThat(result2.getFragment()).isEqualTo("baz");
		expected = URI.create("https://example.com/foo/foo2?bar#baz");
		assertThat(result2.toUri()).as("Invalid result URI").isEqualTo(expected);
	}

	@Test
	void fromPath() {
		UriComponents result = UriComponentsBuilder.fromPath("foo").queryParam("bar").fragment("baz").build();

		assertThat(result.getPath()).isEqualTo("foo");
		assertThat(result.getQuery()).isEqualTo("bar");
		assertThat(result.getFragment()).isEqualTo("baz");
		assertThat(result.toUriString()).as("Invalid result URI String").isEqualTo("foo?bar#baz");

		URI expected = URI.create("foo?bar#baz");
		assertThat(result.toUri()).as("Invalid result URI").isEqualTo(expected);

		result = UriComponentsBuilder.fromPath("/foo").build();
		assertThat(result.getPath()).isEqualTo("/foo");

		expected = URI.create("/foo");
		assertThat(result.toUri()).as("Invalid result URI").isEqualTo(expected);
	}

	@Test
	void fromHierarchicalUri() {
		URI uri = URI.create("https://example.com/foo?bar#baz");
		UriComponents result = UriComponentsBuilder.fromUri(uri).build();

		assertThat(result.getScheme()).isEqualTo("https");
		assertThat(result.getHost()).isEqualTo("example.com");
		assertThat(result.getPath()).isEqualTo("/foo");
		assertThat(result.getQuery()).isEqualTo("bar");
		assertThat(result.getFragment()).isEqualTo("baz");
		assertThat(result.toUri()).as("Invalid result URI").isEqualTo(uri);
	}

	@Test
	void fromOpaqueUri() {
		URI uri = URI.create("mailto:foo@bar.com#baz");
		UriComponents result = UriComponentsBuilder.fromUri(uri).build();

		assertThat(result.getScheme()).isEqualTo("mailto");
		assertThat(result.getSchemeSpecificPart()).isEqualTo("foo@bar.com");
		assertThat(result.getFragment()).isEqualTo("baz");
		assertThat(result.toUri()).as("Invalid result URI").isEqualTo(uri);
	}

	@Test  // SPR-9317
	void fromUriEncodedQuery() {
		URI uri = URI.create("https://www.example.org/?param=aGVsbG9Xb3JsZA%3D%3D");
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

		result = UriComponentsBuilder.fromUriString("mailto:user@example.com?subject=foo").build();
		assertThat(result.getScheme()).isEqualTo("mailto");
		assertThat(result.getUserInfo()).isNull();
		assertThat(result.getHost()).isNull();
		assertThat(result.getPort()).isEqualTo(-1);
		assertThat(result.getSchemeSpecificPart()).isEqualTo("user@example.com?subject=foo");
		assertThat(result.getPath()).isNull();
		assertThat(result.getQuery()).isNull();
		assertThat(result.getFragment()).isNull();

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
		assertThat(result.getHost()).isEqualToIgnoringCase("[1abc:2abc:3abc::5ABC:6abc]");

		UriComponents resultIPv4compatible = UriComponentsBuilder
				.fromUriString("http://[::192.168.1.1]:8080/resource").build().encode();
		assertThat(resultIPv4compatible.getHost()).isEqualTo("[::c0a8:101]");
	}

	@Test
	void fromUriStringInvalidIPv6Host() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				UriComponentsBuilder.fromUriString("http://[1abc:2abc:3abc::5ABC:6abc:8080/resource"));
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
		URI uri = UriComponentsBuilder.fromUriString(httpUrl).build(true).toUri();

		assertThat(uri.toString()).isEqualTo(httpUrl);
	}

	@Test  // SPR-10779
	void fromHttpUrlCaseInsensitiveScheme() {
		assertThat(UriComponentsBuilder.fromUriString("HTTP://www.google.com").build().getScheme()).isEqualTo("http");
		assertThat(UriComponentsBuilder.fromUriString("HTTPS://www.google.com").build().getScheme()).isEqualTo("https");
	}

	@Test  // SPR-10539
	void fromHttpUrlInvalidIPv6Host() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				UriComponentsBuilder.fromUriString("http://[1abc:2abc:3abc::5ABC:6abc:8080/resource"));
	}

	@Test
	void fromHttpUrlWithoutFragment() {
		String httpUrl = "http://localhost:8080/test/print";
		UriComponents uriComponents = UriComponentsBuilder.fromUriString(httpUrl).build();
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
		uriComponents = UriComponentsBuilder.fromUriString(httpUrl).build();
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
		uriComponents = UriComponentsBuilder.fromUriString(httpUrl).build();
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
		String httpUrl = "https://example.com/#baz";
		UriComponents uriComponents = UriComponentsBuilder.fromUriString(httpUrl).build();
		assertThat(uriComponents.getScheme()).isEqualTo("https");
		assertThat(uriComponents.getUserInfo()).isNull();
		assertThat(uriComponents.getHost()).isEqualTo("example.com");
		assertThat(uriComponents.getPort()).isEqualTo(-1);
		assertThat(uriComponents.getPath()).isEqualTo("/");
		assertThat(uriComponents.getPathSegments()).isEmpty();
		assertThat(uriComponents.getQuery()).isNull();
		assertThat(uriComponents.getFragment()).isEqualTo("baz");
		assertThat(uriComponents.toUri().toString()).isEqualTo(httpUrl);

		httpUrl = "http://localhost:8080/test/print#baz";
		uriComponents = UriComponentsBuilder.fromUriString(httpUrl).build();
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
		uriComponents = UriComponentsBuilder.fromUriString(httpUrl).build();
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

	@Test  // SPR-12742
	void fromHttpRequestWithTrailingSlash() {
		UriComponents before = UriComponentsBuilder.fromPath("/foo/").build();
		UriComponents after = UriComponentsBuilder.newInstance().uriComponents(before).build();
		assertThat(after.getPath()).isEqualTo("/foo/");
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
		List<String> values = Arrays.asList("qux", "42");
		UriComponents result = UriComponentsBuilder.newInstance().queryParam("baz", values).build();

		assertThat(result.getQuery()).isEqualTo("baz=qux&baz=42");
		assertThat(result.getQueryParams()).containsOnlyKeys("baz").containsEntry("baz", values);
	}

	@Test
	void queryParamWithOptionalValue() {
		UriComponents result = UriComponentsBuilder.newInstance()
				.queryParam("foo", Optional.empty())
				.queryParam("baz", Optional.of("qux"), 42)
				.build();

		assertThat(result.getQuery()).isEqualTo("foo&baz=qux&baz=42");
		assertThat(result.getQueryParams()).containsOnlyKeys("foo", "baz")
				.containsEntry("foo", Collections.singletonList(null))
				.containsEntry("baz", Arrays.asList("qux", "42"));
	}

	@Test
	void queryParamIfPresent() {
		UriComponents result = UriComponentsBuilder.newInstance()
				.queryParamIfPresent("baz", Optional.of("qux"))
				.queryParamIfPresent("foo", Optional.empty())
				.build();

		assertThat(result.getQuery()).isEqualTo("baz=qux");
		assertThat(result.getQueryParams())
				.containsOnlyKeys("baz")
				.containsEntry("baz", Collections.singletonList("qux"));
	}

	@Test
	void queryParamIfPresentCollection() {
		List<String> values = Arrays.asList("foo", "bar");
		UriComponents result = UriComponentsBuilder.newInstance()
				.queryParamIfPresent("baz", Optional.of(values))
				.build();

		assertThat(result.getQuery()).isEqualTo("baz=foo&baz=bar");
		assertThat(result.getQueryParams()).containsOnlyKeys("baz").containsEntry("baz", values);
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
		assertThat(uriComponents.getQueryParams().get("bar")).containsExactly("baz");
	}

	@Test
	void queryParamWithoutValueWithEquals() {
		UriComponents uriComponents = UriComponentsBuilder.fromUriString("https://example.com/foo?bar=").build();
		assertThat(uriComponents.toUriString()).isEqualTo("https://example.com/foo?bar=");
		assertThat(uriComponents.getQueryParams().get("bar")).element(0).asString().isEmpty();
	}

	@Test
	void queryParamWithoutValueWithoutEquals() {
		UriComponents uriComponents = UriComponentsBuilder.fromUriString("https://example.com/foo?bar").build();
		assertThat(uriComponents.toUriString()).isEqualTo("https://example.com/foo?bar");

		// TODO [SPR-13537] Change equalTo(null) to equalTo("").
		assertThat(uriComponents.getQueryParams().get("bar")).element(0).isNull();
	}

	@Test  // gh-24444
	void opaqueUriDoesNotResetOnNullInput() {
		URI uri = URI.create("urn:ietf:wg:oauth:2.0:oob");
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
				.isEqualTo(baseUrl + "/bar");
		assertThat(UriComponentsBuilder.fromUriString(baseUrl + "/foo/../bar").build().toUriString())
				.isEqualTo(baseUrl + "/bar");
		assertThat(UriComponentsBuilder.fromUriString(baseUrl + "/foo/../bar").build().toUri().getPath())
				.isEqualTo("/bar");
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
		assertThat(components.toString()).isEmpty();
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
		assertThat(result1.getSchemeSpecificPart()).isNull();

		UriComponents result2 = builder2.build();
		assertThat(result2.getScheme()).isEqualTo("http");
		assertThat(result2.getUserInfo()).isEqualTo("user:pwd");
		assertThat(result2.getHost()).isEqualTo("e1.com");
		assertThat(result2.getPath()).isEqualTo("/p1/%s/%s", vars.get("ps1"), vars.get("ps2"));
		assertThat(result2.getQuery()).isEqualTo("q1");
		assertThat(result2.getFragment()).isEqualTo("f1");
		assertThat(result1.getSchemeSpecificPart()).isNull();
	}

	@Test // gh-26466
	void encodeTemplateWithInvalidPlaceholderSyntax() {

		BiConsumer<String, String> tester = (in, out) ->
				assertThat(UriComponentsBuilder.fromUriString(in).encode().toUriString()).isEqualTo(out);

		// empty
		tester.accept("{}", "%7B%7D");
		tester.accept("{ \t}", "%7B%20%7D");
		tester.accept("/a{}b", "/a%7B%7Db");
		tester.accept("/a{ \t}b", "/a%7B%20%7Db");

		// nested, matching
		tester.accept("{foo{}}", "%7Bfoo%7B%7D%7D");
		tester.accept("{foo{bar}baz}", "%7Bfoo%7Bbar%7Dbaz%7D");
		tester.accept("/a{foo{}}b", "/a%7Bfoo%7B%7D%7Db");
		tester.accept("/a{foo{bar}baz}b", "/a%7Bfoo%7Bbar%7Dbaz%7Db");

		// mismatched
		tester.accept("{foo{{}", "%7Bfoo%7B%7B%7D");
		tester.accept("{foo}}", "{foo}%7D");
		tester.accept("/a{foo{{}bar", "/a%7Bfoo%7B%7B%7Dbar");
		tester.accept("/a{foo}}b", "/a{foo}%7Db");

		// variable with regex
		tester.accept("{year:\\d{1,4}}", "{year:\\d{1,4}}");
		tester.accept("/a{year:\\d{1,4}}b", "/a{year:\\d{1,4}}b");
	}

	@Test  // SPR-16364
	void uriComponentsNotEqualAfterNormalization() {
		UriComponents uri1 = UriComponentsBuilder.fromUriString("http://test.com").build().normalize();
		UriComponents uri2 = UriComponentsBuilder.fromUriString("http://test.com/").build();

		assertThat(uri1.getPathSegments()).isEmpty();
		assertThat(uri2.getPathSegments()).isEmpty();
		assertThat(uri2).isNotEqualTo(uri1);
	}

	@Test  // SPR-17256
	void uriComponentsWithMergedQueryParams() {
		String uri = UriComponentsBuilder.fromUriString("http://localhost:8081")
				.uriComponents(UriComponentsBuilder.fromUriString("/{path}?sort={sort}").build())
				.queryParam("sort", "another_value").build().toString();

		assertThat(uri).isEqualTo("http://localhost:8081/{path}?sort={sort}&sort=another_value");
	}

	@Test  // SPR-17630
	void toUriStringWithCurlyBraces() {
		assertThat(UriComponentsBuilder.fromUriString("/path?q={asa}asa").toUriString()).isEqualTo("/path?q=%7Basa%7Dasa");
	}

	@Test  // gh-26012
	void verifyDoubleSlashReplacedWithSingleOne() {
		String path = UriComponentsBuilder.fromPath("/home/").path("/path").build().getPath();
		assertThat(path).isEqualTo("/home/path");
	}

	@Test
	void validPort() {
		UriComponents uriComponents = UriComponentsBuilder.fromUriString("http://localhost:52567/path").build();
		assertThat(uriComponents.getPort()).isEqualTo(52567);
		assertThat(uriComponents.getPath()).isEqualTo("/path");

		uriComponents = UriComponentsBuilder.fromUriString("http://localhost:52567?trace=false").build();
		assertThat(uriComponents.getPort()).isEqualTo(52567);
		assertThat(uriComponents.getQuery()).isEqualTo("trace=false");

		uriComponents = UriComponentsBuilder.fromUriString("http://localhost:52567#fragment").build();
		assertThat(uriComponents.getPort()).isEqualTo(52567);
		assertThat(uriComponents.getFragment()).isEqualTo("fragment");
	}

	@Test
	void verifyInvalidPort() {
		String url = "http://localhost:XXX/path";
		assertThatIllegalArgumentException()
				.isThrownBy(() -> UriComponentsBuilder.fromUriString(url).build().toUri());
		assertThatIllegalArgumentException()
				.isThrownBy(() -> UriComponentsBuilder.fromUriString(url).build().toUri());
	}

	@Test  // gh-27039
	void expandPortAndPathWithoutSeparator() {
		URI uri = UriComponentsBuilder
				.fromUriString("ws://localhost:{port}/{path}")
				.buildAndExpand(7777, "test")
				.toUri();
		assertThat(uri.toString()).isEqualTo("ws://localhost:7777/test");
	}

}
