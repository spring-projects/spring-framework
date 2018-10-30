/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context.junit4.statements;

import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.runners.model.Statement;

import org.springframework.test.annotation.TestAnnotationUtils;

/**
 * {@code SpringRepeat} is a custom JUnit {@link Statement} which adds support
 * for Spring's {@link org.springframework.test.annotation.Repeat @Repeat}
 * annotation by repeating the test the specified number of times.
 *
 * @author Sam Brannen
 * @since 3.0
 * @see #evaluate()
 */
public class SpringRepeat extends Statement {

	protected static final Log logger = LogFactory.getLog(SpringRepeat.class);

	private final Statement next;

	private final Method testMethod;

	private final int repeat;


	/**
	 * Construct a new {@code SpringRepeat} statement for the supplied
	 * {@code testMethod}, retrieving the configured repeat count from the
	 * {@code @Repeat} annotation on the supplied method.
	 * @param next the next {@code Statement} in the execution chain
	 * @param testMethod the current test method
	 * @see TestAnnotationUtils#getRepeatCount(Method)
	 */
	public SpringRepeat(Statement next, Method testMethod) {
		this(next, testMethod, TestAnnotationUtils.getRepeatCount(testMethod));
	}

	/**
	 * Construct a new {@code SpringRepeat} statement for the supplied
	 * {@code testMethod} and {@code repeat} count.
	 * @param next the next {@code Statement} in the execution chain
	 * @param testMethod the current test method
	 * @param repeat the configured repeat count for the current test method
	 */
	public SpringRepeat(Statement next, Method testMethod, int repeat) {
		this.next = next;
		this.testMethod = testMethod;
		this.repeat = Math.max(1, repeat);
	}


	/**
	 * Evaluate the next {@link Statement statement} in the execution chain
	 * repeatedly, using the specified repeat count.
	 */
	@Override
	public void evaluate() throws Throwable {
		for (int i = 0; i < this.repeat; i++) {
			if (this.repeat > 1 && logger.isInfoEnabled()) {
				logger.info(String.format("Repetition %d of test %s#%s()", (i + 1),
						this.testMethod.getDeclaringClass().getSimpleName(), this.testMethod.getName()));
			}
			this.next.evaluate();
		}
	}

}
