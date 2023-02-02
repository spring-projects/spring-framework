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

package org.springframework.transaction.reactive

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.transaction.support.DefaultTransactionDefinition
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

class TransactionalOperatorExtensionsTests {

	private val tm = ReactiveTestTransactionManager(false, true)

	@Test
	@Suppress("UNUSED_VARIABLE")
	fun commitWithSuspendingFunction() {
		val operator = TransactionalOperator.create(tm, DefaultTransactionDefinition())
		runBlocking {
			val returnValue: Boolean = operator.executeAndAwait {
				delay(1)
				true
			}
		}
		assertThat(tm.commit).isTrue()
		assertThat(tm.rollback).isFalse()
	}

	@Test
	@Suppress("UNUSED_VARIABLE")
	fun commitWithEmptySuspendingFunction() {
		val operator = TransactionalOperator.create(tm, DefaultTransactionDefinition())
		runBlocking {
			val returnValue: Boolean? = operator.executeAndAwait {
				delay(1)
				null
			}
		}
		assertThat(tm.commit).isTrue()
		assertThat(tm.rollback).isFalse()
	}

	@Test
	fun rollbackWithSuspendingFunction() {
		val operator = TransactionalOperator.create(tm, DefaultTransactionDefinition())
		runBlocking {
			try {
				operator.executeAndAwait {
					delay(1)
					throw IllegalStateException()
				}
			} catch (ex: IllegalStateException) {
				assertThat(tm.commit).isFalse()
				assertThat(tm.rollback).isTrue()
				return@runBlocking
			}
		}
	}

	@Test
	fun commitWithFlow() {
		val operator = TransactionalOperator.create(tm, DefaultTransactionDefinition())
		val flow = flow {
			emit(1)
			emit(2)
			emit(3)
			emit(4)
		}
		runBlocking {
			val list = flow.transactional(operator).toList()
			assertThat(list).hasSize(4)
		}
		assertThat(tm.commit).isTrue()
		assertThat(tm.rollback).isFalse()
	}

	@Test
	fun rollbackWithFlow() {
		val operator = TransactionalOperator.create(tm, DefaultTransactionDefinition())
		val flow = flow<Int> {
			delay(1)
			throw IllegalStateException()
		}
		runBlocking {
			try {
				flow.transactional(operator).toList()
			} catch (ex: IllegalStateException) {
				assertThat(tm.commit).isFalse()
				assertThat(tm.rollback).isTrue()
				return@runBlocking
			}
		}
	}

	@Test
	fun coroutineContextWithSuspendingFunction() {
		val operator = TransactionalOperator.create(tm, DefaultTransactionDefinition())
		runBlocking(User(role = "admin")) {
			try {
				operator.executeAndAwait {
					delay(1)
					val currentUser = currentCoroutineContext()[User]
					assertThat(currentUser).isNotNull()
					assertThat(currentUser!!.role).isEqualTo("admin")
					throw IllegalStateException()
				}
			} catch (e: IllegalStateException) {
				assertThat(tm.commit).isFalse()
				assertThat(tm.rollback).isTrue()
				return@runBlocking
			}
		}
	}

	@Test
	fun coroutineContextWithFlow() {
		val operator = TransactionalOperator.create(tm, DefaultTransactionDefinition())
		val flow = flow<Int> {
			delay(1)
			val currentUser = currentCoroutineContext()[User]
			assertThat(currentUser).isNotNull()
			assertThat(currentUser!!.role).isEqualTo("admin")
			throw IllegalStateException()
		}
		runBlocking(User(role = "admin")) {
			try {
				flow.transactional(operator, coroutineContext).toList()
			} catch (e: IllegalStateException) {
				assertThat(tm.commit).isFalse()
				assertThat(tm.rollback).isTrue()
				return@runBlocking
			}
		}
	}


	private data class User(val role: String) : AbstractCoroutineContextElement(User) {
		companion object Key : CoroutineContext.Key<User>
	}
}
