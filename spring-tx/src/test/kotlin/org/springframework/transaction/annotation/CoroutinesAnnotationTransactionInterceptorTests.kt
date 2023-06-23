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

package org.springframework.transaction.annotation

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.aop.framework.ProxyFactory
import org.springframework.transaction.interceptor.TransactionInterceptor
import org.springframework.transaction.testfixture.ReactiveCallCountingTransactionManager
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

/**
 * @author Sebastien Deleuze
 */
class CoroutinesAnnotationTransactionInterceptorTests {

	private val rtm = ReactiveCallCountingTransactionManager()

	private val source = AnnotationTransactionAttributeSource()

	@Test
	fun suspendingNoValueSuccess() {
		val proxyFactory = ProxyFactory()
		proxyFactory.setTarget(TestWithCoroutines())
		proxyFactory.addAdvice(TransactionInterceptor(rtm, source))
		val proxy = proxyFactory.proxy as TestWithCoroutines
		runBlocking {
			proxy.suspendingNoValueSuccess()
		}
		assertReactiveGetTransactionAndCommitCount(1)
	}

	@Test
	fun suspendingNoValueFailure() {
		val proxyFactory = ProxyFactory()
		proxyFactory.setTarget(TestWithCoroutines())
		proxyFactory.addAdvice(TransactionInterceptor(rtm, source))
		val proxy = proxyFactory.proxy as TestWithCoroutines
		assertThatIllegalStateException().isThrownBy {
			runBlocking {
				proxy.suspendingNoValueFailure()
			}
		}
		assertReactiveGetTransactionAndRollbackCount(1)
	}

	@Test
	fun suspendingValueSuccess() {
		val proxyFactory = ProxyFactory()
		proxyFactory.setTarget(TestWithCoroutines())
		proxyFactory.addAdvice(TransactionInterceptor(rtm, source))
		val proxy = proxyFactory.proxy as TestWithCoroutines
		runBlocking {
			assertThat(proxy.suspendingValueSuccess()).isEqualTo("foo")
		}
		assertReactiveGetTransactionAndCommitCount(1)
	}

	@Test
	fun suspendingValueFailure() {
		val proxyFactory = ProxyFactory()
		proxyFactory.setTarget(TestWithCoroutines())
		proxyFactory.addAdvice(TransactionInterceptor(rtm, source))
		val proxy = proxyFactory.proxy as TestWithCoroutines
		assertThatIllegalStateException().isThrownBy {
			runBlocking {
				proxy.suspendingValueFailure()
			}
		}
		assertReactiveGetTransactionAndRollbackCount(1)
	}

	@Test
	fun suspendingFlowSuccess() {
		val proxyFactory = ProxyFactory()
		proxyFactory.setTarget(TestWithCoroutines())
		proxyFactory.addAdvice(TransactionInterceptor(rtm, source))
		val proxy = proxyFactory.proxy as TestWithCoroutines
		runBlocking {
			assertThat(proxy.suspendingFlowSuccess().toList()).containsExactly("foo", "foo")
		}
		assertReactiveGetTransactionAndCommitCount(1)
	}

	@Test
	fun flowSuccess() {
		val proxyFactory = ProxyFactory()
		proxyFactory.setTarget(TestWithCoroutines())
		proxyFactory.addAdvice(TransactionInterceptor(rtm, source))
		val proxy = proxyFactory.proxy as TestWithCoroutines
		runBlocking {
			assertThat(proxy.flowSuccess().toList()).containsExactly("foo", "foo")
		}
		assertReactiveGetTransactionAndCommitCount(1)
	}

	@Test
	fun suspendingValueSuccessWithContext() {
		val proxyFactory = ProxyFactory()
		proxyFactory.setTarget(TestWithCoroutines())
		proxyFactory.addAdvice(TransactionInterceptor(rtm, source))
		val proxy = proxyFactory.proxy as TestWithCoroutines
		assertThat(runBlocking {
			withExampleContext("context") {
				proxy.suspendingValueSuccessWithContext()
			}
		}).isEqualTo("context")
		assertReactiveGetTransactionAndCommitCount(1)
	}

	@Test
	fun suspendingValueFailureWithContext() {
		val proxyFactory = ProxyFactory()
		proxyFactory.setTarget(TestWithCoroutines())
		proxyFactory.addAdvice(TransactionInterceptor(rtm, source))
		val proxy = proxyFactory.proxy as TestWithCoroutines
		assertThatIllegalStateException().isThrownBy {
			runBlocking {
				withExampleContext("context") {
					proxy.suspendingValueFailureWithContext()
				}
			}
		}.withMessage("context")
		assertReactiveGetTransactionAndRollbackCount(1)
	}

	private fun assertReactiveGetTransactionAndCommitCount(expectedCount: Int) {
		assertThat(rtm.begun).isEqualTo(expectedCount)
		assertThat(rtm.commits).isEqualTo(expectedCount)
	}

	private fun assertReactiveGetTransactionAndRollbackCount(expectedCount: Int) {
		assertThat(rtm.begun).isEqualTo(expectedCount)
		assertThat(rtm.rollbacks).isEqualTo(expectedCount)
	}

	@Transactional
	open class TestWithCoroutines {

		open suspend fun suspendingNoValueSuccess() {
			delay(1)
		}

		open suspend fun suspendingNoValueFailure() {
			delay(1)
			throw IllegalStateException()
		}

		open suspend fun suspendingValueSuccess(): String {
			delay(1)
			return "foo"
		}

		open suspend fun suspendingValueFailure(): String {
			delay(1)
			throw IllegalStateException()
		}

		open fun flowSuccess(): Flow<String> {
			return flow {
				emit("foo")
				delay(1)
				emit("foo")
			}
		}

		open suspend fun suspendingFlowSuccess(): Flow<String> {
			delay(1)
			return flow {
				emit("foo")
				delay(1)
				emit("foo")
			}
		}

		open suspend fun suspendingValueSuccessWithContext(): String {
			delay(1)
			return coroutineContext[ExampleContext.Key].toString()
		}

		open suspend fun suspendingValueFailureWithContext(): String {
			delay(1)
			throw IllegalStateException(coroutineContext[ExampleContext.Key].toString())
		}
	}
}

data class ExampleContext(val value: String) : AbstractCoroutineContextElement(ExampleContext) {

	companion object Key : CoroutineContext.Key<ExampleContext>

	override fun toString(): String = value
}

private suspend fun withExampleContext(inputValue: String, f: suspend () -> String) =
	withContext(ExampleContext(inputValue)) {
		f()
	}
