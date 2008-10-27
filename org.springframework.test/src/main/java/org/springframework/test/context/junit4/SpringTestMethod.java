/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.test.context.junit4;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.Test.None;
import org.junit.internal.runners.TestClass;

import org.springframework.test.annotation.ExpectedException;
import org.springframework.test.annotation.ProfileValueSource;
import org.springframework.test.annotation.ProfileValueUtils;

/**
 * SpringTestMethod is a custom implementation of JUnit 4.4's
 * {@link org.junit.internal.runners.TestMethod TestMethod}. Due to method and
 * field visibility constraints, the code of TestMethod has been duplicated here
 * instead of subclassing TestMethod directly.
 *
 * <p>SpringTestMethod also provides support for
 * {@link org.springframework.test.annotation.IfProfileValue @IfProfileValue}
 * and {@link ExpectedException @ExpectedException}. See {@link #isIgnored()}
 * and {@link #getExpectedException()} for further details.
 *
 * @author Sam Brannen
 * @since 2.5
 */
class SpringTestMethod {

	private static final Log logger = LogFactory.getLog(SpringTestMethod.class);

	private final Method method;

	private final TestClass testClass;


	/**
	 * Constructs a test method for the supplied {@link Method method} and
	 * {@link TestClass test class}; and retrieves the configured (or default)
	 * {@link ProfileValueSource}.
	 * @param method The test method
	 * @param testClass the test class
	 */
	public SpringTestMethod(Method method, TestClass testClass) {
		this.method = method;
		this.testClass = testClass;
	}


	/**
	 * Determine if this test method is {@link Test#expected() expected} to
	 * throw an exception.
	 */
	public boolean expectsException() {
		return (getExpectedException() != null);
	}

	/**
	 * Get the {@link After @After} methods for this test method.
	 */
	public List<Method> getAfters() {
		return getTestClass().getAnnotatedMethods(After.class);
	}

	/**
	 * Get the {@link Before @Before} methods for this test method.
	 */
	public List<Method> getBefores() {
		return getTestClass().getAnnotatedMethods(Before.class);
	}

	/**
	 * Get the <code>exception</code> that this test method is expected to throw.
	 * <p>Supports both Spring's {@link ExpectedException @ExpectedException(...)}
	 * and JUnit's {@link Test#expected() @Test(expected=...)} annotations, but
	 * not both simultaneously.
	 * @return the expected exception, or <code>null</code> if none was specified
	 */
	public Class<? extends Throwable> getExpectedException() throws IllegalStateException {
		ExpectedException expectedExAnn = getMethod().getAnnotation(ExpectedException.class);
		Test testAnnotation = getMethod().getAnnotation(Test.class);

		Class<? extends Throwable> expectedException = null;
		Class<? extends Throwable> springExpectedException =
				(expectedExAnn != null && expectedExAnn.value() != null ? expectedExAnn.value() : null);
		Class<? extends Throwable> junitExpectedException =
				(testAnnotation != null && testAnnotation.expected() != None.class ? testAnnotation.expected() : null);

		if (springExpectedException != null && junitExpectedException != null) {
			String msg = "Test method [" + getMethod() + "] has been configured with Spring's @ExpectedException(" +
					springExpectedException.getName() + ".class) and JUnit's @Test(expected=" +
					junitExpectedException.getName() + ".class) annotations. " +
					"Only one declaration of an 'expected exception' is permitted per test method.";
			logger.error(msg);
			throw new IllegalStateException(msg);
		}
		else if (springExpectedException != null) {
			expectedException = springExpectedException;
		}
		else if (junitExpectedException != null) {
			expectedException = junitExpectedException;
		}

		return expectedException;
	}

	/**
	 * Get the actual {@link Method method} referenced by this test method.
	 */
	public final Method getMethod() {
		return this.method;
	}

	/**
	 * Get the {@link TestClass test class} for this test method.
	 */
	public final TestClass getTestClass() {
		return this.testClass;
	}

	/**
	 * Get the configured <code>timeout</code> for this test method.
	 * <p>Supports JUnit's {@link Test#timeout() @Test(timeout=...)} annotation.
	 * @return the timeout, or <code>0</code> if none was specified
	 */
	public long getTimeout() {
		Test testAnnotation = getMethod().getAnnotation(Test.class);
		return (testAnnotation != null && testAnnotation.timeout() > 0 ? testAnnotation.timeout() : 0);
	}

	/**
	 * Convenience method for {@link Method#invoke(Object,Object...) invoking}
	 * the method associated with this test method. Throws exceptions consistent
	 * with {@link Method#invoke(Object,Object...) Method.invoke()}.
	 * @param testInstance the test instance upon which to invoke the method
	 */
	public void invoke(Object testInstance) throws IllegalAccessException, InvocationTargetException {
		getMethod().invoke(testInstance);
	}

	/**
	 * Determine if this test method should be ignored.
	 * @return <code>true</code> if this test method should be ignored
	 * @see ProfileValueUtils#isTestEnabledInThisEnvironment
	 */
	public boolean isIgnored() {
		return (getMethod().isAnnotationPresent(Ignore.class) ||
				!ProfileValueUtils.isTestEnabledInThisEnvironment(this.method, this.testClass.getJavaClass()));
	}

	/**
	 * Determine if this test method {@link Test#expected() expects} exceptions
	 * of the type of the supplied <code>exception</code> to be thrown.
	 * @param exception the thrown exception
	 * @return <code>true</code> if the supplied exception was of an expected type
	 */
	public boolean isUnexpected(Throwable exception) {
		return !getExpectedException().isAssignableFrom(exception.getClass());
	}

}
