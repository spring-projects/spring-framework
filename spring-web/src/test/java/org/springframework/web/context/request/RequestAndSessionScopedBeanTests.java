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

package org.springframework.web.context.request;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.*;
import org.junit.Test;

import org.springframework.beans.TestBean;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.StaticWebApplicationContext;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public class RequestAndSessionScopedBeanTests {

	@Test
	public void testPutBeanInRequest() throws Exception {
		String targetBeanName = "target";

		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.setScope(WebApplicationContext.SCOPE_REQUEST);
		bd.getPropertyValues().add("name", "abc");
		wac.registerBeanDefinition(targetBeanName, bd);
		wac.refresh();

		HttpServletRequest request = new MockHttpServletRequest();
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
		TestBean target = (TestBean) wac.getBean(targetBeanName);
		assertEquals("abc", target.getName());
		assertSame(target, request.getAttribute(targetBeanName));

		TestBean target2 = (TestBean) wac.getBean(targetBeanName);
		assertEquals("abc", target2.getName());
		assertSame(target2, target);
		assertSame(target2, request.getAttribute(targetBeanName));

		request = new MockHttpServletRequest();
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
		TestBean target3 = (TestBean) wac.getBean(targetBeanName);
		assertEquals("abc", target3.getName());
		assertSame(target3, request.getAttribute(targetBeanName));
		assertNotSame(target3, target);

		RequestContextHolder.setRequestAttributes(null);
		try {
			wac.getBean(targetBeanName);
			fail("Should have thrown BeanCreationException");
		}
		catch (BeanCreationException ex) {
			// expected
		}
	}

	@Test
	public void testPutBeanInSession() throws Exception {
		String targetBeanName = "target";
		HttpServletRequest request = new MockHttpServletRequest();
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.setScope(WebApplicationContext.SCOPE_SESSION);
		bd.getPropertyValues().add("name", "abc");
		wac.registerBeanDefinition(targetBeanName, bd);
		wac.refresh();

		TestBean target = (TestBean) wac.getBean(targetBeanName);
		assertEquals("abc", target.getName());
		assertSame(target, request.getSession().getAttribute(targetBeanName));

		RequestContextHolder.setRequestAttributes(null);
		try {
			wac.getBean(targetBeanName);
			fail("Should have thrown BeanCreationException");
		}
		catch (BeanCreationException ex) {
			// expected
		}


	}

}
