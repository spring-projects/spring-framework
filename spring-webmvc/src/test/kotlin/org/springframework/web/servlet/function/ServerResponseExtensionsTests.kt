/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.function

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.ParameterizedTypeReference

/**
 * Tests for WebMvc.fn [ServerResponse] extensions.
 *
 * @author Sebastien Deleuze
 */
class ServerResponseExtensionsTests {

	@Test
	fun bodyWithType() {
		val builder = mockk<ServerResponse.BodyBuilder>()
		val response = mockk<ServerResponse>()
		val body = listOf("foo", "bar")
		val typeReference = object: ParameterizedTypeReference<List<String>>() {}
		every { builder.body(body, typeReference) } returns response
		assertThat(builder.bodyWithType<List<String>>(body)).isEqualTo(response)
		verify { builder.body(body, typeReference) }
	}
}
