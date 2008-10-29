/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.aop.config;

import junit.framework.TestCase;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.ITestBean;
import org.springframework.beans.TestBean;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.support.XmlWebApplicationContext;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public class AopNamespaceHandlerScopeTests extends TestCase {

	private ApplicationContext context;

	public void setUp() {
		XmlWebApplicationContext wac = new XmlWebApplicationContext();
		wac.setConfigLocations(new String[] {"classpath:org/springframework/aop/config/aopNamespaceHandlerScopeTests.xml"});
		wac.refresh();
		this.context = wac;
	}

	public void testRequestScoping() throws Exception {
		MockHttpServletRequest oldRequest = new MockHttpServletRequest();
		MockHttpServletRequest newRequest = new MockHttpServletRequest();

		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(oldRequest));

		ITestBean scoped = (ITestBean) this.context.getBean("requestScoped");
		assertTrue("Should be AOP proxy", AopUtils.isAopProxy(scoped));
		assertTrue("Should be target class proxy", scoped instanceof TestBean);

		ITestBean testBean = (ITestBean) this.context.getBean("testBean");
		assertTrue("Should be AOP proxy", AopUtils.isAopProxy(testBean));
		assertFalse("Regular bean should be JDK proxy", testBean instanceof TestBean);

		String rob = "Rob Harrop";
		String bram = "Bram Smeets";

		assertEquals(rob, scoped.getName());
		scoped.setName(bram);
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(newRequest));
		assertEquals(rob, scoped.getName());
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(oldRequest));
		assertEquals(bram, scoped.getName());

		assertTrue("Should have advisors", ((Advised) scoped).getAdvisors().length > 0);
	}

	public void testSessionScoping() throws Exception {
		MockHttpSession oldSession = new MockHttpSession();
		MockHttpSession newSession = new MockHttpSession();

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setSession(oldSession);
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

		ITestBean scoped = (ITestBean) this.context.getBean("sessionScoped");
		assertTrue("Should be AOP proxy", AopUtils.isAopProxy(scoped));
		assertFalse("Should not be target class proxy", scoped instanceof TestBean);

		ITestBean scopedAlias = (ITestBean) this.context.getBean("sessionScopedAlias");
		assertSame(scoped, scopedAlias);

		ITestBean testBean = (ITestBean) this.context.getBean("testBean");
		assertTrue("Should be AOP proxy", AopUtils.isAopProxy(testBean));
		assertFalse("Regular bean should be JDK proxy", testBean instanceof TestBean);

		String rob = "Rob Harrop";
		String bram = "Bram Smeets";

		assertEquals(rob, scoped.getName());
		scoped.setName(bram);
		request.setSession(newSession);
		assertEquals(rob, scoped.getName());
		request.setSession(oldSession);
		assertEquals(bram, scoped.getName());

		assertTrue("Should have advisors", ((Advised) scoped).getAdvisors().length > 0);
	}

}
