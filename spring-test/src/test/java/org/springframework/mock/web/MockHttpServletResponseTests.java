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

package org.springframework.mock.web;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

import org.springframework.web.util.WebUtils;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link MockHttpServletResponse}.
 *
 * @author Juergen Hoeller
 * @author Rick Evans
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @author Sam Brannen
 * @since 19.02.2006
 */
public class MockHttpServletResponseTests {

	private MockHttpServletResponse response = new MockHttpServletResponse();


	@Test
	public void setContentType() {
		String contentType = "test/plain";
		response.setContentType(contentType);
		assertEquals(contentType, response.getContentType());
		assertEquals(contentType, response.getHeader("Content-Type"));
		assertEquals(WebUtils.DEFAULT_CHARACTER_ENCODING, response.getCharacterEncoding());
	}

	@Test
	public void setContentTypeUTF8() {
		String contentType = "test/plain;charset=UTF-8";
		response.setContentType(contentType);
		assertEquals("UTF-8", response.getCharacterEncoding());
		assertEquals(contentType, response.getContentType());
		assertEquals(contentType, response.getHeader("Content-Type"));
	}

	@Test
	public void contentTypeHeader() {
		String contentType = "test/plain";
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

	@Test
	public void contentTypeHeaderUTF8() {
		String contentType = "test/plain;charset=UTF-8";
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

	@Test
	public void setContentTypeThenCharacterEncoding() {
		response.setContentType("test/plain");
		response.setCharacterEncoding("UTF-8");
		assertEquals("test/plain", response.getContentType());
		assertEquals("test/plain;charset=UTF-8", response.getHeader("Content-Type"));
		assertEquals("UTF-8", response.getCharacterEncoding());
	}

	@Test
	public void setCharacterEncodingThenContentType() {
		response.setCharacterEncoding("UTF-8");
		response.setContentType("test/plain");
		assertEquals("test/plain", response.getContentType());
		assertEquals("test/plain;charset=UTF-8", response.getHeader("Content-Type"));
		assertEquals("UTF-8", response.getCharacterEncoding());
	}

	@Test
	public void contentLength() {
		response.setContentLength(66);
		assertEquals(66, response.getContentLength());
		assertEquals("66", response.getHeader("Content-Length"));
	}

	@Test
	public void contentLengthHeader() {
		response.addHeader("Content-Length", "66");
		assertEquals(66, response.getContentLength());
		assertEquals("66", response.getHeader("Content-Length"));
	}

	@Test
	public void httpHeaderNameCasingIsPreserved() throws Exception {
		final String headerName = "Header1";
		response.addHeader(headerName, "value1");
		Collection<String> responseHeaders = response.getHeaderNames();
		assertNotNull(responseHeaders);
		assertEquals(1, responseHeaders.size());
		assertEquals("HTTP header casing not being preserved", headerName, responseHeaders.iterator().next());
	}

	@Test
	public void servletOutputStreamCommittedWhenBufferSizeExceeded() throws IOException {
		assertFalse(response.isCommitted());
		response.getOutputStream().write('X');
		assertFalse(response.isCommitted());
		int size = response.getBufferSize();
		response.getOutputStream().write(new byte[size]);
		assertTrue(response.isCommitted());
		assertEquals(size + 1, response.getContentAsByteArray().length);
	}

	@Test
	public void servletOutputStreamCommittedOnFlushBuffer() throws IOException {
		assertFalse(response.isCommitted());
		response.getOutputStream().write('X');
		assertFalse(response.isCommitted());
		response.flushBuffer();
		assertTrue(response.isCommitted());
		assertEquals(1, response.getContentAsByteArray().length);
	}

	@Test
	public void servletWriterCommittedWhenBufferSizeExceeded() throws IOException {
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

	@Test
	public void servletOutputStreamCommittedOnOutputStreamFlush() throws IOException {
		assertFalse(response.isCommitted());
		response.getOutputStream().write('X');
		assertFalse(response.isCommitted());
		response.getOutputStream().flush();
		assertTrue(response.isCommitted());
		assertEquals(1, response.getContentAsByteArray().length);
	}

	@Test
	public void servletWriterCommittedOnWriterFlush() throws IOException {
		assertFalse(response.isCommitted());
		response.getWriter().write("X");
		assertFalse(response.isCommitted());
		response.getWriter().flush();
		assertTrue(response.isCommitted());
		assertEquals(1, response.getContentAsByteArray().length);
	}

	@Test
	public void servletWriterAutoFlushedForString() throws IOException {
		response.getWriter().write("X");
		assertEquals("X", response.getContentAsString());
	}

	@Test
	public void servletWriterAutoFlushedForChar() throws IOException {
		response.getWriter().write('X');
		assertEquals("X", response.getContentAsString());
	}

	@Test
	public void servletWriterAutoFlushedForCharArray() throws IOException {
		response.getWriter().write("XY".toCharArray());
		assertEquals("XY", response.getContentAsString());
	}

	@Test
	public void sendRedirect() throws IOException {
		String redirectUrl = "/redirect";
		response.sendRedirect(redirectUrl);
		assertEquals(HttpServletResponse.SC_MOVED_TEMPORARILY, response.getStatus());
		assertEquals(redirectUrl, response.getHeader("Location"));
		assertEquals(redirectUrl, response.getRedirectedUrl());
		assertTrue(response.isCommitted());
	}

	@Test
	public void locationHeaderUpdatesGetRedirectedUrl() {
		String redirectUrl = "/redirect";
		response.setHeader("Location", redirectUrl);
		assertEquals(redirectUrl, response.getRedirectedUrl());
	}

}
