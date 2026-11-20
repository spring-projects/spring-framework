/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
