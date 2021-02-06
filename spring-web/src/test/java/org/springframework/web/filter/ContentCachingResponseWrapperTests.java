/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.web.filter;

import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.util.ContentCachingResponseWrapper;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link ContentCachingResponseWrapper}.
 * @author Rossen Stoyanchev
 */
public class ContentCachingResponseWrapperTests {

	@Test
	public void copyBodyToResponse() throws Exception {
		byte[] responseBody = "Hello World".getBytes(StandardCharsets.UTF_8);
		MockHttpServletResponse response = new MockHttpServletResponse();

		ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
		responseWrapper.setStatus(HttpServletResponse.SC_OK);
		FileCopyUtils.copy(responseBody, responseWrapper.getOutputStream());
		responseWrapper.copyBodyToResponse();

		assertEquals(200, response.getStatus());
		assertTrue(response.getContentLength() > 0);
		assertArrayEquals(responseBody, response.getContentAsByteArray());
	}

	@Test
	public void copyBodyToResponseWithTransferEncoding() throws Exception {
		byte[] responseBody = "6\r\nHello 5\r\nWorld0\r\n\r\n".getBytes(StandardCharsets.UTF_8);
		MockHttpServletResponse response = new MockHttpServletResponse();

		ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
		responseWrapper.setStatus(HttpServletResponse.SC_OK);
		responseWrapper.setHeader(HttpHeaders.TRANSFER_ENCODING, "chunked");
		FileCopyUtils.copy(responseBody, responseWrapper.getOutputStream());
		responseWrapper.copyBodyToResponse();

		assertEquals(200, response.getStatus());
		assertEquals("chunked", response.getHeader(HttpHeaders.TRANSFER_ENCODING));
		assertNull(response.getHeader(HttpHeaders.CONTENT_LENGTH));
		assertArrayEquals(responseBody, response.getContentAsByteArray());
	}

}
