/*
 * Copyright 2002-2011 the original author or authors.
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

import static org.junit.Assert.assertTrue;

import java.net.URL;

import javax.servlet.ServletContextEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.mock.web.MockServletContext;

/**
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 21.02.2005
 */
public class Log4jWebConfigurerTests {

	private static final String TESTLOG4J_PROPERTIES = "testlog4j.properties";
	private static final String CLASSPATH_RESOURCE = "classpath:org/springframework/web/util/testlog4j.properties";
	private static final String RELATIVE_PATH = "src/test/resources/org/springframework/web/util/testlog4j.properties";

	@Test
	public void initLoggingWithClasspathResource() {
		initLogging(CLASSPATH_RESOURCE, false);
	}

	@Test
	public void initLoggingWithClasspathResourceAndRefreshInterval() {
		initLogging(CLASSPATH_RESOURCE, true);
	}
	
	@Test
	public void initLoggingWithRelativeFilePath() {
		initLogging(RELATIVE_PATH, false);
	}

	@Test
	public void initLoggingWithRelativeFilePathAndRefreshInterval() {
		initLogging(RELATIVE_PATH, true);
	}
	
	@Test
	public void initLoggingWithUrl() {
		URL url = Log4jWebConfigurerTests.class.getResource(TESTLOG4J_PROPERTIES);
		initLogging(url.toString(), false);
	}

	@Test
	public void initLoggingWithUrlAndRefreshInterval() {
		URL url = Log4jWebConfigurerTests.class.getResource(TESTLOG4J_PROPERTIES);
		initLogging(url.toString(), true);
	}

	@Ignore("Only works on MS Windows")
	@Test
	public void initLoggingWithAbsoluteFilePathAndRefreshInterval() {
		URL url = Log4jWebConfigurerTests.class.getResource(TESTLOG4J_PROPERTIES);
		initLogging(url.getFile(), true);
	}

	private void initLogging(String location, boolean refreshInterval) {
		MockServletContext sc = new MockServletContext("", new FileSystemResourceLoader());
		sc.addInitParameter(Log4jWebConfigurer.CONFIG_LOCATION_PARAM, location);
		if (refreshInterval) {
			sc.addInitParameter(Log4jWebConfigurer.REFRESH_INTERVAL_PARAM, "10");
		}
		Log4jWebConfigurer.initLogging(sc);

		try {
			assertLogOutput();
		} finally {
			Log4jWebConfigurer.shutdownLogging(sc);
		}
		assertTrue(MockLog4jAppender.closeCalled);
	}

	private void assertLogOutput() {
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

	@Test
	public void testLog4jConfigListener() {
		Log4jConfigListener listener = new Log4jConfigListener();
		
		MockServletContext sc = new MockServletContext("", new FileSystemResourceLoader());
		sc.addInitParameter(Log4jWebConfigurer.CONFIG_LOCATION_PARAM, RELATIVE_PATH);
		listener.contextInitialized(new ServletContextEvent(sc));
		
		try {
			assertLogOutput();
		} finally {
			listener.contextDestroyed(new ServletContextEvent(sc));
		}
		assertTrue(MockLog4jAppender.closeCalled);
	}

}
