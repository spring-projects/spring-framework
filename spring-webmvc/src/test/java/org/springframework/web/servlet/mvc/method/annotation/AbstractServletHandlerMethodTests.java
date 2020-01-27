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

package org.springframework.web.servlet.mvc.method.annotation;

import javax.servlet.ServletException;

import org.junit.jupiter.api.AfterEach;

import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.lang.Nullable;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;
import org.springframework.web.testfixture.servlet.MockServletConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for tests using on the DispatcherServlet and HandlerMethod infrastructure classes:
 * <ul>
 * <li>RequestMappingHandlerMapping
 * <li>RequestMappingHandlerAdapter
 * <li>ExceptionHandlerExceptionResolver
 * </ul>
 *
 * @author Rossen Stoyanchev
 */
public abstract class AbstractServletHandlerMethodTests {

	private DispatcherServlet servlet;


	protected DispatcherServlet getServlet() {
		assertThat(servlet).as("DispatcherServlet not initialized").isNotNull();
		return servlet;
	}

	@AfterEach
	public void tearDown() {
		this.servlet = null;
	}

	/**
	 * Initialize a DispatcherServlet instance registering zero or more controller classes.
	 */
	protected WebApplicationContext initServletWithControllers(final Class<?>... controllerClasses)
			throws ServletException {

		return initServlet(null, controllerClasses);
	}

	/**
	 * Initialize a DispatcherServlet instance registering zero or more controller classes
	 * and also providing additional bean definitions through a callback.
	 */
	@SuppressWarnings("serial")
	protected WebApplicationContext initServlet(
			final ApplicationContextInitializer<GenericWebApplicationContext> initializer,
			final Class<?>... controllerClasses) throws ServletException {

		final GenericWebApplicationContext wac = new GenericWebApplicationContext();

		servlet = new DispatcherServlet() {
			@Override
			protected WebApplicationContext createWebApplicationContext(@Nullable WebApplicationContext parent) {
				for (Class<?> clazz : controllerClasses) {
					wac.registerBeanDefinition(clazz.getSimpleName(), new RootBeanDefinition(clazz));
				}

				RootBeanDefinition mappingDef = new RootBeanDefinition(RequestMappingHandlerMapping.class);
				mappingDef.getPropertyValues().add("removeSemicolonContent", "false");
				wac.registerBeanDefinition("handlerMapping", mappingDef);
				wac.registerBeanDefinition("handlerAdapter",
						new RootBeanDefinition(RequestMappingHandlerAdapter.class));
				wac.registerBeanDefinition("requestMappingResolver",
						new RootBeanDefinition(ExceptionHandlerExceptionResolver.class));
				wac.registerBeanDefinition("responseStatusResolver",
						new RootBeanDefinition(ResponseStatusExceptionResolver.class));
				wac.registerBeanDefinition("defaultResolver",
						new RootBeanDefinition(DefaultHandlerExceptionResolver.class));

				if (initializer != null) {
					initializer.initialize(wac);
				}

				wac.refresh();
				return wac;
			}
		};

		servlet.init(new MockServletConfig());

		return wac;
	}

}
