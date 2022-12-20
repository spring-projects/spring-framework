/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.test.web.client.response;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.AbstractClientHttpRequest;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for the {@link ExecutingResponseCreator} implementation.
 *
 * @author Simon Baslé
 */
class ExecutingResponseCreatorTests {

	@Test
	void ensureRequestNotNull() {
		final ExecutingResponseCreator responseCreator = new ExecutingResponseCreator((uri, method) -> null);

		assertThatIllegalStateException()
				.isThrownBy(() -> responseCreator.createResponse(null))
				.withMessage("Request should be an instance of MockClientHttpRequest");
	}

	@Test
	void ensureRequestIsMock() {
		final ExecutingResponseCreator responseCreator = new ExecutingResponseCreator((uri, method) -> null);
		ClientHttpRequest notAMockRequest = new AbstractClientHttpRequest() {
			@Override
			protected OutputStream getBodyInternal(HttpHeaders headers) throws IOException {
				return null;
			}

			@Override
			protected ClientHttpResponse executeInternal(HttpHeaders headers) throws IOException {
				return null;
			}

			@Override
			public HttpMethod getMethod() {
				return null;
			}

			@Override
			public URI getURI() {
				return null;
			}
		};

		assertThatIllegalStateException()
				.isThrownBy(() -> responseCreator.createResponse(notAMockRequest))
				.withMessage("Request should be an instance of MockClientHttpRequest");
	}

	@Test
	void requestIsCopied() throws IOException {
		MockClientHttpRequest originalRequest = new MockClientHttpRequest(HttpMethod.POST,
				"https://example.org");
		String body = "original body";
		originalRequest.getHeaders().add("X-example", "original");
		originalRequest.getBody().write(body.getBytes(StandardCharsets.UTF_8));
		MockClientHttpResponse originalResponse = new MockClientHttpResponse(new byte[0], 500);
		List<MockClientHttpRequest> factoryRequests = new ArrayList<>();
		ClientHttpRequestFactory originalFactory = (uri, httpMethod) -> {
			MockClientHttpRequest request = new MockClientHttpRequest(httpMethod, uri);
			request.setResponse(originalResponse);
			factoryRequests.add(request);
			return request;
		};

		final ExecutingResponseCreator responseCreator = new ExecutingResponseCreator(originalFactory);
		final ClientHttpResponse response = responseCreator.createResponse(originalRequest);

		assertThat(response).as("response").isSameAs(originalResponse);
		assertThat(originalRequest.isExecuted()).as("originalRequest.isExecuted").isFalse();

		assertThat(factoryRequests)
				.hasSize(1)
				.first()
				.isNotSameAs(originalRequest)
				.satisfies(copiedRequest -> {
					assertThat(copiedRequest)
							.as("copied request")
							.isNotSameAs(originalRequest);
					assertThat(copiedRequest.isExecuted())
							.as("copiedRequest.isExecuted").isTrue();
					assertThat(copiedRequest.getBody())
							.as("copiedRequest.body").isNotSameAs(originalRequest.getBody());
					assertThat(copiedRequest.getHeaders())
							.as("copiedRequest.headers").isNotSameAs(originalRequest.getHeaders());
				});
	}
}
