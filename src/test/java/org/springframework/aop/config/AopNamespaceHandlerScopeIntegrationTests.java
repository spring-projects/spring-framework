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

package org.springframework.aop.config;

import org.junit.Before;
import org.junit.Test;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.util.ClassUtils;
import org.springframework.util.SerializationTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.support.XmlWebApplicationContext;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for scoped proxy use in conjunction with aop: namespace.
 * Deemed an integration test because .web mocks and application contexts are required.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Chris Beams
 * @see org.springframework.aop.config.AopNamespaceHandlerTests
 */
public class AopNamespaceHandlerScopeIntegrationTests {

	private static final String CONTEXT = format("classpath:%s-context.xml",
			ClassUtils.convertClassNameToResourcePath(AopNamespaceHandlerScopeIntegrationTests.class.getName()));

	private ApplicationContext context;


	@Before
	public void setUp() {
		XmlWebApplicationContext wac = new XmlWebApplicationContext();
		wac.setConfigLocations(CONTEXT);
		wac.refresh();
		this.context = wac;
	}


	@Test
	public void testSingletonScoping() throws Exception {
		ITestBean scoped = (ITestBean) this.context.getBean("singletonScoped");
		assertThat(AopUtils.isAopProxy(scoped)).as("Should be AOP proxy").isTrue();
		boolean condition = scoped instanceof TestBean;
		assertThat(condition).as("Should be target class proxy").isTrue();
		String rob = "Rob Harrop";
		String bram = "Bram Smeets";
		assertThat(scoped.getName()).isEqualTo(rob);
		scoped.setName(bram);
		assertThat(scoped.getName()).isEqualTo(bram);
		ITestBean deserialized = (ITestBean) SerializationTestUtils.serializeAndDeserialize(scoped);
		assertThat(deserialized.getName()).isEqualTo(bram);
	}

	@Test
	public void testRequestScoping() throws Exception {
		MockHttpServletRequest oldRequest = new MockHttpServletRequest();
		MockHttpServletRequest newRequest = new MockHttpServletRequest();

		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(oldRequest));

		ITestBean scoped = (ITestBean) this.context.getBean("requestScoped");
		assertThat(AopUtils.isAopProxy(scoped)).as("Should be AOP proxy").isTrue();
		boolean condition = scoped instanceof TestBean;
		assertThat(condition).as("Should be target class proxy").isTrue();

		ITestBean testBean = (ITestBean) this.context.getBean("testBean");
		assertThat(AopUtils.isAopProxy(testBean)).as("Should be AOP proxy").isTrue();
		boolean condition1 = testBean instanceof TestBean;
		assertThat(condition1).as("Regular bean should be JDK proxy").isFalse();

		String rob = "Rob Harrop";
		String bram = "Bram Smeets";

		assertThat(scoped.getName()).isEqualTo(rob);
		scoped.setName(bram);
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(newRequest));
		assertThat(scoped.getName()).isEqualTo(rob);
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(oldRequest));
		assertThat(scoped.getName()).isEqualTo(bram);

		assertThat(((Advised) scoped).getAdvisors().length > 0).as("Should have advisors").isTrue();
	}

	@Test
	public void testSessionScoping() throws Exception {
		MockHttpSession oldSession = new MockHttpSession();
		MockHttpSession newSession = new MockHttpSession();

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setSession(oldSession);
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

		ITestBean scoped = (ITestBean) this.context.getBean("sessionScoped");
		assertThat(AopUtils.isAopProxy(scoped)).as("Should be AOP proxy").isTrue();
		boolean condition1 = scoped instanceof TestBean;
		assertThat(condition1).as("Should not be target class proxy").isFalse();

		ITestBean scopedAlias = (ITestBean) this.context.getBean("sessionScopedAlias");
		assertThat(scopedAlias).isSameAs(scoped);

		ITestBean testBean = (ITestBean) this.context.getBean("testBean");
		assertThat(AopUtils.isAopProxy(testBean)).as("Should be AOP proxy").isTrue();
		boolean condition = testBean instanceof TestBean;
		assertThat(condition).as("Regular bean should be JDK proxy").isFalse();

		String rob = "Rob Harrop";
		String bram = "Bram Smeets";

		assertThat(scoped.getName()).isEqualTo(rob);
		scoped.setName(bram);
		request.setSession(newSession);
		assertThat(scoped.getName()).isEqualTo(rob);
		request.setSession(oldSession);
		assertThat(scoped.getName()).isEqualTo(bram);

		assertThat(((Advised) scoped).getAdvisors().length > 0).as("Should have advisors").isTrue();
	}

}
