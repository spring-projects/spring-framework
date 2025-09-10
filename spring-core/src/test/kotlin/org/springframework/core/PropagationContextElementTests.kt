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

package org.springframework.core

import io.micrometer.observation.Observation
import io.micrometer.observation.tck.TestObservationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.reactivestreams.Publisher
import reactor.core.publisher.Hooks
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import kotlin.coroutines.Continuation


/**
 * Kotlin tests for [PropagationContextElement].
 *
 * @author Brian Clozel
 */
class PropagationContextElementTests {

	private val observationRegistry = TestObservationRegistry.create()

	companion object {

		@BeforeAll
		@JvmStatic
		fun init() {
			Hooks.enableAutomaticContextPropagation()
		}

		@AfterAll
		@JvmStatic
		fun cleanup() {
			Hooks.disableAutomaticContextPropagation()
		}

	}

	@Test
	fun restoresFromThreadLocal() {
		val observation = Observation.createNotStarted("coroutine", observationRegistry)
		observation.observe {
			val result = runBlocking(Dispatchers.Unconfined) {
                suspendingFunction("test")
            }
			Assertions.assertThat(result).isEqualTo("coroutine")
		}
	}

	@Test
	@Suppress("UNCHECKED_CAST")
	fun restoresFromReactorContext() {
		val method = PropagationContextElementTests::class.java.getDeclaredMethod("suspendingFunction", String::class.java, Continuation::class.java)
		val publisher = CoroutinesUtils.invokeSuspendingFunction(method, this, "test", null) as Publisher<String>
		val observation = Observation.createNotStarted("coroutine", observationRegistry)
		observation.observe {
			val result = Mono.from<String>(publisher).publishOn(Schedulers.boundedElastic()).block()
			assertThat(result).isEqualTo("coroutine")
		}
	}

	suspend fun suspendingFunction(value: String): String? {
		return withContext(PropagationContextElement(currentCoroutineContext())) {
            val currentObservation = observationRegistry.currentObservation
            assertThat(currentObservation).isNotNull
            currentObservation?.context?.name
        }
	}

}
