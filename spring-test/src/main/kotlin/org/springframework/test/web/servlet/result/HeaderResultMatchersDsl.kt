/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
