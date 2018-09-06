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

import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.result.*


/**
 * **Main entry point for server-side Spring MVC test support.**
 *
 * ### Example
 *
 * ```
 * import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
 *
 * // ...
 *
 * WebApplicationContext wac = ...;
 *
 * MockMvc mockMvc = webAppContextSetup(wac).build();
 *
 * mockMvc.performGet("/form") {
 *     expect {
 *         status { isOk }
 *         content { mimeType("text/html") }
 *         forwardedUrl("/WEB-INF/layouts/main.jsp")
 *     }
 * }
 * ```
 *
 * @constructor Creates an `MockMvcRequestDsl`.
 * @author Clint Checketts
 * @author Petr Balat
 *
 * Dsl for working with [MockMVC]
 *
 * @since 5.1.0
 */
class MockMvcRequestDsl(private val requestBuilder: MockHttpServletRequestBuilder) {

    private val requestBuilders: MutableList<MockHttpServletRequestBuilder.() -> Unit> = mutableListOf()

    private val actions: MutableList<ResultActions.() -> Unit> = mutableListOf()

	/**
	 * Print {@link MvcResult} details to the "standard" output stream.
	 * @see [MockMvcResultHandlers.print]
	 */
    fun print() {
        actions { andDo(MockMvcResultHandlers.print()) }
    }

	/**
	 * Configure the [MockHttpServletRequestBuilder]
	 * @param block receiver block for configuring [MockHttpServletRequestBuilder]
	 */
    fun builder(block: MockHttpServletRequestBuilder.() -> Unit) {
        requestBuilders.add(block)
    }

	/**
	 * Configure the [ResultActionsExpectationsDsl]
	 * @param block receiver block for configuring request expectation via a [ResultActionsExpectationsDsl]
	 */
    fun expect(block: ResultActionsExpectationsDsl.() -> Unit) {
        this.actions { ResultActionsExpectationsDsl(this).apply(block) }
    }

	/**
	 * Allows adding addition post-request actions if the provided [builder] or [expect] blocks aren't sufficient
	 * @param block receiver block for configuring additional [ResultActions]
	 */
    fun actions(block: ResultActions.() -> Unit) {
        this.actions.add(block)
    }

    fun buildRequest(): RequestBuilder {
        requestBuilders.forEach { requestBuilder.apply(it) }
        return requestBuilder
    }

    fun applyResult(result: ResultActions): ResultActions {
        actions.forEach { result.apply(it) }
        return result
    }

    fun andDo(action: ResultHandler): MockMvcRequestDsl {
        actions { andDo(action)}
        return this
    }

    fun andExpect(action: ResultMatcher): MockMvcRequestDsl {
        actions { andExpect(action)}
        return this
    }

    fun withResult(block: MvcResult.() -> Unit) {
        actions { andReturn().apply(block) }
    }
}