/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core

import io.micrometer.observation.Observation
import io.micrometer.observation.tck.TestObservationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.reactivestreams.Publisher
import reactor.core.publisher.Hooks
import reactor.core.publisher.Mono
import kotlin.coroutines.Continuation


/**
 * Kotlin tests for [PropagationContextElement].
 *
 * @author Brian Clozel
 * @author Sebastien Deleuze
 */
class PropagationContextElementTests {

	private val observationRegistry = TestObservationRegistry.create()

	@Test
	fun restoresFromThreadLocal() {
		val observation = Observation.createNotStarted("coroutine", observationRegistry)
		observation.observe {
			val coroutineContext = Dispatchers.IO + PropagationContextElement()
			val result = runBlocking(coroutineContext) {
                suspendingFunction("test")
            }
			Assertions.assertThat(result).isEqualTo("coroutine")
		}
	}

	@Test
	@Suppress("UNCHECKED_CAST")
	fun restoresFromReactorContext() {
		val method = PropagationContextElementTests::class.java.getDeclaredMethod("suspendingFunction", String::class.java, Continuation::class.java)
		val coroutineContext = Dispatchers.IO + PropagationContextElement()
		val publisher = CoroutinesUtils.invokeSuspendingFunction(coroutineContext, method, this, "test", null) as Publisher<String>
		val observation = Observation.createNotStarted("coroutine", observationRegistry)
		Hooks.enableAutomaticContextPropagation()
		observation.observe {
			val mono = Mono.from<String>(publisher)
			val result = mono.block()
			assertThat(result).isEqualTo("coroutine")
		}
		Hooks.disableAutomaticContextPropagation()
	}

	suspend fun suspendingFunction(value: String): String? {
		delay(1)
		val currentObservation = observationRegistry.currentObservation
		assertThat(currentObservation).isNotNull
		return currentObservation?.context?.name
	}

}
