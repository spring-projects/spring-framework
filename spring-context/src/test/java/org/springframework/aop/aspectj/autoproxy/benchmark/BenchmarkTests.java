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

package org.springframework.aop.aspectj.autoproxy.benchmark;

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.Test;

import org.springframework.aop.Advisor;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.StopWatch;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for AspectJ auto proxying. Includes mixing with Spring AOP
 * Advisors to demonstrate that existing autoproxying contract is honoured.
 *
 * @author Rod Johnson
 * @author Chris Beams
 */
class BenchmarkTests {

	private static final Class<?> CLASS = BenchmarkTests.class;

	private static final String ASPECTJ_CONTEXT = CLASS.getSimpleName() + "-aspectj.xml";

	private static final String SPRING_AOP_CONTEXT = CLASS.getSimpleName() + "-springAop.xml";

	@Test
	void repeatedAroundAdviceInvocationsWithAspectJ() {
		testRepeatedAroundAdviceInvocations(ASPECTJ_CONTEXT, getCount(), "AspectJ");
	}

	@Test
	void repeatedAroundAdviceInvocationsWithSpringAop() {
		testRepeatedAroundAdviceInvocations(SPRING_AOP_CONTEXT, getCount(), "Spring AOP");
	}

	@Test
	void repeatedBeforeAdviceInvocationsWithAspectJ() {
		testBeforeAdviceWithoutJoinPoint(ASPECTJ_CONTEXT, getCount(), "AspectJ");
	}

	@Test
	void repeatedBeforeAdviceInvocationsWithSpringAop() {
		testBeforeAdviceWithoutJoinPoint(SPRING_AOP_CONTEXT, getCount(), "Spring AOP");
	}

	@Test
	void repeatedAfterReturningAdviceInvocationsWithAspectJ() {
		testAfterReturningAdviceWithoutJoinPoint(ASPECTJ_CONTEXT, getCount(), "AspectJ");
	}

	@Test
	void repeatedAfterReturningAdviceInvocationsWithSpringAop() {
		testAfterReturningAdviceWithoutJoinPoint(SPRING_AOP_CONTEXT, getCount(), "Spring AOP");
	}

	@Test
	void repeatedMixWithAspectJ() {
		testMix(ASPECTJ_CONTEXT, getCount(), "AspectJ");
	}

	@Test
	void repeatedMixWithSpringAop() {
		testMix(SPRING_AOP_CONTEXT, getCount(), "Spring AOP");
	}

	/**
	 * Change the return number to a higher number to make this test useful.
	 */
	protected int getCount() {
		return 10;
	}

	private long testRepeatedAroundAdviceInvocations(String file, int howmany, String technology) {
		ClassPathXmlApplicationContext ac = new ClassPathXmlApplicationContext(file, CLASS);

		StopWatch sw = new StopWatch();
		sw.start(howmany + " repeated around advice invocations with " + technology);
		ITestBean adrian = (ITestBean) ac.getBean("adrian");

		assertThat(AopUtils.isAopProxy(adrian)).isTrue();
		assertThat(adrian.getAge()).isEqualTo(68);

		for (int i = 0; i < howmany; i++) {
			adrian.getAge();
		}

		sw.stop();
		// System.out.println(sw.prettyPrint());
		ac.close();
		return sw.getTotalTimeMillis();
	}

	private long testBeforeAdviceWithoutJoinPoint(String file, int howmany, String technology) {
		ClassPathXmlApplicationContext ac = new ClassPathXmlApplicationContext(file, CLASS);

		StopWatch sw = new StopWatch();
		sw.start(howmany + " repeated before advice invocations with " + technology);
		ITestBean adrian = (ITestBean) ac.getBean("adrian");

		assertThat(AopUtils.isAopProxy(adrian)).isTrue();
		Advised a = (Advised) adrian;
		assertThat(a.getAdvisors()).hasSizeGreaterThanOrEqualTo(3);
		assertThat(adrian.getName()).isEqualTo("adrian");

		for (int i = 0; i < howmany; i++) {
			adrian.getName();
		}

		sw.stop();
		// System.out.println(sw.prettyPrint());
		ac.close();
		return sw.getTotalTimeMillis();
	}

