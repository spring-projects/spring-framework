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

package org.springframework.web.reactive.function.server.cache.interceptor

import io.mockk.every
import io.mockk.mockk
import org.aopalliance.intercept.MethodInvocation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import org.springframework.cache.interceptor.KeyGenerator
import org.springframework.web.reactive.function.server.cache.context.CoRequestCacheContext
import reactor.core.publisher.Mono
import java.lang.reflect.Method
import kotlin.coroutines.Continuation

/**
 * Tests for [CoRequestCacheInterceptor].
 *
 * @author Angelo Bracaglia
 */
class CoRequestCacheInterceptorTests {
	private val coRequestCacheContext = CoRequestCacheContext()
	private val continuation = mockk<Continuation<*>>()
	private val target = mockk<Any>()
	private val method = mockk<Method>()
	private val argumentsWithContinuation = arrayOf("firstArgument", continuation)
	private val keyGenerator = mockk<KeyGenerator>()
	private lateinit var invocation: MethodInvocation

	private val underTest = CoRequestCacheInterceptor(keyGenerator)

	@BeforeEach
	fun setup() {
		invocation = mockk<MethodInvocation>()
		every { invocation.`this` } returns target
		every { invocation.method } returns method
		every { invocation.arguments } returns argumentsWithContinuation
		every { continuation.context[CoRequestCacheContext] } returns coRequestCacheContext
		every { keyGenerator.generate(target, method, *argumentsWithContinuation) } returns "cacheKey"
	}

	@Test
	fun `should cache the result of the intercepted suspend function within the same coroutine context`() {
		every { invocation.proceed() } returns createExecutionsCounterMono()

		val firsInvocationResult = underTest.invoke(invocation)
		val secondInvocationResult = underTest.invoke(invocation)

		assertThat(firsInvocationResult).isSameAs(secondInvocationResult)

		val sharedMono = assertInstanceOf<Mono<Int>>(firsInvocationResult)

		repeat(3) {
			assertThat(sharedMono.block()).isEqualTo(1)
		}
	}

	@Test
	fun `should cache different results of the intercepted suspend function for different coroutine contexts`() {
		every { invocation.proceed() } returns createExecutionsCounterMono()

		val firstCoRequestCacheContext = CoRequestCacheContext()
		every { continuation.context[CoRequestCacheContext] } returns firstCoRequestCacheContext
		val firsInvocationResult = underTest.invoke(invocation)

		val secondCoRequestCacheContext = CoRequestCacheContext()
		every { continuation.context[CoRequestCacheContext] } returns secondCoRequestCacheContext
		val secondInvocationResult = underTest.invoke(invocation)

		assertThat(firsInvocationResult).isNotSameAs(secondInvocationResult)

		val firstSharedMono = assertInstanceOf<Mono<Int>>(firsInvocationResult)
		val secondSharedMono = assertInstanceOf<Mono<Int>>(secondInvocationResult)

		repeat(3) {
			assertThat(firstSharedMono.block()).isEqualTo(1)
			assertThat(secondSharedMono.block()).isEqualTo(2)
		}
	}

	@Test
	fun `should cache different results of the intercepted suspend function for different cache keys`() {
		every { invocation.proceed() } returns createExecutionsCounterMono()

		val firstInvocationCacheKey = "firstCacheKey"
		every { keyGenerator.generate(target, method, *argumentsWithContinuation) } returns firstInvocationCacheKey
		val firsInvocationResult = underTest.invoke(invocation)

		val secondInvocationCacheKey = "secondCacheKey"
		every { keyGenerator.generate(target, method, *argumentsWithContinuation) } returns secondInvocationCacheKey
		val secondInvocationResult = underTest.invoke(invocation)

		assertThat(firsInvocationResult).isNotSameAs(secondInvocationResult)

		val firstSharedMono = assertInstanceOf<Mono<Int>>(firsInvocationResult)
		val secondSharedMono = assertInstanceOf<Mono<Int>>(secondInvocationResult)

		repeat(3) {
			assertThat(firstSharedMono.block()).isEqualTo(1)
			assertThat(secondSharedMono.block()).isEqualTo(2)
		}
	}

	@Test
	fun `should skip caching of the intercepted function when no coroutine cache context is available`() {
		every { invocation.proceed() } returns createExecutionsCounterMono()
		every { continuation.context[CoRequestCacheContext] } returns null

		val mono = assertInstanceOf<Mono<Int>>(underTest.invoke(invocation))

		repeat(3) {
			val expectedExecutionsCount = it + 1
			assertThat(mono.block()).isEqualTo(expectedExecutionsCount)
		}
	}

	private fun createExecutionsCounterMono(): Mono<Int> {
		var executionsCount = 0
		return Mono.defer {
			executionsCount++
			Mono.just(executionsCount)
		}
	}
}
