/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.aop.support.annotation;

import org.junit.Before;
import org.junit.Test;

import org.springframework.aop.Pointcut;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.tests.sample.beans.Person;
import org.springframework.tests.sample.beans.SerializablePerson;
import org.springframework.util.ReflectionUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Laszlo Csontos
 */
public class AnnotationMatchingMethodPointcutAdvisorTests {

	protected AnnotationMatchingMethodPointcutAdvisor advisor;

	protected Pointcut pc;

	protected Person proxied;

	protected NopAnnotationMethodAdvice nop;

	/**
	 * Create an empty pointcut, populating instance variables.
	 */
	@Before
	public void setUp() {
		ProxyFactory pf = new ProxyFactory(new SerializablePerson());
		nop = new NopAnnotationMethodAdvice();
		advisor = new AnnotationMatchingMethodPointcutAdvisor(NopAnnotation.class, nop);
		pc = advisor.getPointcut();
		pf.addAdvisor(advisor);
		proxied = (Person) pf.getProxy();
	}

	@Test
	public void testMatchingOnly() {
		assertTrue(pc.getMethodMatcher().matches(ReflectionUtils.findMethod(Person.class, "echo", Object.class), SerializablePerson.class));
		assertTrue(pc.getMethodMatcher().matches(ReflectionUtils.findMethod(Person.class, "setAge", int.class), SerializablePerson.class));
		assertTrue(pc.getMethodMatcher().matches(ReflectionUtils.findMethod(Person.class, "setName", String.class), SerializablePerson.class));
		assertFalse(pc.getMethodMatcher().matches(ReflectionUtils.findMethod(Person.class, "getAge"), SerializablePerson.class));
	}

	@Test
	public void testNonAnnotatedMethods() throws Throwable {
		testAdviceCounters(0, 0, 0, 0);
		proxied.getName();
		testAdviceCounters(0, 0, 0, 0);
	}


	@Test
	public void testAnnotatedMethods() throws Throwable {
		testAdviceCounters(0, 0, 0, 0);
		proxied.echo(null);
		testAdviceCounters(1, 1, 0, 1);
		proxied.setName("");
		testAdviceCounters(2, 2, 0, 2);
		proxied.setAge(25);
		assertEquals(25, proxied.getAge());
		testAdviceCounters(3, 3, 0, 3);
	}

	@Test
	public void testAnnotatedThrowingMethods() throws Throwable {
		testAdviceCounters(0, 0, 0, 0);
		try {
			proxied.echo(new Exception());
		} catch (Exception e) {
		}
		testAdviceCounters(1, 0, 1, 1);
	}

	private void testAdviceCounters(
			int beforeCount, int afterReturningCount, int afterThrowingCount, int duringFinallyCount) {

		assertEquals(beforeCount, nop.getBeforeCount());
		assertEquals(afterReturningCount, nop.getAfterReturningCount());
		assertEquals(afterThrowingCount, nop.getAfterThrowingCount());
		assertEquals(duringFinallyCount, nop.getDuringFinallyCount());
	}

}
