/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.reactive.support;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ServletHttpHandlerAdapter;
import org.springframework.util.Assert;
import org.springframework.web.WebApplicationInitializer;

/**
 * Base class for {@link org.springframework.web.WebApplicationInitializer}
 * implementations that register a {@link ServletHttpHandlerAdapter} in the servlet context.
 *
 * <p>Concrete implementations are required to implement
 * {@link #createHttpHandler()}, as well as {@link #getServletMappings()},
 * both of which get invoked from {@link #registerHandlerAdapter(ServletContext)}.
 * Further customization can be achieved by overriding
 * {@link #customizeRegistration(ServletRegistration.Dynamic)}.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public abstract class AbstractServletHttpHandlerAdapterInitializer
		implements WebApplicationInitializer {

	/**
	 * The default servlet name. Can be customized by overriding {@link #getServletName}.
	 */
	public static final String DEFAULT_SERVLET_NAME = "http-handler-adapter";


	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		registerHandlerAdapter(servletContext);
	}

	/**
	 * Register a {@link ServletHttpHandlerAdapter} against the given servlet context.
	 * <p>This method will create a {@code HttpHandler} using {@link #createHttpHandler()},
	 * and use it to create a {@code ServletHttpHandlerAdapter} with the name returned by
	 * {@link #getServletName()}, and mapping it to the patterns
	 * returned from {@link #getServletMappings()}.
	 * <p>Further customization can be achieved by overriding {@link
	 * #customizeRegistration(ServletRegistration.Dynamic)} or
	 * {@link #createServlet(HttpHandler)}.
	 * @param servletContext the context to register the servlet against
	 */
	protected void registerHandlerAdapter(ServletContext servletContext) {
		String servletName = getServletName();
		Assert.hasLength(servletName, "getServletName() must not return empty or null");

		HttpHandler httpHandler = createHttpHandler();
		Assert.notNull(httpHandler,
				"createHttpHandler() did not return a HttpHandler for servlet ["
						+ servletName + "]");

		ServletHttpHandlerAdapter servlet = createServlet(httpHandler);
		Assert.notNull(servlet,
				"createHttpHandler() did not return a ServletHttpHandlerAdapter for servlet ["
						+ servletName + "]");

		ServletRegistration.Dynamic registration = servletContext.addServlet(servletName, servlet);
		Assert.notNull(registration,
				"Failed to register servlet with name '" + servletName + "'." +
						"Check if there is another servlet registered under the same name.");

		registration.setLoadOnStartup(1);
		registration.addMapping(getServletMappings());
		registration.setAsyncSupported(true);

		customizeRegistration(registration);
	}

	/**
	 * Return the name under which the {@link ServletHttpHandlerAdapter} will be registered.
	 * Defaults to {@link #DEFAULT_SERVLET_NAME}.
	 * @see #registerHandlerAdapter(ServletContext)
	 */
	protected String getServletName() {
		return DEFAULT_SERVLET_NAME;
	}

	/**
	 * Create the {@link HttpHandler}.
	 */
	protected abstract HttpHandler createHttpHandler();

	/**
	 * Create a {@link ServletHttpHandlerAdapter}  with the specified .
	 * <p>Default implementation returns a {@code ServletHttpHandlerAdapter} with the provided
	 * {@code httpHandler}.
	 */
	protected ServletHttpHandlerAdapter createServlet(HttpHandler httpHandler) {
		return new ServletHttpHandlerAdapter(httpHandler);
	}

	/**
	 * Specify the servlet mapping(s) for the {@code ServletHttpHandlerAdapter} &mdash;
	 * for example {@code "/"}, {@code "/app"}, etc.
	 * @see #registerHandlerAdapter(ServletContext)
	 */
	protected abstract String[] getServletMappings();

	/**
	 * Optionally perform further registration customization once
	 * {@link #registerHandlerAdapter(ServletContext)} has completed.
	 * @param registration the {@code DispatcherServlet} registration to be customized
	 * @see #registerHandlerAdapter(ServletContext)
	 */
	protected void customizeRegistration(ServletRegistration.Dynamic registration) {
	}
}
