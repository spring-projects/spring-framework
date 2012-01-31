/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.web.portlet.context;

import javax.portlet.PortletContext;
import javax.portlet.PortletSession;
import javax.servlet.ServletContextEvent;

import static org.junit.Assert.*;
import org.junit.Test;

import org.springframework.beans.DerivedTestBean;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.mock.web.MockServletContext;
import org.springframework.mock.web.portlet.MockRenderRequest;
import org.springframework.mock.web.portlet.ServletWrappingPortletContext;
import org.springframework.web.context.ContextCleanupListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.support.GenericWebApplicationContext;

/**
 * @author Juergen Hoeller
 */
public class PortletApplicationContextScopeTests {

	private static final String NAME = "scoped";


	private ConfigurablePortletApplicationContext initApplicationContext(String scope) {
		MockServletContext sc = new MockServletContext();
		GenericWebApplicationContext rac = new GenericWebApplicationContext(sc);
		rac.refresh();
		PortletContext pc = new ServletWrappingPortletContext(sc);
		StaticPortletApplicationContext ac = new StaticPortletApplicationContext();
		ac.setParent(rac);
		ac.setPortletContext(pc);
		GenericBeanDefinition bd = new GenericBeanDefinition();
		bd.setBeanClass(DerivedTestBean.class);
		bd.setScope(scope);
		ac.registerBeanDefinition(NAME, bd);
		ac.refresh();
		return ac;
	}

	@Test
	public void testRequestScope() {
		WebApplicationContext ac = initApplicationContext(WebApplicationContext.SCOPE_REQUEST);
		MockRenderRequest request = new MockRenderRequest();
		PortletRequestAttributes requestAttributes = new PortletRequestAttributes(request);
		RequestContextHolder.setRequestAttributes(requestAttributes);
		try {
			assertNull(request.getAttribute(NAME));
			DerivedTestBean bean = ac.getBean(NAME, DerivedTestBean.class);
			assertSame(bean, request.getAttribute(NAME));
			assertSame(bean, ac.getBean(NAME));
			requestAttributes.requestCompleted();
			assertTrue(bean.wasDestroyed());
		}
		finally {
			RequestContextHolder.setRequestAttributes(null);
		}
	}

	@Test
	public void testSessionScope() {
		WebApplicationContext ac = initApplicationContext(WebApplicationContext.SCOPE_SESSION);
		MockRenderRequest request = new MockRenderRequest();
		PortletRequestAttributes requestAttributes = new PortletRequestAttributes(request);
		RequestContextHolder.setRequestAttributes(requestAttributes);
		try {
			assertNull(request.getPortletSession().getAttribute(NAME));
			DerivedTestBean bean = ac.getBean(NAME, DerivedTestBean.class);
			assertSame(bean, request.getPortletSession().getAttribute(NAME));
			assertSame(bean, ac.getBean(NAME));
			request.getPortletSession().invalidate();
			assertTrue(bean.wasDestroyed());
		}
		finally {
			RequestContextHolder.setRequestAttributes(null);
		}
	}

	@Test
	public void testGlobalSessionScope() {
		WebApplicationContext ac = initApplicationContext(WebApplicationContext.SCOPE_GLOBAL_SESSION);
		MockRenderRequest request = new MockRenderRequest();
		PortletRequestAttributes requestAttributes = new PortletRequestAttributes(request);
		RequestContextHolder.setRequestAttributes(requestAttributes);
		try {
			assertNull(request.getPortletSession().getAttribute(NAME, PortletSession.APPLICATION_SCOPE));
			DerivedTestBean bean = ac.getBean(NAME, DerivedTestBean.class);
			assertSame(bean, request.getPortletSession().getAttribute(NAME, PortletSession.APPLICATION_SCOPE));
			assertSame(bean, ac.getBean(NAME));
			request.getPortletSession().invalidate();
			assertTrue(bean.wasDestroyed());
		}
		finally {
			RequestContextHolder.setRequestAttributes(null);
		}
	}

	@Test
	public void testApplicationScope() {
		ConfigurablePortletApplicationContext ac = initApplicationContext(WebApplicationContext.SCOPE_APPLICATION);
		assertNull(ac.getPortletContext().getAttribute(NAME));
		DerivedTestBean bean = ac.getBean(NAME, DerivedTestBean.class);
		assertSame(bean, ac.getPortletContext().getAttribute(NAME));
		assertSame(bean, ac.getBean(NAME));
		new ContextCleanupListener().contextDestroyed(new ServletContextEvent(ac.getServletContext()));
		assertTrue(bean.wasDestroyed());
	}

}