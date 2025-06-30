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

package org.springframework.aop.interceptor;

import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.NamedBean;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rod Johnson
 * @author Chris Beams
 */
class ExposeBeanNameAdvisorsTests {

	private static class RequiresBeanNameBoundTestBean extends TestBean {
		private final String beanName;

		public RequiresBeanNameBoundTestBean(String beanName) {
			this.beanName = beanName;
		}

		@Override
		public int getAge() {
			assertThat(ExposeBeanNameAdvisors.getBeanName()).isEqualTo(beanName);
			return super.getAge();
		}
	}

	@Test
	void testNoIntroduction() {
		String beanName = "foo";
		TestBean target = new RequiresBeanNameBoundTestBean(beanName);
		ProxyFactory pf = new ProxyFactory(target);
		pf.addAdvisor(ExposeInvocationInterceptor.ADVISOR);
		pf.addAdvisor(ExposeBeanNameAdvisors.createAdvisorWithoutIntroduction(beanName));
		ITestBean proxy = (ITestBean) pf.getProxy();

		boolean condition = proxy instanceof NamedBean;
		assertThat(condition).as("No introduction").isFalse();
		// Requires binding
		proxy.getAge();
	}

	@Test
	void testWithIntroduction() {
		String beanName = "foo";
		TestBean target = new RequiresBeanNameBoundTestBean(beanName);
		ProxyFactory pf = new ProxyFactory(target);
		pf.addAdvisor(ExposeInvocationInterceptor.ADVISOR);
		pf.addAdvisor(ExposeBeanNameAdvisors.createAdvisorIntroducingNamedBean(beanName));
		ITestBean proxy = (ITestBean) pf.getProxy();

		boolean condition = proxy instanceof NamedBean;
		assertThat(condition).as("Introduction was made").isTrue();
		// Requires binding
		proxy.getAge();

		NamedBean nb = (NamedBean) proxy;
		assertThat(nb.getBeanName()).as("Name returned correctly").isEqualTo(beanName);
	}

}
