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

package org.springframework.http.server;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 */
public class ServletServerHttpResponseTests {

	private ServletServerHttpResponse response;

	private MockHttpServletResponse mockResponse;


	@BeforeEach
	public void create() throws Exception {
		mockResponse = new MockHttpServletResponse();
		response = new ServletServerHttpResponse(mockResponse);
	}


	@Test
	public void setStatusCode() throws Exception {
		response.setStatusCode(HttpStatus.NOT_FOUND);
		assertThat(mockResponse.getStatus()).as("Invalid status code").isEqualTo(404);
	}

	@Test
	public void getHeaders() throws Exception {
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
	public void preExistingHeadersFromHttpServletResponse() {
		String headerName = "Access-Control-Allow-Origin";
		String headerValue = "localhost:8080";

		this.mockResponse.addHeader(headerName, headerValue);
		this.response = new ServletServerHttpResponse(this.mockResponse);

		assertThat(this.response.getHeaders().getFirst(headerName)).isEqualTo(headerValue);
		assertThat(this.response.getHeaders().get(headerName)).isEqualTo(Collections.singletonList(headerValue));
		assertThat(this.response.getHeaders().containsKey(headerName)).isTrue();
		assertThat(this.response.getHeaders().getFirst(headerName)).isEqualTo(headerValue);
		assertThat(this.response.getHeaders().getAccessControlAllowOrigin()).isEqualTo(headerValue);
	}

	@Test
	public void getBody() throws Exception {
		byte[] content = "Hello World".getBytes("UTF-8");
		FileCopyUtils.copy(content, response.getBody());

		assertThat(mockResponse.getContentAsByteArray()).as("Invalid content written").isEqualTo(content);
	}

}
