/*
 * Copyright 2002-2013 the original author or authors.
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

import org.junit.Test;

import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.StubMvcResult;

/**
 * @author Brian Clozel
 */
public class MockMvcResultMatchersTests {

	@Test
	public void testRedirect() throws Exception {
		MockMvcResultMatchers.redirectedUrl("/resource/1")
				.match(getRedirectedUrlStubMvcResult("/resource/1"));
	}

	@Test( expected = java.lang.AssertionError.class )
	public void testFailRedirect() throws Exception {
		MockMvcResultMatchers.redirectedUrl("/resource/2")
				.match(getRedirectedUrlStubMvcResult("/resource/1"));
	}

	@Test( expected = java.lang.AssertionError.class )
	public void testFailRedirect_NotRedirect() throws Exception {
		MockMvcResultMatchers.redirectedUrl("/resource/1")
				.match(getForwardedUrlStubMvcResult("/resource/1"));
	}

	@Test
	public void testRedirectPattern() throws Exception {
		MockMvcResultMatchers.redirectedUrlPattern("/resource/*")
				.match(getRedirectedUrlStubMvcResult("/resource/1"));
	}

	@Test( expected = java.lang.AssertionError.class)
	public void testFailRedirectPattern() throws Exception {
		MockMvcResultMatchers.redirectedUrlPattern("/resource/")
				.match(getRedirectedUrlStubMvcResult("/resource/1"));
	}

	@Test( expected = java.lang.AssertionError.class)
	public void testFailRedirectPattern_NotRedirect() throws Exception {
		MockMvcResultMatchers.redirectedUrlPattern("/resource/")
				.match(getForwardedUrlStubMvcResult("/resource/1"));
	}

	@Test
	public void testForward() throws Exception {
		MockMvcResultMatchers.forwardedUrl("/api/resource/1")
				.match(getForwardedUrlStubMvcResult("/api/resource/1"));
	}

	@Test( expected = java.lang.AssertionError.class )
	public void testFailForward() throws Exception {
		MockMvcResultMatchers.forwardedUrl("/api/resource/2")
				.match(getForwardedUrlStubMvcResult("/api/resource/1"));
	}

	@Test( expected = java.lang.AssertionError.class )
	public void testFailForward_NotForward() throws Exception {
		MockMvcResultMatchers.forwardedUrl("/api/resource/1")
				.match(getRedirectedUrlStubMvcResult("/api/resource/1"));
	}

	@Test
	public void testForwardEscapedChars() throws Exception {
		MockMvcResultMatchers.forwardedUrl("/api/resource/1?arg=value")
				.match(getForwardedUrlStubMvcResult("/api/resource/1?arg=value"));
	}

	@Test
	public void testForwardPattern() throws Exception {
		MockMvcResultMatchers.forwardedUrlPattern("/api/**/?")
				.match(getForwardedUrlStubMvcResult("/api/resource/1"));
	}

	@Test( expected = java.lang.AssertionError.class )
	public void testFailForwardPattern() throws Exception {
		MockMvcResultMatchers.forwardedUrlPattern("/api/resource/")
				.match(getForwardedUrlStubMvcResult("/api/resource/1"));
	}

	@Test( expected = java.lang.AssertionError.class )
	public void testFailForwardPattern_NotForward() throws Exception {
		MockMvcResultMatchers.forwardedUrlPattern("/api/resource/")
				.match(getRedirectedUrlStubMvcResult("/api/resource/1"));
	}

	private StubMvcResult getRedirectedUrlStubMvcResult(String redirectUrl) throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.sendRedirect(redirectUrl);
		StubMvcResult mvcResult = new StubMvcResult(null, null, null, null, null, null, response);
		return mvcResult;
	}

	private StubMvcResult getForwardedUrlStubMvcResult(String forwardedUrl) {
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.setForwardedUrl(forwardedUrl);
		StubMvcResult mvcResult = new StubMvcResult(null, null, null, null, null, null, response);
		return mvcResult;
	}

}
