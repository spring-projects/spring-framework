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

package org.springframework.web.util;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Bootstrap servlet for custom log4j initialization in a web environment.
 * Delegates to {@link Log4jWebConfigurer} (see its javadoc for configuration details).
 *
 * <b>WARNING: Assumes an expanded WAR file</b>, both for loading the configuration
 * file and for writing the log files. If you want to keep your WAR unexpanded or
 * don't need application-specific log files within the WAR directory, don't use
 * log4j setup within the application (thus, don't use Log4jConfigListener or
 * Log4jConfigServlet). Instead, use a global, VM-wide log4j setup (for example,
 * in JBoss) or JDK 1.4's <code>java.util.logging</code> (which is global too).
 *
 * <p>Note: This servlet should have a lower <code>load-on-startup</code> value
 * in <code>web.xml</code> than ContextLoaderServlet, when using custom log4j
 * initialization.
 *
 * <p><i>Note that this class has been deprecated for containers implementing
 * Servlet API 2.4 or higher, in favor of {@link Log4jConfigListener}.</i><br>
 * According to Servlet 2.4, listeners must be initialized before load-on-startup
 * servlets. Many Servlet 2.3 containers already enforce this behavior
 * (see ContextLoaderServlet javadocs for details). If you use such a container,
 * this servlet can be replaced with Log4jConfigListener.
 *
 * @author Juergen Hoeller
 * @author Darren Davison
 * @since 12.08.2003
 * @see Log4jWebConfigurer
 * @see Log4jConfigListener
 * @see org.springframework.web.context.ContextLoaderServlet
 */
public class Log4jConfigServlet extends HttpServlet {

	public void init() {
		Log4jWebConfigurer.initLogging(getServletContext());
	}

	public void destroy() {
		Log4jWebConfigurer.shutdownLogging(getServletContext());
	}


	/**
	 * This should never even be called since no mapping to this servlet should
	 * ever be created in web.xml. That's why a correctly invoked Servlet 2.3
	 * listener is much more appropriate for initialization work ;-)
	 */
	public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
		getServletContext().log(
				"Attempt to call service method on Log4jConfigServlet as [" +
				request.getRequestURI() + "] was ignored");
		response.sendError(HttpServletResponse.SC_BAD_REQUEST);
	}

	public String getServletInfo() {
		return "Log4jConfigServlet for Servlet API 2.3 " +
				"(deprecated in favor of Log4jConfigListener for Servlet API 2.4)";
	}

}
