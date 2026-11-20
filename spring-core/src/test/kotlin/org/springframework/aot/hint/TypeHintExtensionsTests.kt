/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.hint

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

/**
 * Tests for [TypeHint] Kotlin extensions.
 *
 * @author Sebastien Deleuze
 */
class TypeHintExtensionsTests {

	private val builder = mockk<TypeHint.Builder>()

	@Test
	fun `onReachableType builder extension`() {
		every { builder.onReachableType(any<Class<*>>()) } returns builder
		builder.onReachableType<String>()
		verify { builder.onReachableType(String::class.java) }
	}

}
