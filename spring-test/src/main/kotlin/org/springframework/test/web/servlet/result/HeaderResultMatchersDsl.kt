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
 * Provide a [HeaderResultMatchers] Kotlin DSL in order to be able to write idiomatic Kotlin code.
 *
 * @author Sebastien Deleuze
 * @since 5.3
 */
class HeaderResultMatchersDsl internal constructor (private val actions: ResultActions) {

	private val matchers = MockMvcResultMatchers.header()

	/**
	 * @see HeaderResultMatchersDsl.string
	 */
	fun string(name: String, matcher: Matcher<String>) {
		actions.andExpect(matchers.string(name, matcher))
	}

	/**
	 * @see HeaderResultMatchersDsl.stringValues
	 */
	fun stringValues(name: String, matcher: Matcher<Iterable<String>>) {
		actions.andExpect(matchers.stringValues(name, matcher))
	}

	/**
	 * @see HeaderResultMatchersDsl.string
	 */
	fun string(name: String, value: String) {
		actions.andExpect(matchers.string(name, value))
	}

	/**
	 * @see HeaderResultMatchersDsl.stringValues
	 */
	fun stringValues(name: String, vararg value: String) {
		actions.andExpect(matchers.stringValues(name, *value))
	}

	/**
	 * @see HeaderResultMatchersDsl.exists
	 */
	fun exists(name: String) {
		actions.andExpect(matchers.exists(name))
	}

	/**
	 * @see HeaderResultMatchersDsl.doesNotExist
	 */
	fun doesNotExist(name: String) {
		actions.andExpect(matchers.doesNotExist(name))
	}

	/**
	 * @see HeaderResultMatchersDsl.longValue
	 */
	fun longValue(name: String, value: Long) {
		actions.andExpect(matchers.longValue(name, value))
	}
	/**
	 * @see HeaderResultMatchersDsl.dateValue
	 */
	fun dateValue(name: String, value: Long) {
		actions.andExpect(matchers.dateValue(name, value))
	}
}
