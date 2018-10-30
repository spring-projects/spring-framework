/*
 * Copyright 2002-2016 the original author or authors.
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

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link MockMvcResultMatchers}.
 *
 * @author Brian Clozel
 * @author Sam Brannen
 */
public class MockMvcResultMatchersTests {

	@Test
	public void redirect() throws Exception {
		redirectedUrl("/resource/1").match(getRedirectedUrlStubMvcResult("/resource/1"));
	}

	@Test
	public void redirectWithUrlTemplate() throws Exception {
		redirectedUrlTemplate("/orders/{orderId}/items/{itemId}", 1, 2).match(getRedirectedUrlStubMvcResult("/orders/1/items/2"));
	}

	@Test
	public void redirectWithMatchingPattern() throws Exception {
		redirectedUrlPattern("/resource/*").match(getRedirectedUrlStubMvcResult("/resource/1"));
	}

	@Test(expected = AssertionError.class)
	public void redirectWithNonMatchingPattern() throws Exception {
		redirectedUrlPattern("/resource/").match(getRedirectedUrlStubMvcResult("/resource/1"));
	}

	@Test
	public void forward() throws Exception {
		forwardedUrl("/api/resource/1").match(getForwardedUrlStubMvcResult("/api/resource/1"));
	}

	@Test
	public void forwardWithQueryString() throws Exception {
		forwardedUrl("/api/resource/1?arg=value").match(getForwardedUrlStubMvcResult("/api/resource/1?arg=value"));
	}

	@Test
	public void forwardWithUrlTemplate() throws Exception {
		forwardedUrlTemplate("/orders/{orderId}/items/{itemId}", 1, 2).match(getForwardedUrlStubMvcResult("/orders/1/items/2"));
	}

	@Test
	public void forwardWithMatchingPattern() throws Exception {
		forwardedUrlPattern("/api/**/?").match(getForwardedUrlStubMvcResult("/api/resource/1"));
	}

	@Test(expected = AssertionError.class)
	public void forwardWithNonMatchingPattern() throws Exception {
		forwardedUrlPattern("/resource/").match(getForwardedUrlStubMvcResult("/resource/1"));
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
