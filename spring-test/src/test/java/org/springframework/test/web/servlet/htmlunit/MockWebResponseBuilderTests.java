/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.test.web.servlet.htmlunit;

import java.net.URL;
import java.util.List;

import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;

import org.springframework.mock.web.MockHttpServletResponse;

/**
 *
 * @author Rob Winch
 */
public class MockWebResponseBuilderTests {

	private WebRequest webRequest;

	private MockHttpServletResponse httpServletResponse;

	private MockWebResponseBuilder responseBuilder;

	@Before
	public void setUp() throws Exception {
		webRequest = new WebRequest(new URL("http://example.com:80/test/this/here"));
		httpServletResponse = new MockHttpServletResponse();

		responseBuilder = new MockWebResponseBuilder(System.currentTimeMillis(), webRequest, httpServletResponse);
	}

	// --- constructor

	@Test(expected = IllegalArgumentException.class)
	public void constructorNullWebRequest() {
		new MockWebResponseBuilder(0L, null, httpServletResponse);
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructorNullResponse() throws Exception {
		new MockWebResponseBuilder(0L, new WebRequest(new URL("http://example.com:80/test/this/here")), null);
	}

	// --- build

	@Test
	public void buildContent() throws Exception {
		httpServletResponse.getWriter().write("expected content");

		WebResponse webResponse = responseBuilder.build();

		assertThat(webResponse.getContentAsString(), equalTo("expected content"));;
	}

	@Test
	public void buildContentCharset() throws Exception {
		httpServletResponse.addHeader("Content-Type", "text/html; charset=UTF-8");
		WebResponse webResponse = responseBuilder.build();

		assertThat(webResponse.getContentCharset(), equalTo("UTF-8"));;
	}

	@Test
	public void buildContentType() throws Exception {
		httpServletResponse.addHeader("Content-Type", "text/html; charset-UTF-8");
		WebResponse webResponse = responseBuilder.build();

		assertThat(webResponse.getContentType(), equalTo("text/html"));;
	}

	@Test
	public void buildResponseHeaders() throws Exception {
		httpServletResponse.addHeader("Content-Type", "text/html");
		httpServletResponse.addHeader("X-Test", "value");
		WebResponse webResponse = responseBuilder.build();

		List<NameValuePair> responseHeaders = webResponse.getResponseHeaders();
		assertThat(responseHeaders.size(), equalTo(2));;
		NameValuePair header = responseHeaders.get(0);
		assertThat(header.getName(), equalTo("Content-Type"));;
		assertThat(header.getValue(), equalTo("text/html"));;
		header = responseHeaders.get(1);
		assertThat(header.getName(), equalTo("X-Test"));;
		assertThat(header.getValue(), equalTo("value"));;
	}

	@Test
	public void buildStatus() throws Exception {
		WebResponse webResponse = responseBuilder.build();

		assertThat(webResponse.getStatusCode(), equalTo(200));;
		assertThat(webResponse.getStatusMessage(), equalTo("OK"));;
	}

	@Test
	public void buildStatusNotOk() throws Exception {
		httpServletResponse.setStatus(401);
		WebResponse webResponse = responseBuilder.build();

		assertThat(webResponse.getStatusCode(), equalTo(401));;
		assertThat(webResponse.getStatusMessage(), equalTo("Unauthorized"));;
	}

	@Test
	public void buildStatusCustomMessage() throws Exception {
		httpServletResponse.sendError(401, "Custom");
		WebResponse webResponse = responseBuilder.build();

		assertThat(webResponse.getStatusCode(), equalTo(401));;
		assertThat(webResponse.getStatusMessage(), equalTo("Custom"));;
	}

	@Test
	public void buildWebRequest() throws Exception {
		WebResponse webResponse = responseBuilder.build();

		assertThat(webResponse.getWebRequest(), equalTo(webRequest));;
	}
}
