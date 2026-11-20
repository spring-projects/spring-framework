/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scheduling.annotation

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.scheduling.annotation.ScheduledAnnotationReactiveSupport.getPublisherFor
import org.springframework.scheduling.annotation.ScheduledAnnotationReactiveSupport.isReactive
import org.springframework.util.ReflectionUtils
import reactor.core.publisher.Mono
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.Continuation

/**
 * @author Simon Baslé
 * @since 6.1
 */
class KotlinScheduledAnnotationReactiveSupportTests {

	private var target: SuspendingFunctions? = SuspendingFunctions()


	@Test
	fun ensureReactor() {
		assertThat(ScheduledAnnotationReactiveSupport.REACTOR_PRESENT).isTrue
	}

	@Test
	fun ensureKotlinCoroutineReactorBridge() {
		assertThat(ScheduledAnnotationReactiveSupport.COROUTINES_REACTOR_PRESENT).isTrue
	}

	@ParameterizedTest
	@ValueSource(strings = ["suspending", "suspendingReturns"])
	fun isReactiveSuspending(methodName: String) {
		val method = ReflectionUtils.findMethod(SuspendingFunctions::class.java, methodName, Continuation::class.java)!!
		assertThat(isReactive(method)).isTrue
	}

	@ParameterizedTest
	@ValueSource(strings = ["flow", "deferred"])
	fun isReactiveKotlinType(methodName: String) {
		val method = ReflectionUtils.findMethod(SuspendingFunctions::class.java, methodName)!!
		assertThat(isReactive(method)).isTrue
	}

	@Test
	fun isNotReactive() {
		val method = ReflectionUtils.findMethod(SuspendingFunctions::class.java, "notSuspending")!!
		assertThat(isReactive(method)).isFalse
	}

	@Test
	fun checkKotlinRuntimeIfNeeded() {
		val suspendingMethod = ReflectionUtils.findMethod(SuspendingFunctions::class.java, "suspending", Continuation::class.java)!!
		val notSuspendingMethod = ReflectionUtils.findMethod(SuspendingFunctions::class.java, "notSuspending")!!

		assertThat(isReactive(suspendingMethod)).describedAs("suspending").isTrue()
		assertThat(isReactive(notSuspendingMethod)).describedAs("not suspending").isFalse()
	}

	@Test
	fun isReactiveRejectsWithParams() {
		val m = ReflectionUtils.findMethod(SuspendingFunctions::class.java, "withParam", String::class.java, Continuation::class.java)!!

		//isReactive rejects with some context
		Assertions.assertThatIllegalArgumentException().isThrownBy { isReactive(m) }
				.withMessage("Kotlin suspending functions may only be annotated with @Scheduled if declared without arguments")
				.withNoCause()
	}

	@Test
	fun rejectNotSuspending() {
		val m = ReflectionUtils.findMethod(SuspendingFunctions::class.java, "notSuspending")

		//static helper method
		Assertions.assertThatIllegalArgumentException().isThrownBy { getPublisherFor(m!!, target!!) }
				.withMessage("Cannot convert @Scheduled reactive method return type to Publisher")
				.withNoCause()
	}

	@Test
	fun suspendingThrowIsTurnedToMonoError() {
		val m = ReflectionUtils.findMethod(SuspendingFunctions::class.java, "throwsIllegalState", Continuation::class.java)

		val mono = Mono.from(getPublisherFor(m!!, target!!))

		Assertions.assertThatIllegalStateException().isThrownBy { mono.block() }
				.withMessage("expected")
				.withNoCause()
	}

	@Test
	fun turningSuspendingFunctionToMonoDoesntExecuteTheMethod() {
		val m = ReflectionUtils.findMethod(SuspendingFunctions::class.java, "suspendingTracking", Continuation::class.java)
		val mono = Mono.from(getPublisherFor(m!!, target!!))

		assertThat(target!!.subscription).hasValue(0)
		mono.block()
		assertThat(target!!.subscription).describedAs("after subscription").hasValue(1)
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

		fun flow(): Flow<Void> {
			return flowOf()
		}

		fun deferred(): Deferred<Void> {
			return CompletableDeferred()
		}
	}

}
