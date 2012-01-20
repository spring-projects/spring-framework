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

package org.springframework.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;

import org.apache.log4j.LogManager;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.xml.DOMConfigurator;

/**
 * Convenience class that features simple methods for custom log4j configuration.
 *
 * <p>Only needed for non-default log4j initialization, for example with a custom
 * config location or a refresh interval. By default, log4j will simply read its
 * configuration from a "log4j.properties" or "log4j.xml" file in the root of
 * the classpath.
 *
 * <p>For web environments, the analogous Log4jWebConfigurer class can be found
 * in the web package, reading in its configuration from context-params in
 * <code>web.xml</code>. In a J2EE web application, log4j is usually set up
 * via Log4jConfigListener or Log4jConfigServlet, delegating to
 * Log4jWebConfigurer underneath.
 *
 * @author Juergen Hoeller
 * @since 13.03.2003
 * @see org.springframework.web.util.Log4jWebConfigurer
 * @see org.springframework.web.util.Log4jConfigListener
 */
public abstract class Log4jConfigurer {

	/** Pseudo URL prefix for loading from the class path: "classpath:" */
	public static final String CLASSPATH_URL_PREFIX = "classpath:";

	/** Extension that indicates a log4j XML config file: ".xml" */
	public static final String XML_FILE_EXTENSION = ".xml";


	/**
	 * Initialize log4j from the given file location, with no config file refreshing.
	 * Assumes an XML file in case of a ".xml" file extension, and a properties file
	 * otherwise.
	 * @param location the location of the config file: either a "classpath:" location
	 * (e.g. "classpath:myLog4j.properties"), an absolute file URL
	 * (e.g. "file:C:/log4j.properties), or a plain absolute path in the file system
	 * (e.g. "C:/log4j.properties")
	 * @throws FileNotFoundException if the location specifies an invalid file path
	 */
	public static void initLogging(String location) throws FileNotFoundException {
		String resolvedLocation = SystemPropertyUtils.resolvePlaceholders(location);
		URL url = ResourceUtils.getURL(resolvedLocation);
		if (resolvedLocation.toLowerCase().endsWith(XML_FILE_EXTENSION)) {
			DOMConfigurator.configure(url);
		}
		else {
			PropertyConfigurator.configure(url);
		}
	}

	/**
	 * Initialize log4j from the given location, with the given refresh interval
	 * for the config file. Assumes an XML file in case of a ".xml" file extension,
	 * and a properties file otherwise.
	 * <p>Log4j's watchdog thread will asynchronously check whether the timestamp
	 * of the config file has changed, using the given interval between checks.
	 * A refresh interval of 1000 milliseconds (one second), which allows to
	 * do on-demand log level changes with immediate effect, is not unfeasible.
	 * <p><b>WARNING:</b> Log4j's watchdog thread does not terminate until VM shutdown;
	 * in particular, it does not terminate on LogManager shutdown. Therefore, it is
	 * recommended to <i>not</i> use config file refreshing in a production J2EE
	 * environment; the watchdog thread would not stop on application shutdown there.
	 * @param location the location of the config file: either a "classpath:" location
	 * (e.g. "classpath:myLog4j.properties"), an absolute file URL
	 * (e.g. "file:C:/log4j.properties), or a plain absolute path in the file system
	 * (e.g. "C:/log4j.properties")
	 * @param refreshInterval interval between config file refresh checks, in milliseconds
	 * @throws FileNotFoundException if the location specifies an invalid file path
	 */
	public static void initLogging(String location, long refreshInterval) throws FileNotFoundException {
		String resolvedLocation = SystemPropertyUtils.resolvePlaceholders(location);
		File file = ResourceUtils.getFile(resolvedLocation);
		if (!file.exists()) {
			throw new FileNotFoundException("Log4j config file [" + resolvedLocation + "] not found");
		}
		if (resolvedLocation.toLowerCase().endsWith(XML_FILE_EXTENSION)) {
			DOMConfigurator.configureAndWatch(file.getAbsolutePath(), refreshInterval);
		}
		else {
			PropertyConfigurator.configureAndWatch(file.getAbsolutePath(), refreshInterval);
		}
	}

	/**
	 * Shut down log4j, properly releasing all file locks.
	 * <p>This isn't strictly necessary, but recommended for shutting down
	 * log4j in a scenario where the host VM stays alive (for example, when
	 * shutting down an application in a J2EE environment).
	 */
	public static void shutdownLogging() {
		LogManager.shutdown();
	}

	/**
	 * Set the specified system property to the current working directory.
	 * <p>This can be used e.g. for test environments, for applications that leverage
	 * Log4jWebConfigurer's "webAppRootKey" support in a web environment.
	 * @param key system property key to use, as expected in Log4j configuration
	 * (for example: "demo.root", used as "${demo.root}/WEB-INF/demo.log")
	 * @see org.springframework.web.util.Log4jWebConfigurer
	 */
	public static void setWorkingDirSystemProperty(String key) {
		System.setProperty(key, new File("").getAbsolutePath());
	}

}
