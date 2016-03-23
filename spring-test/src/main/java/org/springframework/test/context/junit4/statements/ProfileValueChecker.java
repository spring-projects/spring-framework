/*
 * Copyright 2002-2016 the original author or authors.
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.junit.AssumptionViolatedException;
import org.junit.runners.model.Statement;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.annotation.ProfileValueUtils;
import org.springframework.util.Assert;

/**
 * {@code ProfileValueChecker} is a custom JUnit {@link Statement} that checks
 * whether a test class or test method is enabled in the current environment
 * via Spring's {@link IfProfileValue @IfProfileValue} annotation.
 *
 * @author Sam Brannen
 * @author Philippe Marschall
 * @since 4.2
 * @see #evaluate()
 * @see IfProfileValue
 * @see ProfileValueUtils
 */
public class ProfileValueChecker extends Statement {

	private final Statement next;

	private final Class<?> testClass;

	private final Method testMethod;


	/**
	 * Construct a new {@code ProfileValueChecker} statement.
	 * @param next the next {@code Statement} in the execution chain;
	 * never {@code null}
	 * @param testClass the test class to check; never {@code null}
	 * @param testMethod the test method to check; may be {@code null} if
	 * this {@code ProfileValueChecker} is being applied at the class level
	 */
	public ProfileValueChecker(Statement next, Class<?> testClass, Method testMethod) {
		Assert.notNull(next, "The next statement must not be null");
		Assert.notNull(testClass, "The test class must not be null");
		this.next = next;
		this.testClass = testClass;
		this.testMethod = testMethod;
	}


	/**
	 * Determine if the test specified by arguments to the
	 * {@linkplain #ProfileValueChecker constructor} is <em>enabled</em> in
	 * the current environment, as configured via the {@link IfProfileValue
	 * &#064;IfProfileValue} annotation.
	 * <p>If the test is not annotated with {@code @IfProfileValue} it is
	 * considered enabled.
	 * <p>If a test is not enabled, this method will abort further evaluation
	 * of the execution chain with a failed assumption; otherwise, this method
	 * will simply evaluate the next {@link Statement} in the execution chain.
	 * @see ProfileValueUtils#isTestEnabledInThisEnvironment(Class)
	 * @see ProfileValueUtils#isTestEnabledInThisEnvironment(Method, Class)
	 * @throws AssumptionViolatedException if the test is disabled
	 * @throws Throwable if evaluation of the next statement fails
	 */
	@Override
	public void evaluate() throws Throwable {
		if (this.testMethod == null) {
			if (!ProfileValueUtils.isTestEnabledInThisEnvironment(this.testClass)) {
				Annotation ann = AnnotatedElementUtils.findMergedAnnotation(this.testClass, IfProfileValue.class);
				throw new AssumptionViolatedException(String.format(
						"Profile configured via [%s] is not enabled in this environment for test class [%s].",
						ann, this.testClass.getName()));
			}
		}
		else {
			if (!ProfileValueUtils.isTestEnabledInThisEnvironment(this.testMethod, this.testClass)) {
				throw new AssumptionViolatedException(String.format(
						"Profile configured via @IfProfileValue is not enabled in this environment for test method [%s].",
						this.testMethod));
			}
		}

		this.next.evaluate();
	}

}
