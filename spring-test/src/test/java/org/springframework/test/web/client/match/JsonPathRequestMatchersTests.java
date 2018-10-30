/*
 * Copyright 2002-2018 the original author or authors.
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
			"'str':         'foo',           " + //
			"'num':         5,               " + //
			"'bool':        true,            " + //
			"'arr':         [42],            " + //
			"'colorMap':    {'red': 'rojo'}, " + //
			"'emptyString': '',              " + //
			"'emptyArray':  [],              " + //
			"'emptyMap':    {}               " + //
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


	@Test(expected = AssertionError.class)
	public void valueWithMismatch() throws Exception {
		new JsonPathRequestMatchers("$.str").value("bogus").match(request);
	}

	@Test
	public void valueWithDirectMatch() throws Exception {
		new JsonPathRequestMatchers("$.str").value("foo").match(request);
	}

	@Test // SPR-14498
	public void valueWithNumberConversion() throws Exception {
		new JsonPathRequestMatchers("$.num").value(5.0f).match(request);
	}

	@Test
	public void valueWithMatcher() throws Exception {
		new JsonPathRequestMatchers("$.str").value(equalTo("foo")).match(request);
	}

	@Test // SPR-14498
	public void valueWithMatcherAndNumberConversion() throws Exception {
		new JsonPathRequestMatchers("$.num").value(equalTo(5.0f), Float.class).match(request);
	}

	@Test(expected = AssertionError.class)
	public void valueWithMatcherAndMismatch() throws Exception {
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
	public void isEmptyForAnEmptyString() throws Exception {
		new JsonPathRequestMatchers("$.emptyString").isEmpty().match(request);
	}

	@Test
	public void isEmptyForAnEmptyArray() throws Exception {
		new JsonPathRequestMatchers("$.emptyArray").isEmpty().match(request);
	}

	@Test
	public void isEmptyForAnEmptyMap() throws Exception {
		new JsonPathRequestMatchers("$.emptyMap").isEmpty().match(request);
	}

	@Test
	public void isNotEmptyForString() throws Exception {
		new JsonPathRequestMatchers("$.str").isNotEmpty().match(request);
	}

	@Test
	public void isNotEmptyForNumber() throws Exception {
		new JsonPathRequestMatchers("$.num").isNotEmpty().match(request);
	}

	@Test
	public void isNotEmptyForBoolean() throws Exception {
		new JsonPathRequestMatchers("$.bool").isNotEmpty().match(request);
	}

	@Test
	public void isNotEmptyForArray() throws Exception {
		new JsonPathRequestMatchers("$.arr").isNotEmpty().match(request);
	}

	@Test
	public void isNotEmptyForMap() throws Exception {
		new JsonPathRequestMatchers("$.colorMap").isNotEmpty().match(request);
	}

	@Test(expected = AssertionError.class)
	public void isNotEmptyForAnEmptyString() throws Exception {
		new JsonPathRequestMatchers("$.emptyString").isNotEmpty().match(request);
	}

	@Test(expected = AssertionError.class)
	public void isNotEmptyForAnEmptyArray() throws Exception {
		new JsonPathRequestMatchers("$.emptyArray").isNotEmpty().match(request);
	}

	@Test(expected = AssertionError.class)
	public void isNotEmptyForAnEmptyMap() throws Exception {
		new JsonPathRequestMatchers("$.emptyMap").isNotEmpty().match(request);
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

	@Test
	public void isMap() throws Exception {
		new JsonPathRequestMatchers("$.colorMap").isMap().match(request);
	}

	@Test
	public void isMapForAnEmptyMap() throws Exception {
		new JsonPathRequestMatchers("$.emptyMap").isMap().match(request);
	}

	@Test(expected = AssertionError.class)
	public void isMapNoMatch() throws Exception {
		new JsonPathRequestMatchers("$.str").isMap().match(request);
	}

	@Test
	public void isBoolean() throws Exception {
		new JsonPathRequestMatchers("$.bool").isBoolean().match(request);
	}

	@Test(expected = AssertionError.class)
	public void isBooleanNoMatch() throws Exception {
		new JsonPathRequestMatchers("$.str").isBoolean().match(request);
	}

	@Test
	public void isNumber() throws Exception {
		new JsonPathRequestMatchers("$.num").isNumber().match(request);
	}

	@Test(expected = AssertionError.class)
	public void isNumberNoMatch() throws Exception {
		new JsonPathRequestMatchers("$.str").isNumber().match(request);
	}

	@Test
	public void isString() throws Exception {
		new JsonPathRequestMatchers("$.str").isString().match(request);
	}

	@Test(expected = AssertionError.class)
	public void isStringNoMatch() throws Exception {
		new JsonPathRequestMatchers("$.arr").isString().match(request);
	}

}
