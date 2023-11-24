/*
 * Copyright 2002-2023 the original author or authors.
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
 * Provide a [CookieResultMatchers] Kotlin DSL in order to be able to write idiomatic Kotlin code.
 *
 * @author Sebastien Deleuze
 * @since 5.3
 */
class CookieResultMatchersDsl internal constructor (private val actions: ResultActions) {

	private val matchers = MockMvcResultMatchers.cookie()

	/**
	 * @see CookieResultMatchers.value
	 */
	fun value(name: String, matcher: Matcher<String>) {
		actions.andExpect(matchers.value(name, matcher))
	}

	/**
	 * @see CookieResultMatchers.value
	 */
	fun value(name: String, expectedValue: String) {
		actions.andExpect(matchers.value(name, expectedValue))
	}

	/**
	 * @see CookieResultMatchers.exists
	 */
	fun exists(name: String) {
		actions.andExpect(matchers.exists(name))
	}

	/**
	 * @see CookieResultMatchers.doesNotExist
	 */
	fun doesNotExist(name: String) {
		actions.andExpect(matchers.doesNotExist(name))
	}

	/**
	 * @see CookieResultMatchers.maxAge
	 */
	fun maxAge(name: String, matcher: Matcher<Int>) {
		actions.andExpect(matchers.maxAge(name, matcher))
	}

	/**
	 * @see CookieResultMatchers.maxAge
	 */
	fun maxAge(name: String, maxAge: Int) {
		actions.andExpect(matchers.maxAge(name, maxAge))
	}

	/**
	 * @see CookieResultMatchers.path
	 */
	fun path(name: String, matcher: Matcher<String>) {
		actions.andExpect(matchers.path(name, matcher))
	}

	/**
	 * @see CookieResultMatchers.path
	 */
	fun path(name: String, path: String) {
		actions.andExpect(matchers.path(name, path))
	}

	/**
	 * @see CookieResultMatchers.domain
	 */
	fun domain(name: String, matcher: Matcher<String>) {
		actions.andExpect(matchers.domain(name, matcher))
	}

	/**
	 * @see CookieResultMatchers.domain
	 */
	fun domain(name: String, domain: String) {
		actions.andExpect(matchers.domain(name, domain))
	}

	/**
	 * @see CookieResultMatchers.sameSite
	 * @since 6.0.8
	 */
	fun sameSite(name: String, matcher: Matcher<String>) {
		actions.andExpect(matchers.sameSite(name, matcher))
	}

	/**
	 * @see CookieResultMatchers.sameSite
	 * @since 6.0.8
	 */
	fun sameSite(name: String, sameSite: String) {
		actions.andExpect(matchers.sameSite(name, sameSite))
	}

	/**
	 * @see CookieResultMatchers.comment
	 */
	fun comment(name: String, matcher: Matcher<String>) {
		actions.andExpect(matchers.comment(name, matcher))
	}

	/**
	 * @see CookieResultMatchers.comment
	 */
	fun comment(name: String, comment: String) {
		actions.andExpect(matchers.comment(name, comment))
	}

	/**
	 * @see CookieResultMatchers.version
	 */
	fun version(name: String, matcher: Matcher<Int>) {
		actions.andExpect(matchers.version(name, matcher))
	}

	/**
	 * @see CookieResultMatchers.version
	 */
	fun version(name: String, version: Int) {
		actions.andExpect(matchers.version(name, version))
	}

	/**
	 * @see CookieResultMatchers.secure
	 */
	fun secure(name: String, secure: Boolean) {
		actions.andExpect(matchers.secure(name, secure))
	}

	/**
	 * @see CookieResultMatchers.httpOnly
	 */
	fun httpOnly(name: String, httpOnly: Boolean) {
		actions.andExpect(matchers.httpOnly(name, httpOnly))
	}

	/**
	 * @see CookieResultMatchers.attribute
	 * @since 6.0.8
	 */
	fun attribute(name: String, attributeName: String, matcher: Matcher<String>) {
		actions.andExpect(matchers.attribute(name, attributeName, matcher))
	}

	/**
	 * @see CookieResultMatchers.attribute
	 * @since 6.0.8
	 */
	fun attribute(name: String, attributeName: String, attributeValue: String) {
		actions.andExpect(matchers.attribute(name, attributeName, attributeValue))
	}
}
