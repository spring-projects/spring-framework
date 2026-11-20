/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
