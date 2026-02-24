/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.http.server;

import java.nio.charset.StandardCharsets;
import java.util.List;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.SpringProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ServletServerHttpResponse}.
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 */
class ServletServerHttpResponseTests {

	private ServletServerHttpResponse response;

	private MockHttpServletResponse mockResponse;


	@BeforeEach
	void create() {
		mockResponse = new MockHttpServletResponse();
		response = new ServletServerHttpResponse(mockResponse);
	}


	@Test
	void setStatusCode() {
		response.setStatusCode(HttpStatus.NOT_FOUND);
		assertThat(mockResponse.getStatus()).as("Invalid status code").isEqualTo(404);
	}

	@Test
	void getHeaders() {
		HttpHeaders headers = response.getHeaders();
		String headerName = "MyHeader";
		String headerValue1 = "value1";
		headers.add(headerName, headerValue1);
		String headerValue2 = "value2";
		headers.add(headerName, headerValue2);
		headers.setContentType(new MediaType("text", "plain", StandardCharsets.UTF_8));

		response.close();
		assertThat(mockResponse.getHeaderNames().contains(headerName)).as("Header not set").isTrue();
		List<String> headerValues = mockResponse.getHeaders(headerName);
		assertThat(headerValues.contains(headerValue1)).as("Header not set").isTrue();
		assertThat(headerValues.contains(headerValue2)).as("Header not set").isTrue();
		assertThat(mockResponse.getHeader("Content-Type")).as("Invalid Content-Type").isEqualTo("text/plain;charset=UTF-8");
		assertThat(mockResponse.getContentType()).as("Invalid Content-Type").isEqualTo("text/plain;charset=UTF-8");
		assertThat(mockResponse.getCharacterEncoding()).as("Invalid Content-Type").isEqualTo("UTF-8");
	}

	@Test
	void getHeadersWithNoContentType() {
		this.response = new ServletServerHttpResponse(this.mockResponse);
		assertThat(this.response.getHeaders().get(HttpHeaders.CONTENT_TYPE)).isNull();
	}

	@Test
	void getHeadersWithContentType() {
		this.mockResponse.setContentType(MediaType.TEXT_PLAIN_VALUE);
		this.response = new ServletServerHttpResponse(this.mockResponse);
		assertThat(this.response.getHeaders().get(HttpHeaders.CONTENT_TYPE)).containsExactly(MediaType.TEXT_PLAIN_VALUE);
	}

	@Test
	void preExistingHeadersFromHttpServletResponse() {
		String headerName = "Access-Control-Allow-Origin";
		String headerValue = "localhost:8080";

		this.mockResponse.addHeader(headerName, headerValue);
		this.mockResponse.setContentType("text/csv");
		this.response = new ServletServerHttpResponse(this.mockResponse);

		assertThat(this.response.getHeaders().getFirst(headerName)).isEqualTo(headerValue);
		assertThat(this.response.getHeaders().get(headerName)).containsExactly(headerValue);
		assertThat(this.response.getHeaders().containsHeader(headerName)).isTrue();
		assertThat(this.response.getHeaders().getAccessControlAllowOrigin()).isEqualTo(headerValue);
	}

	@Test // gh-25490
	void preExistingContentTypeIsOverriddenImmediately() {
		this.mockResponse.setContentType("text/csv");
		this.response = new ServletServerHttpResponse(this.mockResponse);
		this.response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
	}

	@Test
	void getBody() throws Exception {
		byte[] content = "Hello World".getBytes(StandardCharsets.UTF_8);
		FileCopyUtils.copy(content, response.getBody());

		assertThat(mockResponse.getContentAsByteArray()).as("Invalid content written").isEqualTo(content);
	}

	@Test
	void skipFlushCallsOnOutputStream() throws Exception {
		ServletOutputStream mockStream = mock();
		HttpServletResponse mockResponse = mock();
		when(mockResponse.getOutputStream()).thenReturn(mockStream);

		this.response = new ServletServerHttpResponse(mockResponse);
		byte[] content = "Hello World".getBytes(StandardCharsets.UTF_8);
		FileCopyUtils.copy(content, response.getBody());
		response.getBody().flush();
		verify(mockStream, never()).flush();
	}

	@Test
	void appliesFlushCallsOnOutputStream() throws Exception {
		SpringProperties.setProperty(ServletServerHttpResponse.BODY_FLUSH_ENABLED, Boolean.TRUE.toString());
		ServletOutputStream mockStream = mock();
		HttpServletResponse mockResponse = mock();
		when(mockResponse.getOutputStream()).thenReturn(mockStream);

		this.response = new ServletServerHttpResponse(mockResponse);
		byte[] content = "Hello World".getBytes(StandardCharsets.UTF_8);
		FileCopyUtils.copy(content, response.getBody());
		response.getBody().flush();
		verify(mockStream).flush();

		SpringProperties.setProperty(ServletServerHttpResponse.BODY_FLUSH_ENABLED, null);
	}

}
