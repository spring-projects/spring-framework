/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.function.server

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono

class RenderingResponseExtensionsTests {

	@Test
	suspend fun buildAndAwait() {
		val builder = mockk<RenderingResponse.Builder>()
		val response = mockk<RenderingResponse>()
		every { builder.build() } returns Mono.just(response)
		builder.buildAndAwait()
		verify {
			builder.build()
		}
	}

}