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

package org.springframework.aop.framework

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import kotlin.coroutines.Continuation

/**
 * Tests for [CoroutinesUtils].
 *
 * @author Sebastien Deleuze
 */
class CoroutinesUtilsTests {

	@Test
	fun awaitSingleNonNullValue() {
		val value = "foo"
		val continuation = Continuation<Any>(CoroutineName("test")) { }
		runBlocking {
			assertThat(CoroutinesUtils.awaitSingleOrNull(value, continuation)).isEqualTo(value)
		}
	}

	@Test
	fun awaitSingleNullValue() {
		val value = null
		val continuation = Continuation<Any>(CoroutineName("test")) { }
		runBlocking {
			assertThat(CoroutinesUtils.awaitSingleOrNull(value, continuation)).isNull()
		}
	}

	@Test
	fun awaitSingleMonoValue() {
		val value = "foo"
		val continuation = Continuation<Any>(CoroutineName("test")) { }
		runBlocking {
			assertThat(CoroutinesUtils.awaitSingleOrNull(Mono.just(value), continuation)).isEqualTo(value)
		}
	}

	@Test
	@Suppress("UNCHECKED_CAST")
	fun flow() {
		val value1 = "foo"
		val value2 = "bar"
		val values = Flux.just(value1, value2)
		val flow = CoroutinesUtils.asFlow(values) as Flow<String>
		runBlocking {
			assertThat(flow.toList()).containsExactly(value1, value2)
		}
	}

}
