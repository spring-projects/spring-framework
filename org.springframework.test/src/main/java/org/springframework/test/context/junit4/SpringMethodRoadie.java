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
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assume.AssumptionViolatedException;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;

import org.springframework.test.annotation.Repeat;
import org.springframework.test.annotation.Timed;
import org.springframework.test.context.TestContextManager;

/**
 * <p>
 * <code>SpringMethodRoadie</code> is a custom implementation of JUnit 4.4's
 * {@link org.junit.internal.runners.MethodRoadie MethodRoadie}, which provides
 * the following enhancements:
 * </p>
 * <ul>
 * <li>Notifies a {@link TestContextManager} of
 * {@link TestContextManager#beforeTestMethod(Object,Method) before} and
 * {@link TestContextManager#afterTestMethod(Object,Method,Throwable) after}
 * events.</li>
 * <li>Uses a {@link SpringTestMethod} instead of JUnit 4.4's
 * {@link org.junit.internal.runners.TestMethod TestMethod}.</li>
 * <li>Tracks the exception thrown during execution of the test method.</li>
 * </ul>
 * <p>
 * Due to method and field visibility constraints, the code of
 * <code>MethodRoadie</code> has been duplicated here instead of subclassing
 * <code>MethodRoadie</code> directly.
 * </p>
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 2.5
 */
class SpringMethodRoadie {

	protected static final Log logger = LogFactory.getLog(SpringMethodRoadie.class);

	private final TestContextManager testContextManager;

	private final Object testInstance;

	private final SpringTestMethod testMethod;

	private final RunNotifier notifier;

	private final Description description;

	private Throwable testException;


	/**
	 * Constructs a new <code>SpringMethodRoadie</code>.
	 * @param testContextManager the TestContextManager to notify
	 * @param testInstance the test instance upon which to invoke the test method
	 * @param testMethod the test method to invoke
	 * @param notifier the RunNotifier to notify
	 * @param description the test description
	 */
	public SpringMethodRoadie(TestContextManager testContextManager, Object testInstance,
			SpringTestMethod testMethod, RunNotifier notifier, Description description) {

		this.testContextManager = testContextManager;
		this.testInstance = testInstance;
		this.testMethod = testMethod;
		this.notifier = notifier;
		this.description = description;
	}

	/**
	 * Runs the <em>test</em>, including notification of events to the
	 * {@link RunNotifier} and {@link TestContextManager} as well as proper
	 * handling of {@link org.junit.Ignore @Ignore},
	 * {@link org.junit.Test#expected() expected exceptions},
	 * {@link org.junit.Test#timeout() test timeouts}, and
	 * {@link org.junit.Assume.AssumptionViolatedException assumptions}.
	 */
	public void run() {
		if (this.testMethod.isIgnored()) {
			this.notifier.fireTestIgnored(this.description);
			return;
		}

		this.notifier.fireTestStarted(this.description);
		try {
			Timed timedAnnotation = this.testMethod.getMethod().getAnnotation(Timed.class);
			long springTimeout = (timedAnnotation != null && timedAnnotation.millis() > 0 ?
					timedAnnotation.millis() : 0);
			long junitTimeout = this.testMethod.getTimeout();
			if (springTimeout > 0 && junitTimeout > 0) {
				throw new IllegalStateException("Test method [" + this.testMethod.getMethod() +
						"] has been configured with Spring's @Timed(millis=" + springTimeout +
						") and JUnit's @Test(timeout=" + junitTimeout +
						") annotations. Only one declaration of a 'timeout' is permitted per test method.");
			}
			else if (springTimeout > 0) {
				long startTime = System.currentTimeMillis();
				try {
					runTest();
				}
				finally {
					long elapsed = System.currentTimeMillis() - startTime;
					if (elapsed > springTimeout) {
						addFailure(new TimeoutException("Took " + elapsed + " ms; limit was " + springTimeout));
					}
				}
			}
			else if (junitTimeout > 0) {
				runWithTimeout(junitTimeout);
			}
			else {
				runTest();
			}
		}
		finally {
			this.notifier.fireTestFinished(this.description);
		}
	}

	/**
	 * Runs the test method on the test instance with the specified
	 * <code>timeout</code>.
	 * @param timeout the timeout in milliseconds
	 * @see #runWithRepetitions(Runnable)
	 * @see #runTestMethod()
	 */
	protected void runWithTimeout(final long timeout) throws CancellationException {
		runWithRepetitions(new Runnable() {
			public void run() {
				ExecutorService service = Executors.newSingleThreadExecutor();
				Future result = service.submit(new RunBeforesThenTestThenAfters());
				service.shutdown();
				try {
					boolean terminated = service.awaitTermination(timeout, TimeUnit.MILLISECONDS);
					if (!terminated) {
						service.shutdownNow();
					}
					// Throws the exception if one occurred during the invocation.
					result.get(0, TimeUnit.MILLISECONDS);
				}
				catch (TimeoutException ex) {
					String message = "Test timed out after " + timeout + " milliseconds";
					addFailure(new TimeoutException(message));
					// We're cancelling repetitions here since we don't want
					// the abandoned test method execution to conflict with
					// further execution attempts of the same test method.
					throw new CancellationException(message);
				}
				catch (ExecutionException ex) {
					addFailure(ex.getCause());
				}
				catch (Exception ex) {
					addFailure(ex);
				}
			}
		});
	}

