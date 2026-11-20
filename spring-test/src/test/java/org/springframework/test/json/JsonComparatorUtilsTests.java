/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.json;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link JsonComparatorUtils}.
 *
 * @author Brian Clozel
 */
class JsonComparatorUtilsTests {

	@Test
	void ofUsesJsonUnitWhenPresent() {
		assertThat(JsonComparator.of(JsonCompareMode.STRICT)).isInstanceOf(JsonUnitJsonComparator.class);
	}

	@Test
	void prefersJsonUnitWhenBothArePresent() {
		assertThat(JsonComparatorUtils.forCompareMode(JsonCompareMode.LENIENT, true, true))
				.isInstanceOf(JsonUnitJsonComparator.class);
	}

	@Test
	void usesJsonAssertWhenJsonUnitIsNotPresent() {
		assertThat(JsonComparatorUtils.forCompareMode(JsonCompareMode.LENIENT, false, true))
				.isInstanceOf(JsonAssertJsonComparator.class);
	}

	@Test
	void failsWhenNoLibraryIsPresent() {
		assertThatIllegalStateException()
				.isThrownBy(() -> JsonComparatorUtils.forCompareMode(JsonCompareMode.LENIENT, false, false))
				.withMessageContaining("json-unit-core")
				.withMessageContaining("jsonassert");
	}

}
