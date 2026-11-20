/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.result

import io.mockk.mockk
import io.mockk.verify
import org.hamcrest.Matcher
import org.junit.jupiter.api.Test

class StatusResultMatchersExtensionsTests {

	val matchers = mockk<StatusResultMatchers>(relaxed = true)

	@Test
	fun `StatusResultMatchers#is with Matcher parameter is called as expected when using isEqualTo extension`() {
		val matcher = mockk<Matcher<Int>>()
		matchers.isEqualTo(matcher)
		verify { matchers.`is`(matcher) }
	}

	@Test
	fun `StatusResultMatchers#is with int parameter is called as expected when using isEqualTo extension`() {
		matchers.isEqualTo(200)
		verify { matchers.`is`(200) }
	}

}
