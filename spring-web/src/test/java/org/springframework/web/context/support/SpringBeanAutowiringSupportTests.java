/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.web.context.support;

import org.junit.Test;

import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 */
public class SpringBeanAutowiringSupportTests {

	@Test
	public void testProcessInjectionBasedOnServletContext() {
		MockServletContext sc = new MockServletContext();
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("name", "tb");
		wac.registerSingleton("testBean", TestBean.class, pvs);
		wac.setServletContext(sc);
		wac.refresh();
		sc.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		InjectionTarget target = new InjectionTarget();
		SpringBeanAutowiringSupport.processInjectionBasedOnServletContext(target, sc);
		assertTrue(target.testBean instanceof TestBean);
		assertEquals("tb", target.name);
	}


	public static class InjectionTarget {

		@Autowired
		public ITestBean testBean;

		@Value("#{testBean.name}")
		public String name;
	}

}
