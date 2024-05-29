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

import org.assertj.core.api.AssertProvider;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.json.JsonContent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link AbstractMockHttpServletResponseAssert}.
 *
 * @author Stephane Nicoll
 */
public class AbstractMockHttpServletResponseAssertTests {

	@Test
	void bodyText() {
		MockHttpServletResponse response = createResponse("OK");
		assertThat(fromResponse(response)).bodyText().isEqualTo("OK");
	}

	@Test
	void bodyJsonWithJsonPath() {
		MockHttpServletResponse response = createResponse("{\"albumById\": {\"name\": \"Greatest hits\"}}");
		assertThat(fromResponse(response)).bodyJson()
				.extractingPath("$.albumById.name").isEqualTo("Greatest hits");
	}

	@Test
	void bodyJsonCanLoadResourceRelativeToClass() {
		MockHttpServletResponse response = createResponse("{ \"name\" : \"Spring\", \"age\" : 123 }");
		// See org/springframework/test/json/example.json
		assertThat(fromResponse(response)).bodyJson().withResourceLoadClass(JsonContent.class)
				.isLenientlyEqualTo("example.json");
	}

	@Test
	void bodyWithByteArray() throws UnsupportedEncodingException {
		byte[] bytes = "OK".getBytes(StandardCharsets.UTF_8);
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.getWriter().write("OK");
		response.setContentType(StandardCharsets.UTF_8.name());
		assertThat(fromResponse(response)).body().isEqualTo(bytes);
	}

	@Test
	void hasBodyTextEqualTo() throws UnsupportedEncodingException {
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.getWriter().write("OK");
		response.setContentType(StandardCharsets.UTF_8.name());
		assertThat(fromResponse(response)).hasBodyTextEqualTo("OK");
	}

	@Test
	void hasForwardedUrl() {
		String forwardedUrl = "https://example.com/42";
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.setForwardedUrl(forwardedUrl);
		assertThat(fromResponse(response)).hasForwardedUrl(forwardedUrl);
	}

	@Test
	void hasForwardedUrlWithWrongValue() {
		String forwardedUrl = "https://example.com/42";
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.setForwardedUrl(forwardedUrl);
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(fromResponse(response)).hasForwardedUrl("another"))
				.withMessageContainingAll("Forwarded URL", forwardedUrl, "another");
	}

	@Test
	void hasRedirectedUrl() {
		String redirectedUrl = "https://example.com/42";
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.addHeader(HttpHeaders.LOCATION, redirectedUrl);
		assertThat(fromResponse(response)).hasRedirectedUrl(redirectedUrl);
	}

	@Test
	void hasRedirectedUrlWithWrongValue() {
		String redirectedUrl = "https://example.com/42";
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.addHeader(HttpHeaders.LOCATION, redirectedUrl);
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(fromResponse(response)).hasRedirectedUrl("another"))
				.withMessageContainingAll("Redirected URL", redirectedUrl, "another");
	}


	private MockHttpServletResponse createResponse(String body) {
		try {
			MockHttpServletResponse response = new MockHttpServletResponse();
			response.setContentType(StandardCharsets.UTF_8.name());
			response.getWriter().write(body);
			return response;
		}
		catch (UnsupportedEncodingException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private static AssertProvider<ResponseAssert> fromResponse(MockHttpServletResponse response) {
		return () -> new ResponseAssert(response);
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
