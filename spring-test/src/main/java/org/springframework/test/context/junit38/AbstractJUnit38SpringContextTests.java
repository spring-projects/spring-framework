/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.test.context.junit38;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.annotation.ExpectedException;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.annotation.ProfileValueSource;
import org.springframework.test.annotation.ProfileValueUtils;
import org.springframework.test.annotation.Repeat;
import org.springframework.test.annotation.Timed;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

/**
 * <p>
 * Abstract base {@link TestCase} which integrates the <i>Spring TestContext
 * Framework</i> and explicit {@link ApplicationContext} testing support in a
 * <strong>JUnit 3.8</strong> environment.
 * </p>
 * <p>
 * Concrete subclasses:
 * </p>
 * <ul>
 * <li>Typically declare a class-level
 * {@link org.springframework.test.context.ContextConfiguration
 * &#064;ContextConfiguration} annotation to configure the
 * {@link ApplicationContext application context}
 * {@link org.springframework.test.context.ContextConfiguration#locations()
 * resource locations}. <i>If your test does not need to load an application
 * context, you may choose to omit the
 * {@link org.springframework.test.context.ContextConfiguration
 * &#064;ContextConfiguration} declaration and configure the appropriate
 * {@link org.springframework.test.context.TestExecutionListener
 * TestExecutionListeners} manually.</i></li>
 * <li>Must declare public constructors which match the signatures of
 * {@link #AbstractJUnit38SpringContextTests()
 * AbstractJUnit38SpringContextTests()} and
 * {@link #AbstractJUnit38SpringContextTests(String)
 * AbstractJUnit38SpringContextTests(String)} and delegate to
 * <code>super();</code> and <code>super(name);</code> respectively.</li>
 * </ul>
 * <p>
 * The following list constitutes all annotations currently supported directly
 * by <code>AbstractJUnit38SpringContextTests</code>. <i>(Note that additional
 * annotations may be supported by various
 * {@link org.springframework.test.context.TestExecutionListener
 * TestExecutionListeners})</i>
 * </p>
 * <ul>
 * <li>{@link org.springframework.test.annotation.DirtiesContext
 * &#064;DirtiesContext} (via the configured
 * {@link DirtiesContextTestExecutionListener}; only supported on methods for
 * JUnit 3.8)</li>
 * <li>
 * {@link org.springframework.test.annotation.ProfileValueSourceConfiguration
 * &#064;ProfileValueSourceConfiguration}</li>
 * <li>{@link IfProfileValue &#064;IfProfileValue}</li>
 * <li>{@link ExpectedException &#064;ExpectedException}</li>
 * <li>{@link Timed &#064;Timed}</li>
 * <li>{@link Repeat &#064;Repeat}</li>
 * </ul>
 * <p>
 * JUnit 3.8 does not support <i>before class</i> or <i>after class</i>
 * lifecycle callbacks. The following
 * {@link org.springframework.test.context.TestExecutionListener
 * TestExecutionListener} methods are therefore unsupported in a JUnit 3.8
 * environment:
 * <ul>
 * <li>
 * {@link org.springframework.test.context.TestExecutionListener#beforeTestClass(org.springframework.test.context.TestContext)
 * beforeTestClass()}</li>
 * <li>
 * {@link org.springframework.test.context.TestExecutionListener#afterTestClass(org.springframework.test.context.TestContext)
 * afterTestClass()}</li>
 * </ul>
 * 
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 2.5
 * @see org.springframework.test.context.TestContext
 * @see org.springframework.test.context.TestContextManager
 * @see org.springframework.test.context.TestExecutionListeners
 * @see AbstractTransactionalJUnit38SpringContextTests
 * @see org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests
 * @see org.springframework.test.context.testng.AbstractTestNGSpringContextTests
 * @deprecated as of Spring 3.1, in favor of using
 * {@link org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests AbstractJUnit4SpringContextTests}
 */
@Deprecated
@TestExecutionListeners( { DependencyInjectionTestExecutionListener.class, DirtiesContextTestExecutionListener.class })
public abstract class AbstractJUnit38SpringContextTests extends TestCase implements ApplicationContextAware {

	private static int disabledTestCount = 0;


	/**
	 * Return the number of tests disabled in this environment.
	 */
	public static int getDisabledTestCount() {
		return disabledTestCount;
	}


