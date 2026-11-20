/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.support

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.delay
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.util.ReflectionUtils
import reactor.core.publisher.Mono
import kotlin.coroutines.Continuation

/**
 * Tests for Kotlin support in [AopUtils].
 *
 * @author Sebastien Deleuze
 */
class AopUtilsKotlinTests {

	@Test
	fun `Invoking suspending function should return Mono`() {
		val value = "foo"
		val method = ReflectionUtils.findMethod(WithoutInterface::class.java, "handle",
			String::class. java, Continuation::class.java)!!
		val continuation = Continuation<Any>(CoroutineName("test")) { }
		val result = AopUtils.invokeJoinpointUsingReflection(WithoutInterface(), method, arrayOf(value, continuation))
		assertThat(result).isInstanceOfSatisfying(Mono::class.java) {
			assertThat(it.block()).isEqualTo(value)
		}
	}

	@Test
	fun `Invoking suspending function on bridged method should return Mono`() {
		val value = "foo"
		val bridgedMethod = ReflectionUtils.findMethod(WithInterface::class.java, "handle", Any::class.java, Continuation::class.java)!!
		val continuation = Continuation<Any>(CoroutineName("test")) { }
		val result = AopUtils.invokeJoinpointUsingReflection(WithInterface(), bridgedMethod, arrayOf(value, continuation))
		assertThat(result).isInstanceOfSatisfying(Mono::class.java) {
			assertThat(it.block()).isEqualTo(value)
		}
	}

	@Suppress("unused")
	suspend fun suspendingFunction(value: String): String {
		delay(1)
		return value
	}

	class WithoutInterface {
		suspend fun handle(value: String): String {
			delay(1)
			return value
		}
	}

	interface ProxyInterface<T> {
		suspend fun handle(value: T): T
	}

	class WithInterface : ProxyInterface<String> {
		override suspend fun handle(value: String): String {
			delay(1)
			return value
		}
	}

}
