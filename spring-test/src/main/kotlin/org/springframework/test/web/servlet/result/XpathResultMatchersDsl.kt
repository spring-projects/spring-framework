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
import org.w3c.dom.Node
import org.w3c.dom.NodeList

/**
 * Provide a [XpathResultMatchers] Kotlin DSL in order to be able to write idiomatic Kotlin code.
 *
 * @author Sebastien Deleuze
 * @since 5.3
 */
class XpathResultMatchersDsl internal constructor (private val actions: ResultActions, expression: String, namespaces: Map<String, String>? = null, vararg args: Any?) {

	private val matchers = if (namespaces == null) MockMvcResultMatchers.xpath(expression, args) else MockMvcResultMatchers.xpath(expression, namespaces, args)

	/**
	 * @see XpathResultMatchers.node
	 */
	fun node(matcher: Matcher<Node>) {
		actions.andExpect(matchers.node(matcher))
	}

	/**
	 * @see XpathResultMatchers.nodeList
	 */
	fun nodeList(matcher: Matcher<NodeList>) {
		actions.andExpect(matchers.nodeList(matcher))
	}

	/**
	 * @see XpathResultMatchers.exists
	 */
	fun exists() {
		actions.andExpect(matchers.exists())
	}

	/**
	 * @see XpathResultMatchers.doesNotExist
	 */
	fun doesNotExist() {
		actions.andExpect(matchers.doesNotExist())
	}

	/**
	 * @see XpathResultMatchers.nodeCount
	 */
	fun nodeCount(matcher: Matcher<Int>) {
		actions.andExpect(matchers.nodeCount(matcher))
	}

	/**
	 * @see XpathResultMatchers.nodeCount
	 */
	fun nodeCount(expectedCount: Int) {
		actions.andExpect(matchers.nodeCount(expectedCount))
	}

	/**
	 * @see XpathResultMatchers.string
	 */
	fun string(matcher: Matcher<String>) {
		actions.andExpect(matchers.string(matcher))
	}

	/**
	 * @see XpathResultMatchers.string
	 */
	fun string(expectedValue: String) {
		actions.andExpect(matchers.string(expectedValue))
	}

	/**
	 * @see XpathResultMatchers.number
	 */
	fun number(matcher: Matcher<Double>) {
		actions.andExpect(matchers.number(matcher))
	}

	/**
	 * @see XpathResultMatchers.number
	 */
	fun number(expectedValue: Double) {
		actions.andExpect(matchers.number(expectedValue))
	}

	/**
	 * @see XpathResultMatchers.booleanValue
	 */
	fun booleanValue(value: Boolean) {
		actions.andExpect(matchers.booleanValue(value))
	}
}
