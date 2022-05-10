/*
 * Copyright 2002-2022 the original author or authors.
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.testfixture.beans.Employee;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests ensuring that after-returning advice for generic parameters bound to
 * the advice and the return type follow AspectJ semantics.
 *
 * <p>See SPR-3628 for more details.
 *
 * @author Ramnivas Laddad
 * @author Chris Beams
 */
class AfterReturningGenericTypeMatchingTests {

	private ClassPathXmlApplicationContext ctx;

	private GenericReturnTypeVariationClass testBean;

	private CounterAspect counterAspect;


	@BeforeEach
	void setup() {
		this.ctx = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-context.xml", getClass());

		counterAspect = (CounterAspect) ctx.getBean("counterAspect");
		counterAspect.reset();

		testBean = (GenericReturnTypeVariationClass) ctx.getBean("testBean");
	}

	@AfterEach
	void tearDown() {
		this.ctx.close();
	}


	@Test
	void returnTypeExactMatching() {
		testBean.getStrings();
		assertThat(counterAspect.getStringsInvocationsCount).isEqualTo(1);
		assertThat(counterAspect.getIntegersInvocationsCount).isEqualTo(0);

		counterAspect.reset();

		testBean.getIntegers();
		assertThat(counterAspect.getStringsInvocationsCount).isEqualTo(0);
		assertThat(counterAspect.getIntegersInvocationsCount).isEqualTo(1);
	}

	@Test
	void returnTypeRawMatching() {
		testBean.getStrings();
		assertThat(counterAspect.getRawsInvocationsCount).isEqualTo(1);

		counterAspect.reset();

		testBean.getIntegers();
		assertThat(counterAspect.getRawsInvocationsCount).isEqualTo(1);
	}

	@Test
	void returnTypeUpperBoundMatching() {
		testBean.getIntegers();
		assertThat(counterAspect.getNumbersInvocationsCount).isEqualTo(1);
	}

	@Test
	void returnTypeLowerBoundMatching() {
		testBean.getTestBeans();
		assertThat(counterAspect.getTestBeanInvocationsCount).isEqualTo(1);

		counterAspect.reset();

		testBean.getEmployees();
		assertThat(counterAspect.getTestBeanInvocationsCount).isEqualTo(0);
	}

}


class GenericReturnTypeVariationClass {

	public Collection<String> getStrings() {
		return new ArrayList<>();
	}

	public Collection<Integer> getIntegers() {
		return new ArrayList<>();
	}

	public Collection<TestBean> getTestBeans() {
		return new ArrayList<>();
	}

	public Collection<Employee> getEmployees() {
		return new ArrayList<>();
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

