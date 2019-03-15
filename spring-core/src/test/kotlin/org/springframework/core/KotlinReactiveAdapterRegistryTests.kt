/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono
import java.time.Duration
import kotlin.reflect.KClass

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

	private fun getAdapter(reactiveType: KClass<*>): ReactiveAdapter {
		return this.registry.getAdapter(reactiveType.java)!!
	}
}
