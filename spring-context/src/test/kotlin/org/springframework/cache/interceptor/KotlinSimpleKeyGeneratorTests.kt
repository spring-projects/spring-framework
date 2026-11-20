/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.interceptor

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.util.ReflectionUtils
import kotlin.coroutines.Continuation

/**
 * Tests for [SimpleKeyGenerator] and [SimpleKey].
 *
 * @author Sebastien Deleuze
 */
class KotlinSimpleKeyGeneratorTests {

	private val generator = SimpleKeyGenerator()

	@Test
	fun ignoreContinuationArgumentWithNoParameter() {
		val method = ReflectionUtils.findMethod(javaClass, "suspendingMethod", Continuation::class.java)!!
		val continuation = mockk<Continuation<Any>>()
		val key = generator.generate(this, method, continuation)
		assertThat(key).isEqualTo(SimpleKey.EMPTY)
	}

	@Test
	fun ignoreContinuationArgumentWithOneParameter() {
		val method = ReflectionUtils.findMethod(javaClass, "suspendingMethod", String::class.java, Continuation::class.java)!!
		val continuation = mockk<Continuation<Any>>()
		val key = generator.generate(this, method, "arg", continuation)
		assertThat(key).isEqualTo("arg")
	}

	@Test
	fun ignoreContinuationArgumentWithMultipleParameters() {
		val method = ReflectionUtils.findMethod(javaClass, "suspendingMethod", String::class.java, String::class.java, Continuation::class.java)!!
		val continuation = mockk<Continuation<Any>>()
		val key = generator.generate(this, method, "arg1", "arg2", continuation)
		assertThat(key).isEqualTo(SimpleKey("arg1", "arg2"))
	}


	@Suppress("unused", "RedundantSuspendModifier")
	suspend fun suspendingMethod() {
	}

	@Suppress("unused", "UNUSED_PARAMETER", "RedundantSuspendModifier")
	suspend fun suspendingMethod(param: String) {
	}

	@Suppress("unused", "UNUSED_PARAMETER", "RedundantSuspendModifier")
	suspend fun suspendingMethod(param1: String, param2: String) {
	}

}
