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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;

import org.springframework.util.FileCopyUtils;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.testfixture.servlet.MockHttpSession;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.WebUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AbstractRequestLoggingFilter} and subclasses.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
class RequestLoggingFilterTests {

	private final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/hotels");
	private final MockHttpServletResponse response = new MockHttpServletResponse();
	private final FilterChain filterChain = (request, response) -> {};
	private final MyRequestLoggingFilter filter = new MyRequestLoggingFilter();


	@Test
	void defaultPrefix() throws Exception {
		applyFilter();

		assertThat(filter.beforeRequestMessage).startsWith(AbstractRequestLoggingFilter.DEFAULT_BEFORE_MESSAGE_PREFIX);
		assertThat(filter.afterRequestMessage).startsWith(AbstractRequestLoggingFilter.DEFAULT_AFTER_MESSAGE_PREFIX);
	}

	@Test
	void customPrefix() throws Exception {
		filter.setBeforeMessagePrefix("Before prefix: ");
		filter.setAfterMessagePrefix("After prefix: ");

		applyFilter();

		assertThat(filter.beforeRequestMessage).startsWith("Before prefix: ");
		assertThat(filter.afterRequestMessage).startsWith("After prefix: ");
	}

	@Test
	void defaultSuffix() throws Exception {
		applyFilter();

		assertThat(filter.beforeRequestMessage).endsWith(AbstractRequestLoggingFilter.DEFAULT_BEFORE_MESSAGE_SUFFIX);
		assertThat(filter.afterRequestMessage).endsWith(AbstractRequestLoggingFilter.DEFAULT_AFTER_MESSAGE_SUFFIX);
	}

	@Test
	void customSuffix() throws Exception {
		filter.setBeforeMessageSuffix("}");
		filter.setAfterMessageSuffix(")");

		applyFilter();

		assertThat(filter.beforeRequestMessage).endsWith("}");
		assertThat(filter.afterRequestMessage).endsWith(")");
	}

	@Test
	void method() throws Exception {
		filter.setBeforeMessagePrefix("");
		filter.setAfterMessagePrefix("");

		applyFilter();

		assertThat(filter.beforeRequestMessage).startsWith("POST");
		assertThat(filter.afterRequestMessage).startsWith("POST");
	}

	@Test
	void uri() throws Exception {
		request.setQueryString("booking=42");

		applyFilter();

		assertThat(filter.beforeRequestMessage).contains("/hotel").doesNotContain("booking=42");
		assertThat(filter.afterRequestMessage).contains("/hotel").doesNotContain("booking=42");
	}

	@Test
	void queryStringIncluded() throws Exception {
		request.setQueryString("booking=42");
		filter.setIncludeQueryString(true);

		applyFilter();

		assertThat(filter.beforeRequestMessage).contains("/hotels?booking=42");
		assertThat(filter.afterRequestMessage).contains("/hotels?booking=42");
	}

	@Test
	void noQueryStringAvailable() throws Exception {
		filter.setIncludeQueryString(true);

		applyFilter();

		assertThat(filter.beforeRequestMessage).contains("/hotels]");
		assertThat(filter.afterRequestMessage).contains("/hotels]");
	}

	@Test
	void client() throws Exception {
		request.setRemoteAddr("4.2.2.2");
		filter.setIncludeClientInfo(true);

		applyFilter();

		assertThat(filter.beforeRequestMessage).contains("client=4.2.2.2");
		assertThat(filter.afterRequestMessage).contains("client=4.2.2.2");
	}

	@Test
	void session() throws Exception {
		request.setSession(new MockHttpSession(null, "42"));
		filter.setIncludeClientInfo(true);

		applyFilter();

		assertThat(filter.beforeRequestMessage).contains("session=42");
		assertThat(filter.afterRequestMessage).contains("session=42");
	}

	@Test
	void user() throws Exception {
		request.setRemoteUser("Arthur");
		filter.setIncludeClientInfo(true);

		applyFilter();

		assertThat(filter.beforeRequestMessage).contains("user=Arthur");
		assertThat(filter.afterRequestMessage).contains("user=Arthur");
	}

	@Test
	void headers() throws Exception {
		request.setContentType("application/json");
		request.addHeader("token", "123");
		filter.setIncludeHeaders(true);
		filter.setHeaderPredicate(name -> !name.equalsIgnoreCase("token"));

		applyFilter();

		assertThat(filter.beforeRequestMessage)
			.isEqualTo("Before request [POST /hotels, headers=[Content-Type:\"application/json\", token:\"masked\"]]");
		assertThat(filter.afterRequestMessage)
			.isEqualTo("After request [POST /hotels, headers=[Content-Type:\"application/json\", token:\"masked\"]]");
	}

	@Test
	void payloadInputStream() throws Exception {
		filter.setIncludePayload(true);

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
	void payloadReader() throws Exception {
		filter.setIncludePayload(true);

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
	void payloadMaxLength() throws Exception {
		filter.setIncludePayload(true);
		filter.setMaxPayloadLength(3);

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
	void allOptions() throws Exception {
		filter.setIncludeQueryString(true);
		filter.setIncludeClientInfo(true);
		filter.setIncludeHeaders(true);
		filter.setIncludePayload(true);

		request.setQueryString("booking=42");
		request.setRemoteAddr("4.2.2.2");
		request.setSession(new MockHttpSession(null, "42"));
		request.setRemoteUser("Arthur");
		request.setContentType("application/json");
		String requestBody = "{\"msg\": \"Hello World\"}";
		request.setContent(requestBody.getBytes(StandardCharsets.UTF_8));

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

	private void applyFilter() throws Exception {
		filter.doFilter(request, response, filterChain);
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

}
