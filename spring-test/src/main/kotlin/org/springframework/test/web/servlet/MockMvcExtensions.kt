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

package org.springframework.test.web.servlet

import org.springframework.http.HttpMethod
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import java.net.URI

/**
 * [MockMvc] extension providing access to [MockHttpServletRequestDsl] Kotlin DSL.
 *
 * @see MockMvcRequestBuilders.get
 * @author Sebastien Deleuze
 * @since 5.2
 */
fun MockMvc.get(urlTemplate: String, vararg vars: Any?, dsl: MockHttpServletRequestDsl.() -> Unit = {}): ResultActionsDsl {
	val requestBuilder = MockMvcRequestBuilders.get(urlTemplate, *vars)
	return MockHttpServletRequestDsl(requestBuilder).apply(dsl).perform(this)
}

/**
 * [MockMvc] extension providing access to [MockHttpServletRequestDsl] Kotlin DSL.
 *
 * @see MockMvcRequestBuilders.get
 * @author Sebastien Deleuze
 * @since 5.2
 */
fun MockMvc.get(uri: URI, dsl: MockHttpServletRequestDsl.() -> Unit = {}): ResultActionsDsl {
	val requestBuilder = MockMvcRequestBuilders.get(uri)
	return MockHttpServletRequestDsl(requestBuilder).apply(dsl).perform(this)
}

/**
 * [MockMvc] extension providing access to [MockHttpServletRequestDsl] Kotlin DSL.
 *
 * @see MockMvcRequestBuilders.post
 * @author Sebastien Deleuze
 * @since 5.2
 */
fun MockMvc.post(urlTemplate: String, vararg vars: Any?, dsl: MockHttpServletRequestDsl.() -> Unit = {}): ResultActionsDsl {
	val requestBuilder = MockMvcRequestBuilders.post(urlTemplate, *vars)
	return MockHttpServletRequestDsl(requestBuilder).apply(dsl).perform(this)
}

/**
 * [MockMvc] extension providing access to [MockHttpServletRequestDsl] Kotlin DSL.
 *
 * @see MockMvcRequestBuilders.post
 * @author Sebastien Deleuze
 * @since 5.2
 */
fun MockMvc.post(uri: URI, dsl: MockHttpServletRequestDsl.() -> Unit = {}): ResultActionsDsl {
	val requestBuilder = MockMvcRequestBuilders.post(uri)
	return MockHttpServletRequestDsl(requestBuilder).apply(dsl).perform(this)
}

/**
 * [MockMvc] extension providing access to [MockHttpServletRequestDsl] Kotlin DSL.
 *
 * @see MockMvcRequestBuilders.put
 * @author Sebastien Deleuze
 * @since 5.2
 */
fun MockMvc.put(urlTemplate: String, vararg vars: Any?, dsl: MockHttpServletRequestDsl.() -> Unit = {}): ResultActionsDsl {
	val requestBuilder = MockMvcRequestBuilders.put(urlTemplate, *vars)
	return MockHttpServletRequestDsl(requestBuilder).apply(dsl).perform(this)
}

/**
 * [MockMvc] extension providing access to [MockHttpServletRequestDsl] Kotlin DSL.
 *
 * @see MockMvcRequestBuilders.put
 * @author Sebastien Deleuze
 * @since 5.2
 */
fun MockMvc.put(uri: URI, dsl: MockHttpServletRequestDsl.() -> Unit = {}): ResultActionsDsl {
	val requestBuilder = MockMvcRequestBuilders.put(uri)
	return MockHttpServletRequestDsl(requestBuilder).apply(dsl).perform(this)
}

/**
 * [MockMvc] extension providing access to [MockHttpServletRequestDsl] Kotlin DSL.
 *
 * @see MockMvcRequestBuilders.patch
 * @author Sebastien Deleuze
 * @since 5.2
 */
