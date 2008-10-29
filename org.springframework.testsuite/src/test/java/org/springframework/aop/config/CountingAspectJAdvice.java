/*
 * Copyright 2002-2005 the original author or authors.
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

package org.springframework.aop.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.beans.ITestBean;

/**
 * @author Rob Harrop
 */
public class CountingAspectJAdvice {

	private int beforeCount;

	private int afterCount;

	private int aroundCount;

	public void myBeforeAdvice() throws Throwable {
		this.beforeCount++;
	}

	public void myAfterAdvice() throws Throwable {
		this.afterCount++;
	}

	public void myAroundAdvice(ProceedingJoinPoint pjp) throws Throwable {
		this.aroundCount++;
		pjp.proceed();
	}
	
	public void myAfterReturningAdvice(int age) {
		this.afterCount++;
	}

	public void myAfterThrowingAdvice(RuntimeException ex) {
		this.afterCount++;
	}
	
	public void mySetAgeAdvice(int newAge, ITestBean bean) {
		// no-op
	}
	
	public int getBeforeCount() {
		return this.beforeCount;
	}

	public int getAfterCount() {
		return this.afterCount;
	}

	public int getAroundCount() {
		return this.aroundCount;
	}
}
