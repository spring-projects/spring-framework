/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.aop.aspectj;

import org.aspectj.lang.ProceedingJoinPoint;

/**
 * @author Ramnivas Laddad
 */
public class JoinPointMonitorAspect {

	/**
	 * The counter property is purposefully not used in the aspect to avoid distraction
	 * from the main bug -- merely needing a dependency on an advised bean
	 * is sufficient to reproduce the bug.
	 */
	private ICounter counter;
	
	int beforeExecutions;
	int aroundExecutions;

	public void before() {
		beforeExecutions++;
	}

	public Object around(ProceedingJoinPoint pjp) throws Throwable {
		aroundExecutions++;
		return pjp.proceed();
	}

	public ICounter getCounter() {
		return counter;
	}

	public void setCounter(ICounter counter) {
		this.counter = counter;
	}

}
