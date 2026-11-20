/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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