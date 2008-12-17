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

import java.io.FileNotFoundException;
import java.net.URL;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletException;

import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Ignore;

import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

/**
 * @author Juergen Hoeller
 * @since 21.02.2005
 */
@Ignore
public class Log4jWebConfigurerTests extends TestCase {

	public void testInitLoggingWithClasspath() throws FileNotFoundException {
		doTestInitLogging("classpath:org/springframework/util/testlog4j.properties", false);
	}

	public void testInitLoggingWithRelativeFilePath() throws FileNotFoundException {
		doTestInitLogging("test/org/springframework/util/testlog4j.properties", false);
	}

	public void testInitLoggingWithAbsoluteFilePath() throws FileNotFoundException {
		URL url = Log4jWebConfigurerTests.class.getResource("testlog4j.properties");
		doTestInitLogging(url.toString(), false);
	}

	public void testInitLoggingWithClasspathAndRefreshInterval() throws FileNotFoundException {
		doTestInitLogging("classpath:org/springframework/util/testlog4j.properties", true);
	}

	public void testInitLoggingWithRelativeFilePathAndRefreshInterval() throws FileNotFoundException {
		doTestInitLogging("test/org/springframework/util/testlog4j.properties", true);
	}

	/* only works on Windows
	public void testInitLoggingWithAbsoluteFilePathAndRefreshInterval() throws FileNotFoundException {
		URL url = Log4jConfigurerTests.class.getResource("testlog4j.properties");
		doTestInitLogging(url.getFile(), true);
	}
	*/

	public void testInitLoggingWithFileUrlAndRefreshInterval() throws FileNotFoundException {
		URL url = Log4jWebConfigurerTests.class.getResource("testlog4j.properties");
		doTestInitLogging(url.toString(), true);
	}

	private void doTestInitLogging(String location, boolean refreshInterval) {
		MockServletContext sc = new MockServletContext("", new FileSystemResourceLoader());
		sc.addInitParameter(Log4jWebConfigurer.CONFIG_LOCATION_PARAM, location);
		if (refreshInterval) {
			sc.addInitParameter(Log4jWebConfigurer.REFRESH_INTERVAL_PARAM, "10");
		}
		Log4jWebConfigurer.initLogging(sc);

		try {
			doTestLogOutput();
		}
		finally {
			Log4jWebConfigurer.shutdownLogging(sc);
		}
		assertTrue(MockLog4jAppender.closeCalled);
	}

	private void doTestLogOutput() {
		Log log = LogFactory.getLog(this.getClass());
		log.debug("debug");
		log.info("info");
		log.warn("warn");
		log.error("error");
		log.fatal("fatal");

		assertTrue(MockLog4jAppender.loggingStrings.contains("debug"));
		assertTrue(MockLog4jAppender.loggingStrings.contains("info"));
		assertTrue(MockLog4jAppender.loggingStrings.contains("warn"));
		assertTrue(MockLog4jAppender.loggingStrings.contains("error"));
		assertTrue(MockLog4jAppender.loggingStrings.contains("fatal"));
	}

	public void testLog4jConfigListener() {
		Log4jConfigListener listener = new Log4jConfigListener();

		MockServletContext sc = new MockServletContext("", new FileSystemResourceLoader());
		sc.addInitParameter(Log4jWebConfigurer.CONFIG_LOCATION_PARAM,
				"test/org/springframework/util/testlog4j.properties");
		listener.contextInitialized(new ServletContextEvent(sc));

		try {
			doTestLogOutput();
		}
		finally {
			listener.contextDestroyed(new ServletContextEvent(sc));
		}
		assertTrue(MockLog4jAppender.closeCalled);
	}

}
