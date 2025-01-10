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

import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assert;
import org.assertj.core.api.Assertions;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatus.Series;

/**
 * AssertJ {@link Assert assertions} that can be applied to
 * {@link HttpStatus}.
 *
 * @author Christian Morbach
 */
public class HttpStatusAssert extends AbstractObjectAssert<HttpStatusAssert, HttpStatus> {

	public HttpStatusAssert(int statusCode) {
		this(HttpStatus.valueOf(statusCode));
	}

	HttpStatusAssert(HttpStatus httpStatus) {
		super(httpStatus, HttpStatusAssert.class);
		as("HTTP status code");
	}

	public HttpStatusAssert isEqualTo(int httpStatus) {
		return isEqualTo(HttpStatus.resolve(httpStatus));
	}

	public HttpStatusAssert is1xxInformational() {
		return hasSeries(Series.INFORMATIONAL);
	}

	public HttpStatusAssert is2xxSuccessful() {
		return hasSeries(Series.SUCCESSFUL);
	}

	public HttpStatusAssert is3xxRedirection() {
		return hasSeries(Series.REDIRECTION);
	}

	public HttpStatusAssert is4xxClientError() {
		return hasSeries(Series.CLIENT_ERROR);
	}

	public HttpStatusAssert is5xxServerError() {
		return hasSeries(Series.SERVER_ERROR);
	}

	public HttpStatusAssert isContinue() {
		return isEqualTo(HttpStatus.CONTINUE);
	}

	public HttpStatusAssert isSwitchingProtocols() {
		return isEqualTo(HttpStatus.SWITCHING_PROTOCOLS);
	}

	public HttpStatusAssert isProcessing() {
		return isEqualTo(HttpStatus.PROCESSING);
	}

	public HttpStatusAssert isEarlyHints() {
		return isEqualTo(HttpStatus.EARLY_HINTS);
	}

	public HttpStatusAssert isOk() {
		return isEqualTo(HttpStatus.OK);
	}

	public HttpStatusAssert isCreated() {
		return isEqualTo(HttpStatus.CREATED);
	}

	public HttpStatusAssert isAccepted() {
		return isEqualTo(HttpStatus.ACCEPTED);
	}

	public HttpStatusAssert isNonAuthoritativeInformation() {
		return isEqualTo(HttpStatus.NON_AUTHORITATIVE_INFORMATION);
	}

	public HttpStatusAssert isNoContent() {
		return isEqualTo(HttpStatus.NO_CONTENT);
	}

	public HttpStatusAssert isResetContent() {
		return isEqualTo(HttpStatus.RESET_CONTENT);
	}

	public HttpStatusAssert isPartialContent() {
		return isEqualTo(HttpStatus.PARTIAL_CONTENT);
	}

	public HttpStatusAssert isMultiStatus() {
		return isEqualTo(HttpStatus.MULTI_STATUS);
	}

	public HttpStatusAssert isAlreadyReported() {
		return isEqualTo(HttpStatus.ALREADY_REPORTED);
	}

	public HttpStatusAssert isIMUsed() {
		return isEqualTo(HttpStatus.IM_USED);
	}

	public HttpStatusAssert isMultipleChoices() {
		return isEqualTo(HttpStatus.MULTIPLE_CHOICES);
	}

	public HttpStatusAssert isMovedPermanently() {
		return isEqualTo(HttpStatus.MOVED_PERMANENTLY);
	}

	public HttpStatusAssert isFound() {
		return isEqualTo(HttpStatus.FOUND);
	}

	public HttpStatusAssert isSeeOther() {
		return isEqualTo(HttpStatus.SEE_OTHER);
	}

	public HttpStatusAssert isNotModified() {
		return isEqualTo(HttpStatus.NOT_MODIFIED);
	}

	public HttpStatusAssert isTemporaryRedirect() {
		return isEqualTo(HttpStatus.TEMPORARY_REDIRECT);
	}

	public HttpStatusAssert isPermanentRedirect() {
		return isEqualTo(HttpStatus.PERMANENT_REDIRECT);
	}

	public HttpStatusAssert isBadRequest() {
		return isEqualTo(HttpStatus.BAD_REQUEST);
	}

