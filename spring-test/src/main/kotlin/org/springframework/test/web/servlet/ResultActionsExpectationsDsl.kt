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

import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.result.*

/**
 *
 * @author Clint Checketts
 * @author Petr Balat
 */
class ResultActionsExpectationsDsl(private val actions: ResultActions) {

    fun status(statusInit: StatusResultMatchers.() -> ResultMatcher) {
        val status = MockMvcResultMatchers.status().statusInit()
        actions.andExpect(status)
    }

    fun content(contentInit: ContentResultMatchers.() -> ResultMatcher) {
        val content = MockMvcResultMatchers.content().contentInit()
        actions.andExpect(content)
    }

    fun viewName(viewName: String) {
        val view = MockMvcResultMatchers.view().name(viewName)
        actions.andExpect(view)
    }

    fun model(modelInit: ModelResultMatchers.() -> ResultMatcher) {
        val model = MockMvcResultMatchers.model().modelInit()
        actions.andExpect(model)
    }

    fun <T> model(name: String, modelInit: T.() -> Unit) {
        actions.andExpect { mvcResult ->
            val model = mvcResult.modelAndView?.model?.get(name) as T?
            model?.modelInit() ?: throw AssertionError("Model attribute $name was not found")
        }
    }

    fun redirectedUrl(expectedUrl: String) {
        val header = MockMvcResultMatchers.redirectedUrl(expectedUrl)
        actions.andExpect(header)
    }

    fun redirectedUrlPattern(redirectedUrlPattern: String) {
        val header = MockMvcResultMatchers.redirectedUrl(redirectedUrlPattern)
        actions.andExpect(header)
    }

    fun header(headerInit: HeaderResultMatchers.() -> ResultMatcher) {
        val header = MockMvcResultMatchers.header().headerInit()
        actions.andExpect(header)
    }

    fun flash(flashInit: FlashAttributeResultMatchers.() -> ResultMatcher) {
        val flash = MockMvcResultMatchers.flash().flashInit()
        actions.andExpect(flash)
    }

    fun <T> jsonPath(expression:String, matcher : Matcher<T>) {
        val json = MockMvcResultMatchers.jsonPath(expression, matcher)
        actions.andExpect(json)
    }

    fun jsonPath(expression:String, vararg args:Any, block: JsonPathResultMatchers.() -> ResultMatcher) {
        val xpath = MockMvcResultMatchers.jsonPath(expression, args).block()
        actions.andExpect(xpath)
    }

    fun xpath(expression:String, vararg args:Any, xpathInit: XpathResultMatchers.() -> ResultMatcher) {
        val xpath = MockMvcResultMatchers.xpath(expression, args).xpathInit()
        actions.andExpect(xpath)
    }

    fun cookie(cookieInit: CookieResultMatchers.() -> ResultMatcher) {
        val cookie = MockMvcResultMatchers.cookie().cookieInit()
        actions.andExpect(cookie)
    }

    fun HttpStatus.isStatus() {
        status { `is`(this@isStatus.value()) }
    }

    fun contentString(value: String){
        content { string(value) }
    }

    infix fun String.jsonPath(block: JsonPathResultMatchers.() -> ResultMatcher) {
        actions.andExpect(MockMvcResultMatchers.jsonPath(this).block())
    }

    infix fun String.jsonPathIs(value: Any?) {
        actions.andExpect(MockMvcResultMatchers.jsonPath(this, CoreMatchers.`is`(value)))
    }

    infix fun <T> String.jsonPathMatcher(value: Matcher<T>) {
        actions.andExpect(MockMvcResultMatchers.jsonPath(this, value))
    }

    operator fun ResultMatcher.unaryPlus(){
        actions.andExpect(this)
    }

    fun json(jsonContent: String, strict: Boolean = false) {
        actions.andExpect(MockMvcResultMatchers.content().json(jsonContent, strict))
    }
}