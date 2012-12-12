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

package org.springframework.build.junit;

import static org.junit.Assume.assumeFalse;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.junit.internal.AssumptionViolatedException;

/**
 * Provides utility methods that allow JUnit tests to {@link Assume} certain conditions
 * hold {@code true}. If the assumption fails, it means the test should be skipped.
 *
 * <p>For example, if a set of tests require at least JDK 1.7 it can use
 * {@code Assume#atLeast(JdkVersion.JAVA_17)} as shown below:
 *
 * <pre class="code">
 * public void MyTests {
 *
 *   &#064;BeforeClass
 *   public static assumptions() {
 *       Assume.atLeast(JdkVersion.JAVA_17);
 *   }
 *
 *   // ... all the test methods that require at least JDK 1.7
 * }
 * </pre>
 *
 * If only a single test requires at least JDK 1.7 it can use the
 * {@code Assume#atLeast(JdkVersion.JAVA_17)} as shown below:
 *
 * <pre class="code">
 * public void MyTests {
 *
 *   &#064;Test
 *   public void requiresJdk17 {
 *       Assume.atLeast(JdkVersion.JAVA_17);
 *       // ... perform the actual test
 *   }
 * }
 * </pre>
 *
 * In addition to assumptions based on the JDK version, tests can be categorized into
 * {@link TestGroup}s. Active groups are enabled using the 'testGroups' system property,
 * usually activated from the gradle command line:
 * <pre>
 * gradle test -PtestGroups="performance"
 * </pre>
 *
 * Groups can be specified as a comma separated list of values, or using the pseudo group
 * 'all'. See {@link TestGroup} for a list of valid groups.
 *
 * @author Rob Winch
 * @author Phillip Webb
 * @since 3.2
 * @see #atLeast(JavaVersion)
 * @see #group(TestGroup)
 */
public abstract class Assume {


	private static final Set<TestGroup> GROUPS = TestGroup.parse(System.getProperty("testGroups"));


	/**
	 * Assume a minimum {@link JavaVersion} is running.
	 * @param version the minimum version for the test to run
	 */
	public static void atLeast(JavaVersion version) {
		if (!JavaVersion.runningVersion().isAtLeast(version)) {
			throw new AssumptionViolatedException("Requires JDK " + version + " but running "
					+ JavaVersion.runningVersion());
		}
	}

	/**
	 * Assume that a particular {@link TestGroup} has been specified.
	 * @param group the group that must be specified.
	 */
	public static void group(TestGroup group) {
		if (!GROUPS.contains(group)) {
			throw new AssumptionViolatedException("Requires unspecified group " + group
					+ " from " + GROUPS);
		}
	}

	/**
	 * Assume that the specified log is not set to Trace or Debug.
	 * @param log the log to test
	 */
	public static void notLogging(Log log) {
		assumeFalse(log.isTraceEnabled());
		assumeFalse(log.isDebugEnabled());
	}
}
