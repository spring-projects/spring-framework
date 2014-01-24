/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.support;

import java.util.Collections;
import java.util.EnumSet;
import java.util.EventListener;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration.Dynamic;
import javax.servlet.Servlet;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.junit.Before;
import org.junit.Test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.test.MockServletConfig;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.HiddenHttpMethodFilter;
import org.springframework.web.servlet.DispatcherServlet;

import static org.junit.Assert.*;

/**
 * Test case for {@link AbstractAnnotationConfigDispatcherServletInitializer}.
 *
 * @author Arjen Poutsma
 */
public class AnnotationConfigDispatcherServletInitializerTests {

	private static final String SERVLET_NAME = "myservlet";

	private static final String FILTER_NAME = "hiddenHttpMethodFilter";

	private static final String ROLE_NAME = "role";

	private static final String SERVLET_MAPPING = "/myservlet";

	private AbstractDispatcherServletInitializer initializer;

	private MockServletContext servletContext;

	private Map<String, Servlet> servlets;

	private Map<String, MockServletRegistration> servletRegistrations;

	private Map<String, Filter> filters;

	private Map<String, MockFilterRegistration> filterRegistrations;


	@Before
	public void setUp() throws Exception {
		servletContext = new MyMockServletContext();
		initializer = new MyAnnotationConfigDispatcherServletInitializer();
		servlets = new LinkedHashMap<String, Servlet>(1);
		servletRegistrations = new LinkedHashMap<String, MockServletRegistration>(1);
		filters = new LinkedHashMap<String, Filter>(1);
		filterRegistrations = new LinkedHashMap<String, MockFilterRegistration>();
	}

	@Test
	public void register() throws ServletException {
		initializer.onStartup(servletContext);

		assertEquals(1, servlets.size());
		assertNotNull(servlets.get(SERVLET_NAME));

		DispatcherServlet servlet = (DispatcherServlet) servlets.get(SERVLET_NAME);
		WebApplicationContext wac = servlet.getWebApplicationContext();
		((AnnotationConfigWebApplicationContext) wac).refresh();

		assertTrue(wac.containsBean("bean"));
		assertTrue(wac.getBean("bean") instanceof MyBean);

		assertEquals(1, servletRegistrations.size());
		assertNotNull(servletRegistrations.get(SERVLET_NAME));

		MockServletRegistration servletRegistration = servletRegistrations.get(SERVLET_NAME);

		assertEquals(Collections.singleton(SERVLET_MAPPING), servletRegistration.getMappings());
		assertEquals(1, servletRegistration.getLoadOnStartup());
		assertEquals(ROLE_NAME, servletRegistration.getRunAsRole());
		assertTrue(servletRegistration.isAsyncSupported());

		assertEquals(1, filterRegistrations.size());
		assertNotNull(filterRegistrations.get(FILTER_NAME));

		MockFilterRegistration filterRegistration = filterRegistrations.get(FILTER_NAME);

		assertTrue(filterRegistration.isAsyncSupported());
		assertEquals(EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.INCLUDE, DispatcherType.ASYNC),
				filterRegistration.getMappings().get(SERVLET_NAME));
	}

	@Test
	public void asyncSupportedFalse() throws ServletException {
		initializer = new MyAnnotationConfigDispatcherServletInitializer() {
			@Override
			protected boolean isAsyncSupported() {
				return false;
			}
		};

		initializer.onStartup(servletContext);

		MockServletRegistration servletRegistration = servletRegistrations.get(SERVLET_NAME);
		assertFalse(servletRegistration.isAsyncSupported());

		MockFilterRegistration filterRegistration = filterRegistrations.get(FILTER_NAME);
		assertFalse(filterRegistration.isAsyncSupported());
		assertEquals(EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.INCLUDE),
				filterRegistration.getMappings().get(SERVLET_NAME));
	}

	// SPR-11357
	@Test
	public void rootContextOnly() throws ServletException {
		initializer = new MyAnnotationConfigDispatcherServletInitializer() {
			@Override
			protected Class<?>[] getRootConfigClasses() {
				return new Class<?>[] {MyConfiguration.class};
			}
			@Override
			protected Class<?>[] getServletConfigClasses() {
				return null;
			}
		};

		initializer.onStartup(servletContext);

		DispatcherServlet servlet = (DispatcherServlet) servlets.get(SERVLET_NAME);
		servlet.init(new MockServletConfig(this.servletContext));

		WebApplicationContext wac = servlet.getWebApplicationContext();
		((AnnotationConfigWebApplicationContext) wac).refresh();

		assertTrue(wac.containsBean("bean"));
		assertTrue(wac.getBean("bean") instanceof MyBean);
	}

	@Test
	public void noFilters() throws ServletException {
		initializer = new MyAnnotationConfigDispatcherServletInitializer() {
			@Override
			protected Filter[] getServletFilters() {
				return null;
			}
		};

		initializer.onStartup(servletContext);

		assertEquals(0, filterRegistrations.size());
	}


	private class MyMockServletContext extends MockServletContext {

		@Override
		public <T extends EventListener> void addListener(T t) {
			if (t instanceof ServletContextListener) {
				((ServletContextListener) t).contextInitialized(new ServletContextEvent(this));
			}
		}

		@Override
		public ServletRegistration.Dynamic addServlet(String servletName, Servlet servlet) {
			servlets.put(servletName, servlet);
			MockServletRegistration registration = new MockServletRegistration();
			servletRegistrations.put(servletName, registration);
			return registration;
		}

		@Override
		public Dynamic addFilter(String filterName, Filter filter) {
			filters.put(filterName, filter);
			MockFilterRegistration registration = new MockFilterRegistration();
			filterRegistrations.put(filterName, registration);
			return registration;
		}
	}


	private static class MyAnnotationConfigDispatcherServletInitializer
			extends AbstractAnnotationConfigDispatcherServletInitializer {

		@Override
		protected String getServletName() {
			return SERVLET_NAME;
		}

		@Override
		protected Class<?>[] getServletConfigClasses() {
			return new Class[]{MyConfiguration.class};
		}

		@Override
		protected String[] getServletMappings() {
			return new String[]{"/myservlet"};
		}

		@Override
		protected Filter[] getServletFilters() {
			return new Filter[] { new HiddenHttpMethodFilter() };
		}

		@Override
		protected void customizeRegistration(ServletRegistration.Dynamic registration) {
			registration.setRunAsRole("role");
		}

		@Override
		protected Class<?>[] getRootConfigClasses() {
			return null;
		}
	}


	private static class MyBean {
	}


	@Configuration
	@SuppressWarnings("unused")
	private static class MyConfiguration {

		public MyConfiguration() {
		}

		@Bean
		public MyBean bean() {
			return new MyBean();
		}
	}

}
