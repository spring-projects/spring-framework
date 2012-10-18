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
import org.springframework.test.web.servlet.result.ContentResultMatchers;

/**
 * @author Rossen Stoyanchev
 */
public class ContentResultMatchersTests {

	@Test
	public void typeMatches() throws Exception {
		new ContentResultMatchers().contentType("application/json;charset=UTF-8").match(getStubMvcResult());
	}

	@Test(expected=AssertionError.class)
	public void typeNoMatch() throws Exception {
		new ContentResultMatchers().contentType("text/plain").match(getStubMvcResult());
	}
	
	@Test
	public void encoding() throws Exception {
		new ContentResultMatchers().encoding("UTF-8").match(getStubMvcResult());
	}
	
	@Test(expected=AssertionError.class)
	public void encodingNoMatch() throws Exception {
		new ContentResultMatchers().encoding("ISO-8859-1").match(getStubMvcResult());
	}
	
	@Test
	public void string() throws Exception {
		new ContentResultMatchers().string(new String(CONTENT.getBytes("UTF-8"))).match(getStubMvcResult());
	}
	
	@Test(expected=AssertionError.class)
	public void stringNoMatch() throws Exception {
		new ContentResultMatchers().encoding("bogus").match(getStubMvcResult());
	}
	
	@Test
	public void stringMatcher() throws Exception {
		String content = new String(CONTENT.getBytes("UTF-8"));
		new ContentResultMatchers().string(Matchers.equalTo(content)).match(getStubMvcResult());
	}
	
	@Test(expected=AssertionError.class)
	public void stringMatcherNoMatch() throws Exception {
		new ContentResultMatchers().string(Matchers.equalTo("bogus")).match(getStubMvcResult());
	}
	
	@Test
	public void bytes() throws Exception {
		new ContentResultMatchers().bytes(CONTENT.getBytes("UTF-8")).match(getStubMvcResult());
	}
	
	@Test(expected=AssertionError.class)
	public void bytesNoMatch() throws Exception {
		new ContentResultMatchers().bytes("bogus".getBytes()).match(getStubMvcResult());
	}

	
	private static final String CONTENT = "{\"foo\":\"bar\"}";

	private StubMvcResult getStubMvcResult() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.addHeader("Content-Type", "application/json; charset=UTF-8");
		response.getWriter().print(new String(CONTENT.getBytes("UTF-8")));
		return new StubMvcResult(null, null, null, null, null, null, response);
	}

}
