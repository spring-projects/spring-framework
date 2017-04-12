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

import java.util.Arrays;

import org.junit.After;
import org.junit.AssumptionViolatedException;
import org.junit.Before;
import org.junit.Test;

import static java.util.stream.Collectors.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.tests.Assume.*;
import static org.springframework.tests.TestGroup.*;

/**
 * Tests for {@link Assume}.
 *
 * @author Sam Brannen
 * @since 5.0
 */
public class AssumeTests {

	private String originalTestGroups;


	@Before
	public void trackOriginalTestGroups() {
		this.originalTestGroups = System.getProperty(TEST_GROUPS_SYSTEM_PROPERTY);
	}

	@After
	public void restoreOriginalTestGroups() {
		if (this.originalTestGroups != null) {
			setTestGroups(this.originalTestGroups);
		}
		else {
			setTestGroups("");
		}
	}

	@Test
	public void assumeGroupWithNoActiveTestGroups() {
		setTestGroups("");
		Assume.group(JMXMP);
		fail("assumption should have failed");
	}

	@Test
	public void assumeGroupWithNoMatchingActiveTestGroup() {
		setTestGroups(PERFORMANCE, CI);
		Assume.group(JMXMP);
		fail("assumption should have failed");
	}

	@Test
	public void assumeGroupWithMatchingActiveTestGroup() {
		setTestGroups(JMXMP);
		try {
			Assume.group(JMXMP);
		}
		catch (AssumptionViolatedException ex) {
			fail("assumption should NOT have failed");
		}
	}

	@Test
	public void assumeGroupWithBogusActiveTestGroup() {
		assertBogusActiveTestGroupBehavior("bogus");
	}

	@Test
	public void assumeGroupWithAllMinusBogusActiveTestGroup() {
		assertBogusActiveTestGroupBehavior("all-bogus");
	}

	private void assertBogusActiveTestGroupBehavior(String testGroups) {
		// Should result in something similar to the following:
		//
		// java.lang.IllegalStateException: Failed to parse 'testGroups' system property:
		// Unable to find test group 'bogus' when parsing testGroups value: 'all-bogus'.
		// Available groups include: [LONG_RUNNING,PERFORMANCE,JMXMP,CI]

		setTestGroups(testGroups);
		try {
			Assume.group(JMXMP);
			fail("assumption should have failed");
		}
		catch (IllegalStateException ex) {
			assertThat(ex.getMessage(),
				startsWith("Failed to parse '" + TEST_GROUPS_SYSTEM_PROPERTY + "' system property: "));

			assertThat(ex.getCause(), instanceOf(IllegalArgumentException.class));
			assertThat(ex.getCause().getMessage(),
				equalTo("Unable to find test group 'bogus' when parsing testGroups value: '" + testGroups
						+ "'. Available groups include: [LONG_RUNNING,PERFORMANCE,JMXMP,CI]"));
		}
	}

	private void setTestGroups(TestGroup... testGroups) {
		setTestGroups(Arrays.stream(testGroups).map(TestGroup::name).collect(joining(", ")));
	}

	private void setTestGroups(String testGroups) {
		System.setProperty(TEST_GROUPS_SYSTEM_PROPERTY, testGroups);
	}

}
