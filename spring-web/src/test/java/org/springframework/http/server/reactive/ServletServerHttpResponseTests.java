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

package org.springframework.http.server.reactive;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Duration;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.ResponseCookie;
import org.springframework.web.testfixture.servlet.DelegatingServletInputStream;
import org.springframework.web.testfixture.servlet.DelegatingServletOutputStream;
import org.springframework.web.testfixture.servlet.MockAsyncContext;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ServletServerHttpResponse}.
 *
 * @author SangHoon Han
 */
class ServletServerHttpResponseTests {

	@ParameterizedTest
	@ValueSource(longs = {
			36_500L * 24 * 60 * 60,  // ~100 years
			1L << 32,                // 2^32 seconds
			Long.MAX_VALUE
	})
	void applyCookiesWithMaxAgeExceedingIntegerRange(long maxAgeSeconds) throws Exception {
		MockHttpServletResponse servletResponse = new TestHttpServletResponse();
		ServletServerHttpResponse response = createResponse(servletResponse);

		response.getCookies().add("id", ResponseCookie.from("id", "1")
				.maxAge(Duration.ofSeconds(maxAgeSeconds)).build());
		response.applyCookies();

		assertThat(servletResponse.getCookie("id")).isNotNull();
		assertThat(servletResponse.getCookie("id").getMaxAge()).isEqualTo(Integer.MAX_VALUE);
	}


	private static ServletServerHttpResponse createResponse(MockHttpServletResponse servletResponse)
			throws Exception {

		MockHttpServletRequest servletRequest = new TestHttpServletRequest();
		AsyncContext asyncContext = new MockAsyncContext(servletRequest, servletResponse);
		ServletServerHttpRequest request = new ServletServerHttpRequest(servletRequest, asyncContext, "",
				DefaultDataBufferFactory.sharedInstance, 1024);
		return new ServletServerHttpResponse(servletResponse, asyncContext,
				DefaultDataBufferFactory.sharedInstance, request);
	}


	private static class TestHttpServletRequest extends MockHttpServletRequest {

		TestHttpServletRequest() {
			super("GET", "/");
		}

		@Override
		public ServletInputStream getInputStream() {
			return new DelegatingServletInputStream(new ByteArrayInputStream(new byte[0])) {
				@Override
				public void setReadListener(ReadListener readListener) {
					// Ignore
				}
			};
		}
	}


	private static class TestHttpServletResponse extends MockHttpServletResponse {

		@Override
		public ServletOutputStream getOutputStream() {
			return new DelegatingServletOutputStream(new ByteArrayOutputStream()) {
				@Override
				public void setWriteListener(WriteListener writeListener) {
					// Ignore
				}
			};
		}
	}

}
