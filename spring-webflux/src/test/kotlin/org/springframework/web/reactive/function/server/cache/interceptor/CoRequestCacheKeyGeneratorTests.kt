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

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.delay
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.cache.interceptor.SimpleKey
import org.springframework.web.reactive.function.server.cache.operation.CoRequestCacheOperationSource
import org.springframework.web.reactive.function.server.cache.operation.CoRequestCacheableOperation
import java.lang.reflect.Method
import kotlin.coroutines.Continuation

private const val SAMPLE_METHOD_NAME = "sampleMethodName"

/**
 * Tests for [CoRequestCacheKeyGenerator].
 *
 * @author Angelo Bracaglia
 */
class CoRequestCacheKeyGeneratorTests {
	private val coRequestCacheOperationSource = mockk<CoRequestCacheOperationSource>()
	private val target = mockk<Any>()
	private val method = mockk<Method>()
	private val continuation = mockk<Continuation<*>>()
	private val underTest = CoRequestCacheKeyGenerator(coRequestCacheOperationSource)

	@BeforeEach
	fun setup() {
		every { method.name } returns SAMPLE_METHOD_NAME
	}

	@AfterEach
	fun teardown() {
		clearMocks(coRequestCacheOperationSource, target, method)
	}

	@Test
	fun `should throw an IllegalStateException when used for a not-suspend method`() {
		assertThrows<IllegalStateException> {
			underTest.generate(target, method, "notContinuationObject")
		}
	}

	@Test
	fun `should return a NullaryMethodKey when the only method parameter is a continuation object`() {
		val key = underTest.generate(target, method, continuation)

		val expectedKey = NullaryMethodKey(target::class.java, SAMPLE_METHOD_NAME)
		assertThat(key).isEqualTo(expectedKey)
	}

	@Test
	fun `should return a SimpleKey combining the NullaryMethodKey and all the arguments for empty key expression`() {
		val coRequestCacheableOperation = CoRequestCacheableOperation.Builder().apply { key = "" }.build()
		every { coRequestCacheOperationSource.getCacheOperations(method, target::class.java) } returns
				listOf(coRequestCacheableOperation)

		val firstParameterValue = "firstParameterValue"
		val secondParameterValue = 2
		val key = underTest.generate(target, method, firstParameterValue, secondParameterValue, continuation)

		val expectedKey = SimpleKey(
			NullaryMethodKey(target::class.java, SAMPLE_METHOD_NAME),
			firstParameterValue,
			secondParameterValue
		)
		assertThat(key).isEqualTo(expectedKey)
	}

	@Test
	fun `should return a SimpleKey combining the NullaryMethodKey and evaluated key expression`() {
		class SampleBean {
			@Suppress("Unused")
			suspend fun sampleMethod(firstParameter: String, secondParameter: Int) {
				delay(100)
			}
		}

		val sampleBeanInstance = SampleBean()
		val sampleMethod = SampleBean::class.java.declaredMethods.first()

		val coRequestCacheableOperation =
			CoRequestCacheableOperation.Builder().apply { key = "#firstParameter" }.build()
		every {
			coRequestCacheOperationSource.getCacheOperations(sampleMethod, sampleBeanInstance::class.java)
		} returns listOf(coRequestCacheableOperation)

		val firstParameterValue = "firstParameterValue"
		val secondParameterValue = 2

		val key = underTest.generate(
			sampleBeanInstance,
			sampleMethod,
			firstParameterValue, secondParameterValue,
			continuation
		)

		val expectedKey = SimpleKey(
			NullaryMethodKey(SampleBean::class.java, sampleMethod.name),
			firstParameterValue
		)
		assertThat(key).isEqualTo(expectedKey)
	}
}
