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

package org.springframework.mock.web;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.springframework.web.util.WebUtils;

/**
 * @author Juergen Hoeller
 * @author Rick Evans
 * @author Rossen Stoyanchev
 * @since 19.02.2006
 */
public class MockHttpServletResponseTests extends TestCase {

	public void testSetContentType() {
		String contentType = "test/plain";
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.setContentType(contentType);
		assertEquals(contentType, response.getContentType());
		assertEquals(contentType, response.getHeader("Content-Type"));
		assertEquals(WebUtils.DEFAULT_CHARACTER_ENCODING, response.getCharacterEncoding());
	}

	public void testSetContentTypeUTF8() {
		String contentType = "test/plain;charset=UTF-8";
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.setContentType(contentType);
		assertEquals("UTF-8", response.getCharacterEncoding());
		assertEquals(contentType, response.getContentType());
		assertEquals(contentType, response.getHeader("Content-Type"));
	}
	
	public void testContentTypeHeader() {
		String contentType = "test/plain";
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.addHeader("Content-Type", contentType);
		assertEquals(contentType, response.getContentType());
		assertEquals(contentType, response.getHeader("Content-Type"));
		assertEquals(WebUtils.DEFAULT_CHARACTER_ENCODING, response.getCharacterEncoding());

		response = new MockHttpServletResponse();
		response.setHeader("Content-Type", contentType);
		assertEquals(contentType, response.getContentType());
		assertEquals(contentType, response.getHeader("Content-Type"));
		assertEquals(WebUtils.DEFAULT_CHARACTER_ENCODING, response.getCharacterEncoding());
	}

	public void testContentTypeHeaderUTF8() {
		String contentType = "test/plain;charset=UTF-8";
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.setHeader("Content-Type", contentType);
		assertEquals(contentType, response.getContentType());
		assertEquals(contentType, response.getHeader("Content-Type"));
		assertEquals("UTF-8", response.getCharacterEncoding());

		response = new MockHttpServletResponse();
		response.addHeader("Content-Type", contentType);
		assertEquals(contentType, response.getContentType());
		assertEquals(contentType, response.getHeader("Content-Type"));
		assertEquals("UTF-8", response.getCharacterEncoding());
	}

	public void testSetContentTypeThenCharacterEncoding() {
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.setContentType("test/plain");
		response.setCharacterEncoding("UTF-8");
		assertEquals("test/plain", response.getContentType());
		assertEquals("test/plain;charset=UTF-8", response.getHeader("Content-Type"));
		assertEquals("UTF-8", response.getCharacterEncoding());
	}

	public void testSetCharacterEncodingThenContentType() {
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.setCharacterEncoding("UTF-8");
		response.setContentType("test/plain");
		assertEquals("test/plain", response.getContentType());
		assertEquals("test/plain;charset=UTF-8", response.getHeader("Content-Type"));
		assertEquals("UTF-8", response.getCharacterEncoding());
	}

	public void testContentLength() {
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.setContentLength(66);
		assertEquals(66, response.getContentLength());
		assertEquals("66", response.getHeader("Content-Length"));
	}
	
	public void testContentLengthHeader() {
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.addHeader("Content-Length", "66");
		assertEquals(66,response.getContentLength());
		assertEquals("66", response.getHeader("Content-Length"));
	}
	
	public void testHttpHeaderNameCasingIsPreserved() throws Exception {
		final String headerName = "Header1";

		MockHttpServletResponse response = new MockHttpServletResponse();
		response.addHeader(headerName, "value1");
		Set<String> responseHeaders = response.getHeaderNames();
		assertNotNull(responseHeaders);
		assertEquals(1, responseHeaders.size());
		assertEquals("HTTP header casing not being preserved", headerName, responseHeaders.iterator().next());
	}

	public void testServletOutputStreamCommittedWhenBufferSizeExceeded() throws IOException {
		MockHttpServletResponse response = new MockHttpServletResponse();
		assertFalse(response.isCommitted());
		response.getOutputStream().write('X');
		assertFalse(response.isCommitted());
		int size = response.getBufferSize();
		response.getOutputStream().write(new byte[size]);
		assertTrue(response.isCommitted());
		assertEquals(size + 1, response.getContentAsByteArray().length);
	}

	public void testServletOutputStreamCommittedOnFlushBuffer() throws IOException {
		MockHttpServletResponse response = new MockHttpServletResponse();
		assertFalse(response.isCommitted());
		response.getOutputStream().write('X');
		assertFalse(response.isCommitted());
		response.flushBuffer();
		assertTrue(response.isCommitted());
		assertEquals(1, response.getContentAsByteArray().length);
	}

	public void testServletWriterCommittedWhenBufferSizeExceeded() throws IOException {
		MockHttpServletResponse response = new MockHttpServletResponse();
		assertFalse(response.isCommitted());
		response.getWriter().write("X");
		assertFalse(response.isCommitted());
		int size = response.getBufferSize();
		char[] data = new char[size];
		Arrays.fill(data, 'p');
		response.getWriter().write(data);
		assertTrue(response.isCommitted());
		assertEquals(size + 1, response.getContentAsByteArray().length);
	}

	public void testServletOutputStreamCommittedOnOutputStreamFlush() throws IOException {
		MockHttpServletResponse response = new MockHttpServletResponse();
		assertFalse(response.isCommitted());
		response.getOutputStream().write('X');
		assertFalse(response.isCommitted());
		response.getOutputStream().flush();
		assertTrue(response.isCommitted());
		assertEquals(1, response.getContentAsByteArray().length);
	}

	public void testServletWriterCommittedOnWriterFlush() throws IOException {
		MockHttpServletResponse response = new MockHttpServletResponse();
		assertFalse(response.isCommitted());
		response.getWriter().write("X");
		assertFalse(response.isCommitted());
		response.getWriter().flush();
		assertTrue(response.isCommitted());
		assertEquals(1, response.getContentAsByteArray().length);
	}

	public void testServletWriterAutoFlushedForString() throws IOException {
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.getWriter().write("X");
		assertEquals("X", response.getContentAsString());
	}

	public void testServletWriterAutoFlushedForChar() throws IOException {
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.getWriter().write('X');
		assertEquals("X", response.getContentAsString());
	}

	public void testServletWriterAutoFlushedForCharArray() throws IOException {
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.getWriter().write("XY".toCharArray());
		assertEquals("XY", response.getContentAsString());
	}

	public void testSendRedirect() throws IOException {
		MockHttpServletResponse response = new MockHttpServletResponse();
		String redirectUrl = "/redirect";
		response.sendRedirect(redirectUrl);
		assertEquals(HttpServletResponse.SC_MOVED_TEMPORARILY, response.getStatus());
		assertEquals(redirectUrl, response.getHeader("Location"));
		assertEquals(redirectUrl, response.getRedirectedUrl());
		assertEquals(true, response.isCommitted());
	}

	public void testLocationHeaderUpdatesGetRedirectedUrl() {
		MockHttpServletResponse response = new MockHttpServletResponse();
		String redirectUrl = "/redirect";
		response.setHeader("Location", redirectUrl);
		assertEquals(redirectUrl, response.getRedirectedUrl());
	}
}
