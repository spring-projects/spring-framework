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
package org.springframework.http.server.reactive;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * Unit tests for {@link DefaultPathSegmentContainer}.
 * @author Rossen Stoyanchev
 */
public class DefaultPathSegmentContainerTests {

	@Test
	public void pathSegment() throws Exception {
		// basic
		testPathSegment("cars", "", "cars", "cars", false, new LinkedMultiValueMap<>());

		// empty
		testPathSegment("", "", "", "", true, new LinkedMultiValueMap<>());

		// spaces
		testPathSegment("%20%20", "", "%20%20", "  ", true, new LinkedMultiValueMap<>());
		testPathSegment("%20a%20", "", "%20a%20", " a ", false, new LinkedMultiValueMap<>());
	}

	@Test
	public void pathSegmentParams() throws Exception {
		// basic
		LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("colors", "red");
		params.add("colors", "blue");
		params.add("colors", "green");
		params.add("year", "2012");
		testPathSegment("cars", ";colors=red,blue,green;year=2012", "cars", "cars", false, params);

		// trailing semicolon
		params = new LinkedMultiValueMap<>();
		params.add("p", "1");
		testPathSegment("path", ";p=1;", "path", "path", false, params);

		// params with spaces
		params = new LinkedMultiValueMap<>();
		params.add("param name", "param value");
		testPathSegment("path", ";param%20name=param%20value;%20", "path", "path", false, params);

		// empty params
		params = new LinkedMultiValueMap<>();
		params.add("p", "1");
		testPathSegment("path", ";;;%20;%20;p=1;%20", "path", "path", false, params);
	}

	private void testPathSegment(String pathSegment, String semicolonContent,
			String value, String valueDecoded, boolean empty, MultiValueMap<String, String> params) {

		PathSegment segment = PathSegment.parse(pathSegment + semicolonContent, UTF_8);

		assertEquals("value: '" + pathSegment + "'", value, segment.value());
		assertEquals("valueDecoded: '" + pathSegment + "'", valueDecoded, segment.valueDecoded());
		assertEquals("isEmpty: '" + pathSegment + "'", empty, segment.isEmpty());
		assertEquals("semicolonContent: '" + pathSegment + "'", semicolonContent, segment.semicolonContent());
		assertEquals("params: '" + pathSegment + "'", params, segment.parameters());
	}

	@Test
	public void path() throws Exception {
		// basic
		testPath("/a/b/c", "/a/b/c", false, true, Arrays.asList("a", "b", "c"), false);

		// root path
		testPath("/", "/", false, true, Collections.emptyList(), false);

		// empty path
		testPath("",   "", true, false, Collections.emptyList(), false);
		testPath("%20%20",   "%20%20", true, false, Collections.singletonList("%20%20"), false);

		// trailing slash
		testPath("/a/b/", "/a/b/", false, true, Arrays.asList("a", "b"), true);
		testPath("/a/b//", "/a/b//", false, true, Arrays.asList("a", "b", ""), true);

		// extra slashes and spaces
		testPath("/%20", "/%20", false, true, Collections.singletonList("%20"), false);
		testPath("//%20/%20", "//%20/%20", false, true, Arrays.asList("", "%20", "%20"), false);
	}

	private void testPath(String input, String value, boolean empty, boolean absolute,
			List<String> segments, boolean trailingSlash) {

		PathSegmentContainer path = PathSegmentContainer.parse(input, UTF_8);

		List<String> segmentValues = path.pathSegments().stream().map(PathSegment::value)
				.collect(Collectors.toList());

		assertEquals("value: '" + input + "'", value, path.value());
		assertEquals("empty: '" + input + "'", empty, path.isEmpty());
		assertEquals("isAbsolute: '" + input + "'", absolute, path.isAbsolute());
		assertEquals("pathSegments: " + input, segments, segmentValues);
		assertEquals("hasTrailingSlash: '" + input + "'", trailingSlash, path.hasTrailingSlash());
	}

	@Test
	public void subPath() throws Exception {
		// basic
		PathSegmentContainer path = PathSegmentContainer.parse("/a/b/c", UTF_8);
		assertSame(path, PathSegmentContainer.subPath(path, 0));
		assertEquals("/b/c", PathSegmentContainer.subPath(path, 1).value());
		assertEquals("/c", PathSegmentContainer.subPath(path, 2).value());

		// root path
		path = PathSegmentContainer.parse("/", UTF_8);
		assertEquals("/", PathSegmentContainer.subPath(path, 0).value());

		// trailing slash
		path = PathSegmentContainer.parse("/a/b/", UTF_8);
		assertEquals("/b/", PathSegmentContainer.subPath(path, 1).value());
	}

}
