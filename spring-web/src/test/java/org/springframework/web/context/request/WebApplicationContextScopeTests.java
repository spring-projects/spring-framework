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

package org.springframework.web.context.request;

import javax.servlet.ServletContextEvent;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.tests.sample.beans.DerivedTestBean;
import org.springframework.web.context.ContextCleanupListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 */
public class WebApplicationContextScopeTests {

	private static final String NAME = "scoped";


	private WebApplicationContext initApplicationContext(String scope) {
		MockServletContext sc = new MockServletContext();
		GenericWebApplicationContext ac = new GenericWebApplicationContext(sc);
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
		MockHttpServletRequest request = new MockHttpServletRequest();
		ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
		RequestContextHolder.setRequestAttributes(requestAttributes);
		try {
			assertThat(request.getAttribute(NAME)).isNull();
			DerivedTestBean bean = ac.getBean(NAME, DerivedTestBean.class);
			assertThat(request.getAttribute(NAME)).isSameAs(bean);
			assertThat(ac.getBean(NAME)).isSameAs(bean);
			requestAttributes.requestCompleted();
			assertThat(bean.wasDestroyed()).isTrue();
		}
		finally {
			RequestContextHolder.setRequestAttributes(null);
		}
	}

	@Test
	public void testSessionScope() {
		WebApplicationContext ac = initApplicationContext(WebApplicationContext.SCOPE_SESSION);
		MockHttpServletRequest request = new MockHttpServletRequest();
		ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
		RequestContextHolder.setRequestAttributes(requestAttributes);
		try {
			assertThat(request.getSession().getAttribute(NAME)).isNull();
			DerivedTestBean bean = ac.getBean(NAME, DerivedTestBean.class);
			assertThat(request.getSession().getAttribute(NAME)).isSameAs(bean);
			assertThat(ac.getBean(NAME)).isSameAs(bean);
			request.getSession().invalidate();
			assertThat(bean.wasDestroyed()).isTrue();
		}
		finally {
			RequestContextHolder.setRequestAttributes(null);
		}
	}

	@Test
	public void testApplicationScope() {
		WebApplicationContext ac = initApplicationContext(WebApplicationContext.SCOPE_APPLICATION);
		assertThat(ac.getServletContext().getAttribute(NAME)).isNull();
		DerivedTestBean bean = ac.getBean(NAME, DerivedTestBean.class);
		assertThat(ac.getServletContext().getAttribute(NAME)).isSameAs(bean);
		assertThat(ac.getBean(NAME)).isSameAs(bean);
		new ContextCleanupListener().contextDestroyed(new ServletContextEvent(ac.getServletContext()));
		assertThat(bean.wasDestroyed()).isTrue();
	}

}
