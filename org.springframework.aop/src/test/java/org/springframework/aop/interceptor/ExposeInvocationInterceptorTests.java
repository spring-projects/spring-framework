/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.aop.interceptor;

import static org.junit.Assert.assertEquals;
import junit.framework.TestCase;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.Test;
import org.springframework.beans.ITestBean;
import org.springframework.beans.TestBean;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;

/**
 * Non-XML tests are in AbstractAopProxyTests
 * 
 * @author Rod Johnson
 * @author Chris Beams
 */
public class ExposeInvocationInterceptorTests {
	
	private static final Class<?> CLASS = ExposeInvocationInterceptorTests.class;
	private static final ClassPathResource CONTEXT =
		new ClassPathResource(CLASS.getSimpleName() + "-context.xml", CLASS);

	@Test
	public void testXmlConfig() {
		XmlBeanFactory bf = new XmlBeanFactory(CONTEXT);
		ITestBean tb = (ITestBean) bf.getBean("proxy");
		String name= "tony";
		tb.setName(name);
		// Fires context checks
		assertEquals(name, tb.getName());
	}

}


abstract class ExposedInvocationTestBean extends TestBean {

	public String getName() {
		MethodInvocation invocation = ExposeInvocationInterceptor.currentInvocation();
		assertions(invocation);
		return super.getName();
	}

	public void absquatulate() {
		MethodInvocation invocation = ExposeInvocationInterceptor.currentInvocation();
		assertions(invocation);
		super.absquatulate();
	}
	
	protected abstract void assertions(MethodInvocation invocation);
}


class InvocationCheckExposedInvocationTestBean extends ExposedInvocationTestBean {
	protected void assertions(MethodInvocation invocation) {
		TestCase.assertTrue(invocation.getThis() == this);
		TestCase.assertTrue("Invocation should be on ITestBean: " + invocation.getMethod(), 
				ITestBean.class.isAssignableFrom(invocation.getMethod().getDeclaringClass()));
	}
}
