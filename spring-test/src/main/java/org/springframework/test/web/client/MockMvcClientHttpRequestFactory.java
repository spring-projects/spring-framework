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

package org.springframework.test.web.client;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

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
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * A {@link ClientHttpRequestFactory} for requests executed via {@link MockMvc}.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class MockMvcClientHttpRequestFactory implements ClientHttpRequestFactory {

	private final MockMvc mockMvc;


	public MockMvcClientHttpRequestFactory(MockMvc mockMvc) {
		Assert.notNull(mockMvc, "MockMvc must not be null");
		this.mockMvc = mockMvc;
	}


	@Override
	public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
		return new MockClientHttpRequest(httpMethod, uri) {
			@Override
			public ClientHttpResponse executeInternal() {
				return getClientHttpResponse(httpMethod, uri, getHeaders(), getBodyAsBytes());
			}
		};
	}

	private ClientHttpResponse getClientHttpResponse(
			HttpMethod httpMethod, URI uri, HttpHeaders requestHeaders, byte[] requestBody) {

		try {
			MockHttpServletResponse servletResponse = this.mockMvc
					.perform(request(httpMethod, uri).content(requestBody).headers(requestHeaders))
					.andReturn()
					.getResponse();

			HttpStatusCode status = HttpStatusCode.valueOf(servletResponse.getStatus());
			byte[] body = servletResponse.getContentAsByteArray();
			if (body.length == 0) {
				String error = servletResponse.getErrorMessage();
				if (StringUtils.hasLength(error)) {
					// sendError message as default body
					body = error.getBytes(StandardCharsets.UTF_8);
				}
			}

			MockClientHttpResponse clientResponse = new MockClientHttpResponse(body, status);
			clientResponse.getHeaders().putAll(getResponseHeaders(servletResponse));
			return clientResponse;
		}
		catch (Exception ex) {
			byte[] body = ex.toString().getBytes(StandardCharsets.UTF_8);
			return new MockClientHttpResponse(body, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private HttpHeaders getResponseHeaders(MockHttpServletResponse response) {
		HttpHeaders headers = new HttpHeaders();
		for (String name : response.getHeaderNames()) {
			List<String> values = response.getHeaders(name);
			for (String value : values) {
				headers.add(name, value);
			}
		}
		return headers;
	}

}
