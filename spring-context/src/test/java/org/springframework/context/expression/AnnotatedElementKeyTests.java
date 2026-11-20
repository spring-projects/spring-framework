/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.expression;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stephane Nicoll
 * @author Sam Brannen
 */
class AnnotatedElementKeyTests {

	private Method method;

	@BeforeEach
	void setUpMethod(TestInfo testInfo) {
		this.method = ReflectionUtils.findMethod(getClass(), testInfo.getTestMethod().get().getName());
	}

	@Test
	void sameInstanceEquals() {
		AnnotatedElementKey instance = new AnnotatedElementKey(this.method, getClass());

		assertKeyEquals(instance, instance);
	}

	@Test
	void equals() {
		AnnotatedElementKey first = new AnnotatedElementKey(this.method, getClass());
		AnnotatedElementKey second = new AnnotatedElementKey(this.method, getClass());

		assertKeyEquals(first, second);
	}

	@Test
	void equalsNoTarget() {
		AnnotatedElementKey first = new AnnotatedElementKey(this.method, null);
		AnnotatedElementKey second = new AnnotatedElementKey(this.method, null);

		assertKeyEquals(first, second);
	}

	@Test
	void noTargetClassNotEquals() {
		AnnotatedElementKey first = new AnnotatedElementKey(this.method, getClass());
		AnnotatedElementKey second = new AnnotatedElementKey(this.method, null);

		assertThat(first).isNotEqualTo(second);
	}

	private void assertKeyEquals(AnnotatedElementKey first, AnnotatedElementKey second) {
		assertThat(second).isEqualTo(first);
		assertThat(second.hashCode()).isEqualTo(first.hashCode());
	}

}
