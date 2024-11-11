/*
 * Copyright 2002-2024 the original author or authors.
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
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import kotlin.coroutines.Continuation
import kotlin.coroutines.coroutineContext
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

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
	fun invokeSuspendingFunctionWithNullableParameter() {
		val method = CoroutinesUtilsTests::class.java.getDeclaredMethod("suspendingFunctionWithNullable", String::class.java, Continuation::class.java)
		val mono = CoroutinesUtils.invokeSuspendingFunction(method, this, null, null) as Mono
		runBlocking {
			Assertions.assertThat(mono.awaitSingleOrNull()).isNull()
		}
	}

	@Test
	fun invokePrivateSuspendingFunction() {
		val method = CoroutinesUtilsTests::class.java.getDeclaredMethod("privateSuspendingFunction", String::class.java, Continuation::class.java)
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
	fun invokeSuspendingFunctionWithMono() {
		val method = CoroutinesUtilsTests::class.java.getDeclaredMethod("suspendingFunctionWithMono", Continuation::class.java)
		val publisher = CoroutinesUtils.invokeSuspendingFunction(method, this)
		Assertions.assertThat(publisher).isInstanceOf(Mono::class.java)
		StepVerifier.create(publisher)
			.expectNext("foo")
			.expectComplete()
			.verify()
	}

	@Test
	fun invokeSuspendingFunctionWithFlux() {
		val method = CoroutinesUtilsTests::class.java.getDeclaredMethod("suspendingFunctionWithFlux", Continuation::class.java)
		val publisher = CoroutinesUtils.invokeSuspendingFunction(method, this)
		Assertions.assertThat(publisher).isInstanceOf(Flux::class.java)
		StepVerifier.create(publisher)
			.expectNext("foo")
			.expectNext("bar")
			.expectComplete()
			.verify()
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

	@Test
	fun invokeSuspendingFunctionReturningUnit() {
		val method = CoroutinesUtilsTests::class.java.getDeclaredMethod("suspendingUnit", Continuation::class.java)
		val mono = CoroutinesUtils.invokeSuspendingFunction(method, this) as Mono
		runBlocking {
			Assertions.assertThat(mono.awaitSingleOrNull()).isNull()
		}
	}

	@Test
	fun invokeSuspendingFunctionReturningNull() {
		val method = CoroutinesUtilsTests::class.java.getDeclaredMethod("suspendingNullable", Continuation::class.java)
		val mono = CoroutinesUtils.invokeSuspendingFunction(method, this) as Mono
		runBlocking {
			Assertions.assertThat(mono.awaitSingleOrNull()).isNull()
		}
	}

	@Test
	fun invokeSuspendingFunctionWithValueClassParameter() {
		val method = CoroutinesUtilsTests::class.java.declaredMethods.first { it.name.startsWith("suspendingFunctionWithValueClass") }
		val mono = CoroutinesUtils.invokeSuspendingFunction(method, this, "foo", null) as Mono
		runBlocking {
			Assertions.assertThat(mono.awaitSingle()).isEqualTo("foo")
		}
	}

	@Test
	fun invokeSuspendingFunctionWithValueClassReturnValue() {
		val method = CoroutinesUtilsTests::class.java.declaredMethods.first { it.name.startsWith("suspendingFunctionWithValueClassReturnValue") }
		val mono = CoroutinesUtils.invokeSuspendingFunction(method, this, null) as Mono
		runBlocking {
			Assertions.assertThat(mono.awaitSingle()).isEqualTo("foo")
		}
	}

	@Test
	fun invokeSuspendingFunctionWithValueClassWithInitParameter() {
		val method = CoroutinesUtilsTests::class.java.declaredMethods.first { it.name.startsWith("suspendingFunctionWithValueClassWithInit") }
		val mono = CoroutinesUtils.invokeSuspendingFunction(method, this, "", null) as Mono
		Assertions.assertThatIllegalArgumentException().isThrownBy {
			runBlocking {
				mono.awaitSingle()
			}
		}
	}

	@Test
	fun invokeSuspendingFunctionWithNullableValueClassParameter() {
		val method = CoroutinesUtilsTests::class.java.declaredMethods.first { it.name.startsWith("suspendingFunctionWithNullableValueClass") }
		val mono = CoroutinesUtils.invokeSuspendingFunction(method, this, null, null) as Mono
		runBlocking {
			Assertions.assertThat(mono.awaitSingleOrNull()).isNull()
		}
	}

	@Test
	fun invokeSuspendingFunctionWithValueClassWithPrivateConstructorParameter() {
		val method = CoroutinesUtilsTests::class.java.declaredMethods.first { it.name.startsWith("suspendingFunctionWithValueClassWithPrivateConstructor") }
		val mono = CoroutinesUtils.invokeSuspendingFunction(method, this, "foo", null) as Mono
		runBlocking {
			Assertions.assertThat(mono.awaitSingleOrNull()).isEqualTo("foo")
		}
	}

	@Test
	fun invokeSuspendingFunctionWithExtension() {
		val method = CoroutinesUtilsTests::class.java.getDeclaredMethod("suspendingFunctionWithExtension",
			CustomException::class.java, Continuation::class.java)
		val mono = CoroutinesUtils.invokeSuspendingFunction(method, this, CustomException("foo")) as Mono
		runBlocking {
			Assertions.assertThat(mono.awaitSingleOrNull()).isEqualTo("foo")
		}
	}

	@Test
	fun invokeSuspendingFunctionWithExtensionAndParameter() {
		val method = CoroutinesUtilsTests::class.java.getDeclaredMethod("suspendingFunctionWithExtensionAndParameter",
			CustomException::class.java, Int::class.java, Continuation::class.java)
		val mono = CoroutinesUtils.invokeSuspendingFunction(method, this, CustomException("foo"), 20) as Mono
		runBlocking {
			Assertions.assertThat(mono.awaitSingleOrNull()).isEqualTo("foo-20")
		}
	}

	@Test
	fun invokeSuspendingFunctionWithGenericParameter() {
		val method = GenericController::class.java.declaredMethods.first { it.name.startsWith("handle") }
		val horse = Animal("horse")
		val mono = CoroutinesUtils.invokeSuspendingFunction(method, AnimalController(), horse, null) as Mono
		runBlocking {
			Assertions.assertThat(mono.awaitSingle()).isEqualTo(horse.name)
		}
	}

	suspend fun suspendingFunction(value: String): String {
		delay(1)
		return value
	}

	private suspend fun privateSuspendingFunction(value: String): String {
		delay(1)
		return value
	}

	suspend fun suspendingFunctionWithNullable(value: String?): String? {
		delay(1)
		return value
	}

	suspend fun suspendingFunctionWithMono(): Mono<String> {
		delay(1)
		return Mono.just("foo")
	}

	suspend fun suspendingFunctionWithFlux(): Flux<String> {
		delay(1)
		return Flux.just("foo", "bar")
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

	suspend fun suspendingUnit() {
	}

	suspend fun suspendingNullable(): String? {
		return null
	}

	suspend fun suspendingFunctionWithValueClass(value: ValueClass): String {
		delay(1)
		return value.value
	}

	suspend fun suspendingFunctionWithValueClassReturnValue(): ValueClass {
		delay(1)
		return ValueClass("foo")
	}

	suspend fun suspendingFunctionWithValueClassWithInit(value: ValueClassWithInit): String {
		delay(1)
		return value.value
	}

	suspend fun suspendingFunctionWithNullableValueClass(value: ValueClass?): String? {
		delay(1)
		return value?.value
	}

	suspend fun suspendingFunctionWithValueClassWithPrivateConstructor(value: ValueClassWithPrivateConstructor): String? {
		delay(1)
		return value.value
	}

	suspend fun CustomException.suspendingFunctionWithExtension(): String {
		delay(1)
		return "${this.message}"
	}

	suspend fun CustomException.suspendingFunctionWithExtensionAndParameter(limit: Int): String {
		delay(1)
		return "${this.message}-$limit"
	}

	interface Named {
		val name: String
	}

	data class Animal(override val name: String) : Named

	abstract class GenericController<T : Named> {

		suspend fun handle(named: T): String {
			delay(1)
			return named.name;
		}
	}

	private class AnimalController : GenericController<Animal>()

	@JvmInline
	value class ValueClass(val value: String)

	@JvmInline
	value class ValueClassWithInit(val value: String) {
		 init {
		     if (value.isEmpty()) {
				 throw IllegalArgumentException()
			 }
		 }
	}

	@JvmInline
	value class ValueClassWithPrivateConstructor private constructor(val value: String) {
		companion object {
			fun from(value: String) = ValueClassWithPrivateConstructor(value)
		}
	}

	class CustomException(message: String) : Throwable(message)

}
