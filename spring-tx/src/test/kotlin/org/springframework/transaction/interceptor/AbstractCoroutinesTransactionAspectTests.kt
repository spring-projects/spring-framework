/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.transaction.interceptor

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.assertj.core.api.Fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito
import org.mockito.Mockito
import org.springframework.transaction.*
import org.springframework.transaction.reactive.TransactionContext
import reactor.core.publisher.Mono
import reactor.core.publisher.SynchronousSink
import reactor.util.context.ContextView
import java.lang.reflect.Method
import kotlin.coroutines.Continuation

/**
 * Abstract support class to test [TransactionAspectSupport] with coroutines methods.
 *
 * @author Sebastien Deleuze
 * @author Mark Paluch
 * @author Juergen Hoeller
 */
abstract class AbstractCoroutinesTransactionAspectTests {

	private var getNameMethod: Method? = null
	private var setNameMethod: Method? = null
	private var exceptionalMethod: Method? = null

	@BeforeEach
	fun setup() {
		getNameMethod = TestBean::class.java.getMethod("getName", Continuation::class.java)
		setNameMethod = TestBean::class.java.getMethod("setName", String::class.java, Continuation::class.java)
		exceptionalMethod = TestBean::class.java.getMethod("exceptional", Throwable::class.java, Continuation::class.java)
	}

	@Test
	fun noTransaction() {
		val rtm = Mockito.mock(ReactiveTransactionManager::class.java)
		val tb = DefaultTestBean()
		val tas: TransactionAttributeSource = MapTransactionAttributeSource()

		// All the methods in this class use the advised() template method
		// to obtain a transaction object, configured with the when PlatformTransactionManager
		// and transaction attribute source
		val itb = advised(tb, rtm, tas) as TestBean
		checkReactiveTransaction(false)
		runBlocking {
			itb.getName()
		}
		checkReactiveTransaction(false)

		// expect no calls
		Mockito.verifyNoInteractions(rtm)
	}

	/**
	 * Check that a transaction is created and committed.
	 */
	@Test
	fun transactionShouldSucceed() {
		val txatt: TransactionAttribute = DefaultTransactionAttribute()
		val tas = MapTransactionAttributeSource()
		tas.register(getNameMethod!!, txatt)
		val status = Mockito.mock(ReactiveTransaction::class.java)
		val rtm = Mockito.mock(ReactiveTransactionManager::class.java)
		// expect a transaction
		BDDMockito.given(rtm.getReactiveTransaction(txatt)).willReturn(Mono.just(status))
		BDDMockito.given(rtm.commit(status)).willReturn(Mono.empty())
		val tb = DefaultTestBean()
		val itb = advised(tb, rtm, tas) as TestBean
		runBlocking {
			itb.getName()
		}
		Mockito.verify(rtm).commit(status)
	}

	/**
	 * Check that two transactions are created and committed.
	 */
	@Test
	fun twoTransactionsShouldSucceed() {
		val txatt: TransactionAttribute = DefaultTransactionAttribute()
		val tas1 = MapTransactionAttributeSource()
		tas1.register(getNameMethod!!, txatt)
		val tas2 = MapTransactionAttributeSource()
		tas2.register(setNameMethod!!, txatt)
		val status = Mockito.mock(ReactiveTransaction::class.java)
		val rtm = Mockito.mock(ReactiveTransactionManager::class.java)
		// expect a transaction
		BDDMockito.given(rtm.getReactiveTransaction(txatt)).willReturn(Mono.just(status))
		BDDMockito.given(rtm.commit(status)).willReturn(Mono.empty())
		val tb = DefaultTestBean()
		val itb = advised(tb, rtm, arrayOf(tas1, tas2)) as TestBean
		runBlocking {
			itb.getName()
			itb.setName("myName")
		}
		Mockito.verify(rtm, Mockito.times(2)).commit(status)
	}

