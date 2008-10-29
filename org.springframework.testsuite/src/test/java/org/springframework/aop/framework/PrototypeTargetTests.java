/*
 * Copyright 2002-2005 the original author or authors.
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

import junit.framework.TestCase;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Juergen Hoeller
 * @since 03.09.2004
 */
public class PrototypeTargetTests extends TestCase {

	public void testPrototypeProxyWithPrototypeTarget() {
		TestBeanImpl.constructionCount = 0;
		XmlBeanFactory xbf = new XmlBeanFactory(new ClassPathResource("prototypeTarget.xml", getClass()));
		for (int i = 0; i < 10; i++) {
			TestBean tb = (TestBean) xbf.getBean("testBeanPrototype");
			tb.doSomething();
		}
		TestInterceptor interceptor = (TestInterceptor) xbf.getBean("testInterceptor");
		assertEquals(10, TestBeanImpl.constructionCount);
		assertEquals(10, interceptor.invocationCount);
	}

	public void testSingletonProxyWithPrototypeTarget() {
		TestBeanImpl.constructionCount = 0;
		XmlBeanFactory xbf = new XmlBeanFactory(new ClassPathResource("prototypeTarget.xml", getClass()));
		for (int i = 0; i < 10; i++) {
			TestBean tb = (TestBean) xbf.getBean("testBeanSingleton");
			tb.doSomething();
		}
		TestInterceptor interceptor = (TestInterceptor) xbf.getBean("testInterceptor");
		assertEquals(1, TestBeanImpl.constructionCount);
		assertEquals(10, interceptor.invocationCount);
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
