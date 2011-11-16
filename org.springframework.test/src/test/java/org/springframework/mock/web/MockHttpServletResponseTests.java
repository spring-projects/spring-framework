/*
 * Copyright 2002-2011 the original author or authors.
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

import junit.framework.TestCase;

import org.springframework.web.util.WebUtils;

/**
 * @author Juergen Hoeller
 * @author Rick Evans
 * @author Rossen Stoyanchev
 * @since 19.02.2006
 */
public class MockHttpServletResponseTests extends TestCase {

	public void testSetContentTypeWithNoEncoding() {
		String contentType = "test/plain";
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.setContentType(contentType);
		assertEquals("Character encoding should be the default", WebUtils.DEFAULT_CHARACTER_ENCODING,
			response.getCharacterEncoding());
		assertEquals("Content-Type header not set", contentType, response.getHeader("Content-Type"));
	}

	public void testSetContentTypeWithUTF8() {
		String contentType = "test/plain; charset=UTF-8";
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.setContentType(contentType);
		assertEquals("Character encoding should be 'UTF-8'", "UTF-8", response.getCharacterEncoding());
		assertEquals("Content-Type header not set", contentType, response.getHeader("Content-Type"));
	}
	
	public void testContentTypeHeaderWithNoEncoding() {
		String contentType = "test/plain";
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.addHeader("Content-Type", contentType);
		assertEquals("contentType field not set", contentType, response.getContentType());

		response = new MockHttpServletResponse();
		response.setHeader("Content-Type", contentType);
		assertEquals("contentType field not set", contentType, response.getContentType());
	}

	public void testContentTypeHeaderWithUTF8() {
		String contentType = "test/plain; charset=UTF-8";
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.addHeader("Content-Type", contentType);
		assertEquals("contentType field not set", contentType, response.getContentType());
		assertEquals("Character encoding should be 'UTF-8'", "UTF-8", response.getCharacterEncoding());
		assertEquals("Content-Type header not set", contentType, response.getHeader("Content-Type"));

		response = new MockHttpServletResponse();
		response.setHeader("Content-Type", contentType);
		assertEquals("contentType field not set", contentType, response.getContentType());
		assertEquals("Character encoding should be 'UTF-8'", "UTF-8", response.getCharacterEncoding());
		assertEquals("Content-Type header not set", contentType, response.getHeader("Content-Type"));
	}

	public void testSetCharacterEncoding() {
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.setContentType("test/plain");
		response.setCharacterEncoding("UTF-8");
		assertEquals("Character encoding not set", "UTF-8",	response.getCharacterEncoding());
		assertEquals("contentType field not set", "test/plain;charset=UTF-8", response.getContentType());
		assertEquals("Content-Type header not set", "test/plain;charset=UTF-8", response.getHeader("Content-Type"));
	}

	public void testSetCharacterEncodingOppositeOrder() {
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.setCharacterEncoding("UTF-8");
		response.setContentType("test/plain");
		assertEquals("Character encoding not set", "UTF-8",	response.getCharacterEncoding());
		assertEquals("contentType field not set", "test/plain;charset=UTF-8", response.getContentType());
		assertEquals("Content-Type header not set", "test/plain;charset=UTF-8", response.getHeader("Content-Type"));
	}
	
	public void testReplaceCharacterEncoding() {
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.setContentType("test/plain;charset=ISO-8859-1");
		response.setCharacterEncoding("UTF-8");
		assertEquals("Character encoding not set", "UTF-8",	response.getCharacterEncoding());
		assertEquals("contentType field not set", "test/plain;charset=UTF-8", response.getContentType());
		assertEquals("Content-Type header not set", "test/plain;charset=UTF-8", response.getHeader("Content-Type"));
	}
	
	public void testContentLength() {
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.setContentLength(66);
		assertEquals("Content length field not set", 66,	response.getContentLength());
		assertEquals("Content-Length header not set", "66", response.getHeader("Content-Length"));
	}
	
	public void testContentLengthHeader() {
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.addHeader("Content-Length", "66");
		assertEquals("Content length field not set", 66,	response.getContentLength());
		assertEquals("Content-Length header not set", "66", response.getHeader("Content-Length"));
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

}
