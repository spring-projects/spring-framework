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

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.util.ContentCachingResponseWrapper;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.CONTENT_LENGTH;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.TRANSFER_ENCODING;

/**
 * Tests for {@link ContentCachingResponseWrapper}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
class ContentCachingResponseWrapperTests {

	@Test
	void copyBodyToResponse() throws Exception {
		byte[] responseBody = "Hello World".getBytes(UTF_8);
		MockHttpServletResponse response = new MockHttpServletResponse();

		ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
		responseWrapper.setStatus(HttpServletResponse.SC_OK);
		FileCopyUtils.copy(responseBody, responseWrapper.getOutputStream());
		responseWrapper.copyBodyToResponse();

		assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
		assertThat(response.getContentLength()).isGreaterThan(0);
		assertThat(response.getContentAsByteArray()).isEqualTo(responseBody);
	}

	@Test
	void copyBodyToResponseWithPresetHeaders() throws Exception {
		String PUZZLE = "puzzle";
		String ENIGMA = "enigma";
		String NUMBER = "number";
		String MAGIC = "42";

		byte[] responseBody = "Hello World".getBytes(UTF_8);
		String responseLength = Integer.toString(responseBody.length);
		String contentType = MediaType.APPLICATION_JSON_VALUE;

		MockHttpServletResponse response = new MockHttpServletResponse();
		response.setContentType(contentType);
		response.setContentLength(999);
		response.setHeader(PUZZLE, ENIGMA);
		response.setIntHeader(NUMBER, 42);

		ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
		responseWrapper.setStatus(HttpServletResponse.SC_OK);

		assertThat(responseWrapper.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
		assertThat(responseWrapper.getContentSize()).isZero();
		assertThat(responseWrapper.getHeaderNames())
				.containsExactlyInAnyOrder(PUZZLE, NUMBER, CONTENT_TYPE, CONTENT_LENGTH);

		assertThat(responseWrapper.containsHeader(PUZZLE)).as(PUZZLE).isTrue();
		assertThat(responseWrapper.getHeader(PUZZLE)).as(PUZZLE).isEqualTo(ENIGMA);
		assertThat(responseWrapper.getHeaders(PUZZLE)).as(PUZZLE).containsExactly(ENIGMA);

		assertThat(responseWrapper.containsHeader(NUMBER)).as(NUMBER).isTrue();
		assertThat(responseWrapper.getHeader(NUMBER)).as(NUMBER).isEqualTo(MAGIC);
		assertThat(responseWrapper.getHeaders(NUMBER)).as(NUMBER).containsExactly(MAGIC);

		assertThat(responseWrapper.containsHeader(CONTENT_TYPE)).as(CONTENT_TYPE).isTrue();
		assertThat(responseWrapper.getHeader(CONTENT_TYPE)).as(CONTENT_TYPE).isEqualTo(contentType);
		assertThat(responseWrapper.getHeaders(CONTENT_TYPE)).as(CONTENT_TYPE).containsExactly(contentType);
		assertThat(responseWrapper.getContentType()).as(CONTENT_TYPE).isEqualTo(contentType);

		assertThat(responseWrapper.containsHeader(CONTENT_LENGTH)).as(CONTENT_LENGTH).isTrue();
		assertThat(responseWrapper.getHeader(CONTENT_LENGTH)).as(CONTENT_LENGTH).isEqualTo("999");
		assertThat(responseWrapper.getHeaders(CONTENT_LENGTH)).as(CONTENT_LENGTH).containsExactly("999");

		FileCopyUtils.copy(responseBody, responseWrapper.getOutputStream());
		responseWrapper.copyBodyToResponse();

		assertThat(responseWrapper.getHeaderNames())
				.containsExactlyInAnyOrder(PUZZLE, NUMBER, CONTENT_TYPE, CONTENT_LENGTH);

		assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
		assertThat(response.getContentType()).isEqualTo(contentType);
		assertThat(response.getContentLength()).isEqualTo(responseBody.length);
		assertThat(response.getContentAsByteArray()).isEqualTo(responseBody);
		assertThat(response.getHeaderNames())
				.containsExactlyInAnyOrder(PUZZLE, NUMBER, CONTENT_TYPE, CONTENT_LENGTH);

		assertThat(response.containsHeader(PUZZLE)).as(PUZZLE).isTrue();
		assertThat(response.getHeader(PUZZLE)).as(PUZZLE).isEqualTo(ENIGMA);
		assertThat(response.getHeaders(PUZZLE)).as(PUZZLE).containsExactly(ENIGMA);

		assertThat(response.containsHeader(NUMBER)).as(NUMBER).isTrue();
		assertThat(response.getHeader(NUMBER)).as(NUMBER).isEqualTo(MAGIC);
		assertThat(response.getHeaders(NUMBER)).as(NUMBER).containsExactly(MAGIC);

		assertThat(response.containsHeader(CONTENT_TYPE)).as(CONTENT_TYPE).isTrue();
		assertThat(response.getHeader(CONTENT_TYPE)).as(CONTENT_TYPE).isEqualTo(contentType);
		assertThat(response.getHeaders(CONTENT_TYPE)).as(CONTENT_TYPE).containsExactly(contentType);
		assertThat(response.getContentType()).as(CONTENT_TYPE).isEqualTo(contentType);

		assertThat(response.containsHeader(CONTENT_LENGTH)).as(CONTENT_LENGTH).isTrue();
		assertThat(response.getHeader(CONTENT_LENGTH)).as(CONTENT_LENGTH).isEqualTo(responseLength);
		assertThat(response.getHeaders(CONTENT_LENGTH)).as(CONTENT_LENGTH).containsExactly(responseLength);
	}

	@Test
	void copyBodyToResponseWithTransferEncoding() throws Exception {
		byte[] responseBody = "6\r\nHello 5\r\nWorld0\r\n\r\n".getBytes(UTF_8);
		MockHttpServletResponse response = new MockHttpServletResponse();

		ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
		responseWrapper.setStatus(HttpServletResponse.SC_OK);
		responseWrapper.setHeader(TRANSFER_ENCODING, "chunked");
		FileCopyUtils.copy(responseBody, responseWrapper.getOutputStream());
		responseWrapper.copyBodyToResponse();

		assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
		assertThat(response.getHeader(TRANSFER_ENCODING)).isEqualTo("chunked");
		assertThat(response.getHeader(CONTENT_LENGTH)).isNull();
		assertThat(response.getContentAsByteArray()).isEqualTo(responseBody);
	}

}
