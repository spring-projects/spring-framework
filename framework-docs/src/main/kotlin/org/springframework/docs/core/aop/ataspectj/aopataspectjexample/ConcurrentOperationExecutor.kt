/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.core.aop.ataspectj.aopataspectjexample

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.core.Ordered
import org.springframework.dao.PessimisticLockingFailureException

// tag::snippet[]
@Aspect
class ConcurrentOperationExecutor : Ordered {

	companion object {
		private const val DEFAULT_MAX_RETRIES = 2
	}

	var maxRetries = DEFAULT_MAX_RETRIES

	private var order = 1

	override fun getOrder(): Int {
		return this.order
	}

	fun setOrder(order: Int) {
		this.order = order
	}

	@Around("com.xyz.CommonPointcuts.businessService()")
	fun doConcurrentOperation(pjp: ProceedingJoinPoint): Any {
		var numAttempts = 0
		var lockFailureException: PessimisticLockingFailureException?
		do {
			numAttempts++
			try {
				return pjp.proceed()
			} catch (ex: PessimisticLockingFailureException) {
				lockFailureException = ex
			}
		} while (numAttempts <= this.maxRetries)
		throw lockFailureException
	}
} // end::snippet[]