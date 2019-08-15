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

package org.springframework.tests;

import java.util.Set;

/**
 * Utility methods for working with {@link TestGroup}s.
 *
 * @author Sam Brannen
 * @author Rob Winch
 * @author Phillip Webb
 * @since 5.2
 */
public abstract class TestGroups {

	static final String TEST_GROUPS_SYSTEM_PROPERTY = "testGroups";


	/**
	 * Determine if the provided {@link TestGroup} is active.
	 * @param group the group to check
	 * @since 5.2
	 */
	public static boolean isGroupActive(TestGroup group) {
		return loadTestGroups().contains(group);
	}

	/**
	 * Load test groups dynamically instead of during static initialization in
	 * order to avoid a {@link NoClassDefFoundError} being thrown while attempting
	 * to load the {@link Assume} class.
	 */
	static Set<TestGroup> loadTestGroups() {
		try {
			return TestGroup.parse(System.getProperty(TEST_GROUPS_SYSTEM_PROPERTY));
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to parse '" + TEST_GROUPS_SYSTEM_PROPERTY
					+ "' system property: " + ex.getMessage(), ex);
		}
	}

}
