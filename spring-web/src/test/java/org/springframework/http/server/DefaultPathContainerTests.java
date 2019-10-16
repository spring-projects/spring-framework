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
package org.springframework.http.server;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import org.springframework.http.server.PathContainer.PathSegment;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DefaultPathContainer}.
 * @author Rossen Stoyanchev
 */
public class DefaultPathContainerTests {

	@Test
	public void pathSegment() {
		// basic
		testPathSegment("cars", "cars", new LinkedMultiValueMap<>());

		// empty
		testPathSegment("", "", new LinkedMultiValueMap<>());

		// spaces
		testPathSegment("%20%20", "  ", new LinkedMultiValueMap<>());
		testPathSegment("%20a%20", " a ", new LinkedMultiValueMap<>());
	}

	@Test
	public void pathSegmentParams() throws Exception {
		// basic
		LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("colors", "red");
		params.add("colors", "blue");
		params.add("colors", "green");
		params.add("year", "2012");
		testPathSegment("cars;colors=red,blue,green;year=2012", "cars", params);

		// trailing semicolon
		params = new LinkedMultiValueMap<>();
		params.add("p", "1");
		testPathSegment("path;p=1;", "path", params);

		// params with spaces
		params = new LinkedMultiValueMap<>();
		params.add("param name", "param value");
		testPathSegment("path;param%20name=param%20value;%20", "path", params);

		// empty params
		params = new LinkedMultiValueMap<>();
		params.add("p", "1");
		testPathSegment("path;;;%20;%20;p=1;%20", "path", params);
	}

	private void testPathSegment(String rawValue, String valueToMatch, MultiValueMap<String, String> params) {

		PathContainer container = PathContainer.parsePath(rawValue);

		if ("".equals(rawValue)) {
			assertThat(container.elements().size()).isEqualTo(0);
			return;
		}

		assertThat(container.elements().size()).isEqualTo(1);
		PathSegment segment = (PathSegment) container.elements().get(0);

		assertThat(segment.value()).as("value: '" + rawValue + "'").isEqualTo(rawValue);
		assertThat(segment.valueToMatch()).as("valueToMatch: '" + rawValue + "'").isEqualTo(valueToMatch);
		assertThat(segment.parameters()).as("params: '" + rawValue + "'").isEqualTo(params);
	}

	@Test
	public void path() {
		// basic
		testPath("/a/b/c", "/a/b/c", Arrays.asList("/", "a", "/", "b", "/", "c"));

		// root path
		testPath("/", "/", Collections.singletonList("/"));

		// empty path
		testPath("", "", Collections.emptyList());
		testPath("%20%20", "%20%20", Collections.singletonList("%20%20"));

		// trailing slash
		testPath("/a/b/", "/a/b/", Arrays.asList("/", "a", "/", "b", "/"));
		testPath("/a/b//", "/a/b//", Arrays.asList("/", "a", "/", "b", "/", "/"));

		// extra slashes and spaces
		testPath("/%20", "/%20", Arrays.asList("/", "%20"));
		testPath("//%20/%20", "//%20/%20", Arrays.asList("/", "/", "%20", "/", "%20"));
	}

	private void testPath(String input, PathContainer.Options options, String value, List<String> expectedElements) {
		PathContainer path = PathContainer.parsePath(input, options);

		assertThat(path.value()).as("value: '" + input + "'").isEqualTo(value);
		assertThat(path.elements().stream().map(PathContainer.Element::value).collect(Collectors.toList()))
				.as("elements: " + input).isEqualTo(expectedElements);
	}

	private void testPath(String input, String value, List<String> expectedElements) {
		testPath(input, PathContainer.Options.HTTP_PATH, value, expectedElements);
	}

	@Test
	public void subPath() {
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
	public void pathWithCustomSeparator() {
		PathContainer path = PathContainer.parsePath("a.b%2Eb.c", PathContainer.Options.MESSAGE_ROUTE);

		List<String> decodedSegments = path.elements().stream()
				.filter(e -> e instanceof PathSegment)
				.map(e -> ((PathSegment) e).valueToMatch())
				.collect(Collectors.toList());

		assertThat(decodedSegments).isEqualTo(Arrays.asList("a", "b.b", "c"));
	}

}
