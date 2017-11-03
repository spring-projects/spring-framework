/*
 * Copyright 2002-2017 the original author or authors.
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
package org.springframework.web.server.adapter;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ServletHttpHandlerAdapter;
import org.springframework.util.Assert;
import org.springframework.web.WebApplicationInitializer;

/**
 * Base class for a {@link org.springframework.web.WebApplicationInitializer}
 * that installs a Spring Reactive Web Application on a Servlet container.
 *
 * <p>Spring configuration is loaded and given to
 * {@link WebHttpHandlerBuilder#applicationContext WebHttpHandlerBuilder}
 * which scans the context looking for specific beans and creates a reactive
 * {@link HttpHandler}. The resulting handler is installed as a Servlet through
 * the {@link ServletHttpHandlerAdapter}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0.2
 */
public abstract class AbstractReactiveWebInitializer implements WebApplicationInitializer {

	/**
	 * The default servlet name to use. See {@link #getServletName}.
	 */
	public static final String DEFAULT_SERVLET_NAME = "http-handler-adapter";


	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		String servletName = getServletName();
		Assert.hasLength(servletName, "getServletName() must not return empty or null");

		ApplicationContext applicationContext = createApplicationContext();
		Assert.notNull(applicationContext, "createApplicationContext() must not return null.");

		HttpHandler httpHandler = WebHttpHandlerBuilder.applicationContext(applicationContext).build();
		ServletHttpHandlerAdapter servlet = new ServletHttpHandlerAdapter(httpHandler);

		ServletRegistration.Dynamic registration = servletContext.addServlet(servletName, servlet);
		Assert.notNull(registration, "Failed to register servlet '" + servletName + "'.");

		registration.setLoadOnStartup(1);
		registration.addMapping(getServletMapping());
		registration.setAsyncSupported(true);
	}

	/**
	 * Return the name to use to register the {@link ServletHttpHandlerAdapter}.
	 * <p>By default this is {@link #DEFAULT_SERVLET_NAME}.
	 */
	protected String getServletName() {
		return DEFAULT_SERVLET_NAME;
	}

	/**
	 * Return the Spring configuration that contains application beans including
	 * the ones detected by {@link WebHttpHandlerBuilder#applicationContext}.
	 */
	protected ApplicationContext createApplicationContext() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		Class<?>[] configClasses = getConfigClasses();
		Assert.notEmpty(configClasses, "No Spring configuration provided.");
		return context;
	}

	/**
	 * Specify {@link org.springframework.context.annotation.Configuration @Configuration} and/or
	 * {@link org.springframework.stereotype.Component @Component} classes that
	 * make up the application configuration. The config classes are given to
	 * {@linkplain #createApplicationContext()}.
	 */
	protected abstract Class<?>[] getConfigClasses();

	/**
	 * Return the Servlet mapping to use. Only the default Servlet mapping '/'
	 * and path-based Servlet mappings such as '/api/*' are supported.
	 * <p>By default this is set to '/'.
	 */
	protected String getServletMapping() {
		return "/";
	}

}
