/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.aspectj;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.testfixture.beans.CountingTestBean;
import org.springframework.beans.testfixture.beans.IOther;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.beans.testfixture.beans.subpkg.DeepBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link TypePatternClassFilter}.
 *
 * @author Rod Johnson
 * @author Rick Evans
 * @author Chris Beams
 * @author Sam Brannen
 */
class TypePatternClassFilterTests {

	@Test
	void nullPattern() {
		assertThatIllegalArgumentException().isThrownBy(() -> new TypePatternClassFilter(null));
	}

	@Test
	void invalidPattern() {
		assertThatIllegalArgumentException().isThrownBy(() -> new TypePatternClassFilter("-"));
	}

	@Test
	void invocationOfMatchesMethodBlowsUpWhenNoTypePatternHasBeenSet() {
		assertThatIllegalStateException().isThrownBy(() -> new TypePatternClassFilter().matches(String.class));
	}

	@Test
	void validPatternMatching() {
		TypePatternClassFilter tpcf = new TypePatternClassFilter("org.springframework.beans.testfixture.beans.*");

		assertThat(tpcf.matches(TestBean.class)).as("Must match: in package").isTrue();
		assertThat(tpcf.matches(ITestBean.class)).as("Must match: in package").isTrue();
		assertThat(tpcf.matches(IOther.class)).as("Must match: in package").isTrue();

		assertThat(tpcf.matches(DeepBean.class)).as("Must be excluded: in wrong package").isFalse();
		assertThat(tpcf.matches(BeanFactory.class)).as("Must be excluded: in wrong package").isFalse();
		assertThat(tpcf.matches(DefaultListableBeanFactory.class)).as("Must be excluded: in wrong package").isFalse();
	}

	@Test
	void subclassMatching() {
		TypePatternClassFilter tpcf = new TypePatternClassFilter("org.springframework.beans.testfixture.beans.ITestBean+");

		assertThat(tpcf.matches(TestBean.class)).as("Must match: in package").isTrue();
		assertThat(tpcf.matches(ITestBean.class)).as("Must match: in package").isTrue();
		assertThat(tpcf.matches(CountingTestBean.class)).as("Must match: in package").isTrue();

		assertThat(tpcf.matches(IOther.class)).as("Must be excluded: not subclass").isFalse();
		assertThat(tpcf.matches(DefaultListableBeanFactory.class)).as("Must be excluded: not subclass").isFalse();
	}

	@Test
	void andOrNotReplacement() {
		TypePatternClassFilter tpcf = new TypePatternClassFilter("java.lang.Object or java.lang.String");
		assertThat(tpcf.matches(Number.class)).as("matches Number").isFalse();
		assertThat(tpcf.matches(Object.class)).as("matches Object").isTrue();
		assertThat(tpcf.matches(String.class)).as("matchesString").isTrue();

		tpcf = new TypePatternClassFilter("java.lang.Number+ and java.lang.Float");
		assertThat(tpcf.matches(Float.class)).as("matches Float").isTrue();
		assertThat(tpcf.matches(Double.class)).as("matches Double").isFalse();

		tpcf = new TypePatternClassFilter("java.lang.Number+ and not java.lang.Float");
		assertThat(tpcf.matches(Float.class)).as("matches Float").isFalse();
		assertThat(tpcf.matches(Double.class)).as("matches Double").isTrue();
	}

	@Test
	void equals() {
		TypePatternClassFilter filter1 = new TypePatternClassFilter("org.springframework.beans.testfixture.beans.*");
		TypePatternClassFilter filter2 = new TypePatternClassFilter("org.springframework.beans.testfixture.beans.*");
		TypePatternClassFilter filter3 = new TypePatternClassFilter("org.springframework.tests.*");

		assertThat(filter1).isEqualTo(filter2);
		assertThat(filter1).isNotEqualTo(filter3);
	}

	@Test
	void hashCodeBehavior() {
		TypePatternClassFilter filter1 = new TypePatternClassFilter("org.springframework.beans.testfixture.beans.*");
		TypePatternClassFilter filter2 = new TypePatternClassFilter("org.springframework.beans.testfixture.beans.*");
		TypePatternClassFilter filter3 = new TypePatternClassFilter("org.springframework.tests.*");

		assertThat(filter1.hashCode()).isEqualTo(filter2.hashCode());
		assertThat(filter1.hashCode()).isNotEqualTo(filter3.hashCode());
	}

	@Test
	void toStringOutput() {
		TypePatternClassFilter filter1 = new TypePatternClassFilter("org.springframework.beans.testfixture.beans.*");
		TypePatternClassFilter filter2 = new TypePatternClassFilter("org.springframework.beans.testfixture.beans.*");

		assertThat(filter1.toString())
			.isEqualTo("org.springframework.aop.aspectj.TypePatternClassFilter: org.springframework.beans.testfixture.beans.*");
		assertThat(filter1.toString()).isEqualTo(filter2.toString());
	}

}
