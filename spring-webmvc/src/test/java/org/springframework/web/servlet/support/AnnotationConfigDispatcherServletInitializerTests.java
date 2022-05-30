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

package org.springframework.web.servlet.support;

import java.util.Collections;
import java.util.EnumSet;
import java.util.EventListener;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration.Dynamic;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.filter.HiddenHttpMethodFilter;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.testfixture.servlet.MockServletConfig;
import org.springframework.web.testfixture.servlet.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test case for {@link AbstractAnnotationConfigDispatcherServletInitializer}.
 *
 * @author Arjen Poutsma
 */
public class AnnotationConfigDispatcherServletInitializerTests {

	private static final String SERVLET_NAME = "myservlet";

	private static final String ROLE_NAME = "role";

	private static final String SERVLET_MAPPING = "/myservlet";

	private AbstractDispatcherServletInitializer initializer;

	private MockServletContext servletContext;

	private Map<String, Servlet> servlets;

	private Map<String, MockServletRegistration> servletRegistrations;

	private Map<String, Filter> filters;

	private Map<String, MockFilterRegistration> filterRegistrations;


	@BeforeEach
	public void setUp() throws Exception {
		servletContext = new MyMockServletContext();
		initializer = new MyAnnotationConfigDispatcherServletInitializer();
		servlets = new LinkedHashMap<>(1);
		servletRegistrations = new LinkedHashMap<>(1);
		filters = new LinkedHashMap<>(1);
		filterRegistrations = new LinkedHashMap<>();
	}

	@Test
	public void register() throws ServletException {
		initializer.onStartup(servletContext);

		assertThat(servlets.size()).isEqualTo(1);
		assertThat(servlets.get(SERVLET_NAME)).isNotNull();

		DispatcherServlet servlet = (DispatcherServlet) servlets.get(SERVLET_NAME);
		WebApplicationContext wac = servlet.getWebApplicationContext();
		((AnnotationConfigWebApplicationContext) wac).refresh();

		assertThat(wac.containsBean("bean")).isTrue();
		boolean condition = wac.getBean("bean") instanceof MyBean;
		assertThat(condition).isTrue();

		assertThat(servletRegistrations.size()).isEqualTo(1);
		assertThat(servletRegistrations.get(SERVLET_NAME)).isNotNull();

		MockServletRegistration servletRegistration = servletRegistrations.get(SERVLET_NAME);

		assertThat(servletRegistration.getMappings()).isEqualTo(Collections.singleton(SERVLET_MAPPING));
		assertThat(servletRegistration.getLoadOnStartup()).isEqualTo(1);
		assertThat(servletRegistration.getRunAsRole()).isEqualTo(ROLE_NAME);
		assertThat(servletRegistration.isAsyncSupported()).isTrue();

		assertThat(filterRegistrations.size()).isEqualTo(4);
		assertThat(filterRegistrations.get("hiddenHttpMethodFilter")).isNotNull();
		assertThat(filterRegistrations.get("delegatingFilterProxy")).isNotNull();
		assertThat(filterRegistrations.get("delegatingFilterProxy#0")).isNotNull();
		assertThat(filterRegistrations.get("delegatingFilterProxy#1")).isNotNull();

		for (MockFilterRegistration filterRegistration : filterRegistrations.values()) {
			assertThat(filterRegistration.isAsyncSupported()).isTrue();
			EnumSet<DispatcherType> enumSet = EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD,
					DispatcherType.INCLUDE, DispatcherType.ASYNC);
			assertThat(filterRegistration.getMappings().get(SERVLET_NAME)).isEqualTo(enumSet);
		}

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
		assertThat(servletRegistration.isAsyncSupported()).isFalse();

		for (MockFilterRegistration filterRegistration : filterRegistrations.values()) {
			assertThat(filterRegistration.isAsyncSupported()).isFalse();
			assertThat(filterRegistration.getMappings().get(SERVLET_NAME)).isEqualTo(EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.INCLUDE));
		}
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

		assertThat(wac.containsBean("bean")).isTrue();
		boolean condition = wac.getBean("bean") instanceof MyBean;
		assertThat(condition).isTrue();
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

		assertThat(filterRegistrations.size()).isEqualTo(0);
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
			if (servlets.containsKey(servletName)) {
				return null;
			}
			servlets.put(servletName, servlet);
			MockServletRegistration registration = new MockServletRegistration();
			servletRegistrations.put(servletName, registration);
			return registration;
		}

		@Override
		public Dynamic addFilter(String filterName, Filter filter) {
			if (filters.containsKey(filterName)) {
				return null;
			}
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
			return new Class<?>[] {MyConfiguration.class};
		}

		@Override
		protected String[] getServletMappings() {
			return new String[]{"/myservlet"};
		}

		@Override
		protected Filter[] getServletFilters() {
			return new Filter[] {
					new HiddenHttpMethodFilter(),
					new DelegatingFilterProxy("a"),
					new DelegatingFilterProxy("b"),
					new DelegatingFilterProxy("c")
			};
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


	public static class MyBean {
	}


	@Configuration
	public static class MyConfiguration {

		@Bean
		public MyBean bean() {
			return new MyBean();
		}
	}

}
