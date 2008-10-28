/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.web.context.support;

import java.io.File;

import javax.servlet.ServletContext;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.util.WebUtils;

/**
 * Convenient superclass for application objects running in a WebApplicationContext.
 * Provides <code>getWebApplicationContext()</code>, <code>getServletContext()</code>,
 * and <code>getTempDir()</code> methods.
 *
 * @author Juergen Hoeller
 * @since 28.08.2003
 * @see SpringBeanAutowiringSupport
 */
public abstract class WebApplicationObjectSupport extends ApplicationObjectSupport
		implements ServletContextAware {

	private ServletContext servletContext;


	public final void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
		if (servletContext != null) {
			initServletContext(servletContext);
		}
	}

	/**
	 * Overrides the base class behavior to enforce running in an ApplicationContext.
	 * All accessors will throw IllegalStateException if not running in a context.
	 * @see #getApplicationContext()
	 * @see #getMessageSourceAccessor()
	 * @see #getWebApplicationContext()
	 * @see #getServletContext()
	 * @see #getTempDir()
	 */
	@Override
	protected boolean isContextRequired() {
		return true;
	}

	/**
	 * Calls {@link #initServletContext(javax.servlet.ServletContext)} if the
	 * given ApplicationContext is a {@link WebApplicationContext}.
	 */
	@Override
	protected void initApplicationContext(ApplicationContext context) {
		super.initApplicationContext(context);
		if (context instanceof WebApplicationContext) {
			ServletContext servletContext = ((WebApplicationContext) context).getServletContext();
			if (servletContext != null) {
				initServletContext(servletContext);
			}
		}
	}

	/**
	 * Subclasses may override this for custom initialization based
	 * on the ServletContext that this application object runs in.
	 * <p>The default implementation is empty. Called by
	 * {@link #initApplicationContext(org.springframework.context.ApplicationContext)}
	 * as well as {@link #setServletContext(javax.servlet.ServletContext)}.
	 * @param servletContext the ServletContext that this application object runs in
	 * (never <code>null</code>)
	 */
	protected void initServletContext(ServletContext servletContext) {
	}

	/**
	 * Return the current application context as WebApplicationContext.
	 * <p><b>NOTE:</b> Only use this if you actually need to access
	 * WebApplicationContext-specific functionality. Preferably use
	 * <code>getApplicationContext()</code> or <code>getServletContext()</code>
	 * else, to be able to run in non-WebApplicationContext environments as well.
	 * @throws IllegalStateException if not running in a WebApplicationContext
	 * @see #getApplicationContext()
	 */
	protected final WebApplicationContext getWebApplicationContext() throws IllegalStateException {
		ApplicationContext ctx = getApplicationContext();
		if (ctx instanceof WebApplicationContext) {
			return (WebApplicationContext) getApplicationContext();
		}
		else if (isContextRequired()) {
			throw new IllegalStateException("WebApplicationObjectSupport instance [" + this +
					"] does not run in a WebApplicationContext but in: " + ctx);
		}
		else {
			return null;
		}
	}

	/**
	 * Return the current ServletContext.
	 * @throws IllegalStateException if not running within a ServletContext
	 */
	protected final ServletContext getServletContext() throws IllegalStateException {
		if (this.servletContext != null) {
			return this.servletContext;
		}
		ServletContext servletContext = getWebApplicationContext().getServletContext();
		if (servletContext == null && isContextRequired()) {
			throw new IllegalStateException("WebApplicationObjectSupport instance [" + this +
					"] does not run within a ServletContext. Make sure the object is fully configured!");
		}
		return servletContext;
	}

	/**
	 * Return the temporary directory for the current web application,
	 * as provided by the servlet container.
	 * @return the File representing the temporary directory
	 * @throws IllegalStateException if not running within a ServletContext
	 * @see org.springframework.web.util.WebUtils#getTempDir(javax.servlet.ServletContext)
	 */
	protected final File getTempDir() throws IllegalStateException {
		return WebUtils.getTempDir(getServletContext());
	}

}
