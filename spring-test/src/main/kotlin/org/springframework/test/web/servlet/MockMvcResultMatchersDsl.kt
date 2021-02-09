/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.test.web.servlet

import org.hamcrest.Matcher
import org.springframework.test.web.servlet.result.*

/**
 * Provide a [MockMvcResultMatchers] Kotlin DSL in order to be able to write idiomatic Kotlin code.
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
class MockMvcResultMatchersDsl internal constructor (private val actions: ResultActions) {

	/**
	 * @see MockMvcResultMatchers.request
	 */
	fun request(dsl: RequestResultMatchersDsl.() -> Unit) {
		RequestResultMatchersDsl(actions).dsl()
	}

	/**
	 * @see MockMvcResultMatchers.view
	 */
	fun view(dsl: ViewResultMatchersDsl.() -> Unit) {
		ViewResultMatchersDsl(actions).dsl()
	}

	/**
	 * @see MockMvcResultMatchers.model
	 */
	fun model(dsl: ModelResultMatchersDsl.() -> Unit) {
		ModelResultMatchersDsl(actions).dsl()
	}

	/**
	 * @see MockMvcResultMatchers.flash
	 */
	fun flash(dsl: FlashAttributeResultMatchersDsl.() -> Unit) {
		FlashAttributeResultMatchersDsl(actions).dsl()
	}

	/**
	 * @see MockMvcResultMatchers.forwardedUrl
	 */
	fun forwardedUrl(expectedUrl: String?) {
		actions.andExpect(MockMvcResultMatchers.forwardedUrl(expectedUrl))
	}

	/**
	 * @see MockMvcResultMatchers.forwardedUrlTemplate
	 */
	fun forwardedUrlTemplate(urlTemplate: String, vararg uriVars: Any) {
		actions.andExpect(MockMvcResultMatchers.forwardedUrlTemplate(urlTemplate, *uriVars))
	}

	/**
	 * @see MockMvcResultMatchers.forwardedUrlPattern
	 */
	fun forwardedUrlPattern(urlPattern: String) {
		actions.andExpect(MockMvcResultMatchers.forwardedUrlPattern(urlPattern))
	}

	/**
	 * @see MockMvcResultMatchers.redirectedUrl
	 */
	fun redirectedUrl(expectedUrl: String) {
		actions.andExpect(MockMvcResultMatchers.redirectedUrl(expectedUrl))
	}

	/**
	 * @see MockMvcResultMatchers.redirectedUrlPattern
	 */
	fun redirectedUrlPattern(redirectedUrlPattern: String) {
		actions.andExpect(MockMvcResultMatchers.redirectedUrlPattern(redirectedUrlPattern))
	}

	/**
	 * @see MockMvcResultMatchers.status
	 */
	fun status(dsl: StatusResultMatchersDsl.() -> Unit) {
		StatusResultMatchersDsl(actions).dsl()
	}

	/**
	 * @see MockMvcResultMatchers.header
	 */
	fun header(dsl: HeaderResultMatchersDsl.() -> Unit) {
		HeaderResultMatchersDsl(actions).dsl()
	}

	/**
	 * @see MockMvcResultMatchers.content
	 */
	fun content(dsl: ContentResultMatchersDsl.() -> Unit) {
		ContentResultMatchersDsl(actions).dsl()
	}

	/**
	 * @see MockMvcResultMatchers.jsonPath
	 */
	fun <T> jsonPath(expression: String, matcher: Matcher<T>) {
		actions.andExpect(MockMvcResultMatchers.jsonPath(expression, matcher))
	}

	/**
	 * @see MockMvcResultMatchers.jsonPath
	 */
	fun jsonPath(expression: String, vararg args: Any?, dsl: JsonPathResultMatchersDsl.() -> Unit) {
		JsonPathResultMatchersDsl(actions, expression, *args).dsl()
	}

	/**
	 * @see MockMvcResultMatchers.xpath
	 */
	fun xpath(expression: String, vararg args: Any?, namespaces: Map<String, String>? = null, dsl: XpathResultMatchersDsl.() -> Unit) {
		XpathResultMatchersDsl(actions, expression, namespaces, *args).dsl()
	}

	/**
	 * @see MockMvcResultMatchers.cookie
	 */
	fun cookie(dsl: CookieResultMatchersDsl.() -> Unit) {
		CookieResultMatchersDsl(actions).dsl()
	}

	/**
	 * @see ResultActions.andExpect
	 */
	fun match(matcher: ResultMatcher) {
		actions.andExpect(matcher)
	}
}
