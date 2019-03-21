/*
 * Copyright 2002-2016 the original author or authors.
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

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StreamUtils;

import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @author Juergen Hoeller
 */
public class ShallowEtagHeaderFilterTests {

	private final ShallowEtagHeaderFilter filter = new ShallowEtagHeaderFilter();


	@Test
	public void isEligibleForEtag() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels");
		MockHttpServletResponse response = new MockHttpServletResponse();

		assertTrue(filter.isEligibleForEtag(request, response, 200, StreamUtils.emptyInput()));
		assertFalse(filter.isEligibleForEtag(request, response, 300, StreamUtils.emptyInput()));

		request = new MockHttpServletRequest("HEAD", "/hotels");
		assertFalse(filter.isEligibleForEtag(request, response, 200, StreamUtils.emptyInput()));

		request = new MockHttpServletRequest("POST", "/hotels");
		assertFalse(filter.isEligibleForEtag(request, response, 200, StreamUtils.emptyInput()));

		request = new MockHttpServletRequest("POST", "/hotels");
		request.addHeader("Cache-Control","must-revalidate, no-store");
		assertFalse(filter.isEligibleForEtag(request, response, 200, StreamUtils.emptyInput()));
	}

	@Test
	public void filterNoMatch() throws Exception {
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels");
		MockHttpServletResponse response = new MockHttpServletResponse();

		final byte[] responseBody = "Hello World".getBytes("UTF-8");
		FilterChain filterChain = (filterRequest, filterResponse) -> {
			assertEquals("Invalid request passed", request, filterRequest);
			((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
			FileCopyUtils.copy(responseBody, filterResponse.getOutputStream());
		};
		filter.doFilter(request, response, filterChain);

		assertEquals("Invalid status", 200, response.getStatus());
		assertEquals("Invalid ETag header", "\"0b10a8db164e0754105b7a99be72e3fe5\"", response.getHeader("ETag"));
		assertTrue("Invalid Content-Length header", response.getContentLength() > 0);
		assertArrayEquals("Invalid content", responseBody, response.getContentAsByteArray());
	}

	@Test
	public void filterNoMatchWeakETag() throws Exception {
		this.filter.setWriteWeakETag(true);
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels");
		MockHttpServletResponse response = new MockHttpServletResponse();

		final byte[] responseBody = "Hello World".getBytes("UTF-8");
		FilterChain filterChain = (filterRequest, filterResponse) -> {
			assertEquals("Invalid request passed", request, filterRequest);
			((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
			FileCopyUtils.copy(responseBody, filterResponse.getOutputStream());
		};
		filter.doFilter(request, response, filterChain);

		assertEquals("Invalid status", 200, response.getStatus());
		assertEquals("Invalid ETag header", "W/\"0b10a8db164e0754105b7a99be72e3fe5\"", response.getHeader("ETag"));
		assertTrue("Invalid Content-Length header", response.getContentLength() > 0);
		assertArrayEquals("Invalid content", responseBody, response.getContentAsByteArray());
	}

	@Test
	public void filterMatch() throws Exception {
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels");
		String etag = "\"0b10a8db164e0754105b7a99be72e3fe5\"";
		request.addHeader("If-None-Match", etag);
		MockHttpServletResponse response = new MockHttpServletResponse();

		FilterChain filterChain = (filterRequest, filterResponse) -> {
			assertEquals("Invalid request passed", request, filterRequest);
			byte[] responseBody = "Hello World".getBytes("UTF-8");
			FileCopyUtils.copy(responseBody, filterResponse.getOutputStream());
			filterResponse.setContentLength(responseBody.length);
		};
		filter.doFilter(request, response, filterChain);

		assertEquals("Invalid status", 304, response.getStatus());
		assertEquals("Invalid ETag header", "\"0b10a8db164e0754105b7a99be72e3fe5\"", response.getHeader("ETag"));
		assertFalse("Response has Content-Length header", response.containsHeader("Content-Length"));
		assertArrayEquals("Invalid content", new byte[0], response.getContentAsByteArray());
	}

	@Test
	public void filterMatchWeakEtag() throws Exception {
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels");
		String etag = "\"0b10a8db164e0754105b7a99be72e3fe5\"";
		request.addHeader("If-None-Match", "W/" + etag);
		MockHttpServletResponse response = new MockHttpServletResponse();

		FilterChain filterChain = (filterRequest, filterResponse) -> {
			assertEquals("Invalid request passed", request, filterRequest);
			byte[] responseBody = "Hello World".getBytes("UTF-8");
			FileCopyUtils.copy(responseBody, filterResponse.getOutputStream());
			filterResponse.setContentLength(responseBody.length);
		};
		filter.doFilter(request, response, filterChain);

		assertEquals("Invalid status", 304, response.getStatus());
		assertEquals("Invalid ETag header", "\"0b10a8db164e0754105b7a99be72e3fe5\"", response.getHeader("ETag"));
		assertFalse("Response has Content-Length header", response.containsHeader("Content-Length"));
		assertArrayEquals("Invalid content", new byte[0], response.getContentAsByteArray());
	}

	@Test
	public void filterWriter() throws Exception {
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels");
		String etag = "\"0b10a8db164e0754105b7a99be72e3fe5\"";
		request.addHeader("If-None-Match", etag);
		MockHttpServletResponse response = new MockHttpServletResponse();

		FilterChain filterChain = (filterRequest, filterResponse) -> {
			assertEquals("Invalid request passed", request, filterRequest);
			((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
			String responseBody = "Hello World";
			FileCopyUtils.copy(responseBody, filterResponse.getWriter());
		};
		filter.doFilter(request, response, filterChain);

		assertEquals("Invalid status", 304, response.getStatus());
		assertEquals("Invalid ETag header", "\"0b10a8db164e0754105b7a99be72e3fe5\"", response.getHeader("ETag"));
		assertFalse("Response has Content-Length header", response.containsHeader("Content-Length"));
		assertArrayEquals("Invalid content", new byte[0], response.getContentAsByteArray());
	}

	@Test  // SPR-12960
	public void filterWriterWithDisabledCaching() throws Exception {
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels");
		MockHttpServletResponse response = new MockHttpServletResponse();

		final byte[] responseBody = "Hello World".getBytes("UTF-8");
		FilterChain filterChain = (filterRequest, filterResponse) -> {
			assertEquals("Invalid request passed", request, filterRequest);
			((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
			FileCopyUtils.copy(responseBody, filterResponse.getOutputStream());
		};

		ShallowEtagHeaderFilter.disableContentCaching(request);
		this.filter.doFilter(request, response, filterChain);

		assertEquals(200, response.getStatus());
		assertNull(response.getHeader("ETag"));
		assertArrayEquals(responseBody, response.getContentAsByteArray());
	}

	@Test
	public void filterSendError() throws Exception {
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels");
		MockHttpServletResponse response = new MockHttpServletResponse();

		final byte[] responseBody = "Hello World".getBytes("UTF-8");
		FilterChain filterChain = (filterRequest, filterResponse) -> {
			assertEquals("Invalid request passed", request, filterRequest);
			response.setContentLength(100);
			FileCopyUtils.copy(responseBody, filterResponse.getOutputStream());
			((HttpServletResponse) filterResponse).sendError(HttpServletResponse.SC_FORBIDDEN);
		};
		filter.doFilter(request, response, filterChain);

		assertEquals("Invalid status", 403, response.getStatus());
		assertNull("Invalid ETag header", response.getHeader("ETag"));
		assertEquals("Invalid Content-Length header", 100, response.getContentLength());
		assertArrayEquals("Invalid content", responseBody, response.getContentAsByteArray());
	}

	@Test
	public void filterSendErrorMessage() throws Exception {
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels");
		MockHttpServletResponse response = new MockHttpServletResponse();

		final byte[] responseBody = "Hello World".getBytes("UTF-8");
		FilterChain filterChain = (filterRequest, filterResponse) -> {
			assertEquals("Invalid request passed", request, filterRequest);
			response.setContentLength(100);
			FileCopyUtils.copy(responseBody, filterResponse.getOutputStream());
			((HttpServletResponse) filterResponse).sendError(HttpServletResponse.SC_FORBIDDEN, "ERROR");
		};
		filter.doFilter(request, response, filterChain);

		assertEquals("Invalid status", 403, response.getStatus());
		assertNull("Invalid ETag header", response.getHeader("ETag"));
		assertEquals("Invalid Content-Length header", 100, response.getContentLength());
		assertArrayEquals("Invalid content", responseBody, response.getContentAsByteArray());
		assertEquals("Invalid error message", "ERROR", response.getErrorMessage());
	}

	@Test
	public void filterSendRedirect() throws Exception {
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels");
		MockHttpServletResponse response = new MockHttpServletResponse();

		final byte[] responseBody = "Hello World".getBytes("UTF-8");
		FilterChain filterChain = (filterRequest, filterResponse) -> {
			assertEquals("Invalid request passed", request, filterRequest);
			response.setContentLength(100);
			FileCopyUtils.copy(responseBody, filterResponse.getOutputStream());
			((HttpServletResponse) filterResponse).sendRedirect("https://www.google.com");
		};
		filter.doFilter(request, response, filterChain);

		assertEquals("Invalid status", 302, response.getStatus());
		assertNull("Invalid ETag header", response.getHeader("ETag"));
		assertEquals("Invalid Content-Length header", 100, response.getContentLength());
		assertArrayEquals("Invalid content", responseBody, response.getContentAsByteArray());
		assertEquals("Invalid redirect URL", "https://www.google.com", response.getRedirectedUrl());
	}

	// SPR-13717
	@Test
	public void filterFlushResponse() throws Exception {
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels");
		MockHttpServletResponse response = new MockHttpServletResponse();

		final byte[] responseBody = "Hello World".getBytes("UTF-8");
		FilterChain filterChain = (filterRequest, filterResponse) -> {
			assertEquals("Invalid request passed", request, filterRequest);
			((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
			FileCopyUtils.copy(responseBody, filterResponse.getOutputStream());
			filterResponse.flushBuffer();
		};
		filter.doFilter(request, response, filterChain);

		assertEquals("Invalid status", 200, response.getStatus());
		assertEquals("Invalid ETag header", "\"0b10a8db164e0754105b7a99be72e3fe5\"", response.getHeader("ETag"));
		assertTrue("Invalid Content-Length header", response.getContentLength() > 0);
		assertArrayEquals("Invalid content", responseBody, response.getContentAsByteArray());
	}

}
