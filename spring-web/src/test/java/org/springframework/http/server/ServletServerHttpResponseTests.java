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

package org.springframework.http.server;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.util.FileCopyUtils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class ServletServerHttpResponseTests {

	private ServletServerHttpResponse response;

	private MockHttpServletResponse mockResponse;


	@Before
	public void create() throws Exception {
		mockResponse = new MockHttpServletResponse();
		response = new ServletServerHttpResponse(mockResponse);
	}


	@Test
	public void setStatusCode() throws Exception {
		response.setStatusCode(HttpStatus.NOT_FOUND);
		assertEquals("Invalid status code", 404, mockResponse.getStatus());
	}

	@Test
	public void getHeaders() throws Exception {
		HttpHeaders headers = response.getHeaders();
		String headerName = "MyHeader";
		String headerValue1 = "value1";
		headers.add(headerName, headerValue1);
		String headerValue2 = "value2";
		headers.add(headerName, headerValue2);
		headers.setContentType(new MediaType("text", "plain", Charset.forName("UTF-8")));

		response.close();
		assertTrue("Header not set", mockResponse.getHeaderNames().contains(headerName));
		List<String> headerValues = mockResponse.getHeaders(headerName);
		assertTrue("Header not set", headerValues.contains(headerValue1));
		assertTrue("Header not set", headerValues.contains(headerValue2));
		assertEquals("Invalid Content-Type", "text/plain;charset=UTF-8", mockResponse.getHeader("Content-Type"));
		assertEquals("Invalid Content-Type", "text/plain;charset=UTF-8", mockResponse.getContentType());
		assertEquals("Invalid Content-Type", "UTF-8", mockResponse.getCharacterEncoding());
	}

	@Test
	public void preExistingHeadersFromHttpServletResponse() {

		String headerName = "Access-Control-Allow-Origin";
		String headerValue = "localhost:8080";

		this.mockResponse.addHeader(headerName, headerValue);
		this.response = new ServletServerHttpResponse(this.mockResponse);

		assertEquals(headerValue, this.response.getHeaders().getFirst(headerName));
		assertEquals(Collections.singletonList(headerValue), this.response.getHeaders().get(headerName));
		assertTrue(this.response.getHeaders().containsKey(headerName));
	}

	@Test
	public void getBody() throws Exception {
		byte[] content = "Hello World".getBytes("UTF-8");
		FileCopyUtils.copy(content, response.getBody());

		assertArrayEquals("Invalid content written", content, mockResponse.getContentAsByteArray());
	}
}