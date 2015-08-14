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

package org.springframework.test.web.servlet.result;

import org.hamcrest.Matchers;

import org.junit.Test;

import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.StubMvcResult;

/**
 * Tests for {@link JsonPathResultMatchers}.
 *
 * @author Rossen Stoyanchev
 * @author Craig Andrews
 * @author Sam Brannen
 */
public class JsonPathResultMatchersTests {

	private static final String RESPONSE_CONTENT = "{\"foo\": \"bar\", \"qux\": [\"baz\"], \"emptyArray\": [], \"icanhaz\": true, \"howmanies\": 5, \"cheeseburger\": {\"pickles\": true}, \"emptyMap\": {} }";

	private static final StubMvcResult stubMvcResult;

	static {
		try {
			MockHttpServletResponse response = new MockHttpServletResponse();
			response.addHeader("Content-Type", "application/json");
			response.getWriter().print(new String(RESPONSE_CONTENT.getBytes("ISO-8859-1")));
			stubMvcResult = new StubMvcResult(null, null, null, null, null, null, response);
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}


	@Test
	public void value() throws Exception {
		new JsonPathResultMatchers("$.foo").value("bar").match(stubMvcResult);
	}

	@Test(expected = AssertionError.class)
	public void valueNoMatch() throws Exception {
		new JsonPathResultMatchers("$.foo").value("bogus").match(stubMvcResult);
	}

	@Test
	public void valueWithMatcher() throws Exception {
		new JsonPathResultMatchers("$.foo").value(Matchers.equalTo("bar")).match(stubMvcResult);
	}

	@Test(expected = AssertionError.class)
	public void valueWithMatcherNoMatch() throws Exception {
		new JsonPathResultMatchers("$.foo").value(Matchers.equalTo("bogus")).match(stubMvcResult);
	}

	@Test
	public void exists() throws Exception {
		new JsonPathResultMatchers("$.foo").exists().match(stubMvcResult);
	}

	@Test(expected = AssertionError.class)
	public void existsNoMatch() throws Exception {
		new JsonPathResultMatchers("$.bogus").exists().match(stubMvcResult);
	}

	@Test
	public void doesNotExist() throws Exception {
		new JsonPathResultMatchers("$.bogus").doesNotExist().match(stubMvcResult);
	}

	@Test(expected = AssertionError.class)
	public void doesNotExistNoMatch() throws Exception {
		new JsonPathResultMatchers("$.foo").doesNotExist().match(stubMvcResult);
	}

	@Test
	public void isArray() throws Exception {
		new JsonPathResultMatchers("$.qux").isArray().match(stubMvcResult);
	}

	@Test
	public void isArrayForAnEmptyArray() throws Exception {
		new JsonPathResultMatchers("$.emptyArray").isArray().match(stubMvcResult);
	}

	@Test(expected = AssertionError.class)
	public void isArrayNoMatch() throws Exception {
		new JsonPathResultMatchers("$.bar").isArray().match(stubMvcResult);
	}

	@Test
	public void isMap() throws Exception {
		new JsonPathResultMatchers("$.cheeseburger").isMap().match(stubMvcResult);
	}

	@Test
	public void isMapForAnEmptyMap() throws Exception {
		new JsonPathResultMatchers("$.emptyMap").isMap().match(stubMvcResult);
	}

	@Test(expected = AssertionError.class)
	public void isMapNoMatch() throws Exception {
		new JsonPathResultMatchers("$.foo").isMap().match(stubMvcResult);
	}

	@Test
	public void isBoolean() throws Exception {
		new JsonPathResultMatchers("$.icanhaz").isBoolean().match(stubMvcResult);
	}

	@Test(expected = AssertionError.class)
	public void isBooleanNoMatch() throws Exception {
		new JsonPathResultMatchers("$.foo").isBoolean().match(stubMvcResult);
	}

	@Test
	public void isNumber() throws Exception {
		new JsonPathResultMatchers("$.howmanies").isNumber().match(stubMvcResult);
	}

	@Test(expected = AssertionError.class)
	public void isNumberNoMatch() throws Exception {
		new JsonPathResultMatchers("$.foo").isNumber().match(stubMvcResult);
	}

	@Test
	public void isString() throws Exception {
		new JsonPathResultMatchers("$.foo").isString().match(stubMvcResult);
	}

	@Test(expected = AssertionError.class)
	public void isStringNoMatch() throws Exception {
		new JsonPathResultMatchers("$.qux").isString().match(stubMvcResult);
	}

}