	/**
	 * Logger available to subclasses.
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * The {@link ApplicationContext} that was injected into this test instance
	 * via {@link #setApplicationContext(ApplicationContext)}.
	 */
	protected ApplicationContext applicationContext;

	/**
	 * {@link ProfileValueSource} available to subclasses but primarily intended
	 * for internal use to provide support for {@link IfProfileValue
	 * &#064;IfProfileValue}.
	 */
	protected final ProfileValueSource profileValueSource;

	private final TestContextManager testContextManager;


	/**
	 * Constructs a new AbstractJUnit38SpringContextTests instance; initializes
	 * the internal {@link TestContextManager} for the current test; and
	 * retrieves the configured (or default) {@link ProfileValueSource}.
	 */
	public AbstractJUnit38SpringContextTests() {
		super();
		this.testContextManager = new TestContextManager(getClass());
		this.profileValueSource = ProfileValueUtils.retrieveProfileValueSource(getClass());
	}

	/**
	 * Constructs a new AbstractJUnit38SpringContextTests instance with the
	 * supplied <code>name</code>; initializes the internal
	 * {@link TestContextManager} for the current test; and retrieves the
	 * configured (or default) {@link ProfileValueSource}.
	 * 
	 * @param name the name of the current test to execute
	 */
	public AbstractJUnit38SpringContextTests(String name) {
		super(name);
		this.testContextManager = new TestContextManager(getClass());
		this.profileValueSource = ProfileValueUtils.retrieveProfileValueSource(getClass());
	}

