/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.aop.aspectj.generic;

import java.util.ArrayList;
import java.util.Collection;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.junit.Before;
import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.tests.sample.beans.Employee;
import org.springframework.tests.sample.beans.TestBean;

import static org.junit.Assert.*;

/**
 * Tests ensuring that after-returning advice for generic parameters bound to
 * the advice and the return type follow AspectJ semantics.
 *
 * <p>See SPR-3628 for more details.
 *
 * @author Ramnivas Laddad
 * @author Chris Beams
 */
public final class AfterReturningGenericTypeMatchingTests {

	private GenericReturnTypeVariationClass testBean;

	private CounterAspect counterAspect;


	@Before
	public void setUp() {
		ClassPathXmlApplicationContext ctx =
			new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-context.xml", getClass());

		counterAspect = (CounterAspect) ctx.getBean("counterAspect");
		counterAspect.reset();

		testBean = (GenericReturnTypeVariationClass) ctx.getBean("testBean");
	}

	@Test
	public void testReturnTypeExactMatching() {
		testBean.getStrings();
		assertEquals(1, counterAspect.getStringsInvocationsCount);
		assertEquals(0, counterAspect.getIntegersInvocationsCount);

		counterAspect.reset();

		testBean.getIntegers();
		assertEquals(0, counterAspect.getStringsInvocationsCount);
		assertEquals(1, counterAspect.getIntegersInvocationsCount);
	}

	@Test
	public void testReturnTypeRawMatching() {
		testBean.getStrings();
		assertEquals(1, counterAspect.getRawsInvocationsCount);

		counterAspect.reset();

		testBean.getIntegers();
		assertEquals(1, counterAspect.getRawsInvocationsCount);
	}

	@Test
	public void testReturnTypeUpperBoundMatching() {
		testBean.getIntegers();
		assertEquals(1, counterAspect.getNumbersInvocationsCount);
	}

	@Test
	public void testReturnTypeLowerBoundMatching() {
		testBean.getTestBeans();
		assertEquals(1, counterAspect.getTestBeanInvocationsCount);

		counterAspect.reset();

		testBean.getEmployees();
		assertEquals(0, counterAspect.getTestBeanInvocationsCount);
	}

}


class GenericReturnTypeVariationClass {

	public Collection<String> getStrings() {
		return new ArrayList<String>();
	}

	public Collection<Integer> getIntegers() {
		return new ArrayList<Integer>();
	}

	public Collection<TestBean> getTestBeans() {
		return new ArrayList<TestBean>();
	}

	public Collection<Employee> getEmployees() {
		return new ArrayList<Employee>();
	}
}


@Aspect
class CounterAspect {

	int getRawsInvocationsCount;

	int getStringsInvocationsCount;

	int getIntegersInvocationsCount;

	int getNumbersInvocationsCount;

	int getTestBeanInvocationsCount;

	@Pointcut("execution(* org.springframework.aop.aspectj.generic.GenericReturnTypeVariationClass.*(..))")
	public void anyTestMethod() {
	}

	@AfterReturning(pointcut = "anyTestMethod()", returning = "ret")
	public void incrementGetRawsInvocationsCount(Collection<?> ret) {
		getRawsInvocationsCount++;
	}

	@AfterReturning(pointcut = "anyTestMethod()", returning = "ret")
	public void incrementGetStringsInvocationsCount(Collection<String> ret) {
		getStringsInvocationsCount++;
	}

	@AfterReturning(pointcut = "anyTestMethod()", returning = "ret")
	public void incrementGetIntegersInvocationsCount(Collection<Integer> ret) {
		getIntegersInvocationsCount++;
	}

	@AfterReturning(pointcut = "anyTestMethod()", returning = "ret")
	public void incrementGetNumbersInvocationsCount(Collection<? extends Number> ret) {
		getNumbersInvocationsCount++;
	}

	@AfterReturning(pointcut = "anyTestMethod()", returning = "ret")
	public void incrementTestBeanInvocationsCount(Collection<? super TestBean> ret) {
		getTestBeanInvocationsCount++;
	}

	public void reset() {
		getRawsInvocationsCount = 0;
		getStringsInvocationsCount = 0;
		getIntegersInvocationsCount = 0;
		getNumbersInvocationsCount = 0;
		getTestBeanInvocationsCount = 0;
	}
}

