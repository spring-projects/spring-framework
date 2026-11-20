/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.core.aop.ataspectj.aopataspectjexample.service

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.springframework.stereotype.Service

@Service
class SampleService {

	// tag::snippet[]
	@Around("execution(* com.xyz..service.*.*(..)) && " +
			"@annotation(com.xyz.service.Idempotent)")
	fun doConcurrentOperation(pjp: ProceedingJoinPoint): Any? {
		// ...
		return pjp.proceed(pjp.args)
	}
	// end::snippet[]

}