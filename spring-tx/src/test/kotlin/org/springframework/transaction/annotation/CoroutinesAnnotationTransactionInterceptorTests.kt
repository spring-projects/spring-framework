package org.springframework.transaction.annotation

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.aop.framework.ProxyFactory
import org.springframework.transaction.TransactionManager
import org.springframework.transaction.interceptor.TransactionInterceptor
import org.springframework.transaction.testfixture.CallCountingTransactionManager
import org.springframework.transaction.testfixture.ReactiveCallCountingTransactionManager

class CoroutinesAnnotationTransactionInterceptorTests {

	private val ptm = CallCountingTransactionManager()

	private val rtm = ReactiveCallCountingTransactionManager()

	private val source = AnnotationTransactionAttributeSource()

	private val ti = TransactionInterceptor((ptm as TransactionManager), source)

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
