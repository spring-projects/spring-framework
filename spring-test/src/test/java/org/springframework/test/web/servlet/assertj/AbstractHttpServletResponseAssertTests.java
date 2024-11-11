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

import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link AbstractHttpServletResponseAssert}.
 *
 * @author Stephane Nicoll
 */
class AbstractHttpServletResponseAssertTests {

	@Nested
	class HeadersTests {

		@Test
		void containsHeader() {
			MockHttpServletResponse response = createResponse(Map.of("n1", "v1", "n2", "v2", "n3", "v3"));
			assertThat(response).containsHeader("n1");
		}

		@Test
		void doesNotContainHeader() {
			MockHttpServletResponse response = createResponse(Map.of("n1", "v1", "n2", "v2", "n3", "v3"));
			assertThat(response).doesNotContainHeader("n4");
		}

		@Test
		void hasHeader() {
			MockHttpServletResponse response = createResponse(Map.of("n1", "v1", "n2", "v2", "n3", "v3"));
			assertThat(response).hasHeader("n1", "v1");
		}

		@Test
		void headersAreMatching() {
			MockHttpServletResponse response = createResponse(Map.of("n1", "v1", "n2", "v2", "n3", "v3"));
			assertThat(response).headers().containsHeaders("n1", "n2", "n3");
		}

		private MockHttpServletResponse createResponse(Map<String, String> headers) {
			MockHttpServletResponse response = new MockHttpServletResponse();
			headers.forEach(response::addHeader);
			return response;
		}
	}

	@Nested
	class ContentTypeTests {

		@Test
		void contentType() {
			MockHttpServletResponse response = createResponse("text/plain");
			assertThat(response).hasContentType(MediaType.TEXT_PLAIN);
		}

		@Test
		void contentTypeAndRepresentation() {
			MockHttpServletResponse response = createResponse("text/plain");
			assertThat(response).hasContentType("text/plain");
		}

		@Test
		void contentTypeCompatibleWith() {
			MockHttpServletResponse response = createResponse("application/json;charset=UTF-8");
			assertThat(response).hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON);
		}

		@Test
		void contentTypeCompatibleWithAndStringRepresentation() {
			MockHttpServletResponse response = createResponse("text/plain");
			assertThat(response).hasContentTypeCompatibleWith("text/*");
		}

		@Test
		void contentTypeCanBeAsserted() {
			MockHttpServletResponse response = createResponse("text/plain");
			assertThat(response).contentType().isInstanceOf(MediaType.class).isCompatibleWith("text/*").isNotNull();
		}

		private MockHttpServletResponse createResponse(String contentType) {
			MockHttpServletResponse response = new MockHttpServletResponse();
			response.setContentType(contentType);
			return response;
		}
	}

	@Nested
	class StatusTests {

		@Test
		void hasStatusWithCode() {
			assertThat(createResponse(200)).hasStatus(200);
		}

		@Test
		void hasStatusWithHttpStatus() {
			assertThat(createResponse(200)).hasStatus(HttpStatus.OK);
		}

		@Test
		void hasStatusOK() {
			assertThat(createResponse(200)).hasStatusOk();
		}

		@Test
		void hasStatusWithWrongCode() {
			MockHttpServletResponse response = createResponse(200);
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(response).hasStatus(300))
					.withMessageContainingAll("HTTP status code", "200", "300");
		}

		@Test
		void hasStatus1xxInformational() {
			assertThat(createResponse(199)).hasStatus1xxInformational();
		}

		@Test
		void hasStatus2xxSuccessful() {
			assertThat(createResponse(299)).hasStatus2xxSuccessful();
		}

		@Test
		void hasStatus3xxRedirection() {
			assertThat(createResponse(399)).hasStatus3xxRedirection();
		}

		@Test
		void hasStatus4xxClientError() {
			assertThat(createResponse(499)).hasStatus4xxClientError();
		}

		@Test
		void hasStatus5xxServerError() {
			assertThat(createResponse(599)).hasStatus5xxServerError();
		}

		@Test
		void hasStatusWithWrongSeries() {
			MockHttpServletResponse response = createResponse(500);
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(response).hasStatus2xxSuccessful())
					.withMessageContainingAll("HTTP status series", "SUCCESSFUL", "SERVER_ERROR");
		}

		private MockHttpServletResponse createResponse(int status) {
			MockHttpServletResponse response = new MockHttpServletResponse();
			response.setStatus(status);
			return response;
		}
	}


	private static ResponseAssert assertThat(HttpServletResponse response) {
		return new ResponseAssert(response);
	}


	private static final class ResponseAssert extends AbstractHttpServletResponseAssert<HttpServletResponse, ResponseAssert, HttpServletResponse> {

		ResponseAssert(HttpServletResponse actual) {
			super(actual, ResponseAssert.class);
		}

		@Override
		protected HttpServletResponse getResponse() {
			return this.actual;
		}

	}

}
