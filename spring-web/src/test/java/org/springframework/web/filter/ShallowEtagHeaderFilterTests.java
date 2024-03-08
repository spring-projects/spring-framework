/*
 * Copyright 2002-2024 the original author or authors.
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

import java.io.InputStream;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import org.springframework.util.FileCopyUtils;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

/**
 * Tests for {@link ShallowEtagHeaderFilter}.
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
class ShallowEtagHeaderFilterTests {

	private final ShallowEtagHeaderFilter filter = new ShallowEtagHeaderFilter();


	@Test
	void isEligibleForEtag() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels");
		MockHttpServletResponse response = new MockHttpServletResponse();

		assertThat(filter.isEligibleForEtag(request, response, 200, InputStream.nullInputStream())).isTrue();
		assertThat(filter.isEligibleForEtag(request, response, 300, InputStream.nullInputStream())).isFalse();

		request = new MockHttpServletRequest("HEAD", "/hotels");
		assertThat(filter.isEligibleForEtag(request, response, 200, InputStream.nullInputStream())).isFalse();

		request = new MockHttpServletRequest("POST", "/hotels");
		assertThat(filter.isEligibleForEtag(request, response, 200, InputStream.nullInputStream())).isFalse();

		request = new MockHttpServletRequest("POST", "/hotels");
		request.addHeader("Cache-Control","must-revalidate, no-store");
		assertThat(filter.isEligibleForEtag(request, response, 200, InputStream.nullInputStream())).isFalse();
	}

	@Test
	void filterNoMatch() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels");
		MockHttpServletResponse response = new MockHttpServletResponse();

		byte[] responseBody = "Hello World".getBytes(UTF_8);
		FilterChain filterChain = (filterRequest, filterResponse) -> {
			assertThat(filterRequest).as("Invalid request passed").isEqualTo(request);
			((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
			filterResponse.setContentType(TEXT_PLAIN_VALUE);
			FileCopyUtils.copy(responseBody, filterResponse.getOutputStream());
		};
		filter.doFilter(request, response, filterChain);

		assertThat(response.getStatus()).as("Invalid status").isEqualTo(200);
		assertThat(response.getHeader("ETag")).as("Invalid ETag").isEqualTo("\"0b10a8db164e0754105b7a99be72e3fe5\"");
		assertThat(response.getContentLength()).as("Invalid Content-Length header").isGreaterThan(0);
		assertThat(response.getContentType()).as("Invalid Content-Type header").isEqualTo(TEXT_PLAIN_VALUE);
		assertThat(response.getContentAsByteArray()).as("Invalid content").isEqualTo(responseBody);
	}

	@Test
	void filterNoMatchWeakETag() throws Exception {
		this.filter.setWriteWeakETag(true);
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels");
		MockHttpServletResponse response = new MockHttpServletResponse();

		byte[] responseBody = "Hello World".getBytes(UTF_8);
		FilterChain filterChain = (filterRequest, filterResponse) -> {
			assertThat(filterRequest).as("Invalid request passed").isEqualTo(request);
			((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
			filterResponse.setContentType(TEXT_PLAIN_VALUE);
			FileCopyUtils.copy(responseBody, filterResponse.getOutputStream());
		};
		filter.doFilter(request, response, filterChain);

		assertThat(response.getStatus()).as("Invalid status").isEqualTo(200);
		assertThat(response.getHeader("ETag")).as("Invalid ETag").isEqualTo("W/\"0b10a8db164e0754105b7a99be72e3fe5\"");
		assertThat(response.getContentLength()).as("Invalid Content-Length header").isGreaterThan(0);
		assertThat(response.getContentType()).as("Invalid Content-Type header").isEqualTo(TEXT_PLAIN_VALUE);
		assertThat(response.getContentAsByteArray()).as("Invalid content").isEqualTo(responseBody);
	}

	@Test
	void filterMatch() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels");
		String etag = "\"0b10a8db164e0754105b7a99be72e3fe5\"";
		request.addHeader("If-None-Match", etag);
		MockHttpServletResponse response = new MockHttpServletResponse();

		FilterChain filterChain = (filterRequest, filterResponse) -> {
			assertThat(filterRequest).as("Invalid request passed").isEqualTo(request);
			byte[] responseBody = "Hello World".getBytes(UTF_8);
			filterResponse.setContentLength(responseBody.length);
			filterResponse.setContentType(TEXT_PLAIN_VALUE);
			FileCopyUtils.copy(responseBody, filterResponse.getOutputStream());
		};
		filter.doFilter(request, response, filterChain);

		assertThat(response.getStatus()).as("Invalid status").isEqualTo(304);
		assertThat(response.getHeader("ETag")).as("Invalid ETag").isEqualTo("\"0b10a8db164e0754105b7a99be72e3fe5\"");
		assertThat(response.containsHeader("Content-Length")).as("Response has Content-Length header").isFalse();
		assertThat(response.getContentType()).as("Invalid Content-Type header").isEqualTo(TEXT_PLAIN_VALUE);
		assertThat(response.getContentAsByteArray()).as("Invalid content").isEmpty();
	}

	@Test
	void filterMatchWeakEtag() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels");
		String etag = "\"0b10a8db164e0754105b7a99be72e3fe5\"";
		request.addHeader("If-None-Match", "W/" + etag);
		MockHttpServletResponse response = new MockHttpServletResponse();

		FilterChain filterChain = (filterRequest, filterResponse) -> {
			assertThat(filterRequest).as("Invalid request passed").isEqualTo(request);
			byte[] responseBody = "Hello World".getBytes(UTF_8);
			FileCopyUtils.copy(responseBody, filterResponse.getOutputStream());
			filterResponse.setContentLength(responseBody.length);
		};
		filter.doFilter(request, response, filterChain);

		assertThat(response.getStatus()).as("Invalid status").isEqualTo(304);
		assertThat(response.getHeader("ETag")).as("Invalid ETag").isEqualTo("\"0b10a8db164e0754105b7a99be72e3fe5\"");
		assertThat(response.containsHeader("Content-Length")).as("Response has Content-Length header").isFalse();
		assertThat(response.getContentAsByteArray()).as("Invalid content").isEmpty();
	}

	@Test
	void filterWriter() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels");
		String etag = "\"0b10a8db164e0754105b7a99be72e3fe5\"";
		request.addHeader("If-None-Match", etag);
		MockHttpServletResponse response = new MockHttpServletResponse();

		FilterChain filterChain = (filterRequest, filterResponse) -> {
			assertThat(filterRequest).as("Invalid request passed").isEqualTo(request);
			((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
			String responseBody = "Hello World";
			FileCopyUtils.copy(responseBody, filterResponse.getWriter());
		};
		filter.doFilter(request, response, filterChain);

		assertThat(response.getStatus()).as("Invalid status").isEqualTo(304);
		assertThat(response.getHeader("ETag")).as("Invalid ETag").isEqualTo("\"0b10a8db164e0754105b7a99be72e3fe5\"");
		assertThat(response.containsHeader("Content-Length")).as("Response has Content-Length header").isFalse();
		assertThat(response.getContentAsByteArray()).as("Invalid content").isEmpty();
	}

	@Test  // SPR-12960
	public void filterWriterWithDisabledCaching() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels");
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.setContentType(TEXT_PLAIN_VALUE);

		byte[] responseBody = "Hello World".getBytes(UTF_8);
		FilterChain filterChain = (filterRequest, filterResponse) -> {
			assertThat(filterRequest).as("Invalid request passed").isEqualTo(request);
			((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
			filterResponse.setContentType(APPLICATION_JSON_VALUE);
			FileCopyUtils.copy(responseBody, filterResponse.getOutputStream());
		};

		ShallowEtagHeaderFilter.disableContentCaching(request);
		this.filter.doFilter(request, response, filterChain);

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getHeader("ETag")).isNull();
		assertThat(response.getContentType()).as("Invalid Content-Type header").isEqualTo(APPLICATION_JSON_VALUE);
		assertThat(response.getContentAsByteArray()).isEqualTo(responseBody);
	}

	@Test
	void filterSendError() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels");
		MockHttpServletResponse response = new MockHttpServletResponse();

		byte[] responseBody = "Hello World".getBytes(UTF_8);
		FilterChain filterChain = (filterRequest, filterResponse) -> {
			assertThat(filterRequest).as("Invalid request passed").isEqualTo(request);
			response.setContentLength(100);
			FileCopyUtils.copy(responseBody, filterResponse.getOutputStream());
			((HttpServletResponse) filterResponse).sendError(HttpServletResponse.SC_FORBIDDEN);
		};
		filter.doFilter(request, response, filterChain);

		assertThat(response.getStatus()).as("Invalid status").isEqualTo(403);
		assertThat(response.getHeader("ETag")).as("Invalid ETag").isNull();
		assertThat(response.getContentLength()).as("Invalid Content-Length header").isEqualTo(100);
		assertThat(response.getContentAsByteArray()).as("Invalid content").isEqualTo(responseBody);
	}

	@Test
	void filterSendErrorMessage() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels");
		MockHttpServletResponse response = new MockHttpServletResponse();

		byte[] responseBody = "Hello World".getBytes(UTF_8);
		FilterChain filterChain = (filterRequest, filterResponse) -> {
			assertThat(filterRequest).as("Invalid request passed").isEqualTo(request);
			response.setContentLength(100);
			FileCopyUtils.copy(responseBody, filterResponse.getOutputStream());
			((HttpServletResponse) filterResponse).sendError(HttpServletResponse.SC_FORBIDDEN, "ERROR");
		};
		filter.doFilter(request, response, filterChain);

		assertThat(response.getStatus()).as("Invalid status").isEqualTo(403);
		assertThat(response.getHeader("ETag")).as("Invalid ETag").isNull();
		assertThat(response.getContentLength()).as("Invalid Content-Length header").isEqualTo(100);
		assertThat(response.getContentAsByteArray()).as("Invalid content").isEqualTo(responseBody);
		assertThat(response.getErrorMessage()).as("Invalid error message").isEqualTo("ERROR");
	}

	@Test
	void filterSendRedirect() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels");
		MockHttpServletResponse response = new MockHttpServletResponse();

		byte[] responseBody = "Hello World".getBytes(UTF_8);
		FilterChain filterChain = (filterRequest, filterResponse) -> {
			assertThat(filterRequest).as("Invalid request passed").isEqualTo(request);
			response.setContentLength(100);
			FileCopyUtils.copy(responseBody, filterResponse.getOutputStream());
			((HttpServletResponse) filterResponse).sendRedirect("https://www.google.com");
		};
		filter.doFilter(request, response, filterChain);

		assertThat(response.getStatus()).as("Invalid status").isEqualTo(302);
		assertThat(response.getHeader("ETag")).as("Invalid ETag").isNull();
		assertThat(response.getContentLength()).as("Invalid Content-Length header").isEqualTo(100);
		assertThat(response.getContentAsByteArray()).as("Invalid content").isEqualTo(responseBody);
		assertThat(response.getRedirectedUrl()).as("Invalid redirect URL").isEqualTo("https://www.google.com");
	}

	@Test // SPR-13717
	public void filterFlushResponse() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels");
		MockHttpServletResponse response = new MockHttpServletResponse();

		byte[] responseBody = "Hello World".getBytes(UTF_8);
		FilterChain filterChain = (filterRequest, filterResponse) -> {
			assertThat(filterRequest).as("Invalid request passed").isEqualTo(request);
			((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
			FileCopyUtils.copy(responseBody, filterResponse.getOutputStream());
			filterResponse.flushBuffer();
		};
		filter.doFilter(request, response, filterChain);

		assertThat(response.getStatus()).as("Invalid status").isEqualTo(200);
		assertThat(response.getHeader("ETag")).as("Invalid ETag").isEqualTo("\"0b10a8db164e0754105b7a99be72e3fe5\"");
		assertThat(response.getContentLength()).as("Invalid Content-Length header").isGreaterThan(0);
		assertThat(response.getContentAsByteArray()).as("Invalid content").isEqualTo(responseBody);
	}

}
