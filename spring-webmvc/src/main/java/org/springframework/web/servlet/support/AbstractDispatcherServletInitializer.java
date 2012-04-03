/*
 * Copyright 2002-2012 the original author or authors.
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

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.springframework.util.Assert;
import org.springframework.web.context.AbstractContextLoaderInitializer;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Base class for {@link org.springframework.web.WebApplicationInitializer
 * WebApplicationInitializer} implementations that register a {@link DispatcherServlet} in
 * the servlet context.
 *
 * <p>Concrete implementations are required to implement {@link
 * #createServletApplicationContext()}, as well as {@link #getServletMappings()}, both of
 * which gets invoked from {@link #addDispatcherServlet}. Further
 * customization can be achieved by overriding {@link #customizeRegistration(ServletRegistration.Dynamic)}.
 *
 * <p>Because this class extends from {@link AbstractContextLoaderInitializer}, concrete
 * implementations are also required to implement {@link #createRootApplicationContext()}
 * to set up a <strong>root</strong> or <strong>parent</strong> application context. If a
 * root context is not desired, implementations can simply return {@code null} in the
 * {@code createRootApplicationContext()} implementation.
 *
 * @author Arjen Poutsma
 * @since 3.2
 */
public abstract class AbstractDispatcherServletInitializer
		extends AbstractContextLoaderInitializer {

	/** The default servlet name. Can be changed by overriding {@link #getServletName}. */
	public static final String DEFAULT_SERVLET_NAME = "dispatcher";

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		super.onStartup(servletContext);

		addDispatcherServlet(servletContext);
	}

	/**
	 * Adds a {@link DispatcherServlet} to the given servlet context.
	 *
	 * <p>This method will create a {@code DispatcherServlet} with the name returned by
	 * {@link #getServletName()}, initializing it with the application context returned from
	 * {@link #createServletApplicationContext()}, and mapping it to the patterns returned
	 * from {@link #getServletMappings()}.
	 *
	 * <p>Further customization can be achieved by overriding {@link
	 * #customizeRegistration(ServletRegistration.Dynamic)}.
	 *
	 * @param servletContext the servlet context to register the listener with
	 */
	protected void addDispatcherServlet(ServletContext servletContext) {
		String servletName = getServletName();
		Assert.hasLength(servletName,
				"getServletName() should not return an empty string");

		WebApplicationContext servletAppContext = createServletApplicationContext();
		Assert.notNull(servletAppContext,
				"createServletApplicationContext() did not return an application " +
						"context for servlet [" + servletName + "]");

		DispatcherServlet dispatcherServlet = new DispatcherServlet(servletAppContext);

		ServletRegistration.Dynamic registration =
				servletContext.addServlet(servletName, dispatcherServlet);

		registration.setLoadOnStartup(1);
		registration.addMapping(getServletMappings());

		customizeRegistration(registration);
	}

	/**
	 * Returns the name under which the {@link DispatcherServlet} will be registered.
	 * Defaults to {@link #DEFAULT_SERVLET_NAME}.
	 *
	 * @return the servlet names
	 */
	protected String getServletName() {
		return DEFAULT_SERVLET_NAME;
	}

	/**
	 * Abstract template method that creates and returns a servlet application context.
	 *
	 * <p>The returned context is used as a <strong>servlet</strong> context, i.e. it is
	 * passed on to {@link DispatcherServlet#DispatcherServlet(WebApplicationContext)} As
	 * such, it typically contains controllers, view resolvers, locale resolvers, and other
	 * web-related beans.
	 *
	 * @return the application context for the servlet
	 */
	protected abstract WebApplicationContext createServletApplicationContext();

	/**
	 * Abstract template method that returns the mappings for the servlet with the given
	 * name.
	 *
	 * @return the servlet mappings for the servlet
	 */
	protected abstract String[] getServletMappings();

	/**
	 * Template method that allows for further registration customization of the servlet.
	 *
	 * <p>The default implementation is empty.
	 *
	 * @param registration the registration to be customized
	 */
	protected void customizeRegistration(ServletRegistration.Dynamic registration) {
	}

}
