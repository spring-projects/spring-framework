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

package org.springframework.test.web.servlet.result;

import org.hamcrest.Matcher;

import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.hamcrest.MatcherAssert.*;
import static org.springframework.test.util.AssertionErrors.*;

/**
 * Factory for assertions on the response status.
 *
 * <p>An instance of this class is typically accessed via
 * {@link MockMvcResultMatchers#status}.
 *
 * @author Keesun Baik
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @since 3.2
 */
public class StatusResultMatchers {

	/**
	 * Protected constructor.
	 * Use {@link MockMvcResultMatchers#status()}.
	 */
	protected StatusResultMatchers() {
	}


	/**
	 * Assert the response status code with the given Hamcrest {@link Matcher}.
	 */
	public ResultMatcher is(final Matcher<Integer> matcher) {
		return result -> assertThat("Response status", result.getResponse().getStatus(), matcher);
	}

	/**
	 * Assert the response status code is equal to an integer value.
	 */
	public ResultMatcher is(final int status) {
		return result -> assertEquals("Response status", status, result.getResponse().getStatus());
	}

	/**
	 * Assert the response status code is in the 1xx range.
	 */
	public ResultMatcher is1xxInformational() {
		return result -> assertEquals("Range for response status value " + result.getResponse().getStatus(),
				HttpStatus.Series.INFORMATIONAL, getHttpStatusSeries(result));
	}

	/**
	 * Assert the response status code is in the 2xx range.
	 */
	public ResultMatcher is2xxSuccessful() {
		return result -> assertEquals("Range for response status value " + result.getResponse().getStatus(),
				HttpStatus.Series.SUCCESSFUL, getHttpStatusSeries(result));
	}

	/**
	 * Assert the response status code is in the 3xx range.
	 */
	public ResultMatcher is3xxRedirection() {
		return result -> assertEquals("Range for response status value " + result.getResponse().getStatus(),
				HttpStatus.Series.REDIRECTION, getHttpStatusSeries(result));
	}

	/**
	 * Assert the response status code is in the 4xx range.
	 */
	public ResultMatcher is4xxClientError() {
		return result -> assertEquals("Range for response status value " + result.getResponse().getStatus(),
				HttpStatus.Series.CLIENT_ERROR, getHttpStatusSeries(result));
	}

	/**
	 * Assert the response status code is in the 5xx range.
	 */
	public ResultMatcher is5xxServerError() {
		return result -> assertEquals("Range for response status value " + result.getResponse().getStatus(),
				HttpStatus.Series.SERVER_ERROR, getHttpStatusSeries(result));
	}

	private HttpStatus.Series getHttpStatusSeries(MvcResult result) {
		int statusValue = result.getResponse().getStatus();
		HttpStatus status = HttpStatus.valueOf(statusValue);
		return status.series();
	}

	/**
	 * Assert the Servlet response error message with the given Hamcrest {@link Matcher}.
	 */
	public ResultMatcher reason(final Matcher<? super String> matcher) {
		return result -> assertThat("Response status reason", result.getResponse().getErrorMessage(), matcher);
	}

	/**
	 * Assert the Servlet response error message.
	 */
	public ResultMatcher reason(final String reason) {
		return result -> assertEquals("Response status reason", reason, result.getResponse().getErrorMessage());
	}

	/**
	 * Assert the response status code is {@code HttpStatus.CONTINUE} (100).
	 */
	public ResultMatcher isContinue() {
		return matcher(HttpStatus.CONTINUE);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.SWITCHING_PROTOCOLS} (101).
	 */
	public ResultMatcher isSwitchingProtocols() {
		return matcher(HttpStatus.SWITCHING_PROTOCOLS);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.PROCESSING} (102).
	 */
	public ResultMatcher isProcessing() {
		return matcher(HttpStatus.PROCESSING);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.CHECKPOINT} (103).
	 */
	public ResultMatcher isCheckpoint() {
		return matcher(HttpStatus.valueOf(103));
	}

