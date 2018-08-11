/*
 * Copyright 2002-2018 the original author or authors.
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

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Before;
import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.tests.sample.beans.ITestBean;

import static org.junit.Assert.*;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class AspectJExpressionPointcutAdvisorTests {

	private ITestBean testBean;

	private CallCountingInterceptor interceptor;


	@Before
	public void setup() {
		ClassPathXmlApplicationContext ctx =
				new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());
		testBean = (ITestBean) ctx.getBean("testBean");
		interceptor = (CallCountingInterceptor) ctx.getBean("interceptor");
	}


	@Test
	public void testPointcutting() {
		assertEquals("Count should be 0", 0, interceptor.getCount());
		testBean.getSpouses();
		assertEquals("Count should be 1", 1, interceptor.getCount());
		testBean.getSpouse();
		assertEquals("Count should be 1", 1, interceptor.getCount());
	}

}


class CallCountingInterceptor implements MethodInterceptor {

	private int count;

	@Override
	public Object invoke(MethodInvocation methodInvocation) throws Throwable {
		count++;
		return methodInvocation.proceed();
	}

	public int getCount() {
		return count;
	}

	public void reset() {
		this.count = 0;
	}
}
