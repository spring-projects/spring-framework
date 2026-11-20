/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.support;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import org.junit.jupiter.api.Test;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.testfixture.servlet.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test case for {@link AbstractDispatcherServletInitializer}.
 *
 * @author Arjen Poutsma
 */
class DispatcherServletInitializerTests {

	private static final String SERVLET_NAME = "myservlet";

	private static final String ROLE_NAME = "role";

	private static final String SERVLET_MAPPING = "/myservlet";


	private final MockServletContext servletContext = new MyMockServletContext();

	private final AbstractDispatcherServletInitializer initializer = new MyDispatcherServletInitializer();

	private final Map<String, Servlet> servlets = new LinkedHashMap<>(2);

	private final Map<String, MockServletRegistration> registrations = new LinkedHashMap<>(2);


	@Test
	void register() throws ServletException {
		initializer.onStartup(servletContext);

		assertThat(servlets).hasSize(1);
		assertThat(servlets.get(SERVLET_NAME)).isNotNull();

		DispatcherServlet servlet = (DispatcherServlet) servlets.get(SERVLET_NAME);
		assertThat(servlet.getClass()).isEqualTo(MyDispatcherServlet.class);
		WebApplicationContext servletContext = servlet.getWebApplicationContext();

		assertThat(servletContext.containsBean("bean")).isTrue();
		assertThat(servletContext.getBean("bean")).isInstanceOf(MyBean.class);

		assertThat(registrations).hasSize(1);
		assertThat(registrations.get(SERVLET_NAME)).isNotNull();

		MockServletRegistration registration = registrations.get(SERVLET_NAME);
		assertThat(registration.getMappings()).isEqualTo(Collections.singleton(SERVLET_MAPPING));
		assertThat(registration.getLoadOnStartup()).isEqualTo(1);
		assertThat(registration.getRunAsRole()).isEqualTo(ROLE_NAME);
	}


	private class MyMockServletContext extends MockServletContext {

		@Override
		public ServletRegistration.Dynamic addServlet(String servletName, Servlet servlet) {
			servlets.put(servletName, servlet);
			MockServletRegistration registration = new MockServletRegistration();
			registrations.put(servletName, registration);
			return registration;
		}
	}

	private static class MyDispatcherServletInitializer extends AbstractDispatcherServletInitializer {

		@Override
		protected String getServletName() {
			return SERVLET_NAME;
		}

		@Override
		protected DispatcherServlet createDispatcherServlet(WebApplicationContext servletAppContext) {
			return new MyDispatcherServlet(servletAppContext);
		}

		@Override
		protected WebApplicationContext createServletApplicationContext() {
			StaticWebApplicationContext servletContext = new StaticWebApplicationContext();
			servletContext.registerSingleton("bean", MyBean.class);
			return servletContext;
		}

		@Override
		protected String[] getServletMappings() {
			return new String[] { SERVLET_MAPPING };
		}

		@Override
		protected void customizeRegistration(ServletRegistration.Dynamic registration) {
			registration.setRunAsRole("role");
		}

		@Override
		protected WebApplicationContext createRootApplicationContext() {
			return null;
		}
	}

	private static class MyBean {
	}

	@SuppressWarnings("serial")
	private static class MyDispatcherServlet extends DispatcherServlet {
		public MyDispatcherServlet(WebApplicationContext webApplicationContext) {
			super(webApplicationContext);
		}
	}

}
