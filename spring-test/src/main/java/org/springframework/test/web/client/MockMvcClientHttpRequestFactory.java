/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.web.client;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.Assert;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;


/**
 * A {@link ClientHttpRequestFactory} for requests executed via {@link MockMvc}.
 *
 * <p>As of 5.0 this class also implements
 * {@link org.springframework.http.client.AsyncClientHttpRequestFactory
 * AsyncClientHttpRequestFactory}. However note that
 * {@link org.springframework.web.client.AsyncRestTemplate} and related classes
 * have been deprecated at the same time.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
@SuppressWarnings("deprecation")
public class MockMvcClientHttpRequestFactory
		implements ClientHttpRequestFactory, org.springframework.http.client.AsyncClientHttpRequestFactory {

	private final MockMvc mockMvc;


	public MockMvcClientHttpRequestFactory(MockMvc mockMvc) {
		Assert.notNull(mockMvc, "MockMvc must not be null");
		this.mockMvc = mockMvc;
	}


	@Override
	public ClientHttpRequest createRequest(final URI uri, final HttpMethod httpMethod) {
		return new MockClientHttpRequest(httpMethod, uri) {
			@Override
			public ClientHttpResponse executeInternal() throws IOException {
				return getClientHttpResponse(httpMethod, uri, getHeaders(), getBodyAsBytes());
			}
		};
	}

	@Override
	public org.springframework.http.client.AsyncClientHttpRequest createAsyncRequest(URI uri, HttpMethod method) {
		return new org.springframework.mock.http.client.MockAsyncClientHttpRequest(method, uri) {
			@Override
			protected ClientHttpResponse executeInternal() throws IOException {
				return getClientHttpResponse(method, uri, getHeaders(), getBodyAsBytes());
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

			HttpStatus status = HttpStatus.valueOf(servletResponse.getStatus());
			byte[] body = servletResponse.getContentAsByteArray();
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
