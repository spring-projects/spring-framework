/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
