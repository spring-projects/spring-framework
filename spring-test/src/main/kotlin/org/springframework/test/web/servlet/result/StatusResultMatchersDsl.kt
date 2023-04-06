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

package org.springframework.test.web.servlet.result

import org.hamcrest.Matcher
import org.springframework.test.web.servlet.ResultActions

/**
 * Provide a [StatusResultMatchers] Kotlin DSL in order to be able to write idiomatic Kotlin code.
 *
 * @author Sebastien Deleuze
 * @since 5.3
 */
@Suppress("UsePropertyAccessSyntax")
class StatusResultMatchersDsl internal constructor (private val actions: ResultActions) {

	private val matchers = MockMvcResultMatchers.status()

	/**
	 * @see StatusResultMatchers.is
	 */
	fun isEqualTo(matcher: Matcher<Int>) {
		actions.andExpect(matchers.`is`(matcher))
	}

	/**
	 * @see StatusResultMatchers.is
	 */
	fun isEqualTo(status: Int) {
		actions.andExpect(matchers.`is`(status))
	}

	/**
	 * @see StatusResultMatchers.is1xxInformational
	 */
	fun is1xxInformational() {
		actions.andExpect(matchers.is1xxInformational())
	}

	/**
	 * @see StatusResultMatchers.is2xxSuccessful
	 */
	fun is2xxSuccessful() {
		actions.andExpect(matchers.is2xxSuccessful())
	}

	/**
	 * @see StatusResultMatchers.is3xxRedirection
	 */
	fun is3xxRedirection() {
		actions.andExpect(matchers.is3xxRedirection())
	}

	/**
	 * @see StatusResultMatchers.is4xxClientError
	 */
	fun is4xxClientError() {
		actions.andExpect(matchers.is4xxClientError())
	}

	/**
	 * @see StatusResultMatchers.is5xxServerError
	 */
	fun is5xxServerError() {
		actions.andExpect(matchers.is5xxServerError())
	}

	/**
	 * @see StatusResultMatchers.reason
	 */
	fun reason(matcher: Matcher<String>) {
		actions.andExpect(matchers.reason(matcher))
	}

	/**
	 * @see StatusResultMatchers.reason
	 */
	fun reason(reason: String) {
		actions.andExpect(matchers.reason(reason))
	}

	/**
	 * @see StatusResultMatchers.isContinue
	 */
	fun isContinue() {
		actions.andExpect(matchers.isContinue())
	}

	/**
	 * @see StatusResultMatchers.isSwitchingProtocols
	 */
	fun isSwitchingProtocols() {
		actions.andExpect(matchers.isSwitchingProtocols())
	}

	/**
	 * @see StatusResultMatchers.isProcessing
	 */
	fun isProcessing() {
		actions.andExpect(matchers.isProcessing())
	}

	/**
	 * @see isEarlyHints
	 */
	@Deprecated("use isEarlyHints() instead", replaceWith= ReplaceWith("isEarlyHints()"))
	fun isCheckpoint() {
		@Suppress("DEPRECATION")
		actions.andExpect(matchers.isCheckpoint())
	}

	/**
	 * @see StatusResultMatchers.isEarlyHints
	 * @since 6.0.5
	 */
	fun isEarlyHints() {
		actions.andExpect(matchers.isEarlyHints())
	}

	/**
	 * @see StatusResultMatchers.isOk
	 */
	fun isOk() {
		actions.andExpect(matchers.isOk())
	}

	/**
	 * @see StatusResultMatchers.isCreated
	 */
	fun isCreated() {
		actions.andExpect(matchers.isCreated())
	}

	/**
	 * @see StatusResultMatchers.isAccepted
	 */
	fun isAccepted() {
		actions.andExpect(matchers.isAccepted())
	}

	/**
	 * @see StatusResultMatchers.isNonAuthoritativeInformation
	 */
	fun isNonAuthoritativeInformation() {
		actions.andExpect(matchers.isNonAuthoritativeInformation())
	}

	/**
	 * @see StatusResultMatchers.isNoContent
	 */
	fun isNoContent() {
		actions.andExpect(matchers.isNoContent())
	}

	/**
	 * @see StatusResultMatchers.isResetContent
	 */
	fun isResetContent() {
		actions.andExpect(matchers.isResetContent())
	}

	/**
	 * @see StatusResultMatchers.isPartialContent
	 */
	fun isPartialContent() {
		actions.andExpect(matchers.isPartialContent())
	}

