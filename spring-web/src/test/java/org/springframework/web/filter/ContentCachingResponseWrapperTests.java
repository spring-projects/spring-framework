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

import org.springframework.util.FileCopyUtils;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.util.ContentCachingResponseWrapper;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.CONTENT_LENGTH;
import static org.springframework.http.HttpHeaders.TRANSFER_ENCODING;

/**
 * Tests for {@link ContentCachingResponseWrapper}.
 *
 * @author Rossen Stoyanchev
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
