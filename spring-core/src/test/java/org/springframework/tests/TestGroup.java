/*
 * Copyright 2002-2013 the original author or authors.
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

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * A test group used to limit when certain tests are run.
 *
 * @see Assume#group(TestGroup)
 * @author Phillip Webb
 */
public enum TestGroup {


	/**
	 * Performance related tests that may take a considerable time to run.
	 */
	PERFORMANCE;


	/**
	 * Parse the specified comma separates string of groups.
	 * @param value the comma separated string of groups
	 * @return a set of groups
	 */
	public static Set<TestGroup> parse(String value) {
		if (value == null || "".equals(value)) {
			return Collections.emptySet();
		}
		if("ALL".equalsIgnoreCase(value)) {
			return EnumSet.allOf(TestGroup.class);
		}
		Set<TestGroup> groups = new HashSet<TestGroup>();
		for (String group : value.split(",")) {
			try {
				groups.add(valueOf(group.trim().toUpperCase()));
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("Unable to find test group '" + group.trim()
						+ "' when parsing '" + value + "'");
			}
		}
		return groups;
	}
}
