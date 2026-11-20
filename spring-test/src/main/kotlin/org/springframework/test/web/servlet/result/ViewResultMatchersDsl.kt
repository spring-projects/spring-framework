/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.result

import org.hamcrest.Matcher
import org.springframework.test.web.servlet.ResultActions

/**
 * Provide a [ViewResultMatchers] Kotlin DSL in order to be able to write idiomatic Kotlin code.
 *
 * @author Sebastien Deleuze
 * @since 5.3
 */
class ViewResultMatchersDsl internal constructor (private val actions: ResultActions) {

	private val matchers = MockMvcResultMatchers.view()

	/**
	 * @see ViewResultMatchers.name
	 */
	fun name(matcher: Matcher<String>) {
		actions.andExpect(matchers.name(matcher))
	}

	/**
	 * @see ViewResultMatchers.name
	 */
	fun name(expectedViewName: String) {
		actions.andExpect(matchers.name(expectedViewName))
	}

}
