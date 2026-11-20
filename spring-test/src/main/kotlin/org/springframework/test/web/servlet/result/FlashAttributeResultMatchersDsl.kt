/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.result

import org.hamcrest.Matcher
import org.springframework.test.web.servlet.ResultActions

/**
 * Provide a [FlashAttributeResultMatchers] Kotlin DSL in order to be able to write idiomatic Kotlin code.
 *
 * @author Sebastien Deleuze
 * @since 5.3
 */
class FlashAttributeResultMatchersDsl internal constructor (private val actions: ResultActions) {

	private val matchers = MockMvcResultMatchers.flash()

	/**
	 * @see FlashAttributeResultMatchers.attribute
	 */
	fun <T : Any> attribute(name: String, matcher: Matcher<T>) {
		actions.andExpect(matchers.attribute(name, matcher))
	}

	/**
	 * @see FlashAttributeResultMatchers.attribute
	 */
	fun attribute(name: String, value: Any?) {
		actions.andExpect(matchers.attribute(name, value))
	}

	/**
	 * @see FlashAttributeResultMatchers.attributeExists
	 */
	fun attributeExists(vararg name: String) {
		actions.andExpect(matchers.attributeExists(*name))
	}

	/**
	 * @see FlashAttributeResultMatchers.attributeCount
	 */
	fun attributeCount(count: Int) {
		actions.andExpect(matchers.attributeCount(count))
	}
}
