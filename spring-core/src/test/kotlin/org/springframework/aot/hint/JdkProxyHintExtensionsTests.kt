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
 * Tests for [JdkProxyHint] Kotlin extensions.
 *
 * @author Sebastien Deleuze
 */
class JdkProxyHintExtensionsTests {

	private val builder = mockk<JdkProxyHint.Builder>()

	@Test
	fun `proxiedInterfaces builder extension`() {
		every { builder.proxiedInterfaces(*anyVararg<Class<*>>()) } returns builder
		builder.proxiedInterfaces(String::class, Int::class)
		verify { builder.proxiedInterfaces(String::class.java, Int::class.java) }
	}

}
