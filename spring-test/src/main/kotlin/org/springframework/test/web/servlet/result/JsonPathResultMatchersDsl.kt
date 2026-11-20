/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.result

import org.hamcrest.Matcher
import org.springframework.test.web.servlet.ResultActions

/**
 * Provide a [JsonPathResultMatchers] Kotlin DSL in order to be able to write idiomatic Kotlin code.
 *
 * @author Sebastien Deleuze
 * @since 5.3
 */
@Suppress("UsePropertyAccessSyntax")
class JsonPathResultMatchersDsl internal constructor(@PublishedApi internal val actions: ResultActions, expression: String, vararg args: Any?) {

	@PublishedApi
	internal val matchers = MockMvcResultMatchers.jsonPath(expression, args)

	/**
	 * @see JsonPathResultMatchers.prefix
	 */
	fun prefix(prefix: String) {
		matchers.prefix(prefix)
	}

	/**
	 * @see JsonPathResultMatchers.value
	 */
	inline fun <reified T : Any> value(matcher: Matcher<T>) {
		actions.andExpect(matchers.value(matcher, T::class.java))
	}

	/**
	 * @see JsonPathResultMatchers.value
	 */
	fun value(expectedValue: Any?) {
		actions.andExpect(matchers.value(expectedValue))
	}

	/**
	 * @see JsonPathResultMatchers.exists
	 */
	fun exists() {
		actions.andExpect(matchers.exists())
	}

	/**
	 * @see JsonPathResultMatchers.doesNotExist
	 */
	fun doesNotExist() {
		actions.andExpect(matchers.doesNotExist())
	}

	/**
	 * @see JsonPathResultMatchers.isEmpty
	 */
	fun isEmpty() {
		actions.andExpect(matchers.isEmpty())
	}

	/**
	 * @see JsonPathResultMatchers.isNotEmpty
	 */
	fun isNotEmpty() {
		actions.andExpect(matchers.isNotEmpty())
	}

	/**
	 * @see JsonPathResultMatchers.hasJsonPath
	 */
	fun hasJsonPath() {
		actions.andExpect(matchers.hasJsonPath())
	}

	/**
	 * @see JsonPathResultMatchers.doesNotHaveJsonPath
	 */
	fun doesNotHaveJsonPath() {
		actions.andExpect(matchers.doesNotHaveJsonPath())
	}

	/**
	 * @see JsonPathResultMatchers.isString
	 */
	fun isString() {
		actions.andExpect(matchers.isString())
	}

	/**
	 * @see JsonPathResultMatchers.isBoolean
	 */
	fun isBoolean() {
		actions.andExpect(matchers.isBoolean())
	}

	/**
	 * @see JsonPathResultMatchers.isNumber
	 */
	fun isNumber() {
		actions.andExpect(matchers.isNumber())
	}

	/**
	 * @see JsonPathResultMatchers.isArray
	 */
	fun isArray() {
		actions.andExpect(matchers.isArray())
	}

	/**
	 * @see JsonPathResultMatchers.isMap
	 */
	fun isMap() {
		actions.andExpect(matchers.isMap())
	}
}
