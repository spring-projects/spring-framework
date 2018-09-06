/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.web.servlet

import org.springframework.test.web.servlet.result.*

/**
 *
 * @author Clint Checketts
 * @author Petr Balat
 */
fun ResultActions.expectStatus(statusInit: StatusResultMatchers.() -> ResultMatcher) {
	val status = MockMvcResultMatchers.status().statusInit()
	this.andExpect(status)
}

fun ResultActions.expectContent(contentInit: ContentResultMatchers.() -> ResultMatcher) {
	val content = MockMvcResultMatchers.content().contentInit()
	this.andExpect(content)
}

fun ResultActions.expectViewName(viewName: String): ResultActions {
	val view = MockMvcResultMatchers.view()
	return this.andExpect(view.name(viewName))
}

fun ResultActions.expectModel(modelInit: ModelResultMatchers.() -> ResultMatcher): ResultActions {
	val model = MockMvcResultMatchers.model().modelInit()
	return this.andExpect(model)
}

fun <T> ResultActions.expectModel(name: String, modelInit: T.() -> Unit): ResultActions {
	return this.andExpect { mvcResult ->
		val model = mvcResult.modelAndView?.model?.get(name) as T?
		model?.modelInit()
	}
}

fun ResultActions.expectRedirectedUrl(expectedUrl: String): ResultActions {
	val header = MockMvcResultMatchers.redirectedUrl(expectedUrl)
	return this.andExpect(header)
}

fun ResultActions.expectRedirectedUrlPattern(redirectedUrlPattern: String): ResultActions {
	val header = MockMvcResultMatchers.redirectedUrl(redirectedUrlPattern)
	return this.andExpect(header)
}

fun ResultActions.expectHeader(headerInit: HeaderResultMatchers.() -> ResultMatcher): ResultActions {
	val header = MockMvcResultMatchers.header().headerInit()
	return this.andExpect(header)
}

fun ResultActions.expectFlash(flashInit: FlashAttributeResultMatchers.() -> ResultMatcher): ResultActions {
	val flash = MockMvcResultMatchers.flash().flashInit()
	return this.andExpect(flash)
}

fun ResultActions.expectJsonPath(expression: String, vararg args: Any, jsonInit: JsonPathResultMatchers.() -> ResultMatcher): ResultActions {
	val json = MockMvcResultMatchers.jsonPath(expression, args).jsonInit()
	return this.andExpect(json)
}

fun ResultActions.expectXPath(expression: String, vararg args: Any, xpatInit: XpathResultMatchers.() -> ResultMatcher): ResultActions {
	val xpath = MockMvcResultMatchers.xpath(expression, args).xpatInit()
	return this.andExpect(xpath)
}

fun ResultActions.expectCookie(cookieInit: CookieResultMatchers.() -> ResultMatcher): ResultActions {
	val cookie = MockMvcResultMatchers.cookie().cookieInit()
	return this.andExpect(cookie)
}