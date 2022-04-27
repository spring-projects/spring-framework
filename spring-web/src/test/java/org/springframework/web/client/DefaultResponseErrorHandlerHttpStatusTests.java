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

package org.springframework.web.client;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.GATEWAY_TIMEOUT;
import static org.springframework.http.HttpStatus.HTTP_VERSION_NOT_SUPPORTED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.I_AM_A_TEAPOT;
import static org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED;
import static org.springframework.http.HttpStatus.NOT_ACCEPTABLE;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NOT_IMPLEMENTED;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

/**
 * Unit tests for {@link DefaultResponseErrorHandler} handling of specific
 * HTTP status codes.
 */
class DefaultResponseErrorHandlerHttpStatusTests {

	private final DefaultResponseErrorHandler handler = new DefaultResponseErrorHandler();

	private final ClientHttpResponse response = mock(ClientHttpResponse.class);


	@ParameterizedTest(name = "[{index}] error: [{0}]")
	@DisplayName("hasError() returns true")
	@MethodSource("errorCodes")
	void hasErrorTrue(HttpStatus httpStatus) throws Exception {
		given(this.response.getRawStatusCode()).willReturn(httpStatus.value());
		assertThat(this.handler.hasError(this.response)).isTrue();
	}

	@ParameterizedTest(name = "[{index}] error: {0}, exception: {1}")
	@DisplayName("handleError() throws an exception")
	@MethodSource("errorCodes")
	void handleErrorException(HttpStatus httpStatus, Class<? extends Throwable> expectedExceptionClass) throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);

		given(this.response.getRawStatusCode()).willReturn(httpStatus.value());
		given(this.response.getHeaders()).willReturn(headers);

		assertThatExceptionOfType(expectedExceptionClass).isThrownBy(() -> this.handler.handleError(this.response));
	}

	static Stream<Arguments> errorCodes() {
		return Stream.of(
			// 4xx
			args(BAD_REQUEST, HttpClientErrorException.BadRequest.class),
			args(UNAUTHORIZED, HttpClientErrorException.Unauthorized.class),
			args(FORBIDDEN, HttpClientErrorException.Forbidden.class),
			args(NOT_FOUND, HttpClientErrorException.NotFound.class),
			args(METHOD_NOT_ALLOWED, HttpClientErrorException.MethodNotAllowed.class),
			args(NOT_ACCEPTABLE, HttpClientErrorException.NotAcceptable.class),
			args(CONFLICT, HttpClientErrorException.Conflict.class),
			args(TOO_MANY_REQUESTS, HttpClientErrorException.TooManyRequests.class),
			args(UNPROCESSABLE_ENTITY, HttpClientErrorException.UnprocessableEntity.class),
			args(I_AM_A_TEAPOT, HttpClientErrorException.class),
			// 5xx
			args(INTERNAL_SERVER_ERROR, HttpServerErrorException.InternalServerError.class),
			args(NOT_IMPLEMENTED, HttpServerErrorException.NotImplemented.class),
			args(BAD_GATEWAY, HttpServerErrorException.BadGateway.class),
			args(SERVICE_UNAVAILABLE, HttpServerErrorException.ServiceUnavailable.class),
			args(GATEWAY_TIMEOUT, HttpServerErrorException.GatewayTimeout.class),
			args(HTTP_VERSION_NOT_SUPPORTED, HttpServerErrorException.class)
		);
	}

	private static Arguments args(HttpStatus httpStatus, Class<? extends Throwable> exceptionType) {
		return arguments(
				named(String.valueOf(httpStatus.value()), httpStatus),
				named(exceptionType.getSimpleName(), exceptionType));
	}

}
