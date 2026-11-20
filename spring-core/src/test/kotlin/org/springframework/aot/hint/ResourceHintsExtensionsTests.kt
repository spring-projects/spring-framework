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
 * Tests for [ResourceHints] Kotlin extensions.
 *
 * @author Sebastien Deleuze
 */
class ResourceHintsExtensionsTests {

	private val resourceHints = mockk<ResourceHints>()

	@Test
	fun `registerType extension`() {
		every { resourceHints.registerType(any<Class<String>>()) } returns resourceHints
		resourceHints.registerType<String>()
		verify { resourceHints.registerType(String::class.java) }
	}

}