	/**
	 * @see StatusResultMatchers.isMultiStatus
	 */
	fun isMultiStatus() {
		actions.andExpect(matchers.isMultiStatus())
	}

	/**
	 * @see StatusResultMatchers.isAlreadyReported
	 */
	fun isAlreadyReported() {
		actions.andExpect(matchers.isAlreadyReported())
	}

	/**
	 * @see StatusResultMatchers.isImUsed
	 */
	fun isImUsed() {
		actions.andExpect(matchers.isImUsed())
	}

	/**
	 * @see StatusResultMatchers.isMultipleChoices
	 */
	fun isMultipleChoices() {
		actions.andExpect(matchers.isMultipleChoices())
	}

	/**
	 * @see StatusResultMatchers.isFound
	 */
	fun isFound() {
		actions.andExpect(matchers.isFound())
	}

	/**
	 * @see StatusResultMatchers.isSeeOther
	 */
	fun isSeeOther() {
		actions.andExpect(matchers.isSeeOther())
	}

	/**
	 * @see StatusResultMatchers.isNotModified
	 */
	fun isNotModified() {
		actions.andExpect(matchers.isNotModified())
	}

	/**
	 * @see StatusResultMatchers.isTemporaryRedirect
	 */
	fun isTemporaryRedirect() {
		actions.andExpect(matchers.isTemporaryRedirect())
	}

	/**
	 * @see StatusResultMatchers.isPermanentRedirect
	 */
	fun isPermanentRedirect() {
		actions.andExpect(matchers.isPermanentRedirect())
	}

	/**
	 * @see StatusResultMatchers.isBadRequest
	 */
	fun isBadRequest() {
		actions.andExpect(matchers.isBadRequest())
	}

	/**
	 * @see StatusResultMatchers.isUnauthorized
	 */
	fun isUnauthorized() {
		actions.andExpect(matchers.isUnauthorized())
	}

	/**
	 * @see StatusResultMatchers.isPaymentRequired
	 */
	fun isPaymentRequired() {
		actions.andExpect(matchers.isPaymentRequired())
	}

	/**
	 * @see StatusResultMatchers.isForbidden
	 */
	fun isForbidden() {
		actions.andExpect(matchers.isForbidden())
	}

	/**
	 * @see StatusResultMatchers.isNotFound
	 */
	fun isNotFound() {
		actions.andExpect(matchers.isNotFound())
	}

	/**
	 * @see StatusResultMatchers.isMethodNotAllowed
	 */
	fun isMethodNotAllowed() {
		actions.andExpect(matchers.isMethodNotAllowed())
	}

	/**
	 * @see StatusResultMatchers.isNotAcceptable
	 */
	fun isNotAcceptable() {
		actions.andExpect(matchers.isNotAcceptable())
	}

	/**
	 * @see StatusResultMatchers.isProxyAuthenticationRequired
	 */
	fun isProxyAuthenticationRequired() {
		actions.andExpect(matchers.isProxyAuthenticationRequired())
	}

	/**
	 * @see StatusResultMatchers.isRequestTimeout
	 */
	fun isRequestTimeout() {
		actions.andExpect(matchers.isRequestTimeout())
	}

	/**
	 * @see StatusResultMatchers.isConflict
	 */
	fun isConflict() {
		actions.andExpect(matchers.isConflict())
	}

	/**
	 * @see StatusResultMatchers.isGone
	 */
	fun isGone() {
		actions.andExpect(matchers.isGone())
	}

	/**
	 * @see StatusResultMatchers.isLengthRequired
	 */
	fun isLengthRequired() {
		actions.andExpect(matchers.isLengthRequired())
	}

	/**
	 * @see StatusResultMatchers.isPreconditionFailed
	 */
	fun isPreconditionFailed() {
		actions.andExpect(matchers.isPreconditionFailed())
	}

	/**
	 * @see StatusResultMatchers.isPayloadTooLarge
	 */
	fun isPayloadTooLarge() {
		actions.andExpect(matchers.isPayloadTooLarge())
	}

	/**
	 * @see StatusResultMatchers.isUriTooLong
	 */
	fun isUriTooLong() {
		actions.andExpect(matchers.isUriTooLong())
	}

	/**
	 * @see StatusResultMatchers.isUnsupportedMediaType
	 */
	fun isUnsupportedMediaType() {
		actions.andExpect(matchers.isUnsupportedMediaType())
	}

	/**
	 * @see StatusResultMatchers.isRequestedRangeNotSatisfiable
	 */
	fun isRequestedRangeNotSatisfiable() {
		actions.andExpect(matchers.isRequestedRangeNotSatisfiable())
	}

