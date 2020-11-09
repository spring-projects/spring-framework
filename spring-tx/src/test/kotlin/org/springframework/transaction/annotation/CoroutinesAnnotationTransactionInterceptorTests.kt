/*
 * Copyright 2002-2020 the original author or authors.
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
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.aop.framework.ProxyFactory
import org.springframework.transaction.interceptor.TransactionInterceptor
import org.springframework.transaction.testfixture.ReactiveCallCountingTransactionManager

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
		runBlocking {
			try {
				proxy.suspendingNoValueFailure()
			}
			catch (ex: IllegalStateException) {
			}

		}
		assertReactiveGetTransactionAndRollbackCount(1)
	}

	@Test
	@Disabled("Currently fails due to gh-25998")
	fun suspendingValueSuccess() {
		val proxyFactory = ProxyFactory()
		proxyFactory.setTarget(TestWithCoroutines())
		proxyFactory.addAdvice(TransactionInterceptor(rtm, source))
		val proxy = proxyFactory.proxy as TestWithCoroutines
		runBlocking {
			Assertions.assertThat(proxy.suspendingValueSuccess()).isEqualTo("foo")
		}
		assertReactiveGetTransactionAndCommitCount(1)
	}

	@Test
	fun suspendingValueFailure() {
		val proxyFactory = ProxyFactory()
		proxyFactory.setTarget(TestWithCoroutines())
		proxyFactory.addAdvice(TransactionInterceptor(rtm, source))
		val proxy = proxyFactory.proxy as TestWithCoroutines
		runBlocking {
			try {
				proxy.suspendingValueFailure()
			}
			catch (ex: IllegalStateException) {
			}

		}
		assertReactiveGetTransactionAndRollbackCount(1)
	}

	private fun assertReactiveGetTransactionAndCommitCount(expectedCount: Int) {
		Assertions.assertThat(rtm.begun).isEqualTo(expectedCount)
		Assertions.assertThat(rtm.commits).isEqualTo(expectedCount)
	}

	private fun assertReactiveGetTransactionAndRollbackCount(expectedCount: Int) {
		Assertions.assertThat(rtm.begun).isEqualTo(expectedCount)
		Assertions.assertThat(rtm.rollbacks).isEqualTo(expectedCount)
	}

	@Transactional
	open class TestWithCoroutines {

		open suspend fun suspendingNoValueSuccess() {
			delay(10)
		}

		open suspend fun suspendingNoValueFailure() {
			delay(10)
			throw IllegalStateException()
		}

		open suspend fun suspendingValueSuccess(): String {
			delay(10)
			return "foo"
		}

		open suspend fun suspendingValueFailure(): String {
			delay(10)
			throw IllegalStateException()
		}
	}
}
