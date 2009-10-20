/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.remoting.jaxrpc;

import java.io.File;
import javax.servlet.ServletContext;
import javax.xml.rpc.ServiceException;
import javax.xml.rpc.server.ServiceLifecycle;
import javax.xml.rpc.server.ServletEndpointContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.util.WebUtils;

/**
 * Convenience base class for JAX-RPC servlet endpoint implementations.
 * Provides a reference to the current Spring application context,
 * e.g. for bean lookup or resource loading.
 *
 * <p>The Web Service servlet needs to run in the same web application
 * as the Spring context to allow for access to Spring's facilities.
 * In case of Axis, copy the AxisServlet definition into your web.xml,
 * and set up the endpoint in "server-config.wsdd" (or use the deploy tool).
 *
 * <p>This class does not extend
 * {@link org.springframework.web.context.support.WebApplicationObjectSupport}
 * to not expose any public setters. For some reason, Axis tries to
 * resolve public setters in a special way...
 *
 * <p>JAX-RPC service endpoints are usually required to implement an
 * RMI port interface. However, many JAX-RPC implementations accept plain
 * service endpoint classes too, avoiding the need to maintain an RMI port
 * interface in addition to an existing non-RMI business interface.
 * Therefore, implementing the business interface will usually be sufficient.
 *
 * @author Juergen Hoeller
 * @since 16.12.2003
 * @see #init
 * @see #getWebApplicationContext
 * @deprecated in favor of JAX-WS support in <code>org.springframework.remoting.jaxws</code>
 */
@Deprecated
public abstract class ServletEndpointSupport implements ServiceLifecycle {

	protected final Log logger = LogFactory.getLog(getClass());
	
	private ServletEndpointContext servletEndpointContext;

	private WebApplicationContext webApplicationContext;

	private MessageSourceAccessor messageSourceAccessor;


	/**
	 * Initialize this JAX-RPC servlet endpoint.
	 * Calls onInit after successful context initialization.
	 * @param context ServletEndpointContext
	 * @throws ServiceException if the context is not a ServletEndpointContext
	 * @see #onInit
	 */
	public final void init(Object context) throws ServiceException {
		if (!(context instanceof ServletEndpointContext)) {
			throw new ServiceException("ServletEndpointSupport needs ServletEndpointContext, not [" + context + "]");
		}
		this.servletEndpointContext = (ServletEndpointContext) context;
		ServletContext servletContext = this.servletEndpointContext.getServletContext();
		this.webApplicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
		this.messageSourceAccessor = new MessageSourceAccessor(this.webApplicationContext);
		onInit();
	}

	/**
	 * Return the current JAX-RPC ServletEndpointContext.
	 */
	protected final ServletEndpointContext getServletEndpointContext() {
		return this.servletEndpointContext;
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
	 * Callback for custom initialization after the context has been set up.
	 * @throws ServiceException if initialization failed
	 */
	protected void onInit() throws ServiceException {
	}


	/**
	 * This implementation of destroy is empty.
	 * Can be overridden in subclasses.
	 */
	public void destroy() {
	}

}
