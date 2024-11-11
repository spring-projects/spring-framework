/*
 * Copyright 2002-2022 the original author or authors.
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
	void testConfigParsingWithMismatchedAdviceMethod() {
		new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());
	}

	@Test
	@SuppressWarnings("resource")
	void testExceptionOnConfigParsingWithAmbiguousAdviceMethod() {
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

