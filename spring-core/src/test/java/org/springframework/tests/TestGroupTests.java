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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests for {@link TestGroup}.
 *
 * @author Phillip Webb
 */
public class TestGroupTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void parseNull() throws Exception {
		assertThat(TestGroup.parse(null), is(Collections.<TestGroup> emptySet()));
	}

	@Test
	public void parseEmptyString() throws Exception {
		assertThat(TestGroup.parse(""), is(Collections.<TestGroup> emptySet()));
	}

	@Test
	public void parseWithSpaces() throws Exception {
		assertThat(TestGroup.parse("PERFORMANCE,  PERFORMANCE"),
				is((Set<TestGroup>) EnumSet.of(TestGroup.PERFORMANCE)));
	}

	@Test
	public void parseInMixedCase() throws Exception {
		assertThat(TestGroup.parse("performance,  PERFormaNCE"),
				is((Set<TestGroup>) EnumSet.of(TestGroup.PERFORMANCE)));
	}

	@Test
	public void parseMissing() throws Exception {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Unable to find test group 'missing' when parsing " +
				"testGroups value: 'performance, missing'. Available groups include: " +
				"[LONG_RUNNING,PERFORMANCE,JMXMP]");
		TestGroup.parse("performance, missing");
	}

	@Test
	public void parseAll() throws Exception {
		assertThat(TestGroup.parse("all"), is((Set<TestGroup>)EnumSet.allOf(TestGroup.class)));
	}
}
