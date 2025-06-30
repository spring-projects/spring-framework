/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.context.support;

import java.io.File;

import jakarta.servlet.ServletContext;
import org.jspecify.annotations.Nullable;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.util.Assert;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.util.WebUtils;

/**
 * Convenient superclass for application objects running in a {@link WebApplicationContext}.
 * Provides {@code getWebApplicationContext()}, {@code getServletContext()}, and
 * {@code getTempDir()} accessors.
 *
 * <p>Note: It is generally recommended to use individual callback interfaces for the actual
 * callbacks needed. This broad base class is primarily intended for use within the framework,
 * in case of {@link ServletContext} access etc typically being needed.
 *
 * @author Juergen Hoeller
 * @since 28.08.2003
 * @see SpringBeanAutowiringSupport
 */
public abstract class WebApplicationObjectSupport extends ApplicationObjectSupport implements ServletContextAware {

	private @Nullable ServletContext servletContext;


	@Override
	public final void setServletContext(ServletContext servletContext) {
		if (servletContext != this.servletContext) {
			this.servletContext = servletContext;
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
	 * Calls {@link #initServletContext(jakarta.servlet.ServletContext)} if the
	 * given ApplicationContext is a {@link WebApplicationContext}.
	 */
	@Override
	protected void initApplicationContext(ApplicationContext context) {
		super.initApplicationContext(context);
		if (this.servletContext == null && context instanceof WebApplicationContext wac) {
			this.servletContext = wac.getServletContext();
			if (this.servletContext != null) {
				initServletContext(this.servletContext);
			}
		}
	}

	/**
	 * Subclasses may override this for custom initialization based
	 * on the ServletContext that this application object runs in.
	 * <p>The default implementation is empty. Called by
	 * {@link #initApplicationContext(org.springframework.context.ApplicationContext)}
	 * as well as {@link #setServletContext(jakarta.servlet.ServletContext)}.
	 * @param servletContext the ServletContext that this application object runs in
	 * (never {@code null})
	 */
	protected void initServletContext(ServletContext servletContext) {
	}

	/**
	 * Return the current application context as WebApplicationContext.
	 * <p><b>NOTE:</b> Only use this if you actually need to access
	 * WebApplicationContext-specific functionality. Preferably use
	 * {@code getApplicationContext()} or {@code getServletContext()}
	 * else, to be able to run in non-WebApplicationContext environments as well.
	 * @throws IllegalStateException if not running in a WebApplicationContext
	 * @see #getApplicationContext()
	 */
	protected final @Nullable WebApplicationContext getWebApplicationContext() throws IllegalStateException {
		ApplicationContext ctx = getApplicationContext();
		if (ctx instanceof WebApplicationContext wac) {
			return wac;
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
	 * @throws IllegalStateException if not running within a required ServletContext
	 * @see #isContextRequired()
	 */
	protected final @Nullable ServletContext getServletContext() throws IllegalStateException {
		if (this.servletContext != null) {
			return this.servletContext;
		}
		ServletContext servletContext = null;
		WebApplicationContext wac = getWebApplicationContext();
		if (wac != null) {
			servletContext = wac.getServletContext();
		}
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
	 * @see org.springframework.web.util.WebUtils#getTempDir(jakarta.servlet.ServletContext)
	 */
	protected final File getTempDir() throws IllegalStateException {
		ServletContext servletContext = getServletContext();
		Assert.state(servletContext != null, "ServletContext is required");
		return WebUtils.getTempDir(servletContext);
	}

}
