/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
