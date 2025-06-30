/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.aop.aspectj;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.Advised;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Check that an aspect that depends on another bean, where the referenced bean
 * itself is advised by the same aspect, works correctly.
 *
 * @author Ramnivas Laddad
 * @author Juergen Hoeller
 * @author Chris Beams
 */
class PropertyDependentAspectTests {

	@Test
	void propertyDependentAspectWithPropertyDeclaredBeforeAdvice() {
		checkXmlAspect(getClass().getSimpleName() + "-before.xml");
	}

	@Test
	void propertyDependentAspectWithPropertyDeclaredAfterAdvice() {
		checkXmlAspect(getClass().getSimpleName() + "-after.xml");
	}

	@Test
	void propertyDependentAtAspectJAspectWithPropertyDeclaredBeforeAdvice() {
		checkAtAspectJAspect(getClass().getSimpleName() + "-atAspectJ-before.xml");
	}

	@Test
	void propertyDependentAtAspectJAspectWithPropertyDeclaredAfterAdvice() {
		checkAtAspectJAspect(getClass().getSimpleName() + "-atAspectJ-after.xml");
	}

	private void checkXmlAspect(String appContextFile) {
		ApplicationContext context = new ClassPathXmlApplicationContext(appContextFile, getClass());
		ICounter counter = (ICounter) context.getBean("counter");
		boolean condition = counter instanceof Advised;
		assertThat(condition).as("Proxy didn't get created").isTrue();

		counter.increment();
		JoinPointMonitorAspect callCountingAspect = (JoinPointMonitorAspect)context.getBean("monitoringAspect");
		assertThat(callCountingAspect.beforeExecutions).as("Advise didn't get executed").isEqualTo(1);
		assertThat(callCountingAspect.aroundExecutions).as("Advise didn't get executed").isEqualTo(1);
	}

	private void checkAtAspectJAspect(String appContextFile) {
		ApplicationContext context = new ClassPathXmlApplicationContext(appContextFile, getClass());
		ICounter counter = (ICounter) context.getBean("counter");
		boolean condition = counter instanceof Advised;
		assertThat(condition).as("Proxy didn't get created").isTrue();

		counter.increment();
		JoinPointMonitorAtAspectJAspect callCountingAspect = (JoinPointMonitorAtAspectJAspect)context.getBean("monitoringAspect");
		assertThat(callCountingAspect.beforeExecutions).as("Advise didn't get executed").isEqualTo(1);
		assertThat(callCountingAspect.aroundExecutions).as("Advise didn't get executed").isEqualTo(1);
	}

}


class JoinPointMonitorAspect {

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


@Aspect
class JoinPointMonitorAtAspectJAspect {
	/* The counter property is purposefully not used in the aspect to avoid distraction
	 * from the main bug -- merely needing a dependency on an advised bean
	 * is sufficient to reproduce the bug.
	 */
	private ICounter counter;

	int beforeExecutions;
	int aroundExecutions;

	@Before("execution(* increment*())")
	void before() {
		beforeExecutions++;
	}

	@Around("execution(* increment*())")
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