fun MockMvc.patch(urlTemplate: String, vararg vars: Any?, dsl: MockHttpServletRequestDsl.() -> Unit = {}): ResultActionsDsl {
	val requestBuilder = MockMvcRequestBuilders.patch(urlTemplate, *vars)
	return MockHttpServletRequestDsl(requestBuilder).apply(dsl).perform(this)
}

/**
 * [MockMvc] extension providing access to [MockHttpServletRequestDsl] Kotlin DSL.
 *
 * @see MockMvcRequestBuilders.patch
 * @author Sebastien Deleuze
 * @since 5.2
 */
fun MockMvc.patch(uri: URI, dsl: MockHttpServletRequestDsl.() -> Unit = {}): ResultActionsDsl {
	val requestBuilder = MockMvcRequestBuilders.patch(uri)
	return MockHttpServletRequestDsl(requestBuilder).apply(dsl).perform(this)
}

/**
 * [MockMvc] extension providing access to [MockHttpServletRequestDsl] Kotlin DSL.
 *
 * @see MockMvcRequestBuilders.delete
 * @author Sebastien Deleuze
 * @since 5.2
 */
fun MockMvc.delete(urlTemplate: String, vararg vars: Any?, dsl: MockHttpServletRequestDsl.() -> Unit = {}): ResultActionsDsl {
	val requestBuilder = MockMvcRequestBuilders.delete(urlTemplate, *vars)
	return MockHttpServletRequestDsl(requestBuilder).apply(dsl).perform(this)
}

/**
 * [MockMvc] extension providing access to [MockHttpServletRequestDsl] Kotlin DSL.
 *
 * @see MockMvcRequestBuilders.delete
 * @author Sebastien Deleuze
 * @since 5.2
 */
fun MockMvc.delete(uri: URI, dsl: MockHttpServletRequestDsl.() -> Unit = {}): ResultActionsDsl {
	val requestBuilder = MockMvcRequestBuilders.delete(uri)
	return MockHttpServletRequestDsl(requestBuilder).apply(dsl).perform(this)
}

/**
 * [MockMvc] extension providing access to [MockHttpServletRequestDsl] Kotlin DSL.
 *
 * @see MockMvcRequestBuilders.options
 * @author Sebastien Deleuze
 * @since 5.2
 */
fun MockMvc.options(urlTemplate: String, vararg vars: Any?, dsl: MockHttpServletRequestDsl.() -> Unit = {}): ResultActionsDsl {
	val requestBuilder = MockMvcRequestBuilders.options(urlTemplate, *vars)
	return MockHttpServletRequestDsl(requestBuilder).apply(dsl).perform(this)
}

/**
 * [MockMvc] extension providing access to [MockHttpServletRequestDsl] Kotlin DSL.
 *
 * @see MockMvcRequestBuilders.options
 * @author Sebastien Deleuze
 * @since 5.2
 */
fun MockMvc.options(uri: URI, dsl: MockHttpServletRequestDsl.() -> Unit = {}): ResultActionsDsl {
	val requestBuilder = MockMvcRequestBuilders.options(uri)
	return MockHttpServletRequestDsl(requestBuilder).apply(dsl).perform(this)
}

/**
 * [MockMvc] extension providing access to [MockHttpServletRequestDsl] Kotlin DSL.
 *
 * @see MockMvcRequestBuilders.head
 * @author Sebastien Deleuze
 * @since 5.2
 */
fun MockMvc.head(urlTemplate: String, vararg vars: Any?, dsl: MockHttpServletRequestDsl.() -> Unit = {}): ResultActionsDsl {
	val requestBuilder = MockMvcRequestBuilders.head(urlTemplate, *vars)
	return MockHttpServletRequestDsl(requestBuilder).apply(dsl).perform(this)
}

/**
 * [MockMvc] extension providing access to [MockHttpServletRequestDsl] Kotlin DSL.
 *
 * @see MockMvcRequestBuilders.head
 * @author Sebastien Deleuze
 * @since 5.2
 */
