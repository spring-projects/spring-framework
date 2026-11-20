/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.service.invoker

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

/**
 * Tests for [HttpServiceProxyFactory] Kotlin extensions.
 *
 * @author Sebastien Deleuze
 */
class HttpServiceProxyFactoryExtensionsTests {

	private val proxyFactory = mockk<HttpServiceProxyFactory>()

	@Test
	fun `createClient extension`() {
		every { proxyFactory.createClient(String::class.java) } returns ""
		proxyFactory.createClient<String>()
		verify { proxyFactory.createClient(String::class.java) }
	}

}
