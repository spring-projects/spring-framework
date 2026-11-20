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
 * Tests for [SerializationHints] Kotlin extensions.
 *
 * @author Sebastien Deleuze
 */
@Suppress("REMOVAL", "DEPRECATION")
class SerializationHintsExtensionsTests {

	private val serializationHints = mockk<SerializationHints>()

	@Test
	fun `registerType extension with Consumer`() {
		every { serializationHints.registerType(any<Class<String>>(), any<Consumer<JavaSerializationHint.Builder>>()) } returns serializationHints
		serializationHints.registerType<String> { }
		verify { serializationHints.registerType(String::class.java, any<Consumer<JavaSerializationHint.Builder>>()) }
	}

	@Test
	fun `registerType extension`() {
		every { serializationHints.registerType(any<Class<String>>(), any<Consumer<JavaSerializationHint.Builder>>()) } returns serializationHints
		serializationHints.registerType<String>()
		verify { serializationHints.registerType(String::class.java, any<Consumer<JavaSerializationHint.Builder>>()) }
	}

}
