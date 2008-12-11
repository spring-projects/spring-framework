/*
 * Copyright 2002-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aop.aspectj.annotation;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

/**
 * @author Adrian Colyer
 */
@Aspect
public class NamedPointcutWithArgs {

	@Pointcut("execution(* *(..)) && args(s,..)")
	public void pointcutWithArgs(String s) {}

	@Around("pointcutWithArgs(aString)")
	public Object doAround(ProceedingJoinPoint pjp, String aString) throws Throwable {
		System.out.println("got '" + aString + "' at '" + pjp + "'");
		throw new IllegalArgumentException(aString);
	}

}
