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

package org.springframework.web.context;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.web.WebApplicationInitializer;

/**
 * Base class for {@link org.springframework.web.WebApplicationInitializer}
 * implementations that register a {@link ContextLoaderListener} in the servlet context.
 *
 * <p>The only method to be implemented by subclasses is {@link
 * #createRootApplicationContext()}, which gets invoked from {@link
 * #addContextLoaderListener(javax.servlet.ServletContext)}.
 *
 * @author Arjen Poutsma
 * @since 3.2
 */
public abstract class AbstractContextLoaderInitializer
		implements WebApplicationInitializer {

	/** Logger available to subclasses. */
	protected final Log logger = LogFactory.getLog(getClass());

	public void onStartup(ServletContext servletContext) throws ServletException {
		addContextLoaderListener(servletContext);
	}

	/**
	 * Adds a {@link ContextLoaderListener} to the given servlet context. The {@code
	 * ContextLoaderListener} is initialized with the application context returned from the
	 * {@link #createRootApplicationContext()} template method.
	 *
	 * @param servletContext the servlet context to register the listener with
	 */
	protected void addContextLoaderListener(ServletContext servletContext) {
		WebApplicationContext rootAppContext = createRootApplicationContext();
		if (rootAppContext != null) {
			servletContext.addListener(new ContextLoaderListener(rootAppContext));
		}
		else {
			logger.warn("No ContextLoaderListener registered, as " +
					"createRootApplicationContext() did not return an application context");
		}
	}

	/**
	 * Abstract template method that creates and returns a root application context.
	 *
	 * <p>The returned context is used as a <strong>root</strong> context, i.e. it is passed
	 * on to {@link ContextLoaderListener#ContextLoaderListener(WebApplicationContext)}. As
	 * such, it typically contains middle-tier services, data sources, etc.
	 *
	 * @return the root application context, or {@code null} if a root context is not
	 *         desired
	 */
	protected abstract WebApplicationContext createRootApplicationContext();

}
