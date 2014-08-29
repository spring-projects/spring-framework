/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.test;

import org.springframework.util.Assert;

/**
 * {@code AssertThrows} is a simple method object that encapsulates the
 * <em>'test-for-exception'</em> scenario for unit testing. Intended for
 * use with JUnit or TestNG.
 *
 * <p>Given the following business class...
 *
 * <pre class="code">
 * // the class under test
 * public class Foo {
 *    public void someBusinessLogic(String name) {
 *        if (name == null) {
 *            throw new IllegalArgumentException("The 'name' argument is required");
 *        }
 *        // rest of business logic here...
 *    }
 * }</pre>
 *
 * <p>The test for the above bad argument path can be expressed using
 * {@code AssertThrows} like so:
 *
 * <pre class="code">
 * public class FooTest {
 *    public void testSomeBusinessLogicBadArgumentPath() {
 *        new AssertThrows(IllegalArgumentException.class) {
 *            public void test() {
 *                new Foo().someBusinessLogic(null);
 *            }
 *        }.runTest();
 *    }
 * }</pre>
 *
 * <p>This will result in the test passing if the {@code Foo.someBusinessLogic(..)}
 * method threw an {@code IllegalArgumentException}; if it did not, the
 * test would fail with the following message:
 *
 * <pre class="code">"Must have thrown a [class java.lang.IllegalArgumentException]"</pre>
 *
 * <p>If the <strong>wrong</strong> type of {@code Throwable} was thrown,
 * the test will also fail, this time with a message similar to the following:
 *
 * <pre class="code">"java.lang.AssertionError: Was expecting a [class java.lang.UnsupportedOperationException] to be thrown, but instead a [class java.lang.IllegalArgumentException] was thrown"</pre>
 *
 * <p>The test for the correct {@code Throwable} respects polymorphism,
 * so you can test that any old {@code Exception} is thrown like so:
 *
 * <pre class="code">
 * public class FooTest {
 *    public void testSomeBusinessLogicBadArgumentPath() {
 *        // any Exception will do...
 *        new AssertThrows(Exception.class) {
 *            public void test() {
 *                new Foo().someBusinessLogic(null);
 *            }
 *        }.runTest();
 *    }
 * }</pre>
 *
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 2.0
 * @deprecated Favor use of JUnit's {@code @Test(expected=...)} or
 * {@code @Rule ExpectedException} support or TestNG's
 * {@code @Test(expectedExceptions=...)} support
 */
@Deprecated
public abstract class AssertThrows {

	private final Class<? extends Throwable> expectedException;

	private String failureMessage;

	private Throwable actualException;


	/**
	 * Create a new instance of the {@code AssertThrows} class.
	 * @param expectedException the {@link Throwable} expected to be
	 * thrown during the execution of the surrounding test
	 * @throws IllegalArgumentException if the supplied {@code expectedException} is
	 * {@code null}; or if said argument is not a {@code Throwable}-derived class
	 */
	public AssertThrows(Class<? extends Throwable> expectedException) {
		this(expectedException, null);
	}

	/**
	 * Create a new instance of the {@code AssertThrows} class.
	 * @param expectedException the {@link Throwable} expected to be
	 * thrown during the execution of the surrounding test
	 * @param failureMessage the extra, contextual failure message that will be
	 * included in the failure text if the text fails (can be {@code null})
	 * @throws IllegalArgumentException if the supplied {@code expectedException} is
	 * {@code null}; or if said argument is not a {@code Throwable}-derived class
	 */
	public AssertThrows(Class<? extends Throwable> expectedException, String failureMessage) {
		Assert.notNull(expectedException, "expectedException is required");
		Assert.isAssignable(Throwable.class, expectedException, "expectedException: ");
		this.expectedException = expectedException;
		this.failureMessage = failureMessage;
	}

	/**
	 * Return the {@link java.lang.Throwable} expected to be thrown during
	 * the execution of the surrounding test.
	 */
	protected Class<? extends Throwable> getExpectedException() {
		return this.expectedException;
	}

