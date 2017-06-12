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

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link DefaultRequestPath}.
 * @author Rossen Stoyanchev
 */
public class DefaultRequestPathTests {

	@Test
	public void pathSegment() throws Exception {
		// basic
		testPathSegment("cars", "", "cars", "cars", new LinkedMultiValueMap<>());

		// empty
		testPathSegment("", "", "", "", new LinkedMultiValueMap<>());

		// spaces
		testPathSegment("%20", "", "%20", " ", new LinkedMultiValueMap<>());
		testPathSegment("%20a%20", "", "%20a%20", " a ", new LinkedMultiValueMap<>());
	}

	@Test
	public void pathSegmentWithParams() throws Exception {
		// basic
		LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("colors", "red");
		params.add("colors", "blue");
		params.add("colors", "green");
		params.add("year", "2012");
		testPathSegment("cars", ";colors=red,blue,green;year=2012", "cars", "cars", params);

		// trailing semicolon
		params = new LinkedMultiValueMap<>();
		params.add("p", "1");
		testPathSegment("path", ";p=1;", "path", "path", params);

		// params with spaces
		params = new LinkedMultiValueMap<>();
		params.add("param name", "param value");
		testPathSegment("path", ";param%20name=param%20value;%20", "path", "path", params);

		// empty params
		params = new LinkedMultiValueMap<>();
		params.add("p", "1");
		testPathSegment("path", ";;;%20;%20;p=1;%20", "path", "path", params);
	}

	@Test
	public void path() throws Exception {
		// basic
		testPath("/a/b/c", "/a/b/c", Arrays.asList("a", "b", "c"));

		// root path
		testPath("/%20", "/%20", Collections.singletonList("%20"));
		testPath("",   "", Collections.emptyList());
		testPath("%20", "", Collections.emptyList());

		// trailing slash
		testPath("/a/b/", "/a/b/", Arrays.asList("a", "b", ""));
		testPath("/a/b//", "/a/b//", Arrays.asList("a", "b", "", ""));

		// extra slashes ande spaces
		testPath("//%20/%20", "//%20/%20", Arrays.asList("", "%20", "%20"));
	}

	@Test
	public void contextPath() throws Exception {
		URI uri = URI.create("http://localhost:8080/app/a/b/c");
		RequestPath path = new DefaultRequestPath(uri, "/app", StandardCharsets.UTF_8);

		PathSegmentContainer contextPath = path.contextPath();
		assertEquals("/app", contextPath.value());
		assertEquals(Collections.singletonList("app"), pathSegmentValues(contextPath));

		PathSegmentContainer pathWithinApplication = path.pathWithinApplication();
		assertEquals("/a/b/c", pathWithinApplication.value());
		assertEquals(Arrays.asList("a", "b", "c"), pathSegmentValues(pathWithinApplication));
	}


	private void testPathSegment(String pathSegment, String semicolonContent,
			String value, String valueDecoded, MultiValueMap<String, String> parameters) {

		URI uri = URI.create("http://localhost:8080/" + pathSegment + semicolonContent);
		PathSegment segment = new DefaultRequestPath(uri, "", StandardCharsets.UTF_8).pathSegments().get(0);

		assertEquals(value, segment.value());
		assertEquals(valueDecoded, segment.valueDecoded());
		assertEquals(semicolonContent, segment.semicolonContent());
		assertEquals(parameters, segment.parameters());
	}

	private void testPath(String input, String value, List<String> segments) {
		URI uri = URI.create("http://localhost:8080" + input);
		RequestPath path = new DefaultRequestPath(uri, "", StandardCharsets.UTF_8);

		assertEquals(value, path.value());
		assertEquals(segments, pathSegmentValues(path));
	}

	private static List<String> pathSegmentValues(PathSegmentContainer path) {
		return path.pathSegments().stream().map(PathSegment::value).collect(Collectors.toList());
	}

}
