/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.junit;

import static org.junit.Assume.assumeThat;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Assume;
import org.springframework.core.JdkVersion;

/**
 * Provides utility methods that allow JUnit tests to {@link Assume} certain conditions
 * hold {@code true}. If the assumption fails, it means the test should be skipped.
 *
 * <p>For example, if a set of tests require at least JDK 1.7 it can use
 * {@link #assumeAtLeastJdk17()} as shown below:
 *
 * <pre class="code">
 * public void MyTests {
 *
 *   &#064;BeforeClass
 *   public static assumptions() {
 *       Assumptions.assumeAtLeastJdk17();
 *   }
 *
 *   // ... all the test methods that require at least JDK 1.7
 * }
 * </pre>
 *
 * If only a single test requires at least JDK 1.7 it can use the
 * {@link #assumeAtLeastJdk17()} as shown below:
 *
 * <pre class="code">
 * public void MyTests {
 *
 *   &#064;Test
 *   public void requiresJdk17 {
 *       Assumptions.assumeAtLeastJdk17();
 *       // ... perform the actual test
 *   }
 * }
 * </pre>
 *
 * @author Rob Winch
 * @since 3.2
 */
public final class Assumptions {
	/**
	 * Descriptions of the JDK Version for {@link JdkVersion} constants
	 */
	private static final String[] JDK_VERSION_DESCRIPTIONS = {
		"JDK 1.3", "JDK 1.4", "JDK 1.5", "JDK 1.6", "JDK 1.7"};

	/**
	 * Utility method that allows tests to {@link Assume} that JDK 1.7 or greater is
	 * being used.
	 */
	public static void assumeAtLeastJdk17() {
		Matcher<Integer> atLeastJdk17 = atLeastJdk(JdkVersion.JAVA_17);
		assumeThat(JdkVersion.getMajorJavaVersion(), atLeastJdk17);
	}

	/**
	 * Obtain a {@link TypeSafeMatcher} for {@link JdkVersion} being at least a given
	 * version.
	 *
	 * @param jdkMajorVersion a constant from {@link JdkVersion} to verify that the JDK
	 * is greater than or equal to.
	 * @return
	 */
	private static TypeSafeMatcher<Integer> atLeastJdk(final int jdkMajorVersion) {
		return new TypeSafeMatcher<Integer>() {
			private int actual;

			public void describeTo(Description description) {
				description.appendText("\nExpected: JDK major version of ");
				description.appendValue(jdkVersionDescription(jdkMajorVersion));
				description.appendText(" or greater, but got ");
				description.appendValue(jdkVersionDescription(actual));
			}

			public boolean matchesSafely(Integer other) {
				actual = other;
				return other >= jdkMajorVersion;
			}
		};
	}

	/**
	 * Look up the description for a specific JDK.
	 * @param javaMajorVersion a constant from {@link JdkVersion} that represents the
	 * Java Version.
	 * @return
	 */
	private static String jdkVersionDescription(int javaMajorVersion) {
		if(javaMajorVersion >= 0 && javaMajorVersion < JDK_VERSION_DESCRIPTIONS.length) {
			return JDK_VERSION_DESCRIPTIONS[javaMajorVersion];
		}
		return "[Unknown JDK description org.springframework.core.JavaVersion is \""
			+ javaMajorVersion + "\"]";
	}

	private Assumptions() {}
}