/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core

import io.micrometer.observation.Observation
import io.micrometer.observation.tck.TestObservationRegistry
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Duration
import kotlin.reflect.KClass

/**
 * Kotlin tests for [ReactiveAdapterRegistry].
 *
 * @author Sebastien Deleuze
 */
@OptIn(DelicateCoroutinesApi::class)
class ReactiveAdapterRegistryKotlinTests {

	private val observationRegistry = TestObservationRegistry.create()

	private val registry = ReactiveAdapterRegistry.getSharedInstance()

	@Test
	fun deferredToPublisher() {
		val source = GlobalScope.async { 1 }
		val target: Publisher<Int> = getAdapter(Deferred::class).toPublisher(source)
		assertThat(target).isInstanceOf(Mono::class.java)
		assertThat((target as Mono<Int>).block(Duration.ofMillis(1000))).isEqualTo(1)
	}

	@Test
	suspend fun publisherToDeferred() {
		val source = Mono.just(1)
		val target = getAdapter(Deferred::class).fromPublisher(source)
		assertThat(target).isInstanceOf(Deferred::class.java)
		assertThat((target as Deferred<*>).await()).isEqualTo(1)
	}

	@Test
	fun flowToPublisher() {
		val source = flow {
			emit(1)
			emit(2)
			emit(3)
		}
		val target: Publisher<Int> = getAdapter(Flow::class).toPublisher(source)
		assertThat(target).isInstanceOf(Flux::class.java)
		StepVerifier.create(target)
				.expectNext(1)
				.expectNext(2)
				.expectNext(3)
				.verifyComplete()
	}

	@Test
	suspend fun publisherToFlow() {
		val source = Flux.just(1, 2, 3)
		val target = getAdapter(Flow::class).fromPublisher(source)
		assertThat(target).isInstanceOf(Flow::class.java)
		assertThat((target as Flow<*>).toList()).contains(1, 2, 3)
	}

	@Test
	fun propagateMicrometerContextToFlow() {
		val source = flow {
			val currentObservation = observationRegistry.currentObservation
			assertThat(currentObservation).isNotNull
			emit(currentObservation?.context?.name)
		}
		val observation = Observation.createNotStarted("coroutine", observationRegistry)
		observation.observe {
			val target: Publisher<String> = getAdapter(Flow::class).toPublisher(source)
			val result = runBlocking(Dispatchers.IO) {
				target.awaitSingle()
			}
			assertThat(result).isEqualTo("coroutine")
		}
	}

	private fun getAdapter(reactiveType: KClass<*>): ReactiveAdapter {
		return this.registry.getAdapter(reactiveType.java)!!
	}
}
