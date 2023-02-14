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

package org.springframework.scheduling.annotation

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.scheduling.annotation.ScheduledAnnotationReactiveSupport.ReactiveTask
import org.springframework.scheduling.annotation.ScheduledAnnotationReactiveSupport.checkKotlinRuntimeIfNeeded
import org.springframework.scheduling.annotation.ScheduledAnnotationReactiveSupport.getPublisherForSuspendingFunction
import org.springframework.scheduling.annotation.ScheduledAnnotationReactiveSupportTests.ReactiveMethods
import org.springframework.util.ReflectionUtils
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.Continuation

class KotlinScheduledAnnotationReactiveSupportTests {

	@Test
	fun ensureReactor() {
		Assertions.assertThat(ScheduledAnnotationReactiveSupport.reactorPresent).isTrue
	}

	@Test
	fun ensureKotlinCoroutineReactorBridge() {
		Assertions.assertThat(ScheduledAnnotationReactiveSupport.coroutinesReactorPresent).isTrue
	}

	@Test
	fun kotlinSuspendingFunctionIsNotReactive() {
		val suspending = ReflectionUtils.findMethod(SuspendingFunctions::class.java, "suspending", Continuation::class.java)
		Assertions.assertThat(ScheduledAnnotationReactiveSupport.checkReactorRuntimeIfNeeded(suspending!!)).isFalse
	}

	internal class SuspendingFunctions {
		suspend fun suspending() {
		}

		suspend fun suspendingReturns(): String = "suspended"

		suspend fun withParam(param: String): String {
			return param
		}

		suspend fun throwsIllegalState() {
			throw IllegalStateException("expected")
		}

		var subscription = AtomicInteger()
		suspend fun suspendingTracking() {
			subscription.incrementAndGet()
		}

		fun notSuspending() { }
	}


	private var target: SuspendingFunctions? = null

	@BeforeEach
	fun init() {
		target = SuspendingFunctions()
	}

	@Test
	fun checkKotlinRuntimeIfNeeded() {
		val suspendingMethod = ReflectionUtils.findMethod(SuspendingFunctions::class.java, "suspending", Continuation::class.java)!!
		val notSuspendingMethod = ReflectionUtils.findMethod(SuspendingFunctions::class.java, "notSuspending")!!

		assertThat(checkKotlinRuntimeIfNeeded(suspendingMethod)).describedAs("suspending").isTrue()
		assertThat(checkKotlinRuntimeIfNeeded(notSuspendingMethod)).describedAs("not suspending").isFalse()
	}

	@Test
	fun rejectWithParams() {
		val m = ReflectionUtils.findMethod(SuspendingFunctions::class.java, "withParam", String::class.java, Continuation::class.java)

		//static helper method
		Assertions.assertThatIllegalArgumentException().isThrownBy { getPublisherForSuspendingFunction(m!!, target!!) }
				.withMessage("Kotlin suspending functions may only be annotated with @Scheduled if declared without arguments")
				.withNoCause()

		//constructor of task
		Assertions.assertThatIllegalArgumentException().isThrownBy {
			ReactiveTask(m!!, target!!, Duration.ZERO, Duration.ZERO, false, true)
		}
				.withMessage("Kotlin suspending functions may only be annotated with @Scheduled if declared without arguments")
				.withNoCause()
	}

	@Test
	fun rejectNotSuspending() {
		val m = ReflectionUtils.findMethod(SuspendingFunctions::class.java, "notSuspending")

		//static helper method
		Assertions.assertThatIllegalArgumentException().isThrownBy { getPublisherForSuspendingFunction(m!!, target!!) }
				.withMessage("Kotlin suspending functions may only be annotated with @Scheduled if declared without arguments")
				.withNoCause()

		//constructor of task
		Assertions.assertThatIllegalArgumentException().isThrownBy {
			ReactiveTask(m!!, target!!, Duration.ZERO, Duration.ZERO, false, true)
		}
				.withMessage("Kotlin suspending functions may only be annotated with @Scheduled if declared without arguments")
				.withNoCause()
	}

	@Test
	fun suspendingThrowIsTurnedToMonoError() {
		val m = ReflectionUtils.findMethod(SuspendingFunctions::class.java, "throwsIllegalState", Continuation::class.java)

		val mono = Mono.from(getPublisherForSuspendingFunction(m!!, target!!))

		Assertions.assertThatIllegalStateException().isThrownBy { mono.block() }
				.withMessage("expected")
				.withNoCause()

		//constructor of task doesn't throw
		Assertions.assertThatNoException().isThrownBy {
			ReactiveTask(m, target!!, Duration.ZERO, Duration.ZERO, false, true)
		}
	}

