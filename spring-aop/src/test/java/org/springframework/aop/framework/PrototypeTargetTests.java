/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.aop.framework;

import static org.junit.Assert.assertEquals;
import static test.util.TestResourceUtils.beanFactoryFromQualifiedResource;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 03.09.2004
 */
public final class PrototypeTargetTests {

	@Test
	public void testPrototypeProxyWithPrototypeTarget() {
		assertConstructionAndInvocationCounts("testBeanPrototype", 10, 10);
	}

	@Test
	public void testSingletonProxyWithPrototypeTarget() {
		assertConstructionAndInvocationCounts("testBeanSingleton", 1, 10);
	}

	private void assertConstructionAndInvocationCounts(String beanName,
		int constructionCount, int invocationCount) {
		TestBeanImpl.constructionCount = 0;
		BeanFactory bf = beanFactoryFromQualifiedResource(getClass(), "context.xml");
		for (int i = 0; i < 10; i++) {
			TestBean tb = (TestBean) bf.getBean(beanName);
			tb.doSomething();
		}
		TestInterceptor interceptor = (TestInterceptor) bf.getBean("testInterceptor");
		assertEquals(constructionCount, TestBeanImpl.constructionCount);
		assertEquals(invocationCount, interceptor.invocationCount);
	}

	public static interface TestBean {
		public void doSomething();
	}


	public static class TestBeanImpl implements TestBean {
		private static int constructionCount = 0;

		public TestBeanImpl() {
			constructionCount++;
		}

		public void doSomething() {
		}
	}


	public static class TestInterceptor implements MethodInterceptor {
		private int invocationCount = 0;

		public Object invoke(MethodInvocation methodInvocation) throws Throwable {
			invocationCount++;
			return methodInvocation.proceed();
		}
	}

}
