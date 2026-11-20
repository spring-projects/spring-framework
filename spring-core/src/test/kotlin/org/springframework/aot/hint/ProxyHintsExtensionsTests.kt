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
 * Tests for [ProxyHints] Kotlin extensions.
 *
 * @author Sebastien Deleuze
 */
class ProxyHintsExtensionsTests {

	private val proxyHints = mockk<ProxyHints>()

	@Test
	fun `registerJdkProxy extension`() {
		every { proxyHints.registerJdkProxy(*anyVararg<Class<*>>()) } returns proxyHints
		proxyHints.registerJdkProxy(String::class, Int::class)
		verify { proxyHints.registerJdkProxy(String::class.java, Int::class.java) }
	}

}
