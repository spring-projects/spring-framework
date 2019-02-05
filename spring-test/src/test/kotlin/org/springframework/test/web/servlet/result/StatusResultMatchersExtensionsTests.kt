/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.web.servlet.result

import io.mockk.mockk
import io.mockk.verify
import org.hamcrest.Matcher
import org.junit.Test

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
