/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.http.server;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import org.springframework.http.server.PathContainer.Element;
import org.springframework.http.server.PathContainer.Options;
import org.springframework.http.server.PathContainer.PathSegment;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit tests for {@link DefaultPathContainer}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
class DefaultPathContainerTests {

	@Test
	void pathSegment() {
		// basic
		testPathSegment("cars", "cars", emptyMap());

		// empty
		testPathSegment("", "", emptyMap());

		// spaces
		testPathSegment("%20%20", "  ", emptyMap());
		testPathSegment("%20a%20", " a ", emptyMap());
	}

	@Test
	void pathSegmentParams() {
		// basic
		LinkedMultiValueMap<String, String> params = emptyMap();
		params.add("colors", "red");
		params.add("colors", "blue");
		params.add("colors", "green");
		params.add("year", "2012");
		testPathSegment("cars;colors=red,blue,green;year=2012", "cars", params);

		// trailing semicolon
		params = emptyMap();
		params.add("p", "1");
		testPathSegment("path;p=1;", "path", params);

		// params with spaces
		params = emptyMap();
		params.add("param name", "param value");
		testPathSegment("path;param%20name=param%20value;%20", "path", params);

		// empty params
		params = emptyMap();
		params.add("p", "1");
		testPathSegment("path;;;%20;%20;p=1;%20", "path", params);
	}

	@Test
	void pathSegmentParamsAreImmutable() {
		assertPathSegmentParamsAreImmutable("cars", emptyMap(), Options.HTTP_PATH);

		LinkedMultiValueMap<String, String> params = emptyMap();
		params.add("colors", "red");
		params.add("colors", "blue");
		params.add("colors", "green");
		assertPathSegmentParamsAreImmutable(";colors=red,blue,green", params, Options.HTTP_PATH);

		assertPathSegmentParamsAreImmutable(";colors=red,blue,green", emptyMap(), Options.MESSAGE_ROUTE);
	}

	private void assertPathSegmentParamsAreImmutable(String path, LinkedMultiValueMap<String, String> params, Options options) {
		PathContainer container = PathContainer.parsePath(path, options);
		assertThat(container.elements()).hasSize(1);

		PathSegment segment = (PathSegment) container.elements().get(0);
		MultiValueMap<String, String> segmentParams = segment.parameters();
		assertThat(segmentParams).isEqualTo(params);
		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> segmentParams.add("enigma", "boom"));
	}

	private void testPathSegment(String rawValue, String valueToMatch, MultiValueMap<String, String> params) {
		PathContainer container = PathContainer.parsePath(rawValue);

		if ("".equals(rawValue)) {
			assertThat(container.elements()).isEmpty();
			return;
		}

		assertThat(container.elements()).hasSize(1);
		PathSegment segment = (PathSegment) container.elements().get(0);

		assertThat(segment.value()).as("value: '" + rawValue + "'").isEqualTo(rawValue);
		assertThat(segment.valueToMatch()).as("valueToMatch: '" + rawValue + "'").isEqualTo(valueToMatch);
		assertThat(segment.parameters()).as("params: '" + rawValue + "'").isEqualTo(params);
	}

	@Test
	void path() {
		// basic
		testPath("/a/b/c", "/a/b/c", "/", "a", "/", "b", "/", "c");

		// root path
		testPath("/", "/", "/");

		// empty path
		testPath("", "");
		testPath("%20%20", "%20%20", "%20%20");

		// trailing slash
		testPath("/a/b/", "/a/b/", "/", "a", "/", "b", "/");
		testPath("/a/b//", "/a/b//", "/", "a", "/", "b", "/", "/");

		// extra slashes and spaces
		testPath("/%20", "/%20", "/", "%20");
		testPath("//%20/%20", "//%20/%20", "/", "/", "%20", "/", "%20");
	}

	private void testPath(String input, String value, String... expectedElements) {
		PathContainer path = PathContainer.parsePath(input, Options.HTTP_PATH);

		assertThat(path.value()).as("value: '" + input + "'").isEqualTo(value);
		assertThat(path.elements()).map(Element::value).as("elements: " + input)
				.containsExactly(expectedElements);
	}

	@Test
	void subPath() {
		// basic
		PathContainer path = PathContainer.parsePath("/a/b/c");
		assertThat(path.subPath(0)).isSameAs(path);
		assertThat(path.subPath(2).value()).isEqualTo("/b/c");
		assertThat(path.subPath(4).value()).isEqualTo("/c");

		// root path
		path = PathContainer.parsePath("/");
		assertThat(path.subPath(0).value()).isEqualTo("/");

		// trailing slash
		path = PathContainer.parsePath("/a/b/");
		assertThat(path.subPath(2).value()).isEqualTo("/b/");
	}

	@Test // gh-23310
	void pathWithCustomSeparator() {
		PathContainer path = PathContainer.parsePath("a.b%2Eb.c", Options.MESSAGE_ROUTE);

		Stream<String> decodedSegments = path.elements().stream()
				.filter(PathSegment.class::isInstance)
				.map(PathSegment.class::cast)
				.map(PathSegment::valueToMatch);

		assertThat(decodedSegments).containsExactly("a", "b.b", "c");
	}

	private static LinkedMultiValueMap<String, String> emptyMap() {
		return new LinkedMultiValueMap<>();
	}

}
