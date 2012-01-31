/*
 * Copyright 2002-2005 the original author or authors.
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

import java.io.FileNotFoundException;
import java.net.URL;

import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Alef Arendsen
 * @author Juergen Hoeller
 */
public class Log4jConfigurerTests extends TestCase {

	public void testInitLoggingWithClasspath() throws FileNotFoundException {
		doTestInitLogging("classpath:org/springframework/util/testlog4j.properties", false);
	}

	public void testInitLoggingWithRelativeFilePath() throws FileNotFoundException {
		doTestInitLogging("src/test/resources/org/springframework/util/testlog4j.properties", false);
	}

	public void testInitLoggingWithAbsoluteFilePath() throws FileNotFoundException {
		URL url = getClass().getResource("testlog4j.properties");
		doTestInitLogging(url.toString(), false);
	}

	public void testInitLoggingWithClasspathAndRefreshInterval() throws FileNotFoundException {
		doTestInitLogging("classpath:org/springframework/util/testlog4j.properties", true);
	}

	public void testInitLoggingWithRelativeFilePathAndRefreshInterval() throws FileNotFoundException {
		doTestInitLogging("src/test/resources/org/springframework/util/testlog4j.properties", true);
	}

	/* only works on Windows
	public void testInitLoggingWithAbsoluteFilePathAndRefreshInterval() throws FileNotFoundException {
		URL url = getClass().getResource("testlog4j.properties");
		doTestInitLogging(url.getFile(), true);
	}
	*/

	public void testInitLoggingWithFileUrlAndRefreshInterval() throws FileNotFoundException {
		URL url = getClass().getResource("testlog4j.properties");
		doTestInitLogging(url.toString(), true);
	}

	private void doTestInitLogging(String location, boolean refreshInterval) throws FileNotFoundException {
		if (refreshInterval) {
			Log4jConfigurer.initLogging(location, 10);
		}
		else {
			Log4jConfigurer.initLogging(location);
		}

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

		Log4jConfigurer.shutdownLogging();
		assertTrue(MockLog4jAppender.closeCalled);
	}

	public void testInitLoggingWithRefreshIntervalAndFileNotFound() throws FileNotFoundException {
		try {
			Log4jConfigurer.initLogging("test/org/springframework/util/bla.properties", 10);
			fail("Exception should have been thrown, file does not exist!");
		}
		catch (FileNotFoundException ex) {
			// OK
		}
	}

}