	public HttpStatusAssert isUnauthorized() {
		return isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	public HttpStatusAssert isPaymentRequired() {
		return isEqualTo(HttpStatus.PAYMENT_REQUIRED);
	}

	public HttpStatusAssert isForbidden() {
		return isEqualTo(HttpStatus.FORBIDDEN);
	}

	public HttpStatusAssert isNotFound() {
		return isEqualTo(HttpStatus.NOT_FOUND);
	}

	public HttpStatusAssert isMethodNotAllowed() {
		return isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
	}

	public HttpStatusAssert isNotAcceptable() {
		return isEqualTo(HttpStatus.NOT_ACCEPTABLE);
	}

	public HttpStatusAssert isProxyAuthenticationRequired() {
		return isEqualTo(HttpStatus.PROXY_AUTHENTICATION_REQUIRED);
	}

	public HttpStatusAssert isRequestTimeout() {
		return isEqualTo(HttpStatus.REQUEST_TIMEOUT);
	}

	public HttpStatusAssert isConflict() {
		return isEqualTo(HttpStatus.CONFLICT);
	}

	public HttpStatusAssert isGone() {
		return isEqualTo(HttpStatus.GONE);
	}

	public HttpStatusAssert isLengthRequired() {
		return isEqualTo(HttpStatus.LENGTH_REQUIRED);
	}

	public HttpStatusAssert isPreconditionFailed() {
		return isEqualTo(HttpStatus.PRECONDITION_FAILED);
	}

	public HttpStatusAssert isPayloadTooLarge() {
		return isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
	}

	public HttpStatusAssert isURITooLong() {
		return isEqualTo(HttpStatus.URI_TOO_LONG);
	}

	public HttpStatusAssert isUnsupportedMediaType() {
		return isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
	}

	public HttpStatusAssert isRequestedRangeNotSatisfiable() {
		return isEqualTo(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
	}

	public HttpStatusAssert isExpectationFailed() {
		return isEqualTo(HttpStatus.EXPECTATION_FAILED);
	}

	public HttpStatusAssert isUnprocessableEntity() {
		return isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
	}

	public HttpStatusAssert isLocked() {
		return isEqualTo(HttpStatus.LOCKED);
	}

	public HttpStatusAssert isFailedDependency() {
		return isEqualTo(HttpStatus.FAILED_DEPENDENCY);
	}

	public HttpStatusAssert isTooEarly() {
		return isEqualTo(HttpStatus.TOO_EARLY);
	}

	public HttpStatusAssert isUpgradeRequired() {
		return isEqualTo(HttpStatus.UPGRADE_REQUIRED);
	}

	public HttpStatusAssert isPreconditionRequired() {
		return isEqualTo(HttpStatus.PRECONDITION_REQUIRED);
	}

	public HttpStatusAssert isTooManyRequests() {
		return isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
	}

	public HttpStatusAssert isRequestHeaderFieldsTooLarge() {
		return isEqualTo(HttpStatus.REQUEST_HEADER_FIELDS_TOO_LARGE);
	}

	public HttpStatusAssert isUnavailableForLegalReasons() {
		return isEqualTo(HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS);
	}

	public HttpStatusAssert isInternalServerError() {
		return isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	public HttpStatusAssert isNotImplemented() {
		return isEqualTo(HttpStatus.NOT_IMPLEMENTED);
	}

	public HttpStatusAssert isBadGateway() {
		return isEqualTo(HttpStatus.BAD_GATEWAY);
	}

	public HttpStatusAssert isServiceUnavailable() {
		return isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
	}

	public HttpStatusAssert isGatewayTimeout() {
		return isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
	}

	public HttpStatusAssert isHttpVersionNotSupported() {
		return isEqualTo(HttpStatus.HTTP_VERSION_NOT_SUPPORTED);
	}

	public HttpStatusAssert isVariantAlsoNegotiates() {
		return isEqualTo(HttpStatus.VARIANT_ALSO_NEGOTIATES);
	}

	public HttpStatusAssert isInsufficientStorage() {
		return isEqualTo(HttpStatus.INSUFFICIENT_STORAGE);
	}

	public HttpStatusAssert isLoopDetected() {
		return isEqualTo(HttpStatus.LOOP_DETECTED);
	}

	public HttpStatusAssert isBandwidthLimitExceeded() {
		return isEqualTo(HttpStatus.BANDWIDTH_LIMIT_EXCEEDED);
	}

	public HttpStatusAssert isNotExtended() {
		return isEqualTo(HttpStatus.NOT_EXTENDED);
	}

	public HttpStatusAssert isNetworkAuthenticationRequired() {
		return isEqualTo(HttpStatus.NETWORK_AUTHENTICATION_REQUIRED);
	}

	private HttpStatusAssert hasSeries(Series series) {
		Assertions.assertThat(Series.resolve(this.actual.value())).isEqualTo(series);
		return this.myself;
	}
}
