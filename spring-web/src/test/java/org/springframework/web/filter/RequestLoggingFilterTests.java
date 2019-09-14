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

import java.nio.charset.StandardCharsets;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;

import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockHttpSession;
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
	public void defaultPrefix() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/hotels");
		MockHttpServletResponse response = new MockHttpServletResponse();

		FilterChain filterChain = new NoOpFilterChain();
		filter.doFilter(request, response, filterChain);

		assertThat(filter.beforeRequestMessage).startsWith(AbstractRequestLoggingFilter.DEFAULT_BEFORE_MESSAGE_PREFIX);
		assertThat(filter.afterRequestMessage).startsWith(AbstractRequestLoggingFilter.DEFAULT_AFTER_MESSAGE_PREFIX);
	}

	@Test
	public void customPrefix() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/hotels");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.setBeforeMessagePrefix("Before prefix: ");
		filter.setAfterMessagePrefix("After prefix: ");

		FilterChain filterChain = new NoOpFilterChain();
		filter.doFilter(request, response, filterChain);

		assertThat(filter.beforeRequestMessage).startsWith("Before prefix: ");
		assertThat(filter.afterRequestMessage).startsWith("After prefix: ");
	}

	@Test
	public void defaultSuffix() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/hotels");
		MockHttpServletResponse response = new MockHttpServletResponse();

		FilterChain filterChain = new NoOpFilterChain();
		filter.doFilter(request, response, filterChain);

		assertThat(filter.beforeRequestMessage).endsWith(AbstractRequestLoggingFilter.DEFAULT_BEFORE_MESSAGE_SUFFIX);
		assertThat(filter.afterRequestMessage).endsWith(AbstractRequestLoggingFilter.DEFAULT_AFTER_MESSAGE_SUFFIX);
	}

	@Test
	public void customSuffix() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/hotels");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.setBeforeMessageSuffix("}");
		filter.setAfterMessageSuffix(")");

		FilterChain filterChain = new NoOpFilterChain();
		filter.doFilter(request, response, filterChain);

		assertThat(filter.beforeRequestMessage).endsWith("}");
		assertThat(filter.afterRequestMessage).endsWith(")");
	}

	@Test
	public void method() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/hotels");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.setBeforeMessagePrefix("");
		filter.setAfterMessagePrefix("");

		FilterChain filterChain = new NoOpFilterChain();
		filter.doFilter(request, response, filterChain);

		assertThat(filter.beforeRequestMessage).startsWith("PATCH");
		assertThat(filter.afterRequestMessage).startsWith("PATCH");
	}

	@Test
	public void uri() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/hotels");
		MockHttpServletResponse response = new MockHttpServletResponse();

		request.setQueryString("booking=42");

		FilterChain filterChain = new NoOpFilterChain();
		filter.doFilter(request, response, filterChain);

		assertThat(filter.beforeRequestMessage).contains("/hotel");
		assertThat(filter.beforeRequestMessage).doesNotContain("booking=42");

		assertThat(filter.afterRequestMessage).contains("/hotel");
		assertThat(filter.afterRequestMessage).doesNotContain("booking=42");
	}

	@Test
	public void queryStringIncluded() throws Exception {
		filter.setIncludeQueryString(true);

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/hotels");
		MockHttpServletResponse response = new MockHttpServletResponse();

		request.setQueryString("booking=42");

		FilterChain filterChain = new NoOpFilterChain();
		filter.doFilter(request, response, filterChain);

		assertThat(filter.beforeRequestMessage).contains("/hotels?booking=42");
		assertThat(filter.afterRequestMessage).contains("/hotels?booking=42");
	}

	@Test
	public void noQueryStringAvailable() throws Exception {
		filter.setIncludeQueryString(true);

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/hotels");
		MockHttpServletResponse response = new MockHttpServletResponse();

		FilterChain filterChain = new NoOpFilterChain();
		filter.doFilter(request, response, filterChain);

		assertThat(filter.beforeRequestMessage).contains("/hotels]");
		assertThat(filter.afterRequestMessage).contains("/hotels]");
	}

	@Test
	public void client() throws Exception {
		filter.setIncludeClientInfo(true);

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/hotels");
		request.setRemoteAddr("4.2.2.2");
		MockHttpServletResponse response = new MockHttpServletResponse();

		FilterChain filterChain = new NoOpFilterChain();
		filter.doFilter(request, response, filterChain);

		assertThat(filter.beforeRequestMessage).contains("client=4.2.2.2");
		assertThat(filter.afterRequestMessage).contains("client=4.2.2.2");
	}

	@Test
	public void session() throws Exception {
		filter.setIncludeClientInfo(true);

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/hotels");
		MockHttpSession session = new MockHttpSession(null, "42");
		request.setSession(session);
		MockHttpServletResponse response = new MockHttpServletResponse();

		FilterChain filterChain = new NoOpFilterChain();
		filter.doFilter(request, response, filterChain);

		assertThat(filter.beforeRequestMessage).contains("session=42");
		assertThat(filter.afterRequestMessage).contains("session=42");
	}

	@Test
	public void user() throws Exception {
		filter.setIncludeClientInfo(true);

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/hotels");
		request.setRemoteUser("Arthur");
		MockHttpServletResponse response = new MockHttpServletResponse();

		FilterChain filterChain = new NoOpFilterChain();
		filter.doFilter(request, response, filterChain);

		assertThat(filter.beforeRequestMessage).contains("user=Arthur");
		assertThat(filter.afterRequestMessage).contains("user=Arthur");
	}

	@Test
	public void headers() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/hotels");
		request.setContentType("application/json");
		request.addHeader("token", "123");
		MockHttpServletResponse response = new MockHttpServletResponse();

		FilterChain filterChain = new NoOpFilterChain();
		filter.setIncludeHeaders(true);
		filter.setHeaderPredicate(name -> !name.equalsIgnoreCase("token"));
		filter.doFilter(request, response, filterChain);

		assertThat(filter.beforeRequestMessage).isEqualTo("Before request [POST /hotels, headers=[Content-Type:\"application/json\", token:\"masked\"]]");
		assertThat(filter.afterRequestMessage).isEqualTo("After request [POST /hotels, headers=[Content-Type:\"application/json\", token:\"masked\"]]");
	}

	@Test
	public void payloadInputStream() throws Exception {
		filter.setIncludePayload(true);

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/hotels");
		MockHttpServletResponse response = new MockHttpServletResponse();

		byte[] requestBody = "Hello World".getBytes(StandardCharsets.UTF_8);
		request.setContent(requestBody);

		FilterChain filterChain = (filterRequest, filterResponse) -> {
			((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
			byte[] buf = FileCopyUtils.copyToByteArray(filterRequest.getInputStream());
			assertThat(buf).isEqualTo(requestBody);
		};

		filter.doFilter(request, response, filterChain);

		assertThat(filter.afterRequestMessage).contains("Hello World");
	}

	@Test
	public void payloadReader() throws Exception {
		filter.setIncludePayload(true);

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/hotels");
		MockHttpServletResponse response = new MockHttpServletResponse();

		String requestBody = "Hello World";
		request.setContent(requestBody.getBytes(StandardCharsets.UTF_8));

		FilterChain filterChain = (filterRequest, filterResponse) -> {
			((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
			String buf = FileCopyUtils.copyToString(filterRequest.getReader());
			assertThat(buf).isEqualTo(requestBody);
		};

		filter.doFilter(request, response, filterChain);

		assertThat(filter.afterRequestMessage).contains(requestBody);
	}

	@Test
	public void payloadMaxLength() throws Exception {
		filter.setIncludePayload(true);
		filter.setMaxPayloadLength(3);

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/hotels");
		MockHttpServletResponse response = new MockHttpServletResponse();

		byte[] requestBody = "Hello World".getBytes(StandardCharsets.UTF_8);
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

		assertThat(filter.afterRequestMessage).contains("Hel");
		assertThat(filter.afterRequestMessage).doesNotContain("Hello World");
	}

	@Test
	public void allOptions() throws Exception {
		filter.setIncludeQueryString(true);
		filter.setIncludeClientInfo(true);
		filter.setIncludeHeaders(true);
		filter.setIncludePayload(true);

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/hotels");
		request.setQueryString("booking=42");
		request.setRemoteAddr("4.2.2.2");
		request.setSession(new MockHttpSession(null, "42"));
		request.setRemoteUser("Arthur");
		request.setContentType("application/json");
		String requestBody = "{\"msg\": \"Hello World\"}";
		request.setContent(requestBody.getBytes(StandardCharsets.UTF_8));
		MockHttpServletResponse response = new MockHttpServletResponse();

		FilterChain filterChain = (filterRequest, filterResponse) -> {
			((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
			String buf = FileCopyUtils.copyToString(filterRequest.getReader());
			assertThat(buf).isEqualTo(requestBody);
		};

		filter.doFilter(request, response, filterChain);

		assertThat(filter.beforeRequestMessage)
				.isEqualTo("Before request ["
						+ "POST /hotels?booking=42"
						+ ", client=4.2.2.2"
						+ ", session=42"
						+ ", user=Arthur"
						+ ", headers=[Content-Type:\"application/json;charset=ISO-8859-1\", Content-Length:\"22\"]"
						+ "]");

		assertThat(filter.afterRequestMessage)
				.isEqualTo("After request ["
						+ "POST /hotels?booking=42"
						+ ", client=4.2.2.2"
						+ ", session=42"
						+ ", user=Arthur"
						+ ", headers=[Content-Type:\"application/json;charset=ISO-8859-1\", Content-Length:\"22\"]"
						+ ", payload={\"msg\": \"Hello World\"}"
						+ "]");
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
		public void doFilter(ServletRequest request, ServletResponse response) {
		}
	}

}