	@Test
	fun turningSuspendingFunctionToMonoDoesntExecuteTheMethod() {
		val m = ReflectionUtils.findMethod(SuspendingFunctions::class.java, "suspendingTracking", Continuation::class.java)
		val mono = Mono.from(getPublisherForSuspendingFunction(m!!, target!!))

		assertThat(target!!.subscription).hasValue(0)
		mono.block()
		assertThat(target!!.subscription).describedAs("after subscription").hasValue(1)
	}

	@Test
	fun hasCheckpointToString() {
		val m = ReflectionUtils.findMethod(SuspendingFunctions::class.java, "suspending", Continuation::class.java)
		val reactiveTask = ReactiveTask(m!!, target!!, Duration.ZERO, Duration.ZERO, false, true)
		assertThat(reactiveTask).hasToString("@Scheduled 'suspending()' in bean 'org.springframework.scheduling.annotation.KotlinScheduledAnnotationReactiveSupportTests\$SuspendingFunctions'")
	}

	@Test
	fun cancelledEarlyPreventsSubscription() {
		val m = ReflectionUtils.findMethod(SuspendingFunctions::class.java, "suspendingTracking", Continuation::class.java)
		val reactiveTask = ReactiveTask(m!!, target!!, Duration.ZERO, Duration.ofSeconds(10), false, true)
		reactiveTask.cancel()
		reactiveTask.subscribe()
		assertThat(target!!.subscription).hasValue(0)
	}

	@Test
	fun multipleSubscriptionsTracked() {
		val m = ReflectionUtils.findMethod(SuspendingFunctions::class.java, "suspendingTracking", Continuation::class.java)
		val reactiveTask = ReactiveTask(m!!, target!!, Duration.ZERO, Duration.ofMillis(500), false, true)
		reactiveTask.subscribe()
		Thread.sleep(1500)
		reactiveTask.cancel()
		assertThat(target!!.subscription).hasValueGreaterThanOrEqualTo(3)
	}

	@Test
	@Throws(InterruptedException::class)
	fun noInitialDelayFixedDelay() {
		val m = ReflectionUtils.findMethod(SuspendingFunctions::class.java, "suspendingTracking", Continuation::class.java)
		val reactiveTask = ReactiveTask(m!!, target!!, Duration.ZERO, Duration.ofSeconds(10), false, true)
		reactiveTask.subscribe()
		Thread.sleep(500)
		reactiveTask.cancel()
		assertThat(target!!.subscription).hasValue(1)
	}

	@Test
	@Throws(InterruptedException::class)
	fun noInitialDelayFixedRate() {
		val m = ReflectionUtils.findMethod(SuspendingFunctions::class.java, "suspendingTracking", Continuation::class.java)
		val reactiveTask = ReactiveTask(m!!, target!!, Duration.ZERO, Duration.ofSeconds(10), true, true)
		reactiveTask.subscribe()
		Thread.sleep(500)
		reactiveTask.cancel()
		assertThat(target!!.subscription).hasValue(1)
	}

	@Test
	@Throws(InterruptedException::class)
	fun initialDelayFixedDelay() {
		val m = ReflectionUtils.findMethod(SuspendingFunctions::class.java, "suspendingTracking", Continuation::class.java)
		val reactiveTask = ReactiveTask(m!!, target!!, Duration.ofSeconds(10), Duration.ofMillis(500), false, true)
		reactiveTask.subscribe()
		Thread.sleep(500)
		reactiveTask.cancel()
		assertThat(target!!.subscription).hasValue(0)
	}

	@Test
	@Throws(InterruptedException::class)
	fun initialDelayFixedRate() {
		val m = ReflectionUtils.findMethod(SuspendingFunctions::class.java, "suspendingTracking", Continuation::class.java)
		val reactiveTask = ReactiveTask(m!!, target!!, Duration.ofSeconds(10), Duration.ofMillis(500), true, true)
		reactiveTask.subscribe()
		Thread.sleep(500)
		reactiveTask.cancel()
		assertThat(target!!.subscription).hasValue(0)
	}
}