/*
 * Copyright 2002-2017 the original author or authors.
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

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * Tests for {@link TestGroup}.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 */
public class TestGroupTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();


	@Test
	public void parseNull() {
		assertThat(TestGroup.parse(null), equalTo(Collections.emptySet()));
	}

	@Test
	public void parseEmptyString() {
		assertThat(TestGroup.parse(""), equalTo(Collections.emptySet()));
	}

	@Test
	public void parseBlankString() {
		assertThat(TestGroup.parse("     "), equalTo(Collections.emptySet()));
	}

	@Test
	public void parseWithSpaces() {
		assertThat(TestGroup.parse(" PERFORMANCE,  PERFORMANCE "),
				equalTo(EnumSet.of(TestGroup.PERFORMANCE)));
	}

	@Test
	public void parseInMixedCase() {
		assertThat(TestGroup.parse("performance,  PERFormaNCE"),
				equalTo(EnumSet.of(TestGroup.PERFORMANCE)));
	}

	@Test
	public void parseMissing() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Unable to find test group 'missing' when parsing " +
				"testGroups value: 'performance, missing'. Available groups include: " +
				"[LONG_RUNNING,PERFORMANCE,JMXMP,CI]");
		TestGroup.parse("performance, missing");
	}

	@Test
	public void parseAll() {
		assertThat(TestGroup.parse("all"), equalTo(EnumSet.allOf(TestGroup.class)));
	}

	@Test
	public void parseAllExceptPerformance() {
		Set<TestGroup> expected = EnumSet.allOf(TestGroup.class);
		expected.remove(TestGroup.PERFORMANCE);
		assertThat(TestGroup.parse("all-performance"), equalTo(expected));
	}

	@Test
	public void parseAllExceptMissing() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Unable to find test group 'missing' when parsing " +
				"testGroups value: 'all-missing'. Available groups include: " +
				"[LONG_RUNNING,PERFORMANCE,JMXMP,CI]");
		TestGroup.parse("all-missing");
	}

}
