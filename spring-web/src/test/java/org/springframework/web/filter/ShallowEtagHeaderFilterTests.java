/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.web.filter;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;

import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.util.FileCopyUtils;

import static org.junit.Assert.*;

public class ShallowEtagHeaderFilterTests {

	private ShallowEtagHeaderFilter filter;

	@Before
	public void createFilter() throws Exception {
		filter = new ShallowEtagHeaderFilter();
	}

	@Test
	public void isEligibleForEtag() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels");
		MockHttpServletResponse response = new MockHttpServletResponse();

		assertTrue(filter.isEligibleForEtag(request, response, 200, new byte[0]));
		assertFalse(filter.isEligibleForEtag(request, response, 300, new byte[0]));

		request = new MockHttpServletRequest("POST", "/hotels");
		assertFalse(filter.isEligibleForEtag(request, response, 200, new byte[0]));

		request = new MockHttpServletRequest("POST", "/hotels");
		request.addHeader("Cache-Control","must-revalidate, no-store");
		assertFalse(filter.isEligibleForEtag(request, response, 200, new byte[0]));
	}

	@Test
	public void filterNoMatch() throws Exception {
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels");
		MockHttpServletResponse response = new MockHttpServletResponse();

		final byte[] responseBody = "Hello World".getBytes("UTF-8");
		FilterChain filterChain = new FilterChain() {

			@Override
			public void doFilter(ServletRequest filterRequest, ServletResponse filterResponse)
					throws IOException, ServletException {
				assertEquals("Invalid request passed", request, filterRequest);
				((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
				FileCopyUtils.copy(responseBody, filterResponse.getOutputStream());
			}
		};

		filter.doFilter(request, response, filterChain);

		assertEquals("Invalid status", 200, response.getStatus());
		assertEquals("Invalid ETag header", "\"0b10a8db164e0754105b7a99be72e3fe5\"", response.getHeader("ETag"));
		assertTrue("Invalid Content-Length header", response.getContentLength() > 0);
		assertArrayEquals("Invalid content", responseBody, response.getContentAsByteArray());
	}

	@Test
	public void filterMatch() throws Exception {
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels");
		String etag = "\"0b10a8db164e0754105b7a99be72e3fe5\"";
		request.addHeader("If-None-Match", etag);
		MockHttpServletResponse response = new MockHttpServletResponse();

		FilterChain filterChain = new FilterChain() {

			@Override
			public void doFilter(ServletRequest filterRequest, ServletResponse filterResponse)
					throws IOException, ServletException {
				assertEquals("Invalid request passed", request, filterRequest);
				byte[] responseBody = "Hello World".getBytes("UTF-8");
				FileCopyUtils.copy(responseBody, filterResponse.getOutputStream());
				filterResponse.setContentLength(responseBody.length);
			}
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

		FilterChain filterChain = new FilterChain() {

			@Override
			public void doFilter(ServletRequest filterRequest, ServletResponse filterResponse)
					throws IOException, ServletException {
				assertEquals("Invalid request passed", request, filterRequest);
				((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
				String responseBody = "Hello World";
				FileCopyUtils.copy(responseBody, filterResponse.getWriter());
			}
		};

		filter.doFilter(request, response, filterChain);

		assertEquals("Invalid status", 304, response.getStatus());
		assertEquals("Invalid ETag header", "\"0b10a8db164e0754105b7a99be72e3fe5\"", response.getHeader("ETag"));
		assertFalse("Response has Content-Length header", response.containsHeader("Content-Length"));
		assertArrayEquals("Invalid content", new byte[0], response.getContentAsByteArray());
	}

}