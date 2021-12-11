/*
 * Copyright 2002-2020 the original author or authors.
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
	fun <T> asyncResult(matcher: Matcher<T>) {
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
	fun <T> attribute(name: String, matcher: Matcher<T>) {
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
	fun <T> sessionAttribute(name: String, matcher: Matcher<T>) {
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
