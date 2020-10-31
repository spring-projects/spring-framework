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

package org.springframework.core.testfixture;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.springframework.util.StringUtils;

import static java.lang.String.format;

/**
 * A test group used to limit when certain tests are run.
 *
 * @see EnabledForTestGroups @EnabledForTestGroups
 * @author Phillip Webb
 * @author Chris Beams
 * @author Sam Brannen
 */
public enum TestGroup {

	/**
	 * Tests that take a considerable amount of time to run. Any test lasting longer than
	 * 500ms should be considered a candidate in order to avoid making the overall test
	 * suite too slow to run during the normal development cycle.
	 */
	LONG_RUNNING;


	/**
	 * Determine if this {@link TestGroup} is active.
	 * @since 5.2
	 */
	public boolean isActive() {
		return loadTestGroups().contains(this);
	}


	private static final String TEST_GROUPS_SYSTEM_PROPERTY = "testGroups";

	/**
	 * Load test groups dynamically instead of during static initialization in
	 * order to avoid a {@link NoClassDefFoundError} being thrown while attempting
	 * to load collaborator classes.
	 */
	static Set<TestGroup> loadTestGroups() {
		try {
			return TestGroup.parse(System.getProperty(TEST_GROUPS_SYSTEM_PROPERTY));
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to parse '" + TEST_GROUPS_SYSTEM_PROPERTY +
					"' system property: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Parse the specified comma separated string of groups.
	 * @param value the comma separated string of groups
	 * @return a set of groups
	 * @throws IllegalArgumentException if any specified group name is not a
	 * valid {@link TestGroup}
	 */
	static Set<TestGroup> parse(String value) throws IllegalArgumentException {
		if (!StringUtils.hasText(value)) {
			return Collections.emptySet();
		}
		String originalValue = value;
		value = value.trim();
		if ("ALL".equalsIgnoreCase(value)) {
			return EnumSet.allOf(TestGroup.class);
		}
		if (value.toUpperCase().startsWith("ALL-")) {
			Set<TestGroup> groups = EnumSet.allOf(TestGroup.class);
			groups.removeAll(parseGroups(originalValue, value.substring(4)));
			return groups;
		}
		return parseGroups(originalValue, value);
	}

	private static Set<TestGroup> parseGroups(String originalValue, String value) throws IllegalArgumentException {
		Set<TestGroup> groups = new HashSet<>();
		for (String group : value.split(",")) {
			try {
				groups.add(valueOf(group.trim().toUpperCase()));
			}
			catch (IllegalArgumentException ex) {
				throw new IllegalArgumentException(format(
						"Unable to find test group '%s' when parsing testGroups value: '%s'. " +
						"Available groups include: [%s]", group.trim(), originalValue,
						StringUtils.arrayToCommaDelimitedString(TestGroup.values())));
			}
		}
		return groups;
	}

}
