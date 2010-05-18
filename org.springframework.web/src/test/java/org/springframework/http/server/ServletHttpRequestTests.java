/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.http.server;

import java.net.URI;
import java.util.List;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.FileCopyUtils;

/**
 * @author Arjen Poutsma
 */
public class ServletHttpRequestTests {

	private ServletServerHttpRequest request;

	private MockHttpServletRequest mockRequest;

	@Before
	public void create() throws Exception {
		mockRequest = new MockHttpServletRequest();
		request = new ServletServerHttpRequest(mockRequest);
	}

	@Test
	public void getMethod() throws Exception {
		mockRequest.setMethod("POST");
		assertEquals("Invalid method", HttpMethod.POST, request.getMethod());
	}

	@Test
	public void getURI() throws Exception {
		URI uri = new URI("http://example.com/path?query");
		mockRequest.setServerName(uri.getHost());
		mockRequest.setServerPort(uri.getPort());
		mockRequest.setRequestURI(uri.getPath());
		mockRequest.setQueryString(uri.getQuery());
		assertEquals("Invalid uri", uri, request.getURI());
	}

	@Test
	public void getHeaders() throws Exception {
		String headerName = "MyHeader";
		String headerValue1 = "value1";
		mockRequest.addHeader(headerName, headerValue1);
		String headerValue2 = "value2";
		mockRequest.addHeader(headerName, headerValue2);

		HttpHeaders headers = request.getHeaders();
		assertNotNull("No HttpHeaders returned", headers);
		assertTrue("Invalid headers returned", headers.containsKey(headerName));
		List<String> headerValues = headers.get(headerName);
		assertEquals("Invalid header values returned", 2, headerValues.size());
		assertTrue("Invalid header values returned", headerValues.contains(headerValue1));
		assertTrue("Invalid header values returned", headerValues.contains(headerValue2));
	}

	@Test
	public void getBody() throws Exception {
		byte[] content = "Hello World".getBytes("UTF-8");
		mockRequest.setContent(content);

		byte[] result = FileCopyUtils.copyToByteArray(request.getBody());
		assertArrayEquals("Invalid content returned", content, result);
	}
}