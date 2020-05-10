/*
 * Copyright 2002-2019 the original author or authors.
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

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.springframework.transaction.support.DefaultTransactionDefinition

class TransactionalOperatorExtensionsTests {

	private val tm = ReactiveTestTransactionManager(false, true)

	@Test
	fun commitWithSuspendingFunction() {
		val operator = TransactionalOperator.create(tm, DefaultTransactionDefinition())
		runBlocking {
			operator.executeAndAwait {
				delay(1)
				true
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
			fail("")
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
}
