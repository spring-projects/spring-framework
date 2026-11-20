/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
 * @deprecated since Spring Framework 7.0 in favor of the
 * {@link org.springframework.test.context.junit.jupiter.SpringExtension SpringExtension}
 * and JUnit Jupiter
 */
@Deprecated(since = "7.0")
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
			if (this.repeat > 1 && logger.isTraceEnabled()) {
				logger.trace(String.format("Repetition %d of test %s#%s()", (i + 1),
						this.testMethod.getDeclaringClass().getSimpleName(), this.testMethod.getName()));
			}
			this.next.evaluate();
		}
	}

}