	/**
	 * Check that a transaction is created and committed.
	 */
	@Test
	fun transactionShouldSucceedWithNotNew() {
		val txatt: TransactionAttribute = DefaultTransactionAttribute()
		val tas = MapTransactionAttributeSource()
		tas.register(getNameMethod!!, txatt)
		val status = Mockito.mock(ReactiveTransaction::class.java)
		val rtm = Mockito.mock(ReactiveTransactionManager::class.java)
		// expect a transaction
		BDDMockito.given(rtm.getReactiveTransaction(txatt)).willReturn(Mono.just(status))
		BDDMockito.given(rtm.commit(status)).willReturn(Mono.empty())
		val tb = DefaultTestBean()
		val itb = advised(tb, rtm, tas) as TestBean
		runBlocking {
			itb.getName()
		}
		Mockito.verify(rtm).commit(status)
	}

	@Test
	fun rollbackOnCheckedException() {
		doTestRollbackOnException(Exception("foo"), true, false)
	}

	@Test
	fun noRollbackOnCheckedException() {
		doTestRollbackOnException(Exception("foo"), false, false)
	}

	@Test
	fun rollbackOnUncheckedException() {
		doTestRollbackOnException(RuntimeException("foo"), true, false)
	}

	@Test
	fun noRollbackOnUncheckedException() {
		doTestRollbackOnException(RuntimeException("foo"), false, false)
	}

	@Test
	fun rollbackOnCheckedExceptionWithRollbackException() {
		doTestRollbackOnException(Exception("foo"), true, true)
	}

	@Test
	fun noRollbackOnCheckedExceptionWithRollbackException() {
		doTestRollbackOnException(Exception("foo"), false, true)
	}

	@Test
	fun rollbackOnUncheckedExceptionWithRollbackException() {
		doTestRollbackOnException(RuntimeException("foo"), true, true)
	}

	@Test
	fun noRollbackOnUncheckedExceptionWithRollbackException() {
		doTestRollbackOnException(RuntimeException("foo"), false, true)
	}

	/**
	 * Check that the when exception thrown by the target can produce the
	 * desired behavior with the appropriate transaction attribute.
	 * @param ex exception to be thrown by the target
	 * @param shouldRollback whether this should cause a transaction rollback
	 */
	protected fun doTestRollbackOnException(
			ex: Exception, shouldRollback: Boolean, rollbackException: Boolean) {
		val txatt: TransactionAttribute = object : DefaultTransactionAttribute() {
			override fun rollbackOn(t: Throwable): Boolean {
				Assertions.assertThat(t).isSameAs(ex)
				return shouldRollback
			}
		}
		val m = exceptionalMethod
		val tas = MapTransactionAttributeSource()
		tas.register(m!!, txatt)
		val status = Mockito.mock(ReactiveTransaction::class.java)
		val rtm = Mockito.mock(ReactiveTransactionManager::class.java)
		// Gets additional call(s) from TransactionControl
		BDDMockito.given(rtm.getReactiveTransaction(txatt)).willReturn(Mono.just(status))
		val tex = TransactionSystemException("system exception")
		if (rollbackException) {
			if (shouldRollback) {
				BDDMockito.given(rtm.rollback(status)).willReturn(Mono.error(tex))
			} else {
				BDDMockito.given(rtm.commit(status)).willReturn(Mono.error(tex))
			}
		} else {
			BDDMockito.given(rtm.commit(status)).willReturn(Mono.empty())
			BDDMockito.given(rtm.rollback(status)).willReturn(Mono.empty())
		}
		val tb = DefaultTestBean()
		val itb = advised(tb, rtm, tas) as TestBean
		runBlocking {
			try {
				itb.exceptional(ex)
			}
			catch (actual: Exception) {
				if (rollbackException) {
					Assertions.assertThat(actual).hasMessage(tex.message).isInstanceOf(tex::class.java)
				} else {
					Assertions.assertThat(actual).hasMessage(ex.message).isInstanceOf(ex::class.java)
				}
			}
		}
		if (!rollbackException) {
			if (shouldRollback) {
				Mockito.verify(rtm).rollback(status)
			} else {
				Mockito.verify(rtm).commit(status)
			}
		}
	}

