/*
 * Copyright 2002-2019 the original author or authors.
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

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Duration
import kotlin.reflect.KClass

@ExperimentalCoroutinesApi
class KotlinReactiveAdapterRegistryTests {

	private val registry = ReactiveAdapterRegistry.getSharedInstance()

	@Test
	fun deferredToPublisher() {
		val source = GlobalScope.async { 1 }
		val target: Publisher<Int> = getAdapter(Deferred::class).toPublisher(source)
		assertTrue("Expected Mono Publisher: " + target.javaClass.name, target is Mono<*>)
		assertEquals(1, (target as Mono<Int>).block(Duration.ofMillis(1000)))
	}

	@Test
	fun publisherToDeferred() {
		val source = Mono.just(1)
		val target = getAdapter(Deferred::class).fromPublisher(source)
		assertTrue(target is Deferred<*>)
		assertEquals(1, runBlocking { (target as Deferred<*>).await() })

	}

	@Test
	fun flowToPublisher() {
		val source = flow {
			emit(1)
			emit(2)
			emit(3)
		}
		val target: Publisher<Int> = getAdapter(Flow::class).toPublisher(source)
		assertTrue("Expected Flux Publisher: " + target.javaClass.name, target is Flux<*>)
		StepVerifier.create(target)
				.expectNext(1)
				.expectNext(2)
				.expectNext(3)
				.verifyComplete()
	}

	@Test
	fun publisherToFlow() {
		val source = Flux.just(1, 2, 3)
		val target = getAdapter(Flow::class).fromPublisher(source)
		if (target is Flow<*>) {
			assertEquals(listOf(1, 2, 3),  runBlocking { target.toList() })
		}
		else {
			fail()
		}
	}

	private fun getAdapter(reactiveType: KClass<*>): ReactiveAdapter {
		return this.registry.getAdapter(reactiveType.java)!!
	}
}
