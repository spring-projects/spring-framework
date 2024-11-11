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

package org.springframework.transaction.interceptor

import org.assertj.core.api.Assertions.assertThat
import org.springframework.aop.framework.ProxyFactory
import org.springframework.transaction.ReactiveTransactionManager

/**
 * Tests for [TransactionInterceptor] with coroutines methods.
 *
 * @author Sebastien Deleuze
 * @author Mark Paluch
 */
class CoroutinesTransactionInterceptorTests : AbstractCoroutinesTransactionAspectTests() {
	override fun advised(target: Any, rtm: ReactiveTransactionManager, tas: Array<TransactionAttributeSource>): Any {
		val ti = TransactionInterceptor()
		ti.transactionManager = rtm
		ti.setTransactionAttributeSources(*tas)
		val pf = ProxyFactory(target)
		pf.addAdvice(0, ti)
		return pf.proxy
	}

	/**
	 * Template method to create an advised object given the
	 * target object and transaction setup.
	 * Creates a TransactionInterceptor and applies it.
	 */
	override fun advised(target: Any, rtm: ReactiveTransactionManager, tas: TransactionAttributeSource): Any {
		val ti = TransactionInterceptor()
		ti.transactionManager = rtm
		assertThat(ti.transactionManager).isEqualTo(rtm)
		ti.transactionAttributeSource = tas
		assertThat(ti.transactionAttributeSource).isEqualTo(tas)
		val pf = ProxyFactory(target)
		pf.addAdvice(0, ti)
		return pf.proxy
	}
}