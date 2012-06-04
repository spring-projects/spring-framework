/*
 * Copyright 2002-2012 the original author or authors.
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

import java.io.FileNotFoundException;

import javax.servlet.ServletContext;

import org.springframework.util.Log4jConfigurer;
import org.springframework.util.ResourceUtils;
import org.springframework.util.SystemPropertyUtils;

/**
 * Convenience class that performs custom log4j initialization for web environments,
 * allowing for log file paths within the web application, with the option to
 * perform automatic refresh checks (for runtime changes in logging configuration).
 *
 * <p><b>WARNING: Assumes an expanded WAR file</b>, both for loading the configuration
 * file and for writing the log files. If you want to keep your WAR unexpanded or
 * don't need application-specific log files within the WAR directory, don't use
 * log4j setup within the application (thus, don't use Log4jConfigListener or
 * Log4jConfigServlet). Instead, use a global, VM-wide log4j setup (for example,
 * in JBoss) or JDK 1.4's <code>java.util.logging</code> (which is global too).
 *
 * <p>Supports three init parameters at the servlet context level (that is,
 * context-param entries in web.xml):
 *
 * <ul>
 * <li><i>"log4jConfigLocation":</i><br>
 * Location of the log4j config file; either a "classpath:" location (e.g.
 * "classpath:myLog4j.properties"), an absolute file URL (e.g. "file:C:/log4j.properties),
 * or a plain path relative to the web application root directory (e.g.
 * "/WEB-INF/log4j.properties"). If not specified, default log4j initialization
 * will apply ("log4j.properties" or "log4j.xml" in the class path; see the
 * log4j documentation for details).
 * <li><i>"log4jRefreshInterval":</i><br>
 * Interval between config file refresh checks, in milliseconds. If not specified,
 * no refresh checks will happen, which avoids starting log4j's watchdog thread.
 * <li><i>"log4jExposeWebAppRoot":</i><br>
 * Whether the web app root system property should be exposed, allowing for log
 * file paths relative to the web application root directory. Default is "true";
 * specify "false" to suppress expose of the web app root system property. See
 * below for details on how to use this system property in log file locations.
 * </ul>
 *
 * <p>Note: <code>initLogging</code> should be called before any other Spring activity
 * (when using log4j), for proper initialization before any Spring logging attempts.
 *
 * <p>Log4j's watchdog thread will asynchronously check whether the timestamp
 * of the config file has changed, using the given interval between checks.
 * A refresh interval of 1000 milliseconds (one second), which allows to
 * do on-demand log level changes with immediate effect, is not unfeasible.

 * <p><b>WARNING:</b> Log4j's watchdog thread does not terminate until VM shutdown;
 * in particular, it does not terminate on LogManager shutdown. Therefore, it is
 * recommended to <i>not</i> use config file refreshing in a production J2EE
 * environment; the watchdog thread would not stop on application shutdown there.
 *
 * <p>By default, this configurer automatically sets the web app root system property,
 * for "${key}" substitutions within log file locations in the log4j config file,
 * allowing for log file paths relative to the web application root directory.
 * The default system property key is "webapp.root", to be used in a log4j config
 * file like as follows:
 *
 * <p><code>log4j.appender.myfile.File=${webapp.root}/WEB-INF/demo.log</code>
 *
 * <p>Alternatively, specify a unique context-param "webAppRootKey" per web application.
 * For example, with "webAppRootKey = "demo.root":
 *
 * <p><code>log4j.appender.myfile.File=${demo.root}/WEB-INF/demo.log</code>
 *
 * <p><b>WARNING:</b> Some containers (like Tomcat) do <i>not</i> keep system properties
 * separate per web app. You have to use unique "webAppRootKey" context-params per web
 * app then, to avoid clashes. Other containers like Resin do isolate each web app's
 * system properties: Here you can use the default key (i.e. no "webAppRootKey"
 * context-param at all) without worrying.
 *
 * @author Juergen Hoeller
 * @since 12.08.2003
 * @see org.springframework.util.Log4jConfigurer
 * @see Log4jConfigListener
 */
public abstract class Log4jWebConfigurer {

	/** Parameter specifying the location of the log4j config file */
	public static final String CONFIG_LOCATION_PARAM = "log4jConfigLocation";

	/** Parameter specifying the refresh interval for checking the log4j config file */
	public static final String REFRESH_INTERVAL_PARAM = "log4jRefreshInterval";

	/** Parameter specifying whether to expose the web app root system property */
	public static final String EXPOSE_WEB_APP_ROOT_PARAM = "log4jExposeWebAppRoot";


	/**
	 * Initialize log4j, including setting the web app root system property.
	 * @param servletContext the current ServletContext
	 * @see WebUtils#setWebAppRootSystemProperty
	 */
	public static void initLogging(ServletContext servletContext) {
		// Expose the web app root system property.
		if (exposeWebAppRoot(servletContext)) {
			WebUtils.setWebAppRootSystemProperty(servletContext);
		}

		// Only perform custom log4j initialization in case of a config file.
		String location = servletContext.getInitParameter(CONFIG_LOCATION_PARAM);
		if (location != null) {
			// Perform actual log4j initialization; else rely on log4j's default initialization.
			try {
				// Resolve system property placeholders before potentially
				// resolving a real path.
				location = SystemPropertyUtils.resolvePlaceholders(location);

				// Leave a URL (e.g. "classpath:" or "file:") as-is.
				if (!ResourceUtils.isUrl(location)) {
					// Consider a plain file path as relative to the web
					// application root directory.
					location = WebUtils.getRealPath(servletContext, location);
				}

				// Write log message to server log.
				servletContext.log("Initializing log4j from [" + location + "]");

				// Check whether refresh interval was specified.
				String intervalString = servletContext.getInitParameter(REFRESH_INTERVAL_PARAM);
				if (intervalString != null) {
					// Initialize with refresh interval, i.e. with log4j's watchdog thread,
					// checking the file in the background.
					try {
						long refreshInterval = Long.parseLong(intervalString);
						Log4jConfigurer.initLogging(location, refreshInterval);
					}
					catch (NumberFormatException ex) {
						throw new IllegalArgumentException("Invalid 'log4jRefreshInterval' parameter: " + ex.getMessage());
					}
				}
				else {
					// Initialize without refresh check, i.e. without log4j's watchdog thread.
					Log4jConfigurer.initLogging(location);
				}
			}
			catch (FileNotFoundException ex) {
				throw new IllegalArgumentException("Invalid 'log4jConfigLocation' parameter: " + ex.getMessage());
			}
		}
	}

	/**
	 * Shut down log4j, properly releasing all file locks
	 * and resetting the web app root system property.
	 * @param servletContext the current ServletContext
	 * @see WebUtils#removeWebAppRootSystemProperty
	 */
	public static void shutdownLogging(ServletContext servletContext) {
		servletContext.log("Shutting down log4j");
		try {
			Log4jConfigurer.shutdownLogging();
		}
		finally {
			// Remove the web app root system property.
			if (exposeWebAppRoot(servletContext)) {
				WebUtils.removeWebAppRootSystemProperty(servletContext);
			}
		}
	}

	/**
	 * Return whether to expose the web app root system property,
	 * checking the corresponding ServletContext init parameter.
	 * @see #EXPOSE_WEB_APP_ROOT_PARAM
	 */
	private static boolean exposeWebAppRoot(ServletContext servletContext) {
		String exposeWebAppRootParam = servletContext.getInitParameter(EXPOSE_WEB_APP_ROOT_PARAM);
		return (exposeWebAppRootParam == null || Boolean.valueOf(exposeWebAppRootParam));
	}

}
