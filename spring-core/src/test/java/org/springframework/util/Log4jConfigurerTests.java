/*
 * Copyright 2002-2015 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Alef Arendsen
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
@SuppressWarnings("deprecation")
public class Log4jConfigurerTests {

	@Test
	public void initLoggingWithClasspath() throws FileNotFoundException {
		doTestInitLogging("classpath:org/springframework/util/testlog4j.properties", false);
	}

	@Test
	public void initLoggingWithRelativeFilePath() throws FileNotFoundException {
		doTestInitLogging("src/test/resources/org/springframework/util/testlog4j.properties", false);
	}

	@Test
	public void initLoggingWithAbsoluteFilePath() throws FileNotFoundException {
		URL url = getClass().getResource("testlog4j.properties");
		doTestInitLogging(url.toString(), false);
	}

	@Test
	public void initLoggingWithClasspathAndRefreshInterval() throws FileNotFoundException {
		doTestInitLogging("classpath:org/springframework/util/testlog4j.properties", true);
	}

	@Test
	public void initLoggingWithRelativeFilePathAndRefreshInterval() throws FileNotFoundException {
		doTestInitLogging("src/test/resources/org/springframework/util/testlog4j.properties", true);
	}

	@Test
	public void initLoggingWithAbsoluteFilePathAndRefreshInterval() throws FileNotFoundException {
		URL url = getClass().getResource("testlog4j.properties");
		doTestInitLogging(url.getFile(), true);
	}

	@Test
	public void initLoggingWithFileUrlAndRefreshInterval() throws FileNotFoundException {
		URL url = getClass().getResource("testlog4j.properties");
		doTestInitLogging(url.toString(), true);
	}

	@Test(expected = FileNotFoundException.class)
	public void initLoggingWithRefreshIntervalAndFileNotFound() throws FileNotFoundException {
		Log4jConfigurer.initLogging("test/org/springframework/util/bla.properties", 10);
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

}

