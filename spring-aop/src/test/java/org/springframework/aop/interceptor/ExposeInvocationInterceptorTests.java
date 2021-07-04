/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.aop.interceptor;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.core.testfixture.io.ResourceTestUtils.qualifiedResource;

/**
 * Non-XML tests are in AbstractAopProxyTests
 *
 * @author Rod Johnson
 * @author Chris Beams
 */
public class ExposeInvocationInterceptorTests {

	@Test
	public void testXmlConfig() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
				qualifiedResource(ExposeInvocationInterceptorTests.class, "context.xml"));
		ITestBean tb = (ITestBean) bf.getBean("proxy");
		String name = "tony";
		tb.setName(name);
		// Fires context checks
		assertThat(tb.getName()).isEqualTo(name);
	}

}


abstract class ExposedInvocationTestBean extends TestBean {

	@Override
	public String getName() {
		MethodInvocation invocation = ExposeInvocationInterceptor.currentInvocation();
		assertions(invocation);
		return super.getName();
	}

	@Override
	public void absquatulate() {
		MethodInvocation invocation = ExposeInvocationInterceptor.currentInvocation();
		assertions(invocation);
		super.absquatulate();
	}

	protected abstract void assertions(MethodInvocation invocation);
}


class InvocationCheckExposedInvocationTestBean extends ExposedInvocationTestBean {

	@Override
	protected void assertions(MethodInvocation invocation) {
		assertThat(invocation.getThis() == this).isTrue();
		assertThat(ITestBean.class.isAssignableFrom(invocation.getMethod().getDeclaringClass())).as("Invocation should be on ITestBean: " + invocation.getMethod()).isTrue();
	}
}
