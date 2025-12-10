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

package org.springframework.test.web.servlet.client;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import jakarta.servlet.http.Cookie;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * {@link ClientHttpRequestFactory} for requests executed via {@link MockMvc}.
 *
 * @author Rossen Stoyanchev
 * @author Rob Worsnop
 * @since 7.0
 */
class MockMvcClientHttpRequestFactory implements ClientHttpRequestFactory {

	private final MockMvc mockMvc;


	MockMvcClientHttpRequestFactory(MockMvc mockMvc) {
		Assert.notNull(mockMvc, "MockMvc must not be null");
		this.mockMvc = mockMvc;
	}


	@Override
	public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
		return new MockMvcClientHttpRequest(httpMethod, uri);
	}


	/**
	 * {@link ClientHttpRequest} that executes via MockMvc.
	 */
	private class MockMvcClientHttpRequest extends MockClientHttpRequest {

		MockMvcClientHttpRequest(HttpMethod httpMethod, URI uri) {
			super(httpMethod, uri);
		}

		@Override
		public ClientHttpResponse executeInternal() {
			try {
				MockHttpServletRequestBuilder servletRequestBuilder = request(getMethod(), getURI())
						.headers(getHeaders())
						.content(getBodyAsBytes());

				addCookies(servletRequestBuilder);

				MockHttpServletResponse servletResponse = MockMvcClientHttpRequestFactory.this.mockMvc
						.perform(servletRequestBuilder)
						.andReturn()
						.getResponse();

				MockClientHttpResponse clientResponse = new MockClientHttpResponse(
						getResponseBody(servletResponse),
						HttpStatusCode.valueOf(servletResponse.getStatus()));

				copyHeaders(servletResponse, clientResponse);

				return clientResponse;
			}
			catch (Exception ex) {
				byte[] body = ex.toString().getBytes(StandardCharsets.UTF_8);
				return new MockClientHttpResponse(body, HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

		private void addCookies(MockHttpServletRequestBuilder requestBuilder) {
			List<String> values = getHeaders().get(HttpHeaders.COOKIE);
			if (!ObjectUtils.isEmpty(values)) {
				values.stream()
						.flatMap(header -> StringUtils.commaDelimitedListToSet(header).stream())
						.map(value -> {
							String[] parts = StringUtils.split(value, "=");
							Assert.isTrue(parts != null && parts.length == 2, "Invalid cookie: '" + value + "'");
							return new Cookie(parts[0], parts[1]);
						})
						.forEach(requestBuilder::cookie);
			}
		}

		private static byte[] getResponseBody(MockHttpServletResponse servletResponse) {
			byte[] body = servletResponse.getContentAsByteArray();
			if (body.length == 0) {
				String error = servletResponse.getErrorMessage();
				if (StringUtils.hasLength(error)) {
					body = error.getBytes(StandardCharsets.UTF_8);
				}
			}
			return body;
		}

		private static void copyHeaders(
				MockHttpServletResponse servletResponse, MockClientHttpResponse clientResponse) {

			servletResponse.getHeaderNames()
					.forEach(name -> servletResponse.getHeaders(name)
							.forEach(value -> clientResponse.getHeaders().add(name, value)));
		}
	}

}
