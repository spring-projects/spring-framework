/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.test.web.client.match;

import java.io.IOException;

import org.junit.Test;

import org.springframework.mock.http.client.MockClientHttpRequest;

import static org.hamcrest.CoreMatchers.*;

/**
 * Unit tests for {@link JsonPathRequestMatchers}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
public class JsonPathRequestMatchersTests {

	private static final String REQUEST_CONTENT = "{" + //
			"'str':        'foo',           " + //
			"'num':        5,               " + //
			"'bool':       true,            " + //
			"'arr':        [42],            " + //
			"'emptyArray': [],              " + //
			"'colorMap':   {'red': 'rojo'}, " + //
			"'emptyMap':   {}               " + //
	"}";

	private static final MockClientHttpRequest request = new MockClientHttpRequest();

	static {
		try {
			request.getBody().write(REQUEST_CONTENT.getBytes());
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}


	@Test
	public void value() throws Exception {
		new JsonPathRequestMatchers("$.str").value("foo").match(request);
	}

	@Test(expected = AssertionError.class)
	public void valueNoMatch() throws Exception {
		new JsonPathRequestMatchers("$.str").value("bogus").match(request);
	}

	@Test
	public void valueWithMatcher() throws Exception {
		new JsonPathRequestMatchers("$.str").value(equalTo("foo")).match(request);
	}

	@Test(expected = AssertionError.class)
	public void valueWithMatcherNoMatch() throws Exception {
		new JsonPathRequestMatchers("$.str").value(equalTo("bogus")).match(request);
	}

	@Test
	public void exists() throws Exception {
		new JsonPathRequestMatchers("$.str").exists().match(request);
	}

	@Test
	public void existsForAnEmptyArray() throws Exception {
		new JsonPathRequestMatchers("$.emptyArray").exists().match(request);
	}

	@Test
	public void existsForAnEmptyMap() throws Exception {
		new JsonPathRequestMatchers("$.emptyMap").exists().match(request);
	}

	@Test(expected = AssertionError.class)
	public void existsNoMatch() throws Exception {
		new JsonPathRequestMatchers("$.bogus").exists().match(request);
	}

	@Test
	public void doesNotExist() throws Exception {
		new JsonPathRequestMatchers("$.bogus").doesNotExist().match(request);
	}

	@Test(expected = AssertionError.class)
	public void doesNotExistNoMatch() throws Exception {
		new JsonPathRequestMatchers("$.str").doesNotExist().match(request);
	}

	@Test(expected = AssertionError.class)
	public void doesNotExistForAnEmptyArray() throws Exception {
		new JsonPathRequestMatchers("$.emptyArray").doesNotExist().match(request);
	}

	@Test(expected = AssertionError.class)
	public void doesNotExistForAnEmptyMap() throws Exception {
		new JsonPathRequestMatchers("$.emptyMap").doesNotExist().match(request);
	}

	@Test
	public void isArray() throws Exception {
		new JsonPathRequestMatchers("$.arr").isArray().match(request);
	}

	@Test
	public void isArrayForAnEmptyArray() throws Exception {
		new JsonPathRequestMatchers("$.emptyArray").isArray().match(request);
	}

	@Test(expected = AssertionError.class)
	public void isArrayNoMatch() throws Exception {
		new JsonPathRequestMatchers("$.str").isArray().match(request);
	}

}
