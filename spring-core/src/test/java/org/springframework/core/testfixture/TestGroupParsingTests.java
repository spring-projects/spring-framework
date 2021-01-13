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
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link TestGroup} parsing.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 */
class TestGroupParsingTests {

	@Test
	void parseNull() {
		assertThat(TestGroup.parse(null)).isEqualTo(Collections.emptySet());
	}

	@Test
	void parseEmptyString() {
		assertThat(TestGroup.parse("")).isEqualTo(Collections.emptySet());
	}

	@Test
	void parseBlankString() {
		assertThat(TestGroup.parse("     ")).isEqualTo(Collections.emptySet());
	}

	@Test
	void parseWithSpaces() {
		assertThat(TestGroup.parse(" LONG_RUNNING,  LONG_RUNNING ")).containsOnly(TestGroup.LONG_RUNNING);
	}

	@Test
	void parseInMixedCase() {
		assertThat(TestGroup.parse("long_running,  LonG_RunnING")).containsOnly(TestGroup.LONG_RUNNING);
	}

	@Test
	void parseMissing() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> TestGroup.parse("long_running, missing"))
			.withMessageContaining("Unable to find test group 'missing' when parsing " +
					"testGroups value: 'long_running, missing'. Available groups include: " +
					"[LONG_RUNNING]");
	}

	@Test
	void parseAll() {
		assertThat(TestGroup.parse("all")).isEqualTo(EnumSet.allOf(TestGroup.class));
	}

	@Test
	void parseAllExceptLongRunning() {
		Set<TestGroup> expected = EnumSet.allOf(TestGroup.class);
		expected.remove(TestGroup.LONG_RUNNING);
		assertThat(TestGroup.parse("all-long_running")).isEqualTo(expected);
	}

	@Test
	void parseAllExceptMissing() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> TestGroup.parse("all-missing"))
			.withMessageContaining("Unable to find test group 'missing' when parsing " +
					"testGroups value: 'all-missing'. Available groups include: " +
					"[LONG_RUNNING]");
	}

}
