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
		throw lockFailureException!!
	}
} // end::snippet[]