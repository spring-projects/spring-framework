/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.core.aop.ataspectj.aopataspectjexample.service;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;

import org.springframework.stereotype.Service;

@Service
public class SampleService {

	// tag::snippet[]
	@Around("execution(* com.xyz..service.*.*(..)) && " +
			"@annotation(com.xyz.service.Idempotent)")
	public Object doConcurrentOperation(ProceedingJoinPoint pjp) throws Throwable {
		// ...
		return pjp.proceed(pjp.getArgs());
	}
	// end::snippet[]

}
