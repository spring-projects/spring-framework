/*
 * Copyright 2002-2017 the original author or authors.
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
package org.springframework.test.web.reactive.server;

import org.springframework.http.HttpStatus;

import static org.springframework.test.util.AssertionErrors.assertEquals;

/**
 * Provides methods for asserting the response status.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
@SuppressWarnings("unused")
public class ResponseStatusAssertions {

	private final ExchangeActions exchangeActions;

	private final HttpStatus httpStatus;


	ResponseStatusAssertions(ExchangeActions actions) {
		this.exchangeActions = actions;
		this.httpStatus = actions.andReturn().getResponse().statusCode();
	}


	public ExchangeActions is(int status) {
		assertEquals("Response status", status, this.httpStatus.value());
		return this.exchangeActions;
	}

	/**
	 * Assert the response status code is in the 1xx range.
	 */
	public ExchangeActions is1xxInformational() {
		String message = "Range for response status value " + this.httpStatus;
		assertEquals(message, HttpStatus.Series.INFORMATIONAL, this.httpStatus.series());
		return this.exchangeActions;
	}

	/**
	 * Assert the response status code is in the 2xx range.
	 */
	public ExchangeActions is2xxSuccessful() {
		String message = "Range for response status value " + this.httpStatus;
		assertEquals(message, HttpStatus.Series.SUCCESSFUL, this.httpStatus.series());
		return this.exchangeActions;
	}

	/**
	 * Assert the response status code is in the 3xx range.
	 */
	public ExchangeActions is3xxRedirection() {
		String message = "Range for response status value " + this.httpStatus;
		assertEquals(message, HttpStatus.Series.REDIRECTION, this.httpStatus.series());
		return this.exchangeActions;
	}

	/**
	 * Assert the response status code is in the 4xx range.
	 */
	public ExchangeActions is4xxClientError() {
		String message = "Range for response status value " + this.httpStatus;
		assertEquals(message, HttpStatus.Series.CLIENT_ERROR, this.httpStatus.series());
		return this.exchangeActions;
	}

	/**
	 * Assert the response status code is in the 5xx range.
	 */
	public ExchangeActions is5xxServerError() {
		String message = "Range for response status value " + this.httpStatus;
		assertEquals(message, HttpStatus.Series.SERVER_ERROR, this.httpStatus.series());
		return this.exchangeActions;
	}

	/**
	 * Assert the response error message.
	 */
	public ExchangeActions reason(String reason) {
		assertEquals("Response status reason", reason, this.httpStatus.getReasonPhrase());
		return this.exchangeActions;
	}

	/**
	 * Assert the response status code is {@code HttpStatus.CONTINUE} (100).
	 */
	public ExchangeActions isContinue() {
		return doMatch(HttpStatus.CONTINUE);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.SWITCHING_PROTOCOLS} (101).
	 */
	public ExchangeActions isSwitchingProtocols() {
		return doMatch(HttpStatus.SWITCHING_PROTOCOLS);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.PROCESSING} (102).
	 */
	public ExchangeActions isProcessing() {
		return doMatch(HttpStatus.PROCESSING);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.CHECKPOINT} (103).
	 */
	public ExchangeActions isCheckpoint() {
		return doMatch(HttpStatus.valueOf(103));
	}

	/**
	 * Assert the response status code is {@code HttpStatus.OK} (200).
	 */
	public ExchangeActions isOk() {
		return doMatch(HttpStatus.OK);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.CREATED} (201).
	 */
	public ExchangeActions isCreated() {
		return doMatch(HttpStatus.CREATED);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.ACCEPTED} (202).
	 */
	public ExchangeActions isAccepted() {
		return doMatch(HttpStatus.ACCEPTED);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.NON_AUTHORITATIVE_INFORMATION} (203).
	 */
	public ExchangeActions isNonAuthoritativeInformation() {
		return doMatch(HttpStatus.NON_AUTHORITATIVE_INFORMATION);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.NO_CONTENT} (204).
	 */
	public ExchangeActions isNoContent() {
		return doMatch(HttpStatus.NO_CONTENT);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.RESET_CONTENT} (205).
	 */
	public ExchangeActions isResetContent() {
		return doMatch(HttpStatus.RESET_CONTENT);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.PARTIAL_CONTENT} (206).
	 */
	public ExchangeActions isPartialContent() {
		return doMatch(HttpStatus.PARTIAL_CONTENT);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.MULTI_STATUS} (207).
	 */
	public ExchangeActions isMultiStatus() {
		return doMatch(HttpStatus.MULTI_STATUS);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.ALREADY_REPORTED} (208).
	 */
	public ExchangeActions isAlreadyReported() {
		return doMatch(HttpStatus.ALREADY_REPORTED);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.IM_USED} (226).
	 */
	public ExchangeActions isImUsed() {
		return doMatch(HttpStatus.IM_USED);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.MULTIPLE_CHOICES} (300).
	 */
	public ExchangeActions isMultipleChoices() {
		return doMatch(HttpStatus.MULTIPLE_CHOICES);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.MOVED_PERMANENTLY} (301).
	 */
	public ExchangeActions isMovedPermanently() {
		return doMatch(HttpStatus.MOVED_PERMANENTLY);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.FOUND} (302).
	 */
	public ExchangeActions isFound() {
		return doMatch(HttpStatus.FOUND);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.MOVED_TEMPORARILY} (302).
	 * @see #isFound()
	 * @deprecated in favor of {@link #isFound()}
	 */
	@Deprecated
	public ExchangeActions isMovedTemporarily() {
		return doMatch(HttpStatus.MOVED_TEMPORARILY);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.SEE_OTHER} (303).
	 */
	public ExchangeActions isSeeOther() {
		return doMatch(HttpStatus.SEE_OTHER);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.NOT_MODIFIED} (304).
	 */
	public ExchangeActions isNotModified() {
		return doMatch(HttpStatus.NOT_MODIFIED);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.USE_PROXY} (305).
	 * @deprecated matching the deprecation of {@code HttpStatus.USE_PROXY}
	 */
	@Deprecated
	public ExchangeActions isUseProxy() {
		return doMatch(HttpStatus.USE_PROXY);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.TEMPORARY_REDIRECT} (307).
	 */
	public ExchangeActions isTemporaryRedirect() {
		return doMatch(HttpStatus.TEMPORARY_REDIRECT);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.PERMANENT_REDIRECT} (308).
	 */
	public ExchangeActions isPermanentRedirect() {
		return doMatch(HttpStatus.valueOf(308));
	}

	/**
	 * Assert the response status code is {@code HttpStatus.BAD_REQUEST} (400).
	 */
	public ExchangeActions isBadRequest() {
		return doMatch(HttpStatus.BAD_REQUEST);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.UNAUTHORIZED} (401).
	 */
	public ExchangeActions isUnauthorized() {
		return doMatch(HttpStatus.UNAUTHORIZED);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.PAYMENT_REQUIRED} (402).
	 */
	public ExchangeActions isPaymentRequired() {
		return doMatch(HttpStatus.PAYMENT_REQUIRED);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.FORBIDDEN} (403).
	 */
	public ExchangeActions isForbidden() {
		return doMatch(HttpStatus.FORBIDDEN);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.NOT_FOUND} (404).
	 */
	public ExchangeActions isNotFound() {
		return doMatch(HttpStatus.NOT_FOUND);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.METHOD_NOT_ALLOWED} (405).
	 */
	public ExchangeActions isMethodNotAllowed() {
		return doMatch(HttpStatus.METHOD_NOT_ALLOWED);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.NOT_ACCEPTABLE} (406).
	 */
	public ExchangeActions isNotAcceptable() {
		return doMatch(HttpStatus.NOT_ACCEPTABLE);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.PROXY_AUTHENTICATION_REQUIRED} (407).
	 */
	public ExchangeActions isProxyAuthenticationRequired() {
		return doMatch(HttpStatus.PROXY_AUTHENTICATION_REQUIRED);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.REQUEST_TIMEOUT} (408).
	 */
	public ExchangeActions isRequestTimeout() {
		return doMatch(HttpStatus.REQUEST_TIMEOUT);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.CONFLICT} (409).
	 */
	public ExchangeActions isConflict() {
		return doMatch(HttpStatus.CONFLICT);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.GONE} (410).
	 */
	public ExchangeActions isGone() {
		return doMatch(HttpStatus.GONE);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.LENGTH_REQUIRED} (411).
	 */
	public ExchangeActions isLengthRequired() {
		return doMatch(HttpStatus.LENGTH_REQUIRED);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.PRECONDITION_FAILED} (412).
	 */
	public ExchangeActions isPreconditionFailed() {
		return doMatch(HttpStatus.PRECONDITION_FAILED);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.PAYLOAD_TOO_LARGE} (413).
	 * @since 4.1
	 */
	public ExchangeActions isPayloadTooLarge() {
		return doMatch(HttpStatus.PAYLOAD_TOO_LARGE);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.REQUEST_ENTITY_TOO_LARGE} (413).
	 * @deprecated matching the deprecation of {@code HttpStatus.REQUEST_ENTITY_TOO_LARGE}
	 * @see #isPayloadTooLarge()
	 */
	@Deprecated
	public ExchangeActions isRequestEntityTooLarge() {
		return doMatch(HttpStatus.REQUEST_ENTITY_TOO_LARGE);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.REQUEST_URI_TOO_LONG} (414).
	 * @since 4.1
	 */
	public ExchangeActions isUriTooLong() {
		return doMatch(HttpStatus.URI_TOO_LONG);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.REQUEST_URI_TOO_LONG} (414).
	 * @deprecated matching the deprecation of {@code HttpStatus.REQUEST_URI_TOO_LONG}
	 * @see #isUriTooLong()
	 */
	@Deprecated
	public ExchangeActions isRequestUriTooLong() {
		return doMatch(HttpStatus.REQUEST_URI_TOO_LONG);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.UNSUPPORTED_MEDIA_TYPE} (415).
	 */
	public ExchangeActions isUnsupportedMediaType() {
		return doMatch(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE} (416).
	 */
	public ExchangeActions isRequestedRangeNotSatisfiable() {
		return doMatch(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.EXPECTATION_FAILED} (417).
	 */
	public ExchangeActions isExpectationFailed() {
		return doMatch(HttpStatus.EXPECTATION_FAILED);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.I_AM_A_TEAPOT} (418).
	 */
	public ExchangeActions isIAmATeapot() {
		return doMatch(HttpStatus.valueOf(418));
	}

	/**
	 * Assert the response status code is {@code HttpStatus.INSUFFICIENT_SPACE_ON_RESOURCE} (419).
	 * @deprecated matching the deprecation of {@code HttpStatus.INSUFFICIENT_SPACE_ON_RESOURCE}
	 */
	@Deprecated
	public ExchangeActions isInsufficientSpaceOnResource() {
		return doMatch(HttpStatus.INSUFFICIENT_SPACE_ON_RESOURCE);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.METHOD_FAILURE} (420).
	 * @deprecated matching the deprecation of {@code HttpStatus.METHOD_FAILURE}
	 */
	@Deprecated
	public ExchangeActions isMethodFailure() {
		return doMatch(HttpStatus.METHOD_FAILURE);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.DESTINATION_LOCKED} (421).
	 * @deprecated matching the deprecation of {@code HttpStatus.DESTINATION_LOCKED}
	 */
	@Deprecated
	public ExchangeActions isDestinationLocked() {
		return doMatch(HttpStatus.DESTINATION_LOCKED);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.UNPROCESSABLE_ENTITY} (422).
	 */
	public ExchangeActions isUnprocessableEntity() {
		return doMatch(HttpStatus.UNPROCESSABLE_ENTITY);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.LOCKED} (423).
	 */
	public ExchangeActions isLocked() {
		return doMatch(HttpStatus.LOCKED);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.FAILED_DEPENDENCY} (424).
	 */
	public ExchangeActions isFailedDependency() {
		return doMatch(HttpStatus.FAILED_DEPENDENCY);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.UPGRADE_REQUIRED} (426).
	 */
	public ExchangeActions isUpgradeRequired() {
		return doMatch(HttpStatus.UPGRADE_REQUIRED);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.PRECONDITION_REQUIRED} (428).
	 */
	public ExchangeActions isPreconditionRequired() {
		return doMatch(HttpStatus.valueOf(428));
	}

	/**
	 * Assert the response status code is {@code HttpStatus.TOO_MANY_REQUESTS} (429).
	 */
	public ExchangeActions isTooManyRequests() {
		return doMatch(HttpStatus.valueOf(429));
	}

	/**
	 * Assert the response status code is {@code HttpStatus.REQUEST_HEADER_FIELDS_TOO_LARGE} (431).
	 */
	public ExchangeActions isRequestHeaderFieldsTooLarge() {
		return doMatch(HttpStatus.valueOf(431));
	}

	/**
	 * Assert the response status code is {@code HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS} (451).
	 * @since 4.3
	 */
	public ExchangeActions isUnavailableForLegalReasons() {
		return doMatch(HttpStatus.valueOf(451));
	}

	/**
	 * Assert the response status code is {@code HttpStatus.INTERNAL_SERVER_ERROR} (500).
	 */
	public ExchangeActions isInternalServerError() {
		return doMatch(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.NOT_IMPLEMENTED} (501).
	 */
	public ExchangeActions isNotImplemented() {
		return doMatch(HttpStatus.NOT_IMPLEMENTED);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.BAD_GATEWAY} (502).
	 */
	public ExchangeActions isBadGateway() {
		return doMatch(HttpStatus.BAD_GATEWAY);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.SERVICE_UNAVAILABLE} (503).
	 */
	public ExchangeActions isServiceUnavailable() {
		return doMatch(HttpStatus.SERVICE_UNAVAILABLE);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.GATEWAY_TIMEOUT} (504).
	 */
	public ExchangeActions isGatewayTimeout() {
		return doMatch(HttpStatus.GATEWAY_TIMEOUT);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.HTTP_VERSION_NOT_SUPPORTED} (505).
	 */
	public ExchangeActions isHttpVersionNotSupported() {
		return doMatch(HttpStatus.HTTP_VERSION_NOT_SUPPORTED);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.VARIANT_ALSO_NEGOTIATES} (506).
	 */
	public ExchangeActions isVariantAlsoNegotiates() {
		return doMatch(HttpStatus.VARIANT_ALSO_NEGOTIATES);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.INSUFFICIENT_STORAGE} (507).
	 */
	public ExchangeActions isInsufficientStorage() {
		return doMatch(HttpStatus.INSUFFICIENT_STORAGE);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.LOOP_DETECTED} (508).
	 */
	public ExchangeActions isLoopDetected() {
		return doMatch(HttpStatus.LOOP_DETECTED);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.BANDWIDTH_LIMIT_EXCEEDED} (509).
	 */
	public ExchangeActions isBandwidthLimitExceeded() {
		return doMatch(HttpStatus.valueOf(509));
	}

	/**
	 * Assert the response status code is {@code HttpStatus.NOT_EXTENDED} (510).
	 */
	public ExchangeActions isNotExtended() {
		return doMatch(HttpStatus.NOT_EXTENDED);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.NETWORK_AUTHENTICATION_REQUIRED} (511).
	 */
	public ExchangeActions isNetworkAuthenticationRequired() {
		return doMatch(HttpStatus.valueOf(511));
	}

	private ExchangeActions doMatch(final HttpStatus status) {
		assertEquals("Status", status, this.httpStatus);
		return this.exchangeActions;
	}

}
