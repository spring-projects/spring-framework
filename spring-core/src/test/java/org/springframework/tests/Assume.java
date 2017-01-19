/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.tests;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.junit.AssumptionViolatedException;

import static org.junit.Assume.*;

/**
 * Provides utility methods that allow JUnit tests to {@link org.junit.Assume} certain
 * conditions hold {@code true}. If the assumption fails, it means the test should be
 * skipped.
 *
 * <p>Tests can be categorized into {@link TestGroup}s. Active groups are enabled using
 * the 'testGroups' system property, usually activated from the gradle command line:
 *
 * <pre class="code">
 * gradle test -PtestGroups="performance"
 * </pre>
 *
 * <p>Groups can be specified as a comma separated list of values, or using the pseudo group
 * 'all'. See {@link TestGroup} for a list of valid groups.
 *
 * @author Rob Winch
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 3.2
 * @see #group(TestGroup)
 * @see #group(TestGroup, Executable)
 */
public abstract class Assume {

	static final String TEST_GROUPS_SYSTEM_PROPERTY = "testGroups";


	/**
	 * Assume that a particular {@link TestGroup} has been specified.
	 * @param group the group that must be specified
	 * @throws AssumptionViolatedException if the assumption fails
	 */
	public static void group(TestGroup group) {
		Set<TestGroup> testGroups = loadTestGroups();
		if (!testGroups.contains(group)) {
			throw new AssumptionViolatedException("Requires unspecified group " + group + " from " + testGroups);
		}
	}

	/**
	 * Assume that a particular {@link TestGroup} has been specified before
	 * executing the supplied {@link Executable}.
	 * <p>If the assumption fails, the executable will not be executed, but
	 * no {@link AssumptionViolatedException} will be thrown.
	 * @param group the group that must be specified
	 * @param executable the executable to execute if the test group is active
	 * @since 4.2
	 */
	public static void group(TestGroup group, Executable executable) throws Exception {
		Set<TestGroup> testGroups = loadTestGroups();
		if (testGroups.contains(group)) {
			executable.execute();
		}
	}

	/**
	 * Assume that the specified log is not set to Trace or Debug.
	 * @param log the log to test
	 * @throws AssumptionViolatedException if the assumption fails
	 */
	public static void notLogging(Log log) {
		assumeFalse(log.isTraceEnabled());
		assumeFalse(log.isDebugEnabled());
	}

	/**
	 * Load test groups dynamically instead of during static
	 * initialization in order to avoid a {@link NoClassDefFoundError}
	 * being thrown while attempting to load the {@code Assume} class.
	 */
	private static Set<TestGroup> loadTestGroups() {
		try {
			return TestGroup.parse(System.getProperty(TEST_GROUPS_SYSTEM_PROPERTY));
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to parse '" + TEST_GROUPS_SYSTEM_PROPERTY
					+ "' system property: " + ex.getMessage(), ex);
		}
	}


	/**
	 * @since 4.2
	 */
	@FunctionalInterface
	public interface Executable {

		void execute() throws Exception;
	}

}
