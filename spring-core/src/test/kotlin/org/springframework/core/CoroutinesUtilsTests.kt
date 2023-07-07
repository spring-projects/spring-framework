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

package org.springframework.core

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.reactor.awaitSingle
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import kotlin.coroutines.Continuation
import kotlin.coroutines.coroutineContext

/**
 * Kotlin tests for [CoroutinesUtils].
 *
 * @author Sebastien Deleuze
 */
class CoroutinesUtilsTests {

	@Test
	fun deferredToMono() {
		runBlocking {
			val deferred: Deferred<String> = async(Dispatchers.IO) {
				delay(1)
				"foo"
			}
			val mono = CoroutinesUtils.deferredToMono(deferred)
			StepVerifier.create(mono)
				.expectNext("foo")
				.expectComplete()
				.verify()
		}
	}

	@Test
	fun monoToDeferred() {
		runBlocking {
			val mono = Mono.just("foo")
			val deferred = CoroutinesUtils.monoToDeferred(mono)
			Assertions.assertThat(deferred.await()).isEqualTo("foo")
		}
	}

	@Test
	fun invokeSuspendingFunctionWithNullContinuationParameter() {
		val method = CoroutinesUtilsTests::class.java.getDeclaredMethod("suspendingFunction", String::class.java, Continuation::class.java)
		val publisher = CoroutinesUtils.invokeSuspendingFunction(method, this, "foo", null)
		Assertions.assertThat(publisher).isInstanceOf(Mono::class.java)
		StepVerifier.create(publisher)
			.expectNext("foo")
			.expectComplete()
			.verify()
	}

	@Test
	fun invokeSuspendingFunctionWithoutContinuationParameter() {
		val method = CoroutinesUtilsTests::class.java.getDeclaredMethod("suspendingFunction", String::class.java, Continuation::class.java)
		val publisher = CoroutinesUtils.invokeSuspendingFunction(method, this, "foo")
		Assertions.assertThat(publisher).isInstanceOf(Mono::class.java)
		StepVerifier.create(publisher)
			.expectNext("foo")
			.expectComplete()
			.verify()
	}

	@Test
	fun invokeNonSuspendingFunction() {
		val method = CoroutinesUtilsTests::class.java.getDeclaredMethod("nonSuspendingFunction", String::class.java)
		Assertions.assertThatIllegalArgumentException().isThrownBy { CoroutinesUtils.invokeSuspendingFunction(method, this, "foo") }
	}

	@Test
	fun invokeSuspendingFunctionWithFlow() {
		val method = CoroutinesUtilsTests::class.java.getDeclaredMethod("suspendingFunctionWithFlow", Continuation::class.java)
		val publisher = CoroutinesUtils.invokeSuspendingFunction(method, this)
		Assertions.assertThat(publisher).isInstanceOf(Flux::class.java)
		StepVerifier.create(publisher)
			.expectNext("foo")
			.expectNext("bar")
			.expectComplete()
			.verify()
	}

	@Test
	fun invokeSuspendingFunctionWithNullContinuationParameterAndContext() {
		val method = CoroutinesUtilsTests::class.java.getDeclaredMethod("suspendingFunctionWithContext", String::class.java, Continuation::class.java)
		val context = CoroutineName("name")
		val mono = CoroutinesUtils.invokeSuspendingFunction(context, method, this, "foo", null) as Mono
		runBlocking {
			Assertions.assertThat(mono.awaitSingle()).isEqualTo("foo")
		}
	}

	@Test
	fun invokeSuspendingFunctionWithoutContinuationParameterAndContext() {
		val method = CoroutinesUtilsTests::class.java.getDeclaredMethod("suspendingFunctionWithContext", String::class.java, Continuation::class.java)
		val context = CoroutineName("name")
		val mono = CoroutinesUtils.invokeSuspendingFunction(context, method, this, "foo") as Mono
		runBlocking {
			Assertions.assertThat(mono.awaitSingle()).isEqualTo("foo")
		}
	}

	@Test
	fun invokeNonSuspendingFunctionWithContext() {
		val method = CoroutinesUtilsTests::class.java.getDeclaredMethod("nonSuspendingFunction", String::class.java)
		val context = CoroutineName("name")
		Assertions.assertThatIllegalArgumentException().isThrownBy { CoroutinesUtils.invokeSuspendingFunction(context, method, this, "foo") }
	}

	suspend fun suspendingFunction(value: String): String {
		delay(1)
		return value
	}

	suspend fun suspendingFunctionWithFlow(): Flow<String> {
		delay(1)
		return flowOf("foo", "bar")
	}

	fun nonSuspendingFunction(value: String): String {
		return value
	}

	suspend fun suspendingFunctionWithContext(value: String): String {
		delay(1)
		Assertions.assertThat(coroutineContext[CoroutineName]?.name).isEqualTo("name")
		return value
	}

}