	/**
	 * Sets the {@link ApplicationContext} to be used by this test instance,
	 * provided via {@link ApplicationContextAware} semantics.
	 */
	public final void setApplicationContext(final ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	/**
	 * Runs the <em>Spring TestContext Framework</em> test sequence.
	 * <p>
	 * In addition to standard {@link TestCase#runBare()} semantics, this
	 * implementation performs the following:
	 * <ul>
	 * <li>Calls {@link TestContextManager#prepareTestInstance(Object)
	 * prepareTestInstance()},
	 * {@link TestContextManager#beforeTestMethod(Object,Method)
	 * beforeTestMethod()}, and
	 * {@link TestContextManager#afterTestMethod(Object,Method,Throwable)
	 * afterTestMethod()} on this test's {@link TestContextManager} at the
	 * appropriate test execution points.</li>
	 * <li>Provides support for {@link IfProfileValue &#064;IfProfileValue}.</li>
	 * <li>Provides support for {@link Repeat &#064;Repeat}.</li>
	 * <li>Provides support for {@link Timed &#064;Timed}.</li>
	 * <li>Provides support for {@link ExpectedException
	 * &#064;ExpectedException}.</li>
	 * </ul>
	 * 
	 * @see ProfileValueUtils#isTestEnabledInThisEnvironment
	 */
	@Override
	public void runBare() throws Throwable {
		this.testContextManager.prepareTestInstance(this);
		final Method testMethod = getTestMethod();

		if (!ProfileValueUtils.isTestEnabledInThisEnvironment(this.profileValueSource, testMethod, getClass())) {
			recordDisabled(testMethod);
			return;
		}

		runTestTimed(new TestExecutionCallback() {

			public void run() throws Throwable {
				runManaged(testMethod);
			}
		}, testMethod);
	}

	/**
	 * Get the current test method.
	 */
	private Method getTestMethod() {
		assertNotNull("TestCase.getName() cannot be null", getName());
		Method testMethod = null;
		try {
			testMethod = getClass().getMethod(getName(), (Class[]) null);
		}
		catch (NoSuchMethodException ex) {
			fail("Method \"" + getName() + "\" not found");
		}
		if (!Modifier.isPublic(testMethod.getModifiers())) {
			fail("Method \"" + getName() + "\" should be public");
		}
		return testMethod;
	}

	/**
	 * Runs a <em>timed</em> test via the supplied {@link TestExecutionCallback}
	 * , providing support for the {@link Timed &#064;Timed} annotation.
	 * 
	 * @param tec the test execution callback to run
	 * @param testMethod the actual test method: used to retrieve the
	 * <code>timeout</code>
	 * @throws Throwable if any exception is thrown
	 * @see Timed
	 * @see #runTest
	 */
	private void runTestTimed(TestExecutionCallback tec, Method testMethod) throws Throwable {
		Timed timed = testMethod.getAnnotation(Timed.class);
		if (timed == null) {
			runTest(tec, testMethod);
		}
		else {
			long startTime = System.currentTimeMillis();
			try {
				runTest(tec, testMethod);
			}
			finally {
				long elapsed = System.currentTimeMillis() - startTime;
				if (elapsed > timed.millis()) {
					fail("Took " + elapsed + " ms; limit was " + timed.millis());
				}
			}
		}
	}

	/**
	 * Runs a test via the supplied {@link TestExecutionCallback}, providing
	 * support for the {@link ExpectedException &#064;ExpectedException} and
	 * {@link Repeat &#064;Repeat} annotations.
	 * 
	 * @param tec the test execution callback to run
	 * @param testMethod the actual test method: used to retrieve the
	 * {@link ExpectedException &#064;ExpectedException} and {@link Repeat
	 * &#064;Repeat} annotations
	 * @throws Throwable if any exception is thrown
	 * @see ExpectedException
	 * @see Repeat
	 */
	private void runTest(TestExecutionCallback tec, Method testMethod) throws Throwable {
		ExpectedException expectedExceptionAnnotation = testMethod.getAnnotation(ExpectedException.class);
		boolean exceptionIsExpected = (expectedExceptionAnnotation != null && expectedExceptionAnnotation.value() != null);
		Class<? extends Throwable> expectedException = (exceptionIsExpected ? expectedExceptionAnnotation.value()
				: null);

		Repeat repeat = testMethod.getAnnotation(Repeat.class);
		int runs = ((repeat != null) && (repeat.value() > 1)) ? repeat.value() : 1;

		for (int i = 0; i < runs; i++) {
			try {
				if (runs > 1 && this.logger.isInfoEnabled()) {
					this.logger.info("Repetition " + (i + 1) + " of test " + testMethod.getName());
				}
				tec.run();
				if (exceptionIsExpected) {
					fail("Expected exception: " + expectedException.getName());
				}
			}
			catch (Throwable ex) {
				if (!exceptionIsExpected) {
					throw ex;
				}
				if (!expectedException.isAssignableFrom(ex.getClass())) {
					// Wrap the unexpected throwable with an explicit message.
					AssertionFailedError assertionError = new AssertionFailedError("Unexpected exception, expected <"
							+ expectedException.getName() + "> but was <" + ex.getClass().getName() + ">");
					assertionError.initCause(ex);
					throw assertionError;
				}
			}
		}
	}

	/**
	 * Calls {@link TestContextManager#beforeTestMethod(Object,Method)} and
	 * {@link TestContextManager#afterTestMethod(Object,Method,Throwable)} at
	 * the appropriate test execution points.
	 * 
	 * @param testMethod the test method to run
	 * @throws Throwable if any exception is thrown
	 * @see #runBare()
	 * @see TestCase#runTest()
	 */
	private void runManaged(Method testMethod) throws Throwable {
		Throwable exception = null;
		boolean reachedTest = false;

		try {
			this.testContextManager.beforeTestMethod(this, testMethod);
			setUp();
			reachedTest = true;
			runTest();
		}
		catch (Throwable ex) {
			exception = ex;
		}
		finally {
			try {
				if (reachedTest) {
					tearDown();
				}
			}
			catch (Throwable ex) {
				if (exception == null) {
					exception = ex;
				}
			}
			finally {
				try {
					this.testContextManager.afterTestMethod(this, testMethod, exception);
				}
				catch (Throwable ex) {
					if (exception == null) {
						exception = ex;
					}
				}
			}
		}

		if (exception != null) {
			if (exception.getCause() instanceof AssertionError) {
				exception = exception.getCause();
			}
			throw exception;
		}
	}

	/**
	 * Records the supplied test method as <em>disabled</em> in the current
	 * environment by incrementing the total number of disabled tests and
	 * logging a debug message.
	 * 
	 * @param testMethod the test method that is disabled.
	 * @see #getDisabledTestCount()
	 */
	protected void recordDisabled(Method testMethod) {
		disabledTestCount++;
		if (this.logger.isInfoEnabled()) {
			this.logger.info("**** " + getClass().getName() + "." + getName() + "() is disabled in this environment. "
					+ "Total disabled tests = " + getDisabledTestCount());
		}
	}


	/**
	 * Private inner class that defines a callback analogous to {@link Runnable}
	 * , just declaring Throwable.
	 */
	private static interface TestExecutionCallback {

		void run() throws Throwable;
	}

}