	private long testAfterReturningAdviceWithoutJoinPoint(String file, int howmany, String technology) {
		ClassPathXmlApplicationContext ac = new ClassPathXmlApplicationContext(file, CLASS);

		StopWatch sw = new StopWatch();
		sw.start(howmany + " repeated after returning advice invocations with " + technology);
		ITestBean adrian = (ITestBean) ac.getBean("adrian");

		assertThat(AopUtils.isAopProxy(adrian)).isTrue();
		Advised a = (Advised) adrian;
		assertThat(a.getAdvisors()).hasSizeGreaterThanOrEqualTo(3);
		// Hits joinpoint
		adrian.setAge(25);

		for (int i = 0; i < howmany; i++) {
			adrian.setAge(i);
		}

		sw.stop();
		// System.out.println(sw.prettyPrint());
		ac.close();
		return sw.getTotalTimeMillis();
	}

	private long testMix(String file, int howmany, String technology) {
		ClassPathXmlApplicationContext ac = new ClassPathXmlApplicationContext(file, CLASS);

		StopWatch sw = new StopWatch();
		sw.start(howmany + " repeated mixed invocations with " + technology);
		ITestBean adrian = (ITestBean) ac.getBean("adrian");

		assertThat(AopUtils.isAopProxy(adrian)).isTrue();
		Advised a = (Advised) adrian;
		assertThat(a.getAdvisors()).hasSizeGreaterThanOrEqualTo(3);

		for (int i = 0; i < howmany; i++) {
			// Hit all 3 joinpoints
			adrian.getAge();
			adrian.getName();
			adrian.setAge(i);

			// Invoke three non-advised methods
			adrian.getDoctor();
			adrian.getLawyer();
			adrian.getSpouse();
		}

		sw.stop();
		// System.out.println(sw.prettyPrint());
		ac.close();
		return sw.getTotalTimeMillis();
	}

}


class MultiplyReturnValueInterceptor implements MethodInterceptor {

	private int multiple = 2;

	public int invocations;

	public void setMultiple(int multiple) {
		this.multiple = multiple;
	}

	public int getMultiple() {
		return this.multiple;
	}

	@Override
	public Object invoke(MethodInvocation mi) throws Throwable {
		++invocations;
		int result = (Integer) mi.proceed();
		return result * this.multiple;
	}

}


class TraceAfterReturningAdvice implements AfterReturningAdvice {

	public int afterTakesInt;

	@Override
	public void afterReturning(Object returnValue, Method method, Object[] args, Object target) {
		++afterTakesInt;
	}

	public static Advisor advisor() {
		return new DefaultPointcutAdvisor(
			new StaticMethodMatcherPointcut() {
				@Override
				public boolean matches(Method method, Class<?> targetClass) {
					return method.getParameterCount() == 1 &&
						method.getParameterTypes()[0].equals(Integer.class);
				}
			},
			new TraceAfterReturningAdvice());
	}

}


@Aspect
class TraceAspect {

	public int beforeStringReturn;

	public int afterTakesInt;

	@Before("execution(String *.*(..))")
	public void traceWithoutJoinPoint() {
		++beforeStringReturn;
	}

	@AfterReturning("execution(void *.*(int))")
	public void traceWithoutJoinPoint2() {
		++afterTakesInt;
	}

}


class TraceBeforeAdvice implements MethodBeforeAdvice {

	public int beforeStringReturn;

	@Override
	public void before(Method method, Object[] args, Object target) {
		++beforeStringReturn;
	}

	public static Advisor advisor() {
		return new DefaultPointcutAdvisor(
			new StaticMethodMatcherPointcut() {
				@Override
				public boolean matches(Method method, Class<?> targetClass) {
					return method.getReturnType().equals(String.class);
				}
			},
			new TraceBeforeAdvice());
	}

}
