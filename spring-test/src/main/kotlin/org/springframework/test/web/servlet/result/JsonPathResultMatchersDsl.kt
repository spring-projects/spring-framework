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
	inline fun <reified T> value(matcher: Matcher<T>) {
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