	/**
	 * Runs the test, including {@link #runBefores() @Before} and
	 * {@link #runAfters() @After} methods.
	 * @see #runWithRepetitions(Runnable)
	 * @see #runTestMethod()
	 */
	protected void runTest() {
		runWithRepetitions(new RunBeforesThenTestThenAfters());
	}

	/**
	 * Runs the supplied <code>test</code> with repetitions. Checks for the
	 * presence of {@link Repeat @Repeat} to determine if the test should be run
	 * more than once. The test will be run at least once.
	 * @param test the runnable test
	 * @see Repeat
	 */
	protected void runWithRepetitions(Runnable test) {
		Method method = this.testMethod.getMethod();
		Repeat repeat = method.getAnnotation(Repeat.class);
		int runs = (repeat != null && repeat.value() > 1 ? repeat.value() : 1);

		for (int i = 0; i < runs; i++) {
			if (runs > 1 && logger.isInfoEnabled()) {
				logger.info("Repetition " + (i + 1) + " of test " + method.getName());
			}
			try {
				test.run();
			}
			catch (CancellationException ex) {
				break;
			}
		}
	}

	/**
	 * Runs the test method on the test instance, processing exceptions
	 * (both expected and unexpected), assumptions, and registering
	 * failures as necessary.
	 */
	protected void runTestMethod() {
		this.testException = null;
		try {
			this.testMethod.invoke(this.testInstance);
			if (this.testMethod.expectsException()) {
				addFailure(new AssertionError("Expected exception: " + this.testMethod.getExpectedException().getName()));
			}
		}
		catch (InvocationTargetException ex) {
			this.testException = ex.getTargetException();
			if (!(this.testException instanceof AssumptionViolatedException)) {
				if (!this.testMethod.expectsException()) {
					addFailure(this.testException);
				}
				else if (this.testMethod.isUnexpected(this.testException)) {
					addFailure(new Exception("Unexpected exception, expected <" +
							this.testMethod.getExpectedException().getName() + "> but was <" +
							this.testException.getClass().getName() + ">", this.testException));
				}
			}
		}
		catch (Throwable ex) {
			addFailure(ex);
		}
		finally {
			if (logger.isDebugEnabled()) {
				logger.debug("Test method [" + this.testMethod.getMethod() + "] threw exception: " +
						this.testException);
			}
		}
	}

	/**
	 * Calls {@link TestContextManager#beforeTestMethod} and then runs
	 * {@link org.junit.Before @Before methods}, registering failures
	 * and throwing {@link FailedBefore} exceptions as necessary.
	 * @throws FailedBefore if an error occurs while executing a <em>before</em> method
	 */
	protected void runBefores() throws FailedBefore {
		try {
			this.testContextManager.beforeTestMethod(this.testInstance, this.testMethod.getMethod());
			List<Method> befores = this.testMethod.getBefores();
			for (Method before : befores) {
				before.invoke(this.testInstance);
			}
		}
		catch (InvocationTargetException ex) {
			Throwable targetEx = ex.getTargetException();
			if (!(targetEx instanceof AssumptionViolatedException)) {
				addFailure(targetEx);
			}
			throw new FailedBefore();
		}
		catch (Throwable ex) {
			addFailure(ex);
			throw new FailedBefore();
		}
	}

	/**
	 * Runs {@link org.junit.After @After methods}, registering failures as
	 * necessary, and then calls {@link TestContextManager#afterTestMethod}.
	 */
	protected void runAfters() {
		List<Method> afters = this.testMethod.getAfters();
		for (Method after : afters) {
			try {
				after.invoke(this.testInstance);
			}
			catch (InvocationTargetException ex) {
				addFailure(ex.getTargetException());
			}
			catch (Throwable ex) {
				addFailure(ex);
			}
		}
		try {
			this.testContextManager.afterTestMethod(this.testInstance, this.testMethod.getMethod(), this.testException);
		}
		catch (Throwable ex) {
			addFailure(ex);
		}
	}

	/**
	 * Fire a failure for the supplied <code>exception</code> with the
	 * {@link RunNotifier}.
	 * @param exception the exception upon which to base the failure
	 */
	protected void addFailure(Throwable exception) {
		this.notifier.fireTestFailure(new Failure(this.description, exception));
	}


	/**
	 * Runs the test method, executing <code>@Before</code> and <code>@After</code>
	 * methods accordingly.
	 */
	private class RunBeforesThenTestThenAfters implements Runnable {

		public void run() {
			try {
				runBefores();
				runTestMethod();
			}
			catch (FailedBefore ex) {
			}
			finally {
				runAfters();
			}
		}
	}


	/**
	 * Marker exception to signal that an exception was encountered while
	 * executing an {@link org.junit.Before @Before} method.
	 */
	private static class FailedBefore extends Exception {

	}

}
