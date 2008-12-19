/*
 * Copyright 2002-2005 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aop.framework.adapter;

import static org.junit.Assert.*;

import java.io.Serializable;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Test;
import org.springframework.aop.Advisor;
import org.springframework.aop.BeforeAdvice;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.ITestBean;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * TestCase for AdvisorAdapterRegistrationManager mechanism.
 *
 * @author Dmitriy Kopylenko
 * @author Chris Beams
 */
public final class AdvisorAdapterRegistrationTests {

	@Test
	public void testAdvisorAdapterRegistrationManagerNotPresentInContext() {
		ClassPathXmlApplicationContext ctx =
			new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-without-bpp.xml", getClass());
		ITestBean tb = (ITestBean) ctx.getBean("testBean");
		// just invoke any method to see if advice fired
		try {
			tb.getName();
			fail("Should throw UnknownAdviceTypeException");
		}
		catch (UnknownAdviceTypeException ex) {
			// expected
			assertEquals(0, getAdviceImpl(tb).getInvocationCounter());
		}
	}

	@Test
	public void testAdvisorAdapterRegistrationManagerPresentInContext() {
		ClassPathXmlApplicationContext ctx =
			new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-with-bpp.xml", getClass());
		ITestBean tb = (ITestBean) ctx.getBean("testBean");
		// just invoke any method to see if advice fired
		try {
			tb.getName();
			assertEquals(1, getAdviceImpl(tb).getInvocationCounter());
		}
		catch (UnknownAdviceTypeException ex) {
			fail("Should not throw UnknownAdviceTypeException");
		}
	}

	private SimpleBeforeAdviceImpl getAdviceImpl(ITestBean tb) {
		Advised advised = (Advised) tb;
		Advisor advisor = advised.getAdvisors()[0];
		return (SimpleBeforeAdviceImpl) advisor.getAdvice();
	}

}


interface SimpleBeforeAdvice extends BeforeAdvice {

	void before() throws Throwable;

}


@SuppressWarnings("serial")
class SimpleBeforeAdviceAdapter implements AdvisorAdapter, Serializable {

	public boolean supportsAdvice(Advice advice) {
		return (advice instanceof SimpleBeforeAdvice);
	}

	public MethodInterceptor getInterceptor(Advisor advisor) {
		SimpleBeforeAdvice advice = (SimpleBeforeAdvice) advisor.getAdvice();
		return new SimpleBeforeAdviceInterceptor(advice) ;
	}

}


class SimpleBeforeAdviceImpl implements SimpleBeforeAdvice {
	
	private int invocationCounter;

	public void before() throws Throwable {
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

	public Object invoke(MethodInvocation mi) throws Throwable {
		advice.before();
		return mi.proceed();
	}
	
}