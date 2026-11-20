/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.support;

import org.junit.jupiter.api.Test;

import org.springframework.aop.ClassFilter;
import org.springframework.beans.testfixture.beans.ITestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RootClassFilter}.
 *
 * @author Sam Brannen
 * @since 5.1.10
 */
class RootClassFilterTests {

	private final ClassFilter filter1 = new RootClassFilter(Exception.class);
	private final ClassFilter filter2 = new RootClassFilter(Exception.class);
	private final ClassFilter filter3 = new RootClassFilter(ITestBean.class);


	@Test
	void matches() {
		assertThat(filter1.matches(Exception.class)).isTrue();
		assertThat(filter1.matches(RuntimeException.class)).isTrue();
		assertThat(filter1.matches(Error.class)).isFalse();
	}

	@Test
	void equals() {
		assertThat(filter1).isEqualTo(filter2);
		assertThat(filter1).isNotEqualTo(filter3);
	}

	@Test
	void hashCodeBehavior() {
		assertThat(filter1.hashCode()).isEqualTo(filter2.hashCode());
		assertThat(filter1.hashCode()).isNotEqualTo(filter3.hashCode());
	}

	@Test
	void toStringOutput() {
		assertThat(filter1.toString()).isEqualTo("org.springframework.aop.support.RootClassFilter: java.lang.Exception");
		assertThat(filter1.toString()).isEqualTo(filter2.toString());
	}

}
