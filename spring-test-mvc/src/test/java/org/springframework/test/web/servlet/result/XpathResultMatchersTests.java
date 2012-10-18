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
package org.springframework.test.web.servlet.result;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.StubMvcResult;
import org.springframework.test.web.servlet.result.XpathResultMatchers;

/**
 * Tests for {@link XpathResultMatchers}.
 *
 * @author Rossen Stoyanchev
 */
public class XpathResultMatchersTests {

	@Test
	public void testNodeMatcher() throws Exception {
		new XpathResultMatchers("/foo/bar", null).node(Matchers.notNullValue()).match(getStubMvcResult());
	}

	@Test(expected=AssertionError.class)
	public void testNodeMatcherNoMatch() throws Exception {
		new XpathResultMatchers("/foo/bar", null).node(Matchers.nullValue()).match(getStubMvcResult());
	}

	@Test
	public void testExists() throws Exception {
		new XpathResultMatchers("/foo/bar", null).exists().match(getStubMvcResult());
	}

	@Test(expected=AssertionError.class)
	public void testExistsNoMatch() throws Exception {
		new XpathResultMatchers("/foo/Bar", null).exists().match(getStubMvcResult());
	}

	@Test
	public void testDoesNotExist() throws Exception {
		new XpathResultMatchers("/foo/Bar", null).doesNotExist().match(getStubMvcResult());
	}

	@Test(expected=AssertionError.class)
	public void testDoesNotExistNoMatch() throws Exception {
		new XpathResultMatchers("/foo/bar", null).doesNotExist().match(getStubMvcResult());
	}

	@Test
	public void testNodeCount() throws Exception {
		new XpathResultMatchers("/foo/bar", null).nodeCount(2).match(getStubMvcResult());
	}

	@Test(expected=AssertionError.class)
	public void testNodeCountNoMatch() throws Exception {
		new XpathResultMatchers("/foo/bar", null).nodeCount(1).match(getStubMvcResult());
	}

	@Test
	public void testString() throws Exception {
		new XpathResultMatchers("/foo/bar[1]", null).string("111").match(getStubMvcResult());
	}

	@Test(expected=AssertionError.class)
	public void testStringNoMatch() throws Exception {
		new XpathResultMatchers("/foo/bar[1]", null).string("112").match(getStubMvcResult());
	}

	@Test
	public void testNumber() throws Exception {
		new XpathResultMatchers("/foo/bar[1]", null).number(111.0).match(getStubMvcResult());
	}

	@Test(expected=AssertionError.class)
	public void testNumberNoMatch() throws Exception {
		new XpathResultMatchers("/foo/bar[1]", null).number(111.1).match(getStubMvcResult());
	}

	@Test
	public void testBoolean() throws Exception {
		new XpathResultMatchers("/foo/bar[2]", null).booleanValue(true).match(getStubMvcResult());
	}

	@Test(expected=AssertionError.class)
	public void testBooleanNoMatch() throws Exception {
		new XpathResultMatchers("/foo/bar[2]", null).booleanValue(false).match(getStubMvcResult());
	}


	private static final String RESPONSE_CONTENT = "<foo><bar>111</bar><bar>true</bar></foo>";

	private StubMvcResult getStubMvcResult() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.addHeader("Content-Type", "application/json");
		response.getWriter().print(new String(RESPONSE_CONTENT.getBytes("ISO-8859-1")));
		return new StubMvcResult(null, null, null, null, null, null, response);
	}

}
