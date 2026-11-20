/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.util.comparator;

import java.util.Comparator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Tests for {@link InstanceComparator}.
 *
 * @author Phillip Webb
 */
class InstanceComparatorTests {

	private C1 c1 = new C1();

	private C2 c2 = new C2();

	private C3 c3 = new C3();

	private C4 c4 = new C4();

	@Test
	void shouldCompareClasses() {
		Comparator<Object> comparator = new InstanceComparator<>(C1.class, C2.class);
		assertThat(comparator.compare(c1, c1)).isEqualTo(0);
		assertThat(comparator.compare(c1, c2)).isEqualTo(-1);
		assertThat(comparator.compare(c2, c1)).isEqualTo(1);
		assertThat(comparator.compare(c2, c3)).isEqualTo(-1);
		assertThat(comparator.compare(c2, c4)).isEqualTo(-1);
		assertThat(comparator.compare(c3, c4)).isEqualTo(0);
	}

	@Test
	void shouldCompareInterfaces() {
		Comparator<Object> comparator = new InstanceComparator<>(I1.class, I2.class);
		assertThat(comparator.compare(c1, c1)).isEqualTo(0);
		assertThat(comparator.compare(c1, c2)).isEqualTo(0);
		assertThat(comparator.compare(c2, c1)).isEqualTo(0);
		assertThat(comparator.compare(c1, c3)).isEqualTo(-1);
		assertThat(comparator.compare(c3, c1)).isEqualTo(1);
		assertThat(comparator.compare(c3, c4)).isEqualTo(0);
	}

	@Test
	void shouldCompareMix() {
		Comparator<Object> comparator = new InstanceComparator<>(I1.class, C3.class);
		assertThat(comparator.compare(c1, c1)).isEqualTo(0);
		assertThat(comparator.compare(c3, c4)).isEqualTo(-1);
		assertThat(comparator.compare(c3, null)).isEqualTo(-1);
		assertThat(comparator.compare(c4, null)).isEqualTo(0);
	}

	private interface I1 {

	}

	private interface I2 {

	}

	private static class C1 implements I1 {
	}

	private static class C2 implements I1 {
	}

	private static class C3 implements I2 {
	}

	private static class C4 implements I2 {
	}

}
