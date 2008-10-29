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
 * Aspect used as part of before advice binding tests.
 *
 * @author Adrian Colyer
 */
public class AroundAdviceBindingTestAspect {

	private AroundAdviceBindingCollaborator collaborator = null;

	public void setCollaborator(AroundAdviceBindingCollaborator aCollaborator) {
		this.collaborator = aCollaborator;
	}

	// "advice" methods
	public void oneIntArg(ProceedingJoinPoint pjp, int age) throws Throwable {
		this.collaborator.oneIntArg(age);
		pjp.proceed();
	}

	public int oneObjectArg(ProceedingJoinPoint pjp, Object bean) throws Throwable {
		this.collaborator.oneObjectArg(bean);
		return ((Integer) pjp.proceed()).intValue();
	}

	public void oneIntAndOneObject(ProceedingJoinPoint pjp, int x , Object o) throws Throwable {
		this.collaborator.oneIntAndOneObject(x,o);
		pjp.proceed();
	}

	public int justJoinPoint(ProceedingJoinPoint pjp) throws Throwable {
		this.collaborator.justJoinPoint(pjp.getSignature().getName());
		return ((Integer) pjp.proceed()).intValue();
	}


	/**
	 * Collaborator interface that makes it easy to test this aspect
	 * is working as expected through mocking.
	 */
	public interface AroundAdviceBindingCollaborator {

		void oneIntArg(int x);

		void oneObjectArg(Object o);

		void oneIntAndOneObject(int x, Object o);

		void justJoinPoint(String s);
	}

}
