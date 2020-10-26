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
