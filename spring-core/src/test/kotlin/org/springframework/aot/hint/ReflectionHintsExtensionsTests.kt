/*
 * Copyright 2002-2023 the original author or authors.
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
import java.util.function.Consumer

/**
 * Tests for [ReflectionHints] Kotlin extensions.
 *
 * @author Sebastien Deleuze
 */
class ReflectionHintsExtensionsTests {

	private val reflectionHints = mockk<ReflectionHints>()

	@Test
	fun `getTypeHint extension`() {
		val typeHint = mockk<TypeHint>()
		every { reflectionHints.getTypeHint(any<Class<*>>()) } returns typeHint
		reflectionHints.getTypeHint<String>()
		verify { reflectionHints.getTypeHint(String::class.java) }
	}

	@Test
	fun `registerType extension with Consumer`() {
		every { reflectionHints.registerType(any<Class<String>>(), any<Consumer<TypeHint.Builder>>()) } returns reflectionHints
		reflectionHints.registerType<String> { }
		verify { reflectionHints.registerType(String::class.java, any<Consumer<TypeHint.Builder>>()) }
	}

	@Test
	fun `registerType extension with MemberCategory`() {
		val memberCategory1 = mockk<MemberCategory>()
		val memberCategory2 = mockk<MemberCategory>()
		every { reflectionHints.registerType(any<Class<String>>(), any(), any()) } returns reflectionHints
		reflectionHints.registerType<String>(memberCategory1, memberCategory2)
		verify { reflectionHints.registerType(String::class.java, memberCategory1, memberCategory2) }
	}

}
