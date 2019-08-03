/*
 * Copyright 2002-2019 the original author or authors.
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.WebUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link AbstractRequestLoggingFilter} and subclasses.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 */
public class RequestLoggingFilterTests {

	private final MyRequestLoggingFilter filter = new MyRequestLoggingFilter();


	@Test
	public void uri() throws Exception {
		final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/hotels");
		MockHttpServletResponse response = new MockHttpServletResponse();

		request.setQueryString("booking=42");

		FilterChain filterChain = new NoOpFilterChain();
		filter.doFilter(request, response, filterChain);

		assertThat(filter.beforeRequestMessage).isNotNull();
		assertThat(filter.beforeRequestMessage.contains("uri=/hotel")).isTrue();
		assertThat(filter.beforeRequestMessage.contains("booking=42")).isFalse();

		assertThat(filter.afterRequestMessage).isNotNull();
		assertThat(filter.afterRequestMessage.contains("uri=/hotel")).isTrue();
		assertThat(filter.afterRequestMessage.contains("booking=42")).isFalse();
	}

	@Test
	public void queryStringIncluded() throws Exception {
		filter.setIncludeQueryString(true);

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/hotels");
		MockHttpServletResponse response = new MockHttpServletResponse();

		request.setQueryString("booking=42");

		FilterChain filterChain = new NoOpFilterChain();
		filter.doFilter(request, response, filterChain);

		assertThat(filter.beforeRequestMessage).isNotNull();
		assertThat(filter.beforeRequestMessage.contains("[uri=/hotels?booking=42]")).isTrue();

		assertThat(filter.afterRequestMessage).isNotNull();
		assertThat(filter.afterRequestMessage.contains("[uri=/hotels?booking=42]")).isTrue();
	}

	@Test
	public void noQueryStringAvailable() throws Exception {
		filter.setIncludeQueryString(true);

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/hotels");
		MockHttpServletResponse response = new MockHttpServletResponse();

		FilterChain filterChain = new NoOpFilterChain();
		filter.doFilter(request, response, filterChain);

		assertThat(filter.beforeRequestMessage).isNotNull();
		assertThat(filter.beforeRequestMessage.contains("[uri=/hotels]")).isTrue();

		assertThat(filter.afterRequestMessage).isNotNull();
		assertThat(filter.afterRequestMessage.contains("[uri=/hotels]")).isTrue();
	}

	@Test
	public void headers() throws Exception {
		final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/hotels");
		request.setContentType("application/json");
		request.addHeader("token", "123");
		MockHttpServletResponse response = new MockHttpServletResponse();

		FilterChain filterChain = new NoOpFilterChain();
		filter.setIncludeHeaders(true);
		filter.setHeaderPredicate(name -> !name.equalsIgnoreCase("token"));
		filter.doFilter(request, response, filterChain);

		assertThat(filter.beforeRequestMessage).isNotNull();
		assertThat(filter.beforeRequestMessage).isEqualTo("Before request [uri=/hotels;headers=[Content-Type:\"application/json\", token:\"masked\"]]");

		assertThat(filter.afterRequestMessage).isNotNull();
		assertThat(filter.afterRequestMessage).isEqualTo("After request [uri=/hotels;headers=[Content-Type:\"application/json\", token:\"masked\"]]");
	}

	@Test
	public void payloadInputStream() throws Exception {
		filter.setIncludePayload(true);

		final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/hotels");
		MockHttpServletResponse response = new MockHttpServletResponse();

		final byte[] requestBody = "Hello World".getBytes(StandardCharsets.UTF_8);
		request.setContent(requestBody);

		FilterChain filterChain = (filterRequest, filterResponse) -> {
			((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
			byte[] buf = FileCopyUtils.copyToByteArray(filterRequest.getInputStream());
			assertThat(buf).isEqualTo(requestBody);
		};

		filter.doFilter(request, response, filterChain);

		assertThat(filter.afterRequestMessage).isNotNull();
		assertThat(filter.afterRequestMessage.contains("Hello World")).isTrue();
	}

	@Test
	public void payloadReader() throws Exception {
		filter.setIncludePayload(true);

		final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/hotels");
		MockHttpServletResponse response = new MockHttpServletResponse();

		final String requestBody = "Hello World";
		request.setContent(requestBody.getBytes(StandardCharsets.UTF_8));

		FilterChain filterChain = (filterRequest, filterResponse) -> {
			((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
			String buf = FileCopyUtils.copyToString(filterRequest.getReader());
			assertThat(buf).isEqualTo(requestBody);
		};

		filter.doFilter(request, response, filterChain);

		assertThat(filter.afterRequestMessage).isNotNull();
		assertThat(filter.afterRequestMessage.contains(requestBody)).isTrue();
	}

	@Test
	public void payloadMaxLength() throws Exception {
		filter.setIncludePayload(true);
		filter.setMaxPayloadLength(3);

		final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/hotels");
		MockHttpServletResponse response = new MockHttpServletResponse();

		final byte[] requestBody = "Hello World".getBytes(StandardCharsets.UTF_8);
		request.setContent(requestBody);

		FilterChain filterChain = (filterRequest, filterResponse) -> {
			((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
			byte[] buf = FileCopyUtils.copyToByteArray(filterRequest.getInputStream());
			assertThat(buf).isEqualTo(requestBody);
			ContentCachingRequestWrapper wrapper =
					WebUtils.getNativeRequest(filterRequest, ContentCachingRequestWrapper.class);
			assertThat(wrapper.getContentAsByteArray()).isEqualTo("Hel".getBytes(StandardCharsets.UTF_8));
		};

		filter.doFilter(request, response, filterChain);

		assertThat(filter.afterRequestMessage).isNotNull();
		assertThat(filter.afterRequestMessage.contains("Hel")).isTrue();
		assertThat(filter.afterRequestMessage.contains("Hello World")).isFalse();
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


	private static class NoOpFilterChain implements FilterChain {

		@Override
		public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
		}
	}

}
