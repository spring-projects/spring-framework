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

import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder
import javax.servlet.http.Part

/**
 * Provide a [MockMultipartHttpServletRequestBuilder] Kotlin DSL in order to be able to write idiomatic Kotlin code.
 *
 * @see MockMvc.multipart
 * @author Sebastien Deleuze
 * @since 5.2
 */
class MockMultipartHttpServletRequestDsl(private val builder: MockMultipartHttpServletRequestBuilder) : MockHttpServletRequestDsl(builder) {

	/**
	 * @see [MockMultipartHttpServletRequestBuilder.file]
	 */
	fun file(name: String, content: ByteArray) = builder.file(name, content)

	/**
	 * @see [MockMultipartHttpServletRequestBuilder.file]
	 */
	fun file(file: MockMultipartFile) = builder.file(file)

	/**
	 * @see [MockMultipartHttpServletRequestBuilder.part]
	 */
	fun part(vararg parts: Part) = builder.part(*parts)
}
