/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.context.request;

import jakarta.servlet.ServletContextEvent;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.testfixture.beans.DerivedTestBean;
import org.springframework.web.context.ContextCleanupListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 */
class WebApplicationContextScopeTests {

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
	void requestScope() {
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
	void sessionScope() {
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
	void applicationScope() {
		WebApplicationContext ac = initApplicationContext(WebApplicationContext.SCOPE_APPLICATION);
		assertThat(ac.getServletContext().getAttribute(NAME)).isNull();
		DerivedTestBean bean = ac.getBean(NAME, DerivedTestBean.class);
		assertThat(ac.getServletContext().getAttribute(NAME)).isSameAs(bean);
		assertThat(ac.getBean(NAME)).isSameAs(bean);
		new ContextCleanupListener().contextDestroyed(new ServletContextEvent(ac.getServletContext()));
		assertThat(bean.wasDestroyed()).isTrue();
	}

}
