/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.util.comparator;

import java.util.Comparator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link ComparableComparator}.
 *
 * @author Keith Donald
 * @author Chris Beams
 * @author Phillip Webb
 */
@Deprecated
class ComparableComparatorTests {

	@Test
	void comparableComparator() {
		@SuppressWarnings("deprecation")
		Comparator<String> c = new ComparableComparator<>();
		assertThat(c.compare("abc", "cde")).isLessThan(0);
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void shouldNeedComparable() {
		@SuppressWarnings("deprecation")
		Comparator c = new ComparableComparator();
		Object o1 = new Object();
		Object o2 = new Object();
		assertThatExceptionOfType(ClassCastException.class).isThrownBy(() -> c.compare(o1, o2));
	}

}
