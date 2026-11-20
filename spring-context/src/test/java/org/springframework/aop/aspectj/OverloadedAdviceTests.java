/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.aspectj;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integration tests for overloaded advice.
 *
 * @author Adrian Colyer
 * @author Chris Beams
 * @author Juergen Hoeller
 */
class OverloadedAdviceTests {

	@Test
	@SuppressWarnings("resource")
	void configParsingWithMismatchedAdviceMethod() {
		new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());
	}

	@Test
	@SuppressWarnings("resource")
	void exceptionOnConfigParsingWithAmbiguousAdviceMethod() {
		assertThatExceptionOfType(BeanCreationException.class)
			.isThrownBy(() -> new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-ambiguous.xml", getClass()))
			.havingRootCause()
				.isInstanceOf(IllegalArgumentException.class)
				.withMessageContaining("Cannot resolve method 'myBeforeAdvice' to a unique method");
	}

}


class OverloadedAdviceTestAspect {

	public void myBeforeAdvice(String name) {
		// no-op
	}

	public void myBeforeAdvice(int age) {
		// no-op
	}
}