	/**
	 * Simulate a transaction infrastructure failure.
	 * Shouldn't invoke target method.
	 */
	@Test
	fun cannotCreateTransaction() {
		val txatt: TransactionAttribute = DefaultTransactionAttribute()
		val m = getNameMethod
		val tas = MapTransactionAttributeSource()
		tas.register(m!!, txatt)
		val rtm = Mockito.mock(ReactiveTransactionManager::class.java)
		// Expect a transaction
		val ex = CannotCreateTransactionException("foobar")
		BDDMockito.given(rtm.getReactiveTransaction(txatt)).willThrow(ex)
		val tb: DefaultTestBean = object : DefaultTestBean() {
			override suspend fun getName(): String? {
				throw UnsupportedOperationException(
						"Shouldn't have invoked target method when couldn't create transaction for transactional method")
			}
		}
		val itb = advised(tb, rtm, tas) as TestBean
		runBlocking {
			try {
				itb.getName()
			}
			catch (actual: Exception) {
				Assertions.assertThat(actual).isInstanceOf(CannotCreateTransactionException::class.java)
			}
		}
	}

	/**
	 * Simulate failure of the underlying transaction infrastructure to commit.
	 * Check that the target method was invoked, but that the transaction
	 * infrastructure exception was thrown to the client
	 */
	@Test
	fun cannotCommitTransaction() {
		val txatt: TransactionAttribute = DefaultTransactionAttribute()
		val m = setNameMethod
		val tas = MapTransactionAttributeSource()
		tas.register(m!!, txatt)
		// Method m2 = getNameMethod;
		// No attributes for m2
		val rtm = Mockito.mock(ReactiveTransactionManager::class.java)
		val status = Mockito.mock(ReactiveTransaction::class.java)
		BDDMockito.given(rtm.getReactiveTransaction(txatt)).willReturn(Mono.just(status))
		val ex = UnexpectedRollbackException("foobar")
		BDDMockito.given(rtm.commit(status)).willReturn(Mono.error(ex))
		BDDMockito.given(rtm.rollback(status)).willReturn(Mono.empty())
		val tb = DefaultTestBean()
		val itb = advised(tb, rtm, tas) as TestBean
		val name = "new name"
		runBlocking {
			try {
				itb.setName(name)
			}
			catch (ex: Exception) {
				Assertions.assertThat(ex).isInstanceOf(RuntimeException::class.java)
				Assertions.assertThat(ex.cause).hasMessage(ex.message).isInstanceOf(ex::class.java)
			}
			// Should have invoked target and changed name
			Assertions.assertThat(itb.getName()).isEqualTo(name)
		}
	}

	private fun checkReactiveTransaction(expected: Boolean) {
		Mono.deferContextual{context -> Mono.just(context)}
				.handle { context: ContextView, sink: SynchronousSink<Any?> ->
					if (context.hasKey(TransactionContext::class.java) != expected) {
						Fail.fail<Any>("Should have thrown NoTransactionException")
					}
					sink.complete()
				}
				.block()
	}

	protected open fun advised(target: Any, rtm: ReactiveTransactionManager, tas: Array<TransactionAttributeSource>): Any {
		return advised(target, rtm, CompositeTransactionAttributeSource(*tas))
	}

	/**
	 * Subclasses must implement this to create an advised object based on the
	 * when target. In the case of AspectJ, the  advised object will already
	 * have been created, as there's no distinction between target and proxy.
	 * In the case of Spring's own AOP framework, a proxy must be created
	 * using a suitably configured transaction interceptor
	 * @param target the target if there's a distinct target. If not (AspectJ),
	 * return target.
	 * @return transactional advised object
	 */
	protected abstract fun advised(target: Any, rtm: ReactiveTransactionManager, tas: TransactionAttributeSource): Any

	interface TestBean {
		suspend fun getName(): String?
		suspend fun setName(name: String?)
		suspend fun exceptional(t: Throwable?)
	}

	open class DefaultTestBean : TestBean {

		private var name: String? = null

		override suspend fun getName(): String? {
			delay(10)
			return name
		}

		override suspend fun setName(name: String?) {
			delay(10)
			this.name = name
		}

		override suspend fun exceptional(t: Throwable?) {
			delay(10)
			if (t != null) {
				throw t
			}
		}
	}
}