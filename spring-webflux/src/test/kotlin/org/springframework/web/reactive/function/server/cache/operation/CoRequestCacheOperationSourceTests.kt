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

package org.springframework.web.reactive.function.server.cache.operation

import kotlinx.coroutines.delay
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.assertNotNull
import org.springframework.web.reactive.function.server.cache.CoRequestCacheable

private const val SAMPLE_CACHE_KEY = "sampleCacheKey"
private const val ANNOTATED_SUSPEND_METHOD_NAME = "annotatedSuspendMethod"
private const val ANNOTATED_METHOD_NAME = "annotatedMethod"
private const val NOT_ANNOTATED_SUSPEND_METHOD_NAME = "notAnnotatedSuspendMethod"

/**
 * Tests for [CoRequestCacheOperationSource].
 *
 * @author Angelo Bracaglia
 */
class CoRequestCacheOperationSourceTests {
	class SampleBean {
		@Suppress("Unused")
		@CoRequestCacheable(key = SAMPLE_CACHE_KEY)
		suspend fun annotatedSuspendMethod() {
			delay(10)
		}

		@Suppress("Unused")
		@CoRequestCacheable(key = SAMPLE_CACHE_KEY)
		fun annotatedMethod() {
		}

		@Suppress("Unused")
		suspend fun notAnnotatedSuspendMethod() {
			delay(10)
		}
	}

	private val underTest = CoRequestCacheOperationSource()

	@Test
	fun `should have CoRequestCacheableOperation when the given method is suspend and annotated by CoRequestCacheable`() {
		val target = SampleBean()
		val method = target::class.java.declaredMethods.first { it.name == ANNOTATED_SUSPEND_METHOD_NAME }

		assert(underTest.hasCacheOperations(method, SampleBean::class.java))

		val cacheOperations = underTest.getCacheOperations(method, SampleBean::class.java)
		assertNotNull(cacheOperations)
		assertThat(cacheOperations.size).isEqualTo(1)

		val coRequestCacheableOperation = assertInstanceOf<CoRequestCacheableOperation>(cacheOperations.first())
		assertThat(coRequestCacheableOperation.key).isEqualTo(SAMPLE_CACHE_KEY)
	}

	@Test
	fun `should not have CoRequestCacheableOperation when the given method is annotated by CoRequestCacheable but not suspend`() {
		val target = SampleBean()
		val method = target::class.java.declaredMethods.first { it.name == ANNOTATED_METHOD_NAME }

		assert(!underTest.hasCacheOperations(method, SampleBean::class.java))
	}

	@Test
	fun `should not have CoRequestCacheableOperation when the given method is suspend but not annotated by CoRequestCacheable`() {
		val target = SampleBean()
		val method = target::class.java.declaredMethods.first { it.name == NOT_ANNOTATED_SUSPEND_METHOD_NAME }

		assert(!underTest.hasCacheOperations(method, SampleBean::class.java))
	}
}
