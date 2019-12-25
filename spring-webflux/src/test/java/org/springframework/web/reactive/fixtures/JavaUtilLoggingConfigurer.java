/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.reactive.fixtures;

import java.io.InputStream;
import java.util.logging.LogManager;

import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

/**
 * JUnit Platform {@link TestExecutionListener} that configures Java Util Logging
 * (JUL) from a file named {@code jul-test.properties} in the root of the classpath.
 *
 * <p>This allows for projects to configure JUL for a test suite, analogous to
 * log4j's support via {@code log4j2-test.xml}.
 *
 * <p>This listener can be automatically registered on the JUnit Platform by
 * adding the fully qualified name of this class to a file named
 * {@code /META-INF/services/org.junit.platform.launcher.TestExecutionListener}
 * &mdash; for example, under {@code src/test/resources}.
 *
 * @author Sam Brannen
 * @since 5.2.2
 */
public class JavaUtilLoggingConfigurer implements TestExecutionListener {

	public static final String JUL_TEST_PROPERTIES_FILE = "jul-test.properties";


	@Override
	public void testPlanExecutionStarted(TestPlan testPlan) {
		try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(JUL_TEST_PROPERTIES_FILE)) {
			LogManager.getLogManager().readConfiguration(inputStream);
		}
		catch (Exception ex) {
			System.err.println("WARNING: failed to configure Java Util Logging from classpath resource " +
					JUL_TEST_PROPERTIES_FILE);
			System.err.println(ex);
		}
	}
}
