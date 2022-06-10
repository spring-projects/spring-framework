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
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.ResultActions
import org.w3c.dom.Node
import javax.xml.transform.Source

/**
 * Provide a [ContentResultMatchers] Kotlin DSL in order to be able to write idiomatic Kotlin code.
 *
 * @author Sebastien Deleuze
 * @since 5.3
 */
class ContentResultMatchersDsl internal constructor (private val actions: ResultActions) {

	private val matchers = MockMvcResultMatchers.content()

	/**
	 * @see ContentResultMatchers.contentType
	 */
	fun contentType(contentType: String) {
		actions.andExpect(matchers.contentType(contentType))
	}

	/**
	 * @see ContentResultMatchers.contentType
	 */
	fun contentType(contentType: MediaType) {
		actions.andExpect(matchers.contentType(contentType))
	}

	/**
	 * @see ContentResultMatchers.contentTypeCompatibleWith
	 */
	fun contentTypeCompatibleWith(contentType: String) {
		actions.andExpect(matchers.contentTypeCompatibleWith(contentType))
	}

	/**
	 * @see ContentResultMatchers.contentTypeCompatibleWith
	 */
	fun contentTypeCompatibleWith(contentType: MediaType) {
		actions.andExpect(matchers.contentTypeCompatibleWith(contentType))
	}

	/**
	 * @see ContentResultMatchers.encoding
	 */
	fun encoding(contentType: String) {
		actions.andExpect(matchers.encoding(contentType))
	}

	/**
	 * @see ContentResultMatchers.string
	 */
	fun string(matcher: Matcher<String>) {
		actions.andExpect(matchers.string(matcher))
	}

	/**
	 * @see ContentResultMatchers.string
	 */
	fun string(expectedContent: String) {
		actions.andExpect(matchers.string(expectedContent))
	}

	/**
	 * @see ContentResultMatchers.bytes
	 */
	fun bytes(expectedContent: ByteArray) {
		actions.andExpect(matchers.bytes(expectedContent))
	}

	/**
	 * @see ContentResultMatchers.xml
	 */
	fun xml(xmlContent: String) {
		actions.andExpect(matchers.xml(xmlContent))
	}

	/**
	 * @see ContentResultMatchers.node
	 */
	fun node(matcher: Matcher<Node>) {
		actions.andExpect(matchers.node(matcher))
	}

	/**
	 * @see ContentResultMatchers.source
	 */
	fun source(matcher: Matcher<Source>) {
		actions.andExpect(matchers.source(matcher))
	}

	/**
	 * @see ContentResultMatchers.json
	 */
	fun json(jsonContent: String, strict: Boolean = false) {
		actions.andExpect(matchers.json(jsonContent, strict))
	}
}
