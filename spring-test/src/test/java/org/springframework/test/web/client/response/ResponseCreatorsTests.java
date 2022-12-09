/*
 * Copyright 2002-2022 the original author or authors.
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

import java.net.SocketTimeoutException;
import java.net.URI;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for the {@link MockRestResponseCreators} static factory methods.
 *
 * @author Rossen Stoyanchev
 */
@SuppressWarnings("resource")
class ResponseCreatorsTests {

	@Test
	void success() throws Exception {
		MockClientHttpResponse response = (MockClientHttpResponse) MockRestResponseCreators.withSuccess().createResponse(null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getHeaders()).isEmpty();
		assertThat(StreamUtils.copyToByteArray(response.getBody())).isEmpty();
	}

	@Test
	void successWithContent() throws Exception {
		DefaultResponseCreator responseCreator = MockRestResponseCreators.withSuccess("foo", MediaType.TEXT_PLAIN);
		MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
		assertThat(StreamUtils.copyToByteArray(response.getBody())).isEqualTo("foo".getBytes());
	}

	@Test
	void successWithContentWithoutContentType() throws Exception {
		DefaultResponseCreator responseCreator = MockRestResponseCreators.withSuccess("foo", null);
		MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getHeaders().getContentType()).isNull();
		assertThat(StreamUtils.copyToByteArray(response.getBody())).isEqualTo("foo".getBytes());
	}

	@Test
	void created() throws Exception {
		URI location = URI.create("/foo");
		DefaultResponseCreator responseCreator = MockRestResponseCreators.withCreatedEntity(location);
		MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getHeaders().getLocation()).isEqualTo(location);
		assertThat(StreamUtils.copyToByteArray(response.getBody())).isEmpty();
	}

	@Test
	void accepted() throws Exception {
		DefaultResponseCreator responseCreator = MockRestResponseCreators.withAccepted();
		MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
		assertThat(StreamUtils.copyToByteArray(response.getBody())).isEmpty();
	}

	@Test
	void noContent() throws Exception {
		DefaultResponseCreator responseCreator = MockRestResponseCreators.withNoContent();
		MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		assertThat(response.getHeaders()).isEmpty();
		assertThat(StreamUtils.copyToByteArray(response.getBody())).isEmpty();
	}

	@Test
	void badRequest() throws Exception {
		DefaultResponseCreator responseCreator = MockRestResponseCreators.withBadRequest();
		MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getHeaders()).isEmpty();
		assertThat(StreamUtils.copyToByteArray(response.getBody())).isEmpty();
	}

	@Test
	void unauthorized() throws Exception {
		DefaultResponseCreator responseCreator = MockRestResponseCreators.withUnauthorizedRequest();
		MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(response.getHeaders()).isEmpty();
		assertThat(StreamUtils.copyToByteArray(response.getBody())).isEmpty();
	}

	@Test
	void forbiddenRequest() throws Exception {
		DefaultResponseCreator responseCreator = MockRestResponseCreators.withForbiddenRequest();
		MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		assertThat(StreamUtils.copyToByteArray(response.getBody())).isEmpty();
	}

	@Test
	void resourceNotFound() throws Exception {
		DefaultResponseCreator responseCreator = MockRestResponseCreators.withResourceNotFound();
		MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(StreamUtils.copyToByteArray(response.getBody())).isEmpty();
	}

	@Test
	void requestConflict() throws Exception {
		DefaultResponseCreator responseCreator = MockRestResponseCreators.withRequestConflict();
		MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(StreamUtils.copyToByteArray(response.getBody())).isEmpty();
	}

	@Test
	void tooManyRequests() throws Exception {
		DefaultResponseCreator responseCreator = MockRestResponseCreators.withTooManyRequests();
		MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
		assertThat(response.getHeaders()).doesNotContainKey(HttpHeaders.RETRY_AFTER);
		assertThat(StreamUtils.copyToByteArray(response.getBody())).isEmpty();
	}

	@Test
	void tooManyRequestsWithRetryAfter() throws Exception {
		DefaultResponseCreator responseCreator = MockRestResponseCreators.withTooManyRequests(512);
		MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
		assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("512");
		assertThat(StreamUtils.copyToByteArray(response.getBody())).isEmpty();
	}

	@Test
	void serverError() throws Exception {
		DefaultResponseCreator responseCreator = MockRestResponseCreators.withServerError();
		MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(response.getHeaders()).isEmpty();
		assertThat(StreamUtils.copyToByteArray(response.getBody())).isEmpty();
	}

	@Test
	void badGateway() throws Exception {
		DefaultResponseCreator responseCreator = MockRestResponseCreators.withBadGateway();
		MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
		assertThat(StreamUtils.copyToByteArray(response.getBody())).isEmpty();
	}

	@Test
	void serviceUnavailable() throws Exception {
		DefaultResponseCreator responseCreator = MockRestResponseCreators.withServiceUnavailable();
		MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
		assertThat(StreamUtils.copyToByteArray(response.getBody())).isEmpty();
	}

	@Test
	void gatewayTimeout() throws Exception {
		DefaultResponseCreator responseCreator = MockRestResponseCreators.withGatewayTimeout();
		MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
		assertThat(StreamUtils.copyToByteArray(response.getBody())).isEmpty();
	}

	@Test
	void withStatus() throws Exception {
		DefaultResponseCreator responseCreator = MockRestResponseCreators.withStatus(HttpStatus.FORBIDDEN);
		MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		assertThat(response.getHeaders()).isEmpty();
		assertThat(StreamUtils.copyToByteArray(response.getBody())).isEmpty();
	}

	@Test
	void withCustomStatus() throws Exception {
		DefaultResponseCreator responseCreator = MockRestResponseCreators.withRawStatus(454);
		MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(454));
		assertThat(response.getStatusText()).isEmpty();
	}

	@Test
	void withException() {
		ResponseCreator responseCreator = MockRestResponseCreators.withException(new SocketTimeoutException());
		assertThatExceptionOfType(SocketTimeoutException.class)
				.isThrownBy(() -> responseCreator.createResponse(null));
	}

}
