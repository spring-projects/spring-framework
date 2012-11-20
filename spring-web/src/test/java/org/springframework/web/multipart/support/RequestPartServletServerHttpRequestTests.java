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

package org.springframework.web.multipart.support;

import java.net.URI;

import org.junit.Before;
import org.junit.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.test.MockMultipartFile;
import org.springframework.mock.web.test.MockMultipartHttpServletRequest;
import org.springframework.util.FileCopyUtils;

import static org.junit.Assert.*;

/**
 * @author Rossen Stoyanchev
 */
public class RequestPartServletServerHttpRequestTests {

	private RequestPartServletServerHttpRequest request;
	
	private MockMultipartHttpServletRequest mockRequest;

	private MockMultipartFile mockFile;
	
	@Before
	public void create() throws Exception {
		mockFile = new MockMultipartFile("part", "", "application/json" ,"Part Content".getBytes("UTF-8"));
		mockRequest = new MockMultipartHttpServletRequest();
		mockRequest.addFile(mockFile);
		request = new RequestPartServletServerHttpRequest(mockRequest, "part");
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
	public void getContentType() throws Exception {
		HttpHeaders headers = request.getHeaders();
		assertNotNull("No HttpHeaders returned", headers);

		MediaType expected = MediaType.parseMediaType(mockFile.getContentType());
		MediaType actual = headers.getContentType();
		assertEquals("Invalid content type returned", expected, actual);
	}

	@Test
	public void getBody() throws Exception {
		byte[] result = FileCopyUtils.copyToByteArray(request.getBody());
		assertArrayEquals("Invalid content returned", mockFile.getBytes(), result);
	}
	
}
