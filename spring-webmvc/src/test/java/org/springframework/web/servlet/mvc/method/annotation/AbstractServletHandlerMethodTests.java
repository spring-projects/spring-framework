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

package org.springframework.web.servlet.mvc.method.annotation;

import static org.junit.Assert.assertNotNull;

import javax.servlet.ServletException;

import org.junit.After;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.mock.web.test.MockServletConfig;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;

/**
 * Base class for tests using on the DispatcherServlet and HandlerMethod infrastructure classes:
 * <ul>
 * 	<li>RequestMappingHandlerMapping
 * 	<li>RequestMappingHandlerAdapter
 * 	<li>ExceptionHandlerExceptionResolver
 * </ul>
 *
 * @author Rossen Stoyanchev
 */
public abstract class AbstractServletHandlerMethodTests {

	private DispatcherServlet servlet;

	@After
	public void tearDown() {
		this.servlet = null;
	}

	protected DispatcherServlet getServlet() {
		assertNotNull("DispatcherServlet not initialized", servlet);
		return servlet;
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
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				for (Class<?> clazz : controllerClasses) {
					wac.registerBeanDefinition(clazz.getSimpleName(), new RootBeanDefinition(clazz));
				}

				Class<?> mappingType = RequestMappingHandlerMapping.class;
				RootBeanDefinition beanDef = new RootBeanDefinition(mappingType);
				beanDef.getPropertyValues().add("removeSemicolonContent", "false");
				wac.registerBeanDefinition("handlerMapping", beanDef);

				Class<?> adapterType = RequestMappingHandlerAdapter.class;
				wac.registerBeanDefinition("handlerAdapter", new RootBeanDefinition(adapterType));

				Class<?> resolverType = ExceptionHandlerExceptionResolver.class;
				wac.registerBeanDefinition("requestMappingResolver", new RootBeanDefinition(resolverType));

				resolverType = ResponseStatusExceptionResolver.class;
				wac.registerBeanDefinition("responseStatusResolver", new RootBeanDefinition(resolverType));

				resolverType = DefaultHandlerExceptionResolver.class;
				wac.registerBeanDefinition("defaultResolver", new RootBeanDefinition(resolverType));

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
