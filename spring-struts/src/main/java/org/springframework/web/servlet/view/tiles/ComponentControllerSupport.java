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

package org.springframework.web.servlet.view.tiles;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.ControllerSupport;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.util.NestedServletException;
import org.springframework.web.util.WebUtils;

/**
 * Convenience class for Spring-aware Tiles component controllers.
 * Provides a reference to the current Spring application context,
 * e.g. for bean lookup or resource loading.
 *
 * <p>Derives from the Tiles {@link ControllerSupport} class rather than
 * implementing the Tiles {@link org.apache.struts.tiles.Controller} interface
 * in order to be compatible with Struts 1.1 and 1.2. Implements both Struts 1.1's
 * {@code perform} and Struts 1.2's {@code execute} method accordingly.
 *
 * @author Juergen Hoeller
 * @author Alef Arendsen
 * @since 22.08.2003
 * @see org.springframework.web.context.support.WebApplicationObjectSupport
 * @deprecated as of Spring 3.0
 */
@Deprecated
public abstract class ComponentControllerSupport extends ControllerSupport {

	private WebApplicationContext webApplicationContext;

	private MessageSourceAccessor messageSourceAccessor;


	/**
	 * This implementation delegates to {@code execute},
	 * converting non-Servlet/IO Exceptions to ServletException.
	 * <p>This is the only execution method available in Struts 1.1.
	 * @see #execute
	 */
	@Override
	public final void perform(
			ComponentContext componentContext, HttpServletRequest request,
			HttpServletResponse response, ServletContext servletContext)
		throws ServletException, IOException {

		try {
			execute(componentContext, request, response, servletContext);
		}
		catch (ServletException ex) {
			throw ex;
		}
		catch (IOException ex) {
			throw ex;
		}
		catch (Throwable ex) {
			throw new NestedServletException("Execution of component controller failed", ex);
		}
	}

	/**
	 * This implementation delegates to {@code doPerform},
	 * lazy-initializing the application context reference if necessary.
	 * <p>This is the preferred execution method in Struts 1.2.
	 * When running with Struts 1.1, it will be called by {@code perform}.
	 * @see #perform
	 * @see #doPerform
	 */
	@Override
	public final void execute(
			ComponentContext componentContext, HttpServletRequest request,
			HttpServletResponse response, ServletContext servletContext)
		throws Exception {

		synchronized (this) {
			if (this.webApplicationContext == null) {
				this.webApplicationContext = RequestContextUtils.getWebApplicationContext(request, servletContext);
				this.messageSourceAccessor = new MessageSourceAccessor(this.webApplicationContext);
			}
		}
		doPerform(componentContext, request, response);
	}


	/**
	 * Subclasses can override this for custom initialization behavior.
	 * Gets called on initialization of the context for this controller.
	 * @throws org.springframework.context.ApplicationContextException in case of initialization errors
	 * @throws org.springframework.beans.BeansException if thrown by application context methods
	 */
	protected void initApplicationContext() throws BeansException {
	}

	/**
	 * Return the current Spring ApplicationContext.
	 */
	protected final ApplicationContext getApplicationContext() {
		return this.webApplicationContext;
	}

	/**
	 * Return the current Spring WebApplicationContext.
	 */
	protected final WebApplicationContext getWebApplicationContext() {
		return this.webApplicationContext;
	}

	/**
	 * Return a MessageSourceAccessor for the application context
	 * used by this object, for easy message access.
	 */
	protected final MessageSourceAccessor getMessageSourceAccessor() {
		return this.messageSourceAccessor;
	}

	/**
	 * Return the current ServletContext.
	 */
	protected final ServletContext getServletContext() {
		return this.webApplicationContext.getServletContext();
	}

	/**
	 * Return the temporary directory for the current web application,
	 * as provided by the servlet container.
	 * @return the File representing the temporary directory
	 */
	protected final File getTempDir() {
		return WebUtils.getTempDir(getServletContext());
	}


	/**
	 * Perform the preparation for the component, allowing for any Exception to be thrown.
	 * The ServletContext can be retrieved via getServletContext, if necessary.
	 * The Spring WebApplicationContext can be accessed via getWebApplicationContext.
	 * <p>This method will be called both in the Struts 1.1 and Struts 1.2 case,
	 * by {@code perform} or {@code execute}, respectively.
	 * @param componentContext current Tiles component context
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @throws Exception in case of errors
	 * @see org.apache.struts.tiles.Controller#perform
	 * @see #getServletContext
	 * @see #getWebApplicationContext
	 * @see #perform
	 * @see #execute
	 */
	protected abstract void doPerform(
			ComponentContext componentContext, HttpServletRequest request, HttpServletResponse response)
			throws Exception;

}
