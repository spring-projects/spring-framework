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

package org.springframework.web.context;

import javax.servlet.ServletContext;

import org.springframework.context.ApplicationContext;

/** 
 * Interface to provide configuration for a web application. This is read-only while
 * the application is running, but may be reloaded if the implementation supports this.
 *
 * <p>This interface adds a <code>getServletContext()</code> method to the generic
 * ApplicationContext interface, and defines a well-known application attribute name
 * that the root context must be bound to in the bootstrap process.
 *
 * <p>Like generic application contexts, web application contexts are hierarchical.
 * There is a single root context per application, while each servlet in the application
 * (including a dispatcher servlet in the MVC framework) has its own child context.
 *
 * <p>In addition to standard application context lifecycle capabilities,
 * WebApplicationContext implementations need to detect {@link ServletContextAware}
 * beans and invoke the <code>setServletContext</code> method accordingly.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since January 19, 2001
 * @see ServletContextAware#setServletContext
 */
public interface WebApplicationContext extends ApplicationContext {

	/**
	 * Context attribute to bind root WebApplicationContext to on successful startup.
	 * <p>Note: If the startup of the root context fails, this attribute can contain
	 * an exception or error as value. Use WebApplicationContextUtils for convenient
	 * lookup of the root WebApplicationContext.
	 * @see org.springframework.web.context.support.WebApplicationContextUtils#getWebApplicationContext
	 * @see org.springframework.web.context.support.WebApplicationContextUtils#getRequiredWebApplicationContext
	 */
	String ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE = WebApplicationContext.class.getName() + ".ROOT";

	/**
	 * Scope identifier for request scope: "request".
	 * Supported in addition to the standard scopes "singleton" and "prototype".
	 */
	String SCOPE_REQUEST = "request";

	/**
	 * Scope identifier for session scope: "session".
	 * Supported in addition to the standard scopes "singleton" and "prototype".
	 */
	String SCOPE_SESSION = "session";

	/**
	 * Scope identifier for global session scope: "globalSession".
	 * Supported in addition to the standard scopes "singleton" and "prototype".
	 */
	String SCOPE_GLOBAL_SESSION = "globalSession";

	/**
	 * Scope identifier for the global web application scope: "application".
	 * Supported in addition to the standard scopes "singleton" and "prototype".
	 */
	String SCOPE_APPLICATION = "application";

	/**
	 * Name of the ServletContext environment bean in the factory.
	 * @see javax.servlet.ServletContext
	 */
	String SERVLET_CONTEXT_BEAN_NAME = "servletContext";

	/**
	 * Name of the ServletContext/PortletContext init-params environment bean in the factory.
	 * <p>Note: Possibly merged with ServletConfig/PortletConfig parameters.
	 * ServletConfig parameters override ServletContext parameters of the same name.
	 * @see javax.servlet.ServletContext#getInitParameterNames()
	 * @see javax.servlet.ServletContext#getInitParameter(String)
	 * @see javax.servlet.ServletConfig#getInitParameterNames()
	 * @see javax.servlet.ServletConfig#getInitParameter(String)
	 */
	String CONTEXT_PARAMETERS_BEAN_NAME = "contextParameters";

	/**
	 * Name of the ServletContext/PortletContext attributes environment bean in the factory.
	 * @see javax.servlet.ServletContext#getAttributeNames()
	 * @see javax.servlet.ServletContext#getAttribute(String)
	 */
	String CONTEXT_ATTRIBUTES_BEAN_NAME = "contextAttributes";


	/**
	 * Return the standard Servlet API ServletContext for this application.
	 * <p>Also available for a Portlet application, in addition to the PortletContext.
	 */
	ServletContext getServletContext();
	
}
