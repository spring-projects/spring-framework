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

import org.springframework.http.HttpMethod
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import java.net.URI

/**
 *
 * @author Clint Checketts
 * @author Petr Balat
 */

/**
 * Create a [MockMvcRequestDsl] with the given HTTP verb.
 * @param method the HTTP method (GET, POST, etc)
 * @param urlTemplate a URL template; the resulting URL will be encoded
 * @param uriVars zero or more URI variables
 * @param block lambda for configuring [MockMvcRequestDsl]
 * @return the [MvcResult] (never `null`)
 * @since 5.1.0
 *
 */
fun MockMvc.perform(method: HttpMethod, urlTemplate:String, vararg uriVars: String,  block: MockMvcRequestDsl.() -> Unit = {}): MvcResult {
    return performDsl(this, MockMvcRequestBuilders.request(method, urlTemplate, *uriVars), block)
}

/**
 * Create a [MockMvcRequestDsl] with the given HTTP verb.
 * @param method the HTTP method (GET, POST, etc)
 * @param uri the URL
 * @param block lambda for configuring [MockMvcRequestDsl]
 * @return the [MvcResult] (never `null`)
 * @since 5.1.0
 */
fun MockMvc.perform(method: HttpMethod, uri: URI, block: MockMvcRequestDsl.() -> Unit = {}): MvcResult {
    return performDsl(this, MockMvcRequestBuilders.request(method, uri), block)
}

private fun performDsl(mockMvc: MockMvc,
					   requestBuilder: MockHttpServletRequestBuilder,
					   block: MockMvcRequestDsl.() -> Unit = {}): MvcResult {
    val request = MockMvcRequestDsl(requestBuilder).apply(block)
    val result = mockMvc.perform(request.buildRequest())
    return request.applyResult(result).andReturn()
}