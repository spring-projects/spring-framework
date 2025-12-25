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
import org.aopalliance.aop.Advice
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.server.cache.operation.CoRequestCacheOperationSource
import java.lang.reflect.Method

/**
 * Tests for [CoRequestCacheAdvisor].
 *
 * @author Angelo Bracaglia
 */
class CoRequestCacheAdvisorTests {
	private val target = mockk<Any>()
	private val method = mockk<Method>()
	private val coRequestCacheOperationSource = mockk<CoRequestCacheOperationSource>()

	private val underTest = CoRequestCacheAdvisor(coRequestCacheOperationSource, mockk<Advice>())

	@Test
	fun `should match when operation source has cache operations`() {
		every { coRequestCacheOperationSource.hasCacheOperations(method, target::class.java) } returns true
		assert(underTest.matches(method, target::class.java))
	}

	@Test
	fun `should not match when operation source does not have cache operations`() {
		every { coRequestCacheOperationSource.hasCacheOperations(method, target::class.java) } returns false
		assert(!underTest.matches(method, target::class.java))
	}
}
