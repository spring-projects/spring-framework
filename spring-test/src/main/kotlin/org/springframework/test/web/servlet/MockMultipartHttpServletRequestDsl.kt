/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet

import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder
import jakarta.servlet.http.Part

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
