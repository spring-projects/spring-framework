/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.result

import org.hamcrest.Matcher
import org.springframework.test.web.servlet.ResultActions

/**
 * Provide a [RequestResultMatchers] Kotlin DSL in order to be able to write idiomatic Kotlin code.
 *
 * @author Sebastien Deleuze
 * @since 5.3
 */
class RequestResultMatchersDsl internal constructor (private val actions: ResultActions) {

	private val matchers = MockMvcResultMatchers.request()

	/**
	 * @see RequestResultMatchers.asyncStarted
	 */
	fun asyncStarted() {
		actions.andExpect(matchers.asyncStarted())
	}

	/**
	 * @see RequestResultMatchers.asyncStarted
	 */
	fun asyncNotStarted() {
		actions.andExpect(matchers.asyncNotStarted())
	}

	/**
	 * @see RequestResultMatchers.asyncResult
	 */
	fun <T : Any> asyncResult(matcher: Matcher<T>) {
		actions.andExpect(matchers.asyncResult(matcher))
	}

	/**
	 * @see RequestResultMatchers.asyncResult
	 */
	fun asyncResult(expectedResult: Any?) {
		actions.andExpect(matchers.asyncResult(expectedResult))
	}

	/**
	 * @see RequestResultMatchers.attribute
	 */
	fun <T : Any> attribute(name: String, matcher: Matcher<T>) {
		actions.andExpect(matchers.attribute(name, matcher))
	}

	/**
	 * @see RequestResultMatchers.attribute
	 */
	fun attribute(name: String, expectedValue: Any?) {
		actions.andExpect(matchers.attribute(name, expectedValue))
	}

	/**
	 * @see RequestResultMatchers.sessionAttribute
	 */
	fun <T : Any> sessionAttribute(name: String, matcher: Matcher<T>) {
		actions.andExpect(matchers.sessionAttribute(name, matcher))
	}

	/**
	 * @see RequestResultMatchers.sessionAttribute
	 */
	fun sessionAttribute(name: String, expectedValue: Any?) {
		actions.andExpect(matchers.sessionAttribute(name, expectedValue))
	}

	/**
	 * @see RequestResultMatchers.sessionAttributeDoesNotExist
	 */
	fun sessionAttributeDoesNotExist(vararg names: String) {
		actions.andExpect(matchers.sessionAttributeDoesNotExist(*names))
	}
}
