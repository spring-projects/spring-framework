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
 * Provide a [ModelResultMatchers] Kotlin DSL in order to be able to write idiomatic Kotlin code.
 *
 * @author Sebastien Deleuze
 * @since 5.3
 */
class ModelResultMatchersDsl internal constructor (private val actions: ResultActions) {

	private val matchers = MockMvcResultMatchers.model()

	/**
	 * @see ModelResultMatchers.attribute
	 */
	fun <T> attribute(name: String, matcher: Matcher<T>) {
		actions.andExpect(matchers.attribute(name, matcher))
	}

	/**
	 * @see ModelResultMatchers.attribute
	 */
	fun attribute(name: String, value: Any?) {
		actions.andExpect(matchers.attribute(name, value))
	}

	/**
	 * @see ModelResultMatchers.attributeExists
	 */
	fun attributeExists(vararg name: String) {
		actions.andExpect(matchers.attributeExists(*name))
	}

	/**
	 * @see ModelResultMatchers.attributeDoesNotExist
	 */
	fun attributeDoesNotExist(vararg name: String) {
		actions.andExpect(matchers.attributeDoesNotExist(*name))
	}

	/**
	 * @see ModelResultMatchers.attributeErrorCount
	 */
	fun <T> attributeErrorCount(name: String, expectedCount: Int) {
		actions.andExpect(matchers.attributeErrorCount(name, expectedCount))
	}

	/**
	 * @see ModelResultMatchers.attributeHasErrors
	 */
	fun attributeHasErrors(vararg name: String) {
		actions.andExpect(matchers.attributeHasErrors(*name))
	}

	/**
	 * @see ModelResultMatchers.attributeHasNoErrors
	 */
	fun attributeHasNoErrors(vararg name: String) {
		actions.andExpect(matchers.attributeHasNoErrors(*name))
	}

	/**
	 * @see ModelResultMatchers.attributeHasFieldErrors
	 */
	fun attributeHasFieldErrors(name: String, vararg fieldNames: String) {
		actions.andExpect(matchers.attributeHasFieldErrors(name, *fieldNames))
	}

	/**
	 * @see ModelResultMatchers.attributeHasFieldErrorCode
	 */
	fun attributeHasFieldErrorCode(name: String, fieldName: String, code: String) {
		actions.andExpect(matchers.attributeHasFieldErrorCode(name, fieldName, code))
	}

	/**
	 * @see ModelResultMatchers.attributeHasFieldErrorCode
	 */
	fun attributeHasFieldErrorCode(name: String, fieldName: String, matcher: Matcher<String>) {
		actions.andExpect(matchers.attributeHasFieldErrorCode(name, fieldName, matcher))
	}

	/**
	 * @see ModelResultMatchers.errorCount
	 */
	fun errorCount(expectedCount: Int) {
		actions.andExpect(matchers.errorCount(expectedCount))
	}

	/**
	 * @see ModelResultMatchers.hasErrors
	 */
	fun hasErrors() {
		actions.andExpect(matchers.hasErrors())
	}

	/**
	 * @see ModelResultMatchers.hasNoErrors
	 */
	fun hasNoErrors() {
		actions.andExpect(matchers.hasNoErrors())
	}

	/**
	 * @see ModelResultMatchers.size
	 */
	fun size(size: Int) {
		actions.andExpect(matchers.size(size))
	}
}
