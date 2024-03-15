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

package org.springframework.test.web.servlet.assertj;


import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Tests for {@link AbstractMockHttpServletResponseAssert}.
 *
 * @author Stephane Nicoll
 */
public class AbstractMockHttpServletResponseAssertTests {

	@Test
	void hasForwardedUrl() {
		String forwardedUrl = "https://example.com/42";
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.setForwardedUrl(forwardedUrl);
		assertThat(response).hasForwardedUrl(forwardedUrl);
	}

	@Test
	void hasForwardedUrlWithWrongValue() {
		String forwardedUrl = "https://example.com/42";
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.setForwardedUrl(forwardedUrl);
		Assertions.assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(response).hasForwardedUrl("another"))
				.withMessageContainingAll("Forwarded URL", forwardedUrl, "another");
	}

	@Test
	void hasRedirectedUrl() {
		String redirectedUrl = "https://example.com/42";
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.addHeader(HttpHeaders.LOCATION, redirectedUrl);
		assertThat(response).hasRedirectedUrl(redirectedUrl);
	}

	@Test
	void hasRedirectedUrlWithWrongValue() {
		String redirectedUrl = "https://example.com/42";
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.addHeader(HttpHeaders.LOCATION, redirectedUrl);
		Assertions.assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(response).hasRedirectedUrl("another"))
				.withMessageContainingAll("Redirected URL", redirectedUrl, "another");
	}

	@Test
	void bodyHasContent() throws UnsupportedEncodingException {
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.getWriter().write("OK");
		assertThat(response).body().asString().isEqualTo("OK");
	}

	@Test
	void bodyHasContentWithResponseCharacterEncoding() throws UnsupportedEncodingException {
		byte[] bytes = "OK".getBytes(StandardCharsets.UTF_8);
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.getWriter().write("OK");
		response.setContentType(StandardCharsets.UTF_8.name());
		assertThat(response).body().isEqualTo(bytes);
	}


	private static ResponseAssert assertThat(MockHttpServletResponse response) {
		return new ResponseAssert(response);
	}


	private static final class ResponseAssert extends AbstractMockHttpServletResponseAssert<ResponseAssert, MockHttpServletResponse> {

		ResponseAssert(MockHttpServletResponse actual) {
			super(null, actual, ResponseAssert.class);
		}

		@Override
		protected MockHttpServletResponse getResponse() {
			return this.actual;
		}

	}
}
