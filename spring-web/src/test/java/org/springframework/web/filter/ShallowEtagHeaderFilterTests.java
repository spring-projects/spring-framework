/*
 * Copyright 2002-2020 the original author or authors.
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
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;

import org.springframework.util.FileCopyUtils;
import org.springframework.util.StreamUtils;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

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

		assertThat(filter.isEligibleForEtag(request, response, 200, StreamUtils.emptyInput())).isTrue();
		assertThat(filter.isEligibleForEtag(request, response, 300, StreamUtils.emptyInput())).isFalse();

		request = new MockHttpServletRequest("HEAD", "/hotels");
		assertThat(filter.isEligibleForEtag(request, response, 200, StreamUtils.emptyInput())).isFalse();

		request = new MockHttpServletRequest("POST", "/hotels");
		assertThat(filter.isEligibleForEtag(request, response, 200, StreamUtils.emptyInput())).isFalse();

		request = new MockHttpServletRequest("POST", "/hotels");
		request.addHeader("Cache-Control","must-revalidate, no-store");
		assertThat(filter.isEligibleForEtag(request, response, 200, StreamUtils.emptyInput())).isFalse();
	}

	@Test
	public void filterNoMatch() throws Exception {
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels");
		MockHttpServletResponse response = new MockHttpServletResponse();

		final byte[] responseBody = "Hello World".getBytes(StandardCharsets.UTF_8);
		FilterChain filterChain = (filterRequest, filterResponse) -> {
			assertThat(filterRequest).as("Invalid request passed").isEqualTo(request);
			((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
			FileCopyUtils.copy(responseBody, filterResponse.getOutputStream());
		};
		filter.doFilter(request, response, filterChain);

		assertThat(response.getStatus()).as("Invalid status").isEqualTo(200);
		assertThat(response.getHeader("ETag")).as("Invalid ETag").isEqualTo("\"0b10a8db164e0754105b7a99be72e3fe5\"");
		assertThat(response.getContentLength() > 0).as("Invalid Content-Length header").isTrue();
		assertThat(response.getContentAsByteArray()).as("Invalid content").isEqualTo(responseBody);
	}

	@Test
	public void filterNoMatchWeakETag() throws Exception {
		this.filter.setWriteWeakETag(true);
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels");
		MockHttpServletResponse response = new MockHttpServletResponse();

		final byte[] responseBody = "Hello World".getBytes(StandardCharsets.UTF_8);
		FilterChain filterChain = (filterRequest, filterResponse) -> {
			assertThat(filterRequest).as("Invalid request passed").isEqualTo(request);
			((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
			FileCopyUtils.copy(responseBody, filterResponse.getOutputStream());
		};
		filter.doFilter(request, response, filterChain);

		assertThat(response.getStatus()).as("Invalid status").isEqualTo(200);
		assertThat(response.getHeader("ETag")).as("Invalid ETag").isEqualTo("W/\"0b10a8db164e0754105b7a99be72e3fe5\"");
		assertThat(response.getContentLength() > 0).as("Invalid Content-Length header").isTrue();
		assertThat(response.getContentAsByteArray()).as("Invalid content").isEqualTo(responseBody);
	}

	@Test
	public void filterMatch() throws Exception {
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels");
		String etag = "\"0b10a8db164e0754105b7a99be72e3fe5\"";
		request.addHeader("If-None-Match", etag);
		MockHttpServletResponse response = new MockHttpServletResponse();

		FilterChain filterChain = (filterRequest, filterResponse) -> {
			assertThat(filterRequest).as("Invalid request passed").isEqualTo(request);
			byte[] responseBody = "Hello World".getBytes(StandardCharsets.UTF_8);
			FileCopyUtils.copy(responseBody, filterResponse.getOutputStream());
			filterResponse.setContentLength(responseBody.length);
		};
		filter.doFilter(request, response, filterChain);

		assertThat(response.getStatus()).as("Invalid status").isEqualTo(304);
		assertThat(response.getHeader("ETag")).as("Invalid ETag").isEqualTo("\"0b10a8db164e0754105b7a99be72e3fe5\"");
		assertThat(response.containsHeader("Content-Length")).as("Response has Content-Length header").isFalse();
		byte[] expecteds = new byte[0];
		assertThat(response.getContentAsByteArray()).as("Invalid content").isEqualTo(expecteds);
	}

	@Test
	public void filterMatchWeakEtag() throws Exception {
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels");
		String etag = "\"0b10a8db164e0754105b7a99be72e3fe5\"";
		request.addHeader("If-None-Match", "W/" + etag);
		MockHttpServletResponse response = new MockHttpServletResponse();

		FilterChain filterChain = (filterRequest, filterResponse) -> {
			assertThat(filterRequest).as("Invalid request passed").isEqualTo(request);
			byte[] responseBody = "Hello World".getBytes(StandardCharsets.UTF_8);
			FileCopyUtils.copy(responseBody, filterResponse.getOutputStream());
			filterResponse.setContentLength(responseBody.length);
		};
		filter.doFilter(request, response, filterChain);

		assertThat(response.getStatus()).as("Invalid status").isEqualTo(304);
		assertThat(response.getHeader("ETag")).as("Invalid ETag").isEqualTo("\"0b10a8db164e0754105b7a99be72e3fe5\"");
		assertThat(response.containsHeader("Content-Length")).as("Response has Content-Length header").isFalse();
		byte[] expecteds = new byte[0];
		assertThat(response.getContentAsByteArray()).as("Invalid content").isEqualTo(expecteds);
	}

	@Test
	public void filterWriter() throws Exception {
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels");
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
		byte[] expecteds = new byte[0];
		assertThat(response.getContentAsByteArray()).as("Invalid content").isEqualTo(expecteds);
	}

	@Test  // SPR-12960
	public void filterWriterWithDisabledCaching() throws Exception {
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels");
		MockHttpServletResponse response = new MockHttpServletResponse();

		final byte[] responseBody = "Hello World".getBytes(StandardCharsets.UTF_8);
		FilterChain filterChain = (filterRequest, filterResponse) -> {
			assertThat(filterRequest).as("Invalid request passed").isEqualTo(request);
			((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
			FileCopyUtils.copy(responseBody, filterResponse.getOutputStream());
		};

		ShallowEtagHeaderFilter.disableContentCaching(request);
		this.filter.doFilter(request, response, filterChain);

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getHeader("ETag")).isNull();
		assertThat(response.getContentAsByteArray()).isEqualTo(responseBody);
	}

	@Test
	public void filterSendError() throws Exception {
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels");
		MockHttpServletResponse response = new MockHttpServletResponse();

		final byte[] responseBody = "Hello World".getBytes(StandardCharsets.UTF_8);
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
	public void filterSendErrorMessage() throws Exception {
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels");
		MockHttpServletResponse response = new MockHttpServletResponse();

		final byte[] responseBody = "Hello World".getBytes(StandardCharsets.UTF_8);
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
	public void filterSendRedirect() throws Exception {
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels");
		MockHttpServletResponse response = new MockHttpServletResponse();

		final byte[] responseBody = "Hello World".getBytes(StandardCharsets.UTF_8);
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
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels");
		MockHttpServletResponse response = new MockHttpServletResponse();

		final byte[] responseBody = "Hello World".getBytes(StandardCharsets.UTF_8);
		FilterChain filterChain = (filterRequest, filterResponse) -> {
			assertThat(filterRequest).as("Invalid request passed").isEqualTo(request);
			((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
			FileCopyUtils.copy(responseBody, filterResponse.getOutputStream());
			filterResponse.flushBuffer();
		};
		filter.doFilter(request, response, filterChain);

		assertThat(response.getStatus()).as("Invalid status").isEqualTo(200);
		assertThat(response.getHeader("ETag")).as("Invalid ETag").isEqualTo("\"0b10a8db164e0754105b7a99be72e3fe5\"");
		assertThat(response.getContentLength() > 0).as("Invalid Content-Length header").isTrue();
		assertThat(response.getContentAsByteArray()).as("Invalid content").isEqualTo(responseBody);
	}

}
