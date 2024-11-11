/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.aop.framework.adapter;

import java.io.Serializable;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aop.Advisor;
import org.springframework.aop.BeforeAdvice;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * TestCase for AdvisorAdapterRegistrationManager mechanism.
 *
 * @author Dmitriy Kopylenko
 * @author Chris Beams
 */
class AdvisorAdapterRegistrationTests {

	@BeforeEach
	@AfterEach
	void resetGlobalAdvisorAdapterRegistry() {
		GlobalAdvisorAdapterRegistry.reset();
	}

	@Test
	void advisorAdapterRegistrationManagerNotPresentInContext() {
		ClassPathXmlApplicationContext ctx =
			new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-without-bpp.xml", getClass());
		ITestBean tb = (ITestBean) ctx.getBean("testBean");
		// just invoke any method to see if advice fired
		assertThatExceptionOfType(UnknownAdviceTypeException.class).isThrownBy(tb::getName);
		assertThat(getAdviceImpl(tb).getInvocationCounter()).isZero();
		ctx.close();
	}

	@Test
	void advisorAdapterRegistrationManagerPresentInContext() {
		ClassPathXmlApplicationContext ctx =
			new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-with-bpp.xml", getClass());
		ITestBean tb = (ITestBean) ctx.getBean("testBean");
		// just invoke any method to see if advice fired
		tb.getName();
		getAdviceImpl(tb).getInvocationCounter();
		ctx.close();
	}

	private SimpleBeforeAdviceImpl getAdviceImpl(ITestBean tb) {
		Advised advised = (Advised) tb;
		Advisor advisor = advised.getAdvisors()[0];
		return (SimpleBeforeAdviceImpl) advisor.getAdvice();
	}

}


interface SimpleBeforeAdvice extends BeforeAdvice {

	void before();

}


@SuppressWarnings("serial")
class SimpleBeforeAdviceAdapter implements AdvisorAdapter, Serializable {

	@Override
	public boolean supportsAdvice(Advice advice) {
		return (advice instanceof SimpleBeforeAdvice);
	}

	@Override
	public MethodInterceptor getInterceptor(Advisor advisor) {
		SimpleBeforeAdvice advice = (SimpleBeforeAdvice) advisor.getAdvice();
		return new SimpleBeforeAdviceInterceptor(advice) ;
	}

}


class SimpleBeforeAdviceImpl implements SimpleBeforeAdvice {

	private int invocationCounter;

	@Override
	public void before() {
		++invocationCounter;
	}

	public int getInvocationCounter() {
		return invocationCounter;
	}

}


final class SimpleBeforeAdviceInterceptor implements MethodInterceptor {

	private SimpleBeforeAdvice advice;

	public SimpleBeforeAdviceInterceptor(SimpleBeforeAdvice advice) {
		this.advice = advice;
	}

	@Override
	public Object invoke(MethodInvocation mi) throws Throwable {
		advice.before();
		return mi.proceed();
	}
}