	/**
	 * Assert the response status code is {@code HttpStatus.OK} (200).
	 */
	public ResultMatcher isOk() {
		return matcher(HttpStatus.OK);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.CREATED} (201).
	 */
	public ResultMatcher isCreated() {
		return matcher(HttpStatus.CREATED);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.ACCEPTED} (202).
	 */
	public ResultMatcher isAccepted() {
		return matcher(HttpStatus.ACCEPTED);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.NON_AUTHORITATIVE_INFORMATION} (203).
	 */
	public ResultMatcher isNonAuthoritativeInformation() {
		return matcher(HttpStatus.NON_AUTHORITATIVE_INFORMATION);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.NO_CONTENT} (204).
	 */
	public ResultMatcher isNoContent() {
		return matcher(HttpStatus.NO_CONTENT);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.RESET_CONTENT} (205).
	 */
	public ResultMatcher isResetContent() {
		return matcher(HttpStatus.RESET_CONTENT);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.PARTIAL_CONTENT} (206).
	 */
	public ResultMatcher isPartialContent() {
		return matcher(HttpStatus.PARTIAL_CONTENT);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.MULTI_STATUS} (207).
	 */
	public ResultMatcher isMultiStatus() {
		return matcher(HttpStatus.MULTI_STATUS);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.ALREADY_REPORTED} (208).
	 */
	public ResultMatcher isAlreadyReported() {
		return matcher(HttpStatus.ALREADY_REPORTED);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.IM_USED} (226).
	 */
	public ResultMatcher isImUsed() {
		return matcher(HttpStatus.IM_USED);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.MULTIPLE_CHOICES} (300).
	 */
	public ResultMatcher isMultipleChoices() {
		return matcher(HttpStatus.MULTIPLE_CHOICES);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.MOVED_PERMANENTLY} (301).
	 */
	public ResultMatcher isMovedPermanently() {
		return matcher(HttpStatus.MOVED_PERMANENTLY);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.FOUND} (302).
	 */
	public ResultMatcher isFound() {
		return matcher(HttpStatus.FOUND);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.MOVED_TEMPORARILY} (302).
	 * @see #isFound()
	 * @deprecated in favor of {@link #isFound()}
	 */
	@Deprecated
	public ResultMatcher isMovedTemporarily() {
		return matcher(HttpStatus.MOVED_TEMPORARILY);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.SEE_OTHER} (303).
	 */
	public ResultMatcher isSeeOther() {
		return matcher(HttpStatus.SEE_OTHER);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.NOT_MODIFIED} (304).
	 */
	public ResultMatcher isNotModified() {
		return matcher(HttpStatus.NOT_MODIFIED);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.USE_PROXY} (305).
	 * @deprecated matching the deprecation of {@code HttpStatus.USE_PROXY}
	 */
	@Deprecated
	public ResultMatcher isUseProxy() {
		return matcher(HttpStatus.USE_PROXY);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.TEMPORARY_REDIRECT} (307).
	 */
	public ResultMatcher isTemporaryRedirect() {
		return matcher(HttpStatus.TEMPORARY_REDIRECT);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.PERMANENT_REDIRECT} (308).
	 */
	public ResultMatcher isPermanentRedirect() {
		return matcher(HttpStatus.valueOf(308));
	}

	/**
	 * Assert the response status code is {@code HttpStatus.BAD_REQUEST} (400).
	 */
	public ResultMatcher isBadRequest() {
		return matcher(HttpStatus.BAD_REQUEST);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.UNAUTHORIZED} (401).
	 */
	public ResultMatcher isUnauthorized() {
		return matcher(HttpStatus.UNAUTHORIZED);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.PAYMENT_REQUIRED} (402).
	 */
	public ResultMatcher isPaymentRequired() {
		return matcher(HttpStatus.PAYMENT_REQUIRED);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.FORBIDDEN} (403).
	 */
	public ResultMatcher isForbidden() {
		return matcher(HttpStatus.FORBIDDEN);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.NOT_FOUND} (404).
	 */
	public ResultMatcher isNotFound() {
		return matcher(HttpStatus.NOT_FOUND);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.METHOD_NOT_ALLOWED} (405).
	 */
	public ResultMatcher isMethodNotAllowed() {
		return matcher(HttpStatus.METHOD_NOT_ALLOWED);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.NOT_ACCEPTABLE} (406).
	 */
	public ResultMatcher isNotAcceptable() {
		return matcher(HttpStatus.NOT_ACCEPTABLE);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.PROXY_AUTHENTICATION_REQUIRED} (407).
	 */
	public ResultMatcher isProxyAuthenticationRequired() {
		return matcher(HttpStatus.PROXY_AUTHENTICATION_REQUIRED);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.REQUEST_TIMEOUT} (408).
	 */
	public ResultMatcher isRequestTimeout() {
		return matcher(HttpStatus.REQUEST_TIMEOUT);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.CONFLICT} (409).
	 */
	public ResultMatcher isConflict() {
		return matcher(HttpStatus.CONFLICT);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.GONE} (410).
	 */
	public ResultMatcher isGone() {
		return matcher(HttpStatus.GONE);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.LENGTH_REQUIRED} (411).
	 */
	public ResultMatcher isLengthRequired() {
		return matcher(HttpStatus.LENGTH_REQUIRED);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.PRECONDITION_FAILED} (412).
	 */
	public ResultMatcher isPreconditionFailed() {
		return matcher(HttpStatus.PRECONDITION_FAILED);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.PAYLOAD_TOO_LARGE} (413).
	 * @since 4.1
	 */
	public ResultMatcher isPayloadTooLarge() {
		return matcher(HttpStatus.PAYLOAD_TOO_LARGE);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.REQUEST_ENTITY_TOO_LARGE} (413).
	 * @deprecated matching the deprecation of {@code HttpStatus.REQUEST_ENTITY_TOO_LARGE}
	 * @see #isPayloadTooLarge()
	 */
	@Deprecated
	public ResultMatcher isRequestEntityTooLarge() {
		return matcher(HttpStatus.REQUEST_ENTITY_TOO_LARGE);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.REQUEST_URI_TOO_LONG} (414).
	 * @since 4.1
	 */
	public ResultMatcher isUriTooLong() {
		return matcher(HttpStatus.URI_TOO_LONG);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.REQUEST_URI_TOO_LONG} (414).
	 * @deprecated matching the deprecation of {@code HttpStatus.REQUEST_URI_TOO_LONG}
	 * @see #isUriTooLong()
	 */
	@Deprecated
	public ResultMatcher isRequestUriTooLong() {
		return matcher(HttpStatus.REQUEST_URI_TOO_LONG);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.UNSUPPORTED_MEDIA_TYPE} (415).
	 */
	public ResultMatcher isUnsupportedMediaType() {
		return matcher(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE} (416).
	 */
	public ResultMatcher isRequestedRangeNotSatisfiable() {
		return matcher(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.EXPECTATION_FAILED} (417).
	 */
	public ResultMatcher isExpectationFailed() {
		return matcher(HttpStatus.EXPECTATION_FAILED);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.I_AM_A_TEAPOT} (418).
	 */
	public ResultMatcher isIAmATeapot() {
		return matcher(HttpStatus.valueOf(418));
	}

	/**
	  * Assert the response status code is {@code HttpStatus.INSUFFICIENT_SPACE_ON_RESOURCE} (419).
	  * @deprecated matching the deprecation of {@code HttpStatus.INSUFFICIENT_SPACE_ON_RESOURCE}
	  */
	 @Deprecated
	 public ResultMatcher isInsufficientSpaceOnResource() {
		 return matcher(HttpStatus.INSUFFICIENT_SPACE_ON_RESOURCE);
	 }

	 /**
	  * Assert the response status code is {@code HttpStatus.METHOD_FAILURE} (420).
	  * @deprecated matching the deprecation of {@code HttpStatus.METHOD_FAILURE}
	  */
	 @Deprecated
	 public ResultMatcher isMethodFailure() {
		 return matcher(HttpStatus.METHOD_FAILURE);
	 }

	 /**
	  * Assert the response status code is {@code HttpStatus.DESTINATION_LOCKED} (421).
	  * @deprecated matching the deprecation of {@code HttpStatus.DESTINATION_LOCKED}
	  */
	 @Deprecated
	 public ResultMatcher isDestinationLocked() {
		 return matcher(HttpStatus.DESTINATION_LOCKED);
	 }

	/**
	 * Assert the response status code is {@code HttpStatus.UNPROCESSABLE_ENTITY} (422).
	 */
	public ResultMatcher isUnprocessableEntity() {
		return matcher(HttpStatus.UNPROCESSABLE_ENTITY);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.LOCKED} (423).
	 */
	public ResultMatcher isLocked() {
		return matcher(HttpStatus.LOCKED);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.FAILED_DEPENDENCY} (424).
	 */
	public ResultMatcher isFailedDependency() {
		return matcher(HttpStatus.FAILED_DEPENDENCY);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.UPGRADE_REQUIRED} (426).
	 */
	public ResultMatcher isUpgradeRequired() {
		return matcher(HttpStatus.UPGRADE_REQUIRED);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.PRECONDITION_REQUIRED} (428).
	 */
	public ResultMatcher isPreconditionRequired() {
		return matcher(HttpStatus.valueOf(428));
	}

	/**
	 * Assert the response status code is {@code HttpStatus.TOO_MANY_REQUESTS} (429).
	 */
	public ResultMatcher isTooManyRequests() {
		return matcher(HttpStatus.valueOf(429));
	}

	/**
	 * Assert the response status code is {@code HttpStatus.REQUEST_HEADER_FIELDS_TOO_LARGE} (431).
	 */
	public ResultMatcher isRequestHeaderFieldsTooLarge() {
		return matcher(HttpStatus.valueOf(431));
	}

	/**
	 * Assert the response status code is {@code HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS} (451).
	 * @since 4.3
	 */
	public ResultMatcher isUnavailableForLegalReasons() {
		return matcher(HttpStatus.valueOf(451));
	}

	/**
	 * Assert the response status code is {@code HttpStatus.INTERNAL_SERVER_ERROR} (500).
	 */
	public ResultMatcher isInternalServerError() {
		return matcher(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.NOT_IMPLEMENTED} (501).
	 */
	public ResultMatcher isNotImplemented() {
		return matcher(HttpStatus.NOT_IMPLEMENTED);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.BAD_GATEWAY} (502).
	 */
	public ResultMatcher isBadGateway() {
		return matcher(HttpStatus.BAD_GATEWAY);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.SERVICE_UNAVAILABLE} (503).
	 */
	public ResultMatcher isServiceUnavailable() {
		return matcher(HttpStatus.SERVICE_UNAVAILABLE);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.GATEWAY_TIMEOUT} (504).
	 */
	public ResultMatcher isGatewayTimeout() {
		return matcher(HttpStatus.GATEWAY_TIMEOUT);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.HTTP_VERSION_NOT_SUPPORTED} (505).
	 */
	public ResultMatcher isHttpVersionNotSupported() {
		return matcher(HttpStatus.HTTP_VERSION_NOT_SUPPORTED);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.VARIANT_ALSO_NEGOTIATES} (506).
	 */
	public ResultMatcher isVariantAlsoNegotiates() {
		return matcher(HttpStatus.VARIANT_ALSO_NEGOTIATES);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.INSUFFICIENT_STORAGE} (507).
	 */
	public ResultMatcher isInsufficientStorage() {
		return matcher(HttpStatus.INSUFFICIENT_STORAGE);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.LOOP_DETECTED} (508).
	 */
	public ResultMatcher isLoopDetected() {
		return matcher(HttpStatus.LOOP_DETECTED);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.BANDWIDTH_LIMIT_EXCEEDED} (509).
	 */
	public ResultMatcher isBandwidthLimitExceeded() {
		return matcher(HttpStatus.valueOf(509));
	}

	/**
	 * Assert the response status code is {@code HttpStatus.NOT_EXTENDED} (510).
	 */
	public ResultMatcher isNotExtended() {
		return matcher(HttpStatus.NOT_EXTENDED);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.NETWORK_AUTHENTICATION_REQUIRED} (511).
	 */
	public ResultMatcher isNetworkAuthenticationRequired() {
		return matcher(HttpStatus.valueOf(511));
	}

	/**
	 * Match the expected response status to that of the HttpServletResponse
	 */
	private ResultMatcher matcher(final HttpStatus status) {
		return result -> assertEquals("Status", status.value(), result.getResponse().getStatus());
	}

}