fun MockMvc.head(uri: URI, dsl: MockHttpServletRequestDsl.() -> Unit = {}): ResultActionsDsl {
	val requestBuilder = MockMvcRequestBuilders.head(uri)
	return MockHttpServletRequestDsl(requestBuilder).apply(dsl).perform(this)
}

/**
 * [MockMvc] extension providing access to [MockHttpServletRequestDsl] Kotlin DSL.
 *
 * @see MockMvcRequestBuilders.request
 * @author Sebastien Deleuze
 * @since 5.2
 */
fun MockMvc.request(method: HttpMethod, urlTemplate: String, vararg vars: Any?, dsl: MockHttpServletRequestDsl.() -> Unit = {}): ResultActionsDsl {
	val requestBuilder = MockMvcRequestBuilders.request(method, urlTemplate, *vars)
	return MockHttpServletRequestDsl(requestBuilder).apply(dsl).perform(this)
}

/**
 * [MockMvc] extension providing access to [MockHttpServletRequestDsl] Kotlin DSL.
 *
 * @see MockMvcRequestBuilders.request
 * @author Sebastien Deleuze
 * @since 5.2
 */
fun MockMvc.request(method: HttpMethod, uri: URI, dsl: MockHttpServletRequestDsl.() -> Unit = {}): ResultActionsDsl {
	val requestBuilder = MockMvcRequestBuilders.request(method, uri)
	return MockHttpServletRequestDsl(requestBuilder).apply(dsl).perform(this)
}

/**
 * [MockMvc] extension providing access to [MockMultipartHttpServletRequestDsl] Kotlin DSL.
 *
 * @see MockMvcRequestBuilders.multipart
 * @author Sebastien Deleuze
 * @since 5.2
 */
fun MockMvc.multipart(urlTemplate: String, vararg vars: Any?, dsl: MockMultipartHttpServletRequestDsl.() -> Unit = {}): ResultActionsDsl {
	val requestBuilder = MockMvcRequestBuilders.multipart(urlTemplate, *vars)
	return MockMultipartHttpServletRequestDsl(requestBuilder).apply(dsl).perform(this)
}

/**
 * [MockMvc] extension providing access to [MockMultipartHttpServletRequestDsl] Kotlin DSL.
 *
 * @see MockMvcRequestBuilders.multipart
 * @author Sebastien Deleuze
 * @since 5.3.26
 */
fun MockMvc.multipart(httpMethod: HttpMethod, urlTemplate: String, vararg vars: Any?, dsl: MockMultipartHttpServletRequestDsl.() -> Unit = {}): ResultActionsDsl {
	val requestBuilder = MockMvcRequestBuilders.multipart(httpMethod, urlTemplate, *vars)
	return MockMultipartHttpServletRequestDsl(requestBuilder).apply(dsl).perform(this)
}

/**
 * [MockMvc] extension providing access to [MockMultipartHttpServletRequestDsl] Kotlin DSL.
 *
 * @see MockMvcRequestBuilders.multipart
 * @author Sebastien Deleuze
 * @since 5.2
 */
fun MockMvc.multipart(uri: URI, dsl: MockMultipartHttpServletRequestDsl.() -> Unit = {}): ResultActionsDsl {
	val requestBuilder = MockMvcRequestBuilders.multipart(uri)
	return MockMultipartHttpServletRequestDsl(requestBuilder).apply(dsl).perform(this)
}

/**
 * [MockMvc] extension providing access to [MockMultipartHttpServletRequestDsl] Kotlin DSL.
 *
 * @see MockMvcRequestBuilders.multipart
 * @author Sebastien Deleuze
 * @since 5.3.26
 */
fun MockMvc.multipart(httpMethod: HttpMethod, uri: URI, dsl: MockMultipartHttpServletRequestDsl.() -> Unit = {}): ResultActionsDsl {
	val requestBuilder = MockMvcRequestBuilders.multipart(httpMethod, uri)
	return MockMultipartHttpServletRequestDsl(requestBuilder).apply(dsl).perform(this)
}
