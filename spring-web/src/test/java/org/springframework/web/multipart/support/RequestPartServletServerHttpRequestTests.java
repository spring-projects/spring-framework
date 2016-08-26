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

package org.springframework.web.multipart.support;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.junit.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.mock.web.test.MockMultipartFile;
import org.springframework.mock.web.test.MockMultipartHttpServletRequest;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.Assert.*;

/**
 * @author Rossen Stoyanchev
 */
public class RequestPartServletServerHttpRequestTests {

	private final MockMultipartHttpServletRequest mockRequest = new MockMultipartHttpServletRequest();


	@Test
	public void getMethod() throws Exception {
		this.mockRequest.addFile(new MockMultipartFile("part", "", "", "content".getBytes("UTF-8")));
		ServerHttpRequest request = new RequestPartServletServerHttpRequest(this.mockRequest, "part");
		this.mockRequest.setMethod("POST");

		assertEquals(HttpMethod.POST, request.getMethod());
	}

	@Test
	public void getURI() throws Exception {
		this.mockRequest.addFile(new MockMultipartFile("part", "", "application/json", "content".getBytes("UTF-8")));
		ServerHttpRequest request = new RequestPartServletServerHttpRequest(this.mockRequest, "part");

		URI uri = new URI("http://example.com/path?query");
		this.mockRequest.setServerName(uri.getHost());
		this.mockRequest.setServerPort(uri.getPort());
		this.mockRequest.setRequestURI(uri.getPath());
		this.mockRequest.setQueryString(uri.getQuery());
		assertEquals(uri, request.getURI());
	}

	@Test
	public void getContentType() throws Exception {
		MultipartFile part = new MockMultipartFile("part", "", "application/json", "content".getBytes("UTF-8"));
		this.mockRequest.addFile(part);
		ServerHttpRequest request = new RequestPartServletServerHttpRequest(this.mockRequest, "part");

		HttpHeaders headers = request.getHeaders();
		assertNotNull(headers);
		assertEquals(MediaType.APPLICATION_JSON, headers.getContentType());
	}

	@Test
	public void getBody() throws Exception {
		byte[] bytes = "content".getBytes("UTF-8");
		MultipartFile part = new MockMultipartFile("part", "", "application/json", bytes);
		this.mockRequest.addFile(part);
		ServerHttpRequest request = new RequestPartServletServerHttpRequest(this.mockRequest, "part");

		byte[] result = FileCopyUtils.copyToByteArray(request.getBody());
		assertArrayEquals(bytes, result);
	}

	@Test  // SPR-13317
	public void getBodyWithWrappedRequest() throws Exception {
		byte[] bytes = "content".getBytes("UTF-8");
		MultipartFile part = new MockMultipartFile("part", "", "application/json", bytes);
		this.mockRequest.addFile(part);
		HttpServletRequest wrapped = new HttpServletRequestWrapper(this.mockRequest);
		ServerHttpRequest request = new RequestPartServletServerHttpRequest(wrapped, "part");

		byte[] result = FileCopyUtils.copyToByteArray(request.getBody());
		assertArrayEquals(bytes, result);
	}

	@Test  // SPR-13096
	public void getBodyViaRequestParameter() throws Exception {
		MockMultipartHttpServletRequest mockRequest = new MockMultipartHttpServletRequest() {
			@Override
			public HttpHeaders getMultipartHeaders(String paramOrFileName) {
				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(new MediaType("application", "octet-stream", StandardCharsets.ISO_8859_1));
				return headers;
			}
		};
		byte[] bytes = {(byte) 0xC4};
		mockRequest.setParameter("part", new String(bytes, StandardCharsets.ISO_8859_1));
		ServerHttpRequest request = new RequestPartServletServerHttpRequest(mockRequest, "part");
		byte[] result = FileCopyUtils.copyToByteArray(request.getBody());
		assertArrayEquals(bytes, result);
	}

	@Test
	public void getBodyViaRequestParameterWithRequestEncoding() throws Exception {
		MockMultipartHttpServletRequest mockRequest = new MockMultipartHttpServletRequest() {
			@Override
			public HttpHeaders getMultipartHeaders(String paramOrFileName) {
				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
				return headers;
			}
		};
		byte[] bytes = {(byte) 0xC4};
		mockRequest.setParameter("part", new String(bytes, StandardCharsets.ISO_8859_1));
		mockRequest.setCharacterEncoding("iso-8859-1");
		ServerHttpRequest request = new RequestPartServletServerHttpRequest(mockRequest, "part");
		byte[] result = FileCopyUtils.copyToByteArray(request.getBody());
		assertArrayEquals(bytes, result);
	}

}
