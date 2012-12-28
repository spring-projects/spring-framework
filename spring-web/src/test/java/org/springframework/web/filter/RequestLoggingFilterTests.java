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

package org.springframework.web.filter;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.util.FileCopyUtils;

/**
 * Test for {@link AbstractRequestLoggingFilter} and sub classes.
 *
 * @author Arjen Poutsma
 */
public class RequestLoggingFilterTests {

	private MyRequestLoggingFilter filter;

	@Before
	public void createFilter() throws Exception {
		filter = new MyRequestLoggingFilter();
	}

	@Test
	public void uri() throws Exception {
		final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/hotels");
		MockHttpServletResponse response = new MockHttpServletResponse();

		request.setQueryString("booking=42");

		FilterChain filterChain = new NoopFilterChain();

		filter.doFilter(request, response, filterChain);

		assertNotNull(filter.beforeRequestMessage);
		assertTrue(filter.beforeRequestMessage.indexOf("uri=/hotel") != -1);
		assertFalse(filter.beforeRequestMessage.indexOf("booking=42") != -1);

		assertNotNull(filter.afterRequestMessage);
		assertTrue(filter.afterRequestMessage.indexOf("uri=/hotel") != -1);
		assertFalse(filter.afterRequestMessage.indexOf("booking=42") != -1);
	}

	@Test
	public void queryString() throws Exception {
		filter.setIncludeQueryString(true);

		final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/hotels");
		MockHttpServletResponse response = new MockHttpServletResponse();

		request.setQueryString("booking=42");

		FilterChain filterChain = new NoopFilterChain();

		filter.doFilter(request, response, filterChain);

		assertNotNull(filter.beforeRequestMessage);
		assertTrue(filter.beforeRequestMessage.indexOf("uri=/hotels?booking=42") != -1);

		assertNotNull(filter.afterRequestMessage);
		assertTrue(filter.afterRequestMessage.indexOf("uri=/hotels?booking=42") != -1);
	}

	@Test
	public void payloadInputStream() throws Exception {
		filter.setIncludePayload(true);

		final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/hotels");
		MockHttpServletResponse response = new MockHttpServletResponse();

		final byte[] requestBody = "Hello World".getBytes("UTF-8");
		request.setContent(requestBody);
		FilterChain filterChain = new FilterChain() {

			@Override
			public void doFilter(ServletRequest filterRequest, ServletResponse filterResponse)
					throws IOException, ServletException {
				((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
				byte[] buf = FileCopyUtils.copyToByteArray(filterRequest.getInputStream());
				assertArrayEquals(requestBody, buf);
			}
		};

		filter.doFilter(request, response, filterChain);

		assertNotNull(filter.afterRequestMessage);
		assertTrue(filter.afterRequestMessage.indexOf("Hello World") != -1);
	}

	@Test
	public void payloadReader() throws Exception {
		filter.setIncludePayload(true);

		final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/hotels");
		MockHttpServletResponse response = new MockHttpServletResponse();

		final String requestBody = "Hello World";
		request.setContent(requestBody.getBytes("UTF-8"));
		FilterChain filterChain = new FilterChain() {

			@Override
			public void doFilter(ServletRequest filterRequest, ServletResponse filterResponse)
					throws IOException, ServletException {
				((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
				String buf = FileCopyUtils.copyToString(filterRequest.getReader());
				assertEquals(requestBody, buf);
			}
		};

		filter.doFilter(request, response, filterChain);

		assertNotNull(filter.afterRequestMessage);
		assertTrue(filter.afterRequestMessage.indexOf(requestBody) != -1);
	}

	@Test
	public void payloadMaxLength() throws Exception {
		filter.setIncludePayload(true);
		filter.setMaxPayloadLength(3);

		final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/hotels");
		MockHttpServletResponse response = new MockHttpServletResponse();

		final byte[] requestBody = "Hello World".getBytes("UTF-8");
		request.setContent(requestBody);
		FilterChain filterChain = new FilterChain() {

			@Override
			public void doFilter(ServletRequest filterRequest, ServletResponse filterResponse)
					throws IOException, ServletException {
				((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
				byte[] buf = FileCopyUtils.copyToByteArray(filterRequest.getInputStream());
				assertArrayEquals(requestBody, buf);
			}
		};

		filter.doFilter(request, response, filterChain);

		assertNotNull(filter.afterRequestMessage);
		assertTrue(filter.afterRequestMessage.indexOf("Hel") != -1);
		assertFalse(filter.afterRequestMessage.indexOf("Hello World") != -1);
	}

	private static class MyRequestLoggingFilter extends AbstractRequestLoggingFilter {

		private String beforeRequestMessage;

		private String afterRequestMessage;

		@Override
		protected void beforeRequest(HttpServletRequest request, String message) {
			this.beforeRequestMessage = message;
		}

		@Override
		protected void afterRequest(HttpServletRequest request, String message) {
			this.afterRequestMessage = message;
		}
	}

	private static class NoopFilterChain implements FilterChain {

		@Override
		public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
		}
	}

}
