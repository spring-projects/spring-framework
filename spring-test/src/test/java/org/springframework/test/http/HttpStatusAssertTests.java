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

package org.springframework.test.http;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for {@link HttpStatusAssert}.
 *
 * @author Christian Morbach
 */
class HttpStatusAssertTests {

	@Test
	void isEqualWhenExpectedIsNullShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(HttpStatus.OK).isEqualTo(null))
				.withMessageContaining("HTTP status code");
	}

	@ParameterizedTest
	@MethodSource
	void isStatusCodeWithSameStatusCodeShouldPass(HttpStatus status, String methodName) throws Exception {
		var function = HttpStatusAssert.class.getMethod(methodName);
		assertThatNoException().isThrownBy(() -> function.invoke(assertThat(status)));
	}

	private static Stream<Arguments> isStatusCodeWithSameStatusCodeShouldPass() {
		return Stream.of(
				Arguments.of(HttpStatus.CONTINUE, "isContinue"),
				Arguments.of(HttpStatus.SWITCHING_PROTOCOLS, "isSwitchingProtocols"),
				Arguments.of(HttpStatus.PROCESSING, "isProcessing"),
				Arguments.of(HttpStatus.EARLY_HINTS, "isEarlyHints"),
				Arguments.of(HttpStatus.OK, "isOk"),
				Arguments.of(HttpStatus.CREATED, "isCreated"),
				Arguments.of(HttpStatus.ACCEPTED, "isAccepted"),
				Arguments.of(HttpStatus.NON_AUTHORITATIVE_INFORMATION, "isNonAuthoritativeInformation"),
				Arguments.of(HttpStatus.NO_CONTENT, "isNoContent"),
				Arguments.of(HttpStatus.RESET_CONTENT, "isResetContent"),
				Arguments.of(HttpStatus.PARTIAL_CONTENT, "isPartialContent"),
				Arguments.of(HttpStatus.MULTI_STATUS, "isMultiStatus"),
				Arguments.of(HttpStatus.ALREADY_REPORTED, "isAlreadyReported"),
				Arguments.of(HttpStatus.IM_USED, "isIMUsed"),
				Arguments.of(HttpStatus.MULTIPLE_CHOICES, "isMultipleChoices"),
				Arguments.of(HttpStatus.FOUND, "isFound"),
				Arguments.of(HttpStatus.SEE_OTHER, "isSeeOther"),
				Arguments.of(HttpStatus.NOT_MODIFIED, "isNotModified"),
				Arguments.of(HttpStatus.TEMPORARY_REDIRECT, "isTemporaryRedirect"),
				Arguments.of(HttpStatus.PERMANENT_REDIRECT, "isPermanentRedirect"),
				Arguments.of(HttpStatus.BAD_REQUEST, "isBadRequest"),
				Arguments.of(HttpStatus.UNAUTHORIZED, "isUnauthorized"),
				Arguments.of(HttpStatus.PAYMENT_REQUIRED, "isPaymentRequired"),
				Arguments.of(HttpStatus.FORBIDDEN, "isForbidden"),
				Arguments.of(HttpStatus.NOT_FOUND, "isNotFound"),
				Arguments.of(HttpStatus.METHOD_NOT_ALLOWED, "isMethodNotAllowed"),
				Arguments.of(HttpStatus.NOT_ACCEPTABLE, "isNotAcceptable"),
				Arguments.of(HttpStatus.PROXY_AUTHENTICATION_REQUIRED, "isProxyAuthenticationRequired"),
				Arguments.of(HttpStatus.REQUEST_TIMEOUT, "isRequestTimeout"),
				Arguments.of(HttpStatus.CONFLICT, "isConflict"),
				Arguments.of(HttpStatus.GONE, "isGone"),
				Arguments.of(HttpStatus.LENGTH_REQUIRED, "isLengthRequired"),
				Arguments.of(HttpStatus.PRECONDITION_FAILED, "isPreconditionFailed"),
				Arguments.of(HttpStatus.PAYLOAD_TOO_LARGE, "isPayloadTooLarge"),
				Arguments.of(HttpStatus.URI_TOO_LONG, "isURITooLong"),
				Arguments.of(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "isUnsupportedMediaType"),
				Arguments.of(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE, "isRequestedRangeNotSatisfiable"),
				Arguments.of(HttpStatus.EXPECTATION_FAILED, "isExpectationFailed"),
				Arguments.of(HttpStatus.UNPROCESSABLE_ENTITY, "isUnprocessableEntity"),
				Arguments.of(HttpStatus.LOCKED, "isLocked"),
				Arguments.of(HttpStatus.FAILED_DEPENDENCY, "isFailedDependency"),
				Arguments.of(HttpStatus.TOO_EARLY, "isTooEarly"),
				Arguments.of(HttpStatus.UPGRADE_REQUIRED, "isUpgradeRequired"),
				Arguments.of(HttpStatus.PRECONDITION_REQUIRED, "isPreconditionRequired"),
				Arguments.of(HttpStatus.TOO_MANY_REQUESTS, "isTooManyRequests"),
				Arguments.of(HttpStatus.REQUEST_HEADER_FIELDS_TOO_LARGE, "isRequestHeaderFieldsTooLarge"),
				Arguments.of(HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS, "isUnavailableForLegalReasons"),
				Arguments.of(HttpStatus.INTERNAL_SERVER_ERROR, "isInternalServerError"),
				Arguments.of(HttpStatus.NOT_IMPLEMENTED, "isNotImplemented"),
				Arguments.of(HttpStatus.BAD_GATEWAY, "isBadGateway"),
				Arguments.of(HttpStatus.SERVICE_UNAVAILABLE, "isServiceUnavailable"),
				Arguments.of(HttpStatus.GATEWAY_TIMEOUT, "isGatewayTimeout"),
				Arguments.of(HttpStatus.HTTP_VERSION_NOT_SUPPORTED, "isHttpVersionNotSupported"),
				Arguments.of(HttpStatus.VARIANT_ALSO_NEGOTIATES, "isVariantAlsoNegotiates"),
				Arguments.of(HttpStatus.INSUFFICIENT_STORAGE, "isInsufficientStorage"),
				Arguments.of(HttpStatus.LOOP_DETECTED, "isLoopDetected"),
				Arguments.of(HttpStatus.BANDWIDTH_LIMIT_EXCEEDED, "isBandwidthLimitExceeded"),
				Arguments.of(HttpStatus.NOT_EXTENDED, "isNotExtended"),
				Arguments.of(HttpStatus.NETWORK_AUTHENTICATION_REQUIRED, "isNetworkAuthenticationRequired")
		);
	}

	@ParameterizedTest
	@MethodSource
	void isSeriesShouldPassOnSameSeries(HttpStatus status, String methodName) throws Exception {
		var function = HttpStatusAssert.class.getMethod(methodName);
		assertThatNoException().isThrownBy(() -> function.invoke(assertThat(status)));
	}

	private static Stream<Arguments> isSeriesShouldPassOnSameSeries() {
		return Stream.of(
				Arguments.of(HttpStatus.CONTINUE, "is1xxInformational"),
				Arguments.of(HttpStatus.SWITCHING_PROTOCOLS, "is1xxInformational"),
				Arguments.of(HttpStatus.PROCESSING, "is1xxInformational"),
				Arguments.of(HttpStatus.EARLY_HINTS, "is1xxInformational"),
				Arguments.of(HttpStatus.OK, "is2xxSuccessful"),
				Arguments.of(HttpStatus.CREATED, "is2xxSuccessful"),
				Arguments.of(HttpStatus.ACCEPTED, "is2xxSuccessful"),
				Arguments.of(HttpStatus.NON_AUTHORITATIVE_INFORMATION, "is2xxSuccessful"),
				Arguments.of(HttpStatus.NO_CONTENT, "is2xxSuccessful"),
				Arguments.of(HttpStatus.RESET_CONTENT, "is2xxSuccessful"),
				Arguments.of(HttpStatus.PARTIAL_CONTENT, "is2xxSuccessful"),
				Arguments.of(HttpStatus.MULTI_STATUS, "is2xxSuccessful"),
				Arguments.of(HttpStatus.ALREADY_REPORTED, "is2xxSuccessful"),
				Arguments.of(HttpStatus.IM_USED, "is2xxSuccessful"),
				Arguments.of(HttpStatus.MULTIPLE_CHOICES, "is3xxRedirection"),
				Arguments.of(HttpStatus.FOUND, "is3xxRedirection"),
				Arguments.of(HttpStatus.SEE_OTHER, "is3xxRedirection"),
				Arguments.of(HttpStatus.NOT_MODIFIED, "is3xxRedirection"),
				Arguments.of(HttpStatus.TEMPORARY_REDIRECT, "is3xxRedirection"),
				Arguments.of(HttpStatus.PERMANENT_REDIRECT, "is3xxRedirection"),
				Arguments.of(HttpStatus.BAD_REQUEST, "is4xxClientError"),
				Arguments.of(HttpStatus.UNAUTHORIZED, "is4xxClientError"),
				Arguments.of(HttpStatus.PAYMENT_REQUIRED, "is4xxClientError"),
				Arguments.of(HttpStatus.FORBIDDEN, "is4xxClientError"),
				Arguments.of(HttpStatus.NOT_FOUND, "is4xxClientError"),
				Arguments.of(HttpStatus.METHOD_NOT_ALLOWED, "is4xxClientError"),
				Arguments.of(HttpStatus.NOT_ACCEPTABLE, "is4xxClientError"),
				Arguments.of(HttpStatus.PROXY_AUTHENTICATION_REQUIRED, "is4xxClientError"),
				Arguments.of(HttpStatus.REQUEST_TIMEOUT, "is4xxClientError"),
				Arguments.of(HttpStatus.CONFLICT, "is4xxClientError"),
				Arguments.of(HttpStatus.GONE, "is4xxClientError"),
				Arguments.of(HttpStatus.LENGTH_REQUIRED, "is4xxClientError"),
				Arguments.of(HttpStatus.PRECONDITION_FAILED, "is4xxClientError"),
				Arguments.of(HttpStatus.PAYLOAD_TOO_LARGE, "is4xxClientError"),
				Arguments.of(HttpStatus.URI_TOO_LONG, "is4xxClientError"),
				Arguments.of(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "is4xxClientError"),
				Arguments.of(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE, "is4xxClientError"),
				Arguments.of(HttpStatus.EXPECTATION_FAILED, "is4xxClientError"),
				Arguments.of(HttpStatus.UNPROCESSABLE_ENTITY, "is4xxClientError"),
				Arguments.of(HttpStatus.LOCKED, "is4xxClientError"),
				Arguments.of(HttpStatus.FAILED_DEPENDENCY, "is4xxClientError"),
				Arguments.of(HttpStatus.TOO_EARLY, "is4xxClientError"),
				Arguments.of(HttpStatus.UPGRADE_REQUIRED, "is4xxClientError"),
				Arguments.of(HttpStatus.PRECONDITION_REQUIRED, "is4xxClientError"),
				Arguments.of(HttpStatus.TOO_MANY_REQUESTS, "is4xxClientError"),
				Arguments.of(HttpStatus.REQUEST_HEADER_FIELDS_TOO_LARGE, "is4xxClientError"),
				Arguments.of(HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS, "is4xxClientError"),
				Arguments.of(HttpStatus.INTERNAL_SERVER_ERROR, "is5xxServerError"),
				Arguments.of(HttpStatus.NOT_IMPLEMENTED, "is5xxServerError"),
				Arguments.of(HttpStatus.BAD_GATEWAY, "is5xxServerError"),
				Arguments.of(HttpStatus.SERVICE_UNAVAILABLE, "is5xxServerError"),
				Arguments.of(HttpStatus.GATEWAY_TIMEOUT, "is5xxServerError"),
				Arguments.of(HttpStatus.HTTP_VERSION_NOT_SUPPORTED, "is5xxServerError"),
				Arguments.of(HttpStatus.VARIANT_ALSO_NEGOTIATES, "is5xxServerError"),
				Arguments.of(HttpStatus.INSUFFICIENT_STORAGE, "is5xxServerError"),
				Arguments.of(HttpStatus.LOOP_DETECTED, "is5xxServerError"),
				Arguments.of(HttpStatus.BANDWIDTH_LIMIT_EXCEEDED, "is5xxServerError"),
				Arguments.of(HttpStatus.NOT_EXTENDED, "is5xxServerError"),
				Arguments.of(HttpStatus.NETWORK_AUTHENTICATION_REQUIRED, "is5xxServerError")
		);
	}

	private static HttpStatusAssert assertThat(@NonNull HttpStatus httpStatus) {
		return new HttpStatusAssert(httpStatus);
	}
}
