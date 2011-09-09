/*
 * Copyright 2002-2011 the original author or authors.
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

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.junit.Assert.*;

/** @author Arjen Poutsma */
public class UriComponentsTests {

	private UriComponents components;

	@Before
	public void createComponents() {
		components = new UriComponents();
	}
	
	@Test
	public void pathSegments() {
		String path = "/foo/bar";
		components.setPath(path);
		List<String> expected = Arrays.asList("foo", "bar");

		List<String> pathSegments = components.getPathSegments();
		assertEquals(expected, pathSegments);

		components.setPath(null);

		components.setPathSegments(expected);
		assertEquals(path, components.getPath());
	}
	
	@Test
	public void queryParams() {
		String query = "foo=bar&foo=baz&qux";
		components.setQuery(query);
		MultiValueMap<String, String> expected = new LinkedMultiValueMap<String, String>(1);
		expected.put("foo", Arrays.asList("bar", "baz"));
		expected.set("qux", null);

		MultiValueMap<String, String> result  = components.getQueryParams();
		assertEquals(expected, result);

		components.setQuery(null);

		components.setQueryParams(expected);
		assertEquals(query, components.getQuery());

	}

}