	/**
	 * Set the extra, contextual failure message that will be included
	 * in the failure text if the text fails.
	 */
	public void setFailureMessage(String failureMessage) {
		this.failureMessage = failureMessage;
	}

	/**
	 * Return the extra, contextual failure message that will be included
	 * in the failure text if the text fails.
	 */
	protected String getFailureMessage() {
		return this.failureMessage;
	}

	/**
	 * Subclass must override this {@code abstract} method and
	 * provide the test logic.
	 * @throws Throwable if an error occurs during the execution of the
	 * aforementioned test logic
	 */
	public abstract void test() throws Throwable;

	/**
	 * The main template method that drives the running of the
	 * {@linkplain #test() test logic} and the
	 * {@linkplain #checkExceptionExpectations(Throwable) checking} of the
	 * resulting (expected) {@link java.lang.Throwable}.
	 * @see #test()
	 * @see #doFail()
	 * @see #checkExceptionExpectations(Throwable)
	 */
	public void runTest() {
		try {
			test();
			doFail();
		}
		catch (Throwable actualException) {
			this.actualException = actualException;
			checkExceptionExpectations(actualException);
		}
	}

	/**
	 * Template method called when the test fails; i.e. the expected
	 * {@link java.lang.Throwable} is <b>not</b> thrown.
	 * <p>The default implementation simply fails the test by throwing an
	 * {@link AssertionError}.
	 * <p>If you want to customize the failure message, consider overriding
	 * {@link #createMessageForNoExceptionThrown()}, and / or supplying an
	 * extra, contextual failure message via the appropriate constructor.
	 * @see #getFailureMessage()
	 * @see #createMessageForNoExceptionThrown()
	 */
	protected void doFail() {
		throw new AssertionError(createMessageForNoExceptionThrown());
	}

	/**
	 * Creates the failure message used if the test fails
	 * (i.e. the expected exception is not thrown in the body of the test).
	 * @return the failure message used if the test fails
	 * @see #getFailureMessage()
	 */
	protected String createMessageForNoExceptionThrown() {
		StringBuilder sb = new StringBuilder();
		sb.append("Should have thrown a [").append(this.getExpectedException()).append("]");
		if (getFailureMessage() != null) {
			sb.append(": ").append(getFailureMessage());
		}
		return sb.toString();
	}

	/**
	 * Does the donkey work of checking (verifying) that the
	 * {@link Throwable} that was thrown in the body of the test is
	 * an instance of the {@link #getExpectedException()} class (or an
	 * instance of a subclass).
	 * <p>If you want to customize the failure message, consider overriding
	 * {@link #createMessageForWrongThrownExceptionType(Throwable)}.
	 * @param actualException the {@link Throwable} that has been thrown
	 * in the body of a test method (will never be {@code null})
	 */
	protected void checkExceptionExpectations(Throwable actualException) {
		if (!getExpectedException().isAssignableFrom(actualException.getClass())) {
			AssertionError error = new AssertionError(createMessageForWrongThrownExceptionType(actualException));
			error.initCause(actualException);
			throw error;
		}
	}

	/**
	 * Creates the failure message used if the wrong type
	 * of {@link java.lang.Throwable} is thrown in the body of the test.
	 * @param actualException the actual exception thrown
	 * @return the message for the given exception
	 */
	protected String createMessageForWrongThrownExceptionType(Throwable actualException) {
		StringBuilder sb = new StringBuilder();
		sb.append("Was expecting a [").append(getExpectedException().getName());
		sb.append("] to be thrown, but instead a [").append(actualException.getClass().getName());
		sb.append("] was thrown.");
		return sb.toString();
	}

	/**
	 * Expose the actual exception thrown from {@link #test}, if any.
	 * @return the actual exception, or {@code null} if none
	 */
	public final Throwable getActualException() {
		return this.actualException;
	}

}
