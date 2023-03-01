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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * Tests for the {@link ExecutingResponseCreator} implementation.
 *
 * @author Simon Baslé
 */
class ExecutingResponseCreatorTests {

	@Test
	void ensureRequestNotNull() {
		ExecutingResponseCreator responseCreator = new ExecutingResponseCreator((uri, method) -> null);

		assertThatIllegalStateException()
				.isThrownBy(() -> responseCreator.createResponse(null))
				.withMessage("Expected a MockClientHttpRequest");
	}

	@Test
	void ensureRequestIsMock() {
		ExecutingResponseCreator responseCreator = new ExecutingResponseCreator((uri, method) -> null);
		ClientHttpRequest mockitoMockRequest = mock();

		assertThatIllegalStateException()
				.isThrownBy(() -> responseCreator.createResponse(mockitoMockRequest))
				.withMessage("Expected a MockClientHttpRequest");
	}

	@Test
	void requestIsCopied() throws IOException {
		MockClientHttpRequest originalRequest = new MockClientHttpRequest(HttpMethod.POST, "https://example.org");
		originalRequest.getHeaders().add("X-example", "original");
		originalRequest.getBody().write("original body".getBytes(StandardCharsets.UTF_8));

		MockClientHttpResponse originalResponse = new MockClientHttpResponse(new byte[0], 500);
		List<MockClientHttpRequest> factoryRequests = new ArrayList<>();
		ClientHttpRequestFactory originalFactory = (uri, httpMethod) -> {
			MockClientHttpRequest request = new MockClientHttpRequest(httpMethod, uri);
			request.setResponse(originalResponse);
			factoryRequests.add(request);
			return request;
		};

		ExecutingResponseCreator responseCreator = new ExecutingResponseCreator(originalFactory);
		ClientHttpResponse response = responseCreator.createResponse(originalRequest);

		assertThat(response).as("response").isSameAs(originalResponse);
		assertThat(originalRequest.isExecuted()).as("originalRequest.isExecuted").isFalse();

		assertThat(factoryRequests)
				.hasSize(1)
				.first()
				.isNotSameAs(originalRequest)
				.satisfies(request -> {
					assertThat(request.isExecuted()).isTrue();
					assertThat(request.getBody()).isNotSameAs(originalRequest.getBody());
					assertThat(request.getHeaders()).isNotSameAs(originalRequest.getHeaders());
				});
	}

}