	/**
	 * @see StatusResultMatchers.isExpectationFailed
	 */
	fun isExpectationFailed() {
		actions.andExpect(matchers.isExpectationFailed())
	}

	/**
	 * @see StatusResultMatchers.isIAmATeapot
	 */
	fun isIAmATeapot() {
		actions.andExpect(matchers.isIAmATeapot())
	}

	/**
	 * @see StatusResultMatchers.isUnprocessableEntity
	 */
	fun isUnprocessableEntity() {
		actions.andExpect(matchers.isUnprocessableEntity())
	}

	/**
	 * @see StatusResultMatchers.isLocked
	 */
	fun isLocked() {
		actions.andExpect(matchers.isLocked())
	}

	/**
	 * @see StatusResultMatchers.isFailedDependency
	 */
	fun isFailedDependency() {
		actions.andExpect(matchers.isFailedDependency())
	}

	/**
	 * @see StatusResultMatchers.isTooEarly
	 */
	fun isTooEarly() {
		actions.andExpect(matchers.isTooEarly())
	}

	/**
	 * @see StatusResultMatchers.isUpgradeRequired
	 */
	fun isUpgradeRequired() {
		actions.andExpect(matchers.isUpgradeRequired())
	}

	/**
	 * @see StatusResultMatchers.isPreconditionRequired
	 */
	fun isPreconditionRequired() {
		actions.andExpect(matchers.isPreconditionRequired())
	}

	/**
	 * @see StatusResultMatchers.isTooManyRequests
	 */
	fun isTooManyRequests() {
		actions.andExpect(matchers.isTooManyRequests())
	}

	/**
	 * @see StatusResultMatchers.isRequestHeaderFieldsTooLarge
	 */
	fun isRequestHeaderFieldsTooLarge() {
		actions.andExpect(matchers.isRequestHeaderFieldsTooLarge())
	}

	/**
	 * @see StatusResultMatchers.isUnavailableForLegalReasons
	 */
	fun isUnavailableForLegalReasons() {
		actions.andExpect(matchers.isUnavailableForLegalReasons())
	}

	/**
	 * @see StatusResultMatchers.isInternalServerError
	 */
	fun isInternalServerError() {
		actions.andExpect(matchers.isInternalServerError())
	}

	/**
	 * @see StatusResultMatchers.isNotImplemented
	 */
	fun isNotImplemented() {
		actions.andExpect(matchers.isNotImplemented())
	}

	/**
	 * @see StatusResultMatchers.isBadGateway
	 */
	fun isBadGateway() {
		actions.andExpect(matchers.isBadGateway())
	}

	/**
	 * @see StatusResultMatchers.isServiceUnavailable
	 */
	fun isServiceUnavailable() {
		actions.andExpect(matchers.isServiceUnavailable())
	}

	/**
	 * @see StatusResultMatchers.isGatewayTimeout
	 */
	fun isGatewayTimeout() {
		actions.andExpect(matchers.isGatewayTimeout())
	}

	/**
	 * @see StatusResultMatchers.isHttpVersionNotSupported
	 */
	fun isHttpVersionNotSupported() {
		actions.andExpect(matchers.isHttpVersionNotSupported())
	}

	/**
	 * @see StatusResultMatchers.isVariantAlsoNegotiates
	 */
	fun isVariantAlsoNegotiates() {
		actions.andExpect(matchers.isVariantAlsoNegotiates())
	}

	/**
	 * @see StatusResultMatchers.isInsufficientStorage
	 */
	fun isInsufficientStorage() {
		actions.andExpect(matchers.isInsufficientStorage())
	}

	/**
	 * @see StatusResultMatchers.isLoopDetected
	 */
	fun isLoopDetected() {
		actions.andExpect(matchers.isLoopDetected())
	}

	/**
	 * @see StatusResultMatchers.isBandwidthLimitExceeded
	 */
	fun isBandwidthLimitExceeded() {
		actions.andExpect(matchers.isBandwidthLimitExceeded())
	}

	/**
	 * @see StatusResultMatchers.isNotExtended
	 */
	fun isNotExtended() {
		actions.andExpect(matchers.isNotExtended())
	}

	/**
	 * @see StatusResultMatchers.isNetworkAuthenticationRequired
	 */
	fun isNetworkAuthenticationRequired() {
		actions.andExpect(matchers.isNetworkAuthenticationRequired())
	}
}
