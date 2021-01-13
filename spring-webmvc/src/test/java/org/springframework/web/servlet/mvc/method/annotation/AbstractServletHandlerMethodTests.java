/*
 * Copyright 2002-2020 the original author or authors.
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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.lang.Nullable;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;
import org.springframework.web.testfixture.servlet.MockServletConfig;
import org.springframework.web.util.pattern.PathPatternParser;

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

	@Nullable
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
	protected WebApplicationContext initDispatcherServlet(
			Class<?> controllerClass, boolean usePathPatterns) throws ServletException {

		return initDispatcherServlet(controllerClass, usePathPatterns, null);
	}

	@SuppressWarnings("serial")
	WebApplicationContext initDispatcherServlet(
			@Nullable Class<?> controllerClass, boolean usePathPatterns,
			@Nullable ApplicationContextInitializer<GenericWebApplicationContext> initializer)
			throws ServletException {

		final GenericWebApplicationContext wac = new GenericWebApplicationContext();

		servlet = new DispatcherServlet() {

			@Override
			protected WebApplicationContext createWebApplicationContext(@Nullable WebApplicationContext parent) {

				if (controllerClass != null) {
					wac.registerBeanDefinition(
							controllerClass.getSimpleName(), new RootBeanDefinition(controllerClass));
				}

				if (initializer != null) {
					initializer.initialize(wac);
				}

				if (!wac.containsBeanDefinition("handlerMapping")) {
					BeanDefinition def = register("handlerMapping", RequestMappingHandlerMapping.class, wac);
					def.getPropertyValues().add("removeSemicolonContent", "false");
				}

				BeanDefinition mappingDef = wac.getBeanDefinition("handlerMapping");
				if (usePathPatterns && !mappingDef.hasAttribute("patternParser")) {
					BeanDefinition parserDef = register("parser", PathPatternParser.class, wac);
					mappingDef.getPropertyValues().add("patternParser", parserDef);
				}

				register("handlerAdapter", RequestMappingHandlerAdapter.class, wac);
				register("requestMappingResolver", ExceptionHandlerExceptionResolver.class, wac);
				register("responseStatusResolver", ResponseStatusExceptionResolver.class, wac);
				register("defaultResolver", DefaultHandlerExceptionResolver.class, wac);

				wac.refresh();
				return wac;
			}
		};

		servlet.init(new MockServletConfig());

		return wac;
	}

	private BeanDefinition register(String beanName, Class<?> beanType, GenericWebApplicationContext wac) {
		if (wac.containsBeanDefinition(beanName)) {
			return wac.getBeanDefinition(beanName);
		}
		RootBeanDefinition beanDef = new RootBeanDefinition(beanType);
		wac.registerBeanDefinition(beanName, beanDef);
		return beanDef;
	}

}
