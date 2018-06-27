/*
 * Copyright 2002-2018 the original author or authors.
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
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ServletHttpHandlerAdapter;
import org.springframework.util.Assert;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.adapter.HttpWebHandlerAdapter;

/**
 * Base class for {@link org.springframework.web.WebApplicationInitializer}
 * implementations that register a {@link DispatcherHandler} in the servlet
 * context, wrapping it in a {@link ServletHttpHandlerAdapter}.
 *
 * @author Arjen Poutsma
 * @since 5.0
 * @deprecated as of 5.0.2, in favor of
 * {@link org.springframework.web.server.adapter.AbstractReactiveWebInitializer}
 */
@Deprecated
public abstract class AbstractDispatcherHandlerInitializer implements WebApplicationInitializer {

	/**
	 * The default servlet name. Can be customized by overriding {@link #getServletName}.
	 */
	public static final String DEFAULT_SERVLET_NAME = "dispatcher-handler";

	/**
	 * The default servlet mapping. Can be customized by overriding {@link #getServletMapping()}.
	 */
	public static final String DEFAULT_SERVLET_MAPPING = "/";


	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		registerDispatcherHandler(servletContext);
	}

	/**
	 * Register a {@link DispatcherHandler} against the given servlet context.
	 * <p>This method will create a {@link DispatcherHandler}, initializing it with the application
	 * context returned from {@link #createApplicationContext()}. The created handler will be
	 * wrapped in a {@link ServletHttpHandlerAdapter} servlet with the name
     * returned by {@link #getServletName()}, mapping it to the pattern
	 * returned from {@link #getServletMapping()}.
	 * <p>Further customization can be achieved by overriding {@link
	 * #customizeRegistration(ServletRegistration.Dynamic)} or
	 * {@link #createDispatcherHandler(ApplicationContext)}.
	 * @param servletContext the context to register the servlet against
	 */
	protected void registerDispatcherHandler(ServletContext servletContext) {
		String servletName = getServletName();
		Assert.hasLength(servletName, "getServletName() must not return null or empty");

		ApplicationContext applicationContext = createApplicationContext();
		Assert.notNull(applicationContext, "createApplicationContext() must not return null");

		refreshApplicationContext(applicationContext);
		registerCloseListener(servletContext, applicationContext);

		WebHandler dispatcherHandler = createDispatcherHandler(applicationContext);
		Assert.notNull(dispatcherHandler, "createDispatcherHandler(ApplicationContext) must not return null");

		ServletHttpHandlerAdapter handlerAdapter = createHandlerAdapter(dispatcherHandler);
		Assert.notNull(handlerAdapter, "createHandlerAdapter(WebHandler) must not return null");

		ServletRegistration.Dynamic registration = servletContext.addServlet(servletName, handlerAdapter);
		if (registration == null) {
			throw new IllegalStateException("Failed to register servlet with name '" + servletName + "'. " +
					"Check if there is another servlet registered under the same name.");
		}

		registration.setLoadOnStartup(1);
		registration.addMapping(getServletMapping());
		registration.setAsyncSupported(true);

		customizeRegistration(registration);
	}

	/**
	 * Return the name under which the {@link ServletHttpHandlerAdapter} will be registered.
	 * Defaults to {@link #DEFAULT_SERVLET_NAME}.
	 * @see #registerDispatcherHandler(ServletContext)
	 */
	protected String getServletName() {
		return DEFAULT_SERVLET_NAME;
	}

	/**
	 * Create an application context to be provided to the {@code DispatcherHandler}.
	 * <p>The returned context is delegated to Spring's
	 * {@link DispatcherHandler#DispatcherHandler(ApplicationContext)}. As such,
	 * it typically contains controllers, view resolvers, and other web-related beans.
	 * @see #registerDispatcherHandler(ServletContext)
	 */
	protected abstract ApplicationContext createApplicationContext();

	/**
	 * Refresh the given application context, if necessary.
	 */
	protected void refreshApplicationContext(ApplicationContext context) {
		if (context instanceof ConfigurableApplicationContext) {
			ConfigurableApplicationContext cac = (ConfigurableApplicationContext) context;
			if (!cac.isActive()) {
				cac.refresh();
			}
		}
	}

	/**
	 * Create a {@link DispatcherHandler} (or other kind of {@link WebHandler}-derived
	 * dispatcher) with the specified {@link ApplicationContext}.
	 */
	protected WebHandler createDispatcherHandler(ApplicationContext applicationContext) {
		return new DispatcherHandler(applicationContext);
	}

	/**
	 * Create a {@link ServletHttpHandlerAdapter}.
	 * <p>Default implementation returns a {@code ServletHttpHandlerAdapter} with the provided
	 * {@code webHandler}.
	 */
	protected ServletHttpHandlerAdapter createHandlerAdapter(WebHandler webHandler) {
		HttpHandler httpHandler = new HttpWebHandlerAdapter(webHandler);
		return new ServletHttpHandlerAdapter(httpHandler);
	}

	/**
	 * Specify the servlet mapping for the {@code ServletHttpHandlerAdapter}.
	 * <p>Default implementation returns {@code /}.
	 * @see #registerDispatcherHandler(ServletContext)
	 */
	protected String getServletMapping() {
		return DEFAULT_SERVLET_MAPPING;
	}

	/**
	 * Optionally perform further registration customization once
	 * {@link #registerDispatcherHandler(ServletContext)} has completed.
	 * @param registration the {@code ServletHttpHandlerAdapter} registration to be customized
	 * @see #registerDispatcherHandler(ServletContext)
	 */
	protected void customizeRegistration(ServletRegistration.Dynamic registration) {
	}

	/**
	 * Register a {@link ServletContextListener} that closes the given application context
	 * when the servlet context is destroyed.
	 * @param servletContext the servlet context to listen to
	 * @param applicationContext the application context that is to be closed when
	 * {@code servletContext} is destroyed
	 */
	protected void registerCloseListener(ServletContext servletContext, ApplicationContext applicationContext) {
		if (applicationContext instanceof ConfigurableApplicationContext) {
			ConfigurableApplicationContext context = (ConfigurableApplicationContext) applicationContext;
			ServletContextDestroyedListener listener = new ServletContextDestroyedListener(context);
			servletContext.addListener(listener);
		}
	}


	private static class ServletContextDestroyedListener implements ServletContextListener {

		private final ConfigurableApplicationContext applicationContext;

		public ServletContextDestroyedListener(ConfigurableApplicationContext applicationContext) {
			this.applicationContext = applicationContext;
		}

		@Override
		public void contextInitialized(ServletContextEvent sce) {
		}

		@Override
		public void contextDestroyed(ServletContextEvent sce) {
			this.applicationContext.close();
		}
	}

}
