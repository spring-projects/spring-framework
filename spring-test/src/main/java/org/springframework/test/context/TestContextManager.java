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

package org.springframework.test.context;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * {@code TestContextManager} is the main entry point into the <em>Spring
 * TestContext Framework</em>.
 *
 * <p>Specifically, a {@code TestContextManager} is responsible for managing a
 * single {@link TestContext} and signaling events to all registered
 * {@link TestExecutionListener TestExecutionListeners} at the following test
 * execution points:
 *
 * <ul>
 * <li>{@link #beforeTestClass() before test class execution}: prior to any
 * <em>before class callbacks</em> of a particular testing framework (e.g.,
 * JUnit 4's {@link org.junit.BeforeClass @BeforeClass})</li>
 * <li>{@link #prepareTestInstance(Object) test instance preparation}:
 * immediately following instantiation of the test instance</li>
 * <li>{@link #beforeTestMethod(Object, Method) before test method execution}:
 * prior to any <em>before method callbacks</em> of a particular testing framework
 * (e.g., JUnit 4's {@link org.junit.Before @Before})</li>
 * <li>{@link #afterTestMethod(Object, Method, Throwable) after test method
 * execution}: after any <em>after method callbacks</em> of a particular testing
 * framework (e.g., JUnit 4's {@link org.junit.After @After})</li>
 * <li>{@link #afterTestClass() after test class execution}: after any
 * <em>after class callbacks</em> of a particular testing framework (e.g., JUnit
 * 4's {@link org.junit.AfterClass @AfterClass})</li>
 * </ul>
 *
 * <p>Support for loading and accessing
 * {@link org.springframework.context.ApplicationContext application contexts},
 * dependency injection of test instances,
 * {@link org.springframework.transaction.annotation.Transactional transactional}
 * execution of test methods, etc. is provided by
 * {@link SmartContextLoader ContextLoaders} and {@link TestExecutionListener
 * TestExecutionListeners}, which are configured via
 * {@link ContextConfiguration @ContextConfiguration} and
 * {@link TestExecutionListeners @TestExecutionListeners}.
 *
 * <p>Bootstrapping of the {@code TestContext}, the default {@code ContextLoader},
 * default {@code TestExecutionListeners}, and their collaborators is performed
 * by a {@link TestContextBootstrapper}, which is configured via
 * {@link BootstrapWith @BootstrapWith}.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 2.5
 * @see BootstrapWith
 * @see BootstrapContext
 * @see TestContextBootstrapper
 * @see TestContext
 * @see TestExecutionListener
 * @see TestExecutionListeners
 * @see ContextConfiguration
 * @see ContextHierarchy
 */
public class TestContextManager {

	private static final Log logger = LogFactory.getLog(TestContextManager.class);

	private final TestContext testContext;

	private final List<TestExecutionListener> testExecutionListeners = new ArrayList<TestExecutionListener>();


	/**
	 * Construct a new {@code TestContextManager} for the supplied {@linkplain Class test class}.
	 * <p>Delegates to {@link #TestContextManager(TestContextBootstrapper)} with
	 * the {@link TestContextBootstrapper} configured for the test class. If the
	 * {@link BootstrapWith @BootstrapWith} annotation is present on the test
	 * class, either directly or as a meta-annotation, then its
	 * {@link BootstrapWith#value value} will be used as the bootstrapper type;
	 * otherwise, the {@link org.springframework.test.context.support.DefaultTestContextBootstrapper
	 * DefaultTestContextBootstrapper} will be used.
	 * @param testClass the test class to be managed
	 * @see #TestContextManager(TestContextBootstrapper)
	 */
	public TestContextManager(Class<?> testClass) {
		this(BootstrapUtils.resolveTestContextBootstrapper(BootstrapUtils.createBootstrapContext(testClass)));
	}

	/**
	 * Construct a new {@code TestContextManager} using the supplied {@link TestContextBootstrapper}
	 * and {@linkplain #registerTestExecutionListeners register} the necessary
	 * {@link TestExecutionListener TestExecutionListeners}.
	 * <p>Delegates to the supplied {@code TestContextBootstrapper} for building
	 * the {@code TestContext} and retrieving the {@code TestExecutionListeners}.
	 * @param testContextBootstrapper the bootstrapper to use
	 * @see TestContextBootstrapper#buildTestContext
	 * @see TestContextBootstrapper#getTestExecutionListeners
	 * @see #registerTestExecutionListeners
	 */
	public TestContextManager(TestContextBootstrapper testContextBootstrapper) {
		this.testContext = testContextBootstrapper.buildTestContext();
		registerTestExecutionListeners(testContextBootstrapper.getTestExecutionListeners());
	}

	/**
	 * Get the {@link TestContext} managed by this {@code TestContextManager}.
	 */
	public final TestContext getTestContext() {
		return this.testContext;
	}

	/**
	 * Register the supplied list of {@link TestExecutionListener TestExecutionListeners}
	 * by appending them to the list of listeners used by this {@code TestContextManager}.
	 * @see #registerTestExecutionListeners(TestExecutionListener...)
	 */
	public void registerTestExecutionListeners(List<TestExecutionListener> testExecutionListeners) {
		registerTestExecutionListeners(testExecutionListeners.toArray(new TestExecutionListener[testExecutionListeners.size()]));
	}

	/**
	 * Register the supplied array of {@link TestExecutionListener TestExecutionListeners}
	 * by appending them to the list of listeners used by this {@code TestContextManager}.
	 */
	public void registerTestExecutionListeners(TestExecutionListener... testExecutionListeners) {
		for (TestExecutionListener listener : testExecutionListeners) {
			if (logger.isTraceEnabled()) {
				logger.trace("Registering TestExecutionListener: " + listener);
			}
			this.testExecutionListeners.add(listener);
		}
	}

	/**
	 * Get the current {@link TestExecutionListener TestExecutionListeners}
	 * registered for this {@code TestContextManager}.
	 * <p>Allows for modifications, e.g. adding a listener to the beginning of the list.
	 * However, make sure to keep the list stable while actually executing tests.
	 */
	public final List<TestExecutionListener> getTestExecutionListeners() {
		return this.testExecutionListeners;
	}

	/**
	 * Get a copy of the {@link TestExecutionListener TestExecutionListeners}
	 * registered for this {@code TestContextManager} in reverse order.
	 */
	private List<TestExecutionListener> getReversedTestExecutionListeners() {
		List<TestExecutionListener> listenersReversed = new ArrayList<TestExecutionListener>(getTestExecutionListeners());
		Collections.reverse(listenersReversed);
		return listenersReversed;
	}

	/**
	 * Hook for pre-processing a test class <em>before</em> execution of any
	 * tests within the class. Should be called prior to any framework-specific
	 * <em>before class methods</em> (e.g., methods annotated with JUnit's
	 * {@link org.junit.BeforeClass @BeforeClass}).
	 * <p>An attempt will be made to give each registered
	 * {@link TestExecutionListener} a chance to pre-process the test class
	 * execution. If a listener throws an exception, however, the remaining
	 * registered listeners will <strong>not</strong> be called.
	 * @throws Exception if a registered TestExecutionListener throws an
	 * exception
	 * @see #getTestExecutionListeners()
	 */
	public void beforeTestClass() throws Exception {
		Class<?> testClass = getTestContext().getTestClass();
		if (logger.isTraceEnabled()) {
			logger.trace("beforeTestClass(): class [" + testClass.getName() + "]");
		}
		getTestContext().updateState(null, null, null);

		for (TestExecutionListener testExecutionListener : getTestExecutionListeners()) {
			try {
				testExecutionListener.beforeTestClass(getTestContext());
			}
			catch (Throwable ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Caught exception while allowing TestExecutionListener [" + testExecutionListener +
							"] to process 'before class' callback for test class [" + testClass + "]", ex);
				}
				ReflectionUtils.rethrowException(ex);
			}
		}
	}

	/**
	 * Hook for preparing a test instance prior to execution of any individual
	 * test methods, for example for injecting dependencies, etc. Should be
	 * called immediately after instantiation of the test instance.
	 * <p>The managed {@link TestContext} will be updated with the supplied
	 * {@code testInstance}.
	 * <p>An attempt will be made to give each registered
	 * {@link TestExecutionListener} a chance to prepare the test instance. If a
	 * listener throws an exception, however, the remaining registered listeners
	 * will <strong>not</strong> be called.
	 * @param testInstance the test instance to prepare (never {@code null})
	 * @throws Exception if a registered TestExecutionListener throws an exception
	 * @see #getTestExecutionListeners()
	 */
	public void prepareTestInstance(Object testInstance) throws Exception {
		Assert.notNull(testInstance, "Test instance must not be null");
		if (logger.isTraceEnabled()) {
			logger.trace("prepareTestInstance(): instance [" + testInstance + "]");
		}
		getTestContext().updateState(testInstance, null, null);

		for (TestExecutionListener testExecutionListener : getTestExecutionListeners()) {
			try {
				testExecutionListener.prepareTestInstance(getTestContext());
			}
			catch (Throwable ex) {
				if (logger.isErrorEnabled()) {
					logger.error("Caught exception while allowing TestExecutionListener [" + testExecutionListener +
							"] to prepare test instance [" + testInstance + "]", ex);
				}
				ReflectionUtils.rethrowException(ex);
			}
		}
	}

	/**
	 * Hook for pre-processing a test <em>before</em> execution of the supplied
	 * {@link Method test method}, for example for setting up test fixtures,
	 * starting a transaction, etc. Should be called prior to any
	 * framework-specific <em>before methods</em> (e.g., methods annotated with
	 * JUnit's {@link org.junit.Before @Before}).
	 * <p>The managed {@link TestContext} will be updated with the supplied
	 * {@code testInstance} and {@code testMethod}.
	 * <p>An attempt will be made to give each registered
	 * {@link TestExecutionListener} a chance to pre-process the test method
	 * execution. If a listener throws an exception, however, the remaining
	 * registered listeners will <strong>not</strong> be called.
	 * @param testInstance the current test instance (never {@code null})
	 * @param testMethod the test method which is about to be executed on the
	 * test instance
	 * @throws Exception if a registered TestExecutionListener throws an exception
	 * @see #getTestExecutionListeners()
	 */
	public void beforeTestMethod(Object testInstance, Method testMethod) throws Exception {
		Assert.notNull(testInstance, "Test instance must not be null");
		if (logger.isTraceEnabled()) {
			logger.trace("beforeTestMethod(): instance [" + testInstance + "], method [" + testMethod + "]");
		}
		getTestContext().updateState(testInstance, testMethod, null);

		for (TestExecutionListener testExecutionListener : getTestExecutionListeners()) {
			try {
				testExecutionListener.beforeTestMethod(getTestContext());
			}
			catch (Throwable ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Caught exception while allowing TestExecutionListener [" + testExecutionListener +
							"] to process 'before' execution of test method [" + testMethod + "] for test instance [" +
							testInstance + "]", ex);
				}
				ReflectionUtils.rethrowException(ex);
			}
		}
	}

	/**
	 * Hook for post-processing a test <em>after</em> execution of the supplied
	 * {@link Method test method}, for example for tearing down test fixtures,
	 * ending a transaction, etc. Should be called after any framework-specific
	 * <em>after methods</em> (e.g., methods annotated with JUnit's
	 * {@link org.junit.After @After}).
	 * <p>The managed {@link TestContext} will be updated with the supplied
	 * {@code testInstance}, {@code testMethod}, and
	 * {@code exception}.
	 * <p>Each registered {@link TestExecutionListener} will be given a chance to
	 * post-process the test method execution. If a listener throws an
	 * exception, the remaining registered listeners will still be called, but
	 * the first exception thrown will be tracked and rethrown after all
	 * listeners have executed. Note that registered listeners will be executed
	 * in the opposite order in which they were registered.
	 * @param testInstance the current test instance (never {@code null})
	 * @param testMethod the test method which has just been executed on the
	 * test instance
	 * @param exception the exception that was thrown during execution of the
	 * test method or by a TestExecutionListener, or {@code null} if none
	 * was thrown
	 * @throws Exception if a registered TestExecutionListener throws an exception
	 * @see #getTestExecutionListeners()
	 */
	public void afterTestMethod(Object testInstance, Method testMethod, Throwable exception) throws Exception {
		Assert.notNull(testInstance, "Test instance must not be null");
		if (logger.isTraceEnabled()) {
			logger.trace("afterTestMethod(): instance [" + testInstance + "], method [" + testMethod +
					"], exception [" + exception + "]");
		}
		getTestContext().updateState(testInstance, testMethod, exception);

		Throwable afterTestMethodException = null;
		// Traverse the TestExecutionListeners in reverse order to ensure proper
		// "wrapper"-style execution of listeners.
		for (TestExecutionListener testExecutionListener : getReversedTestExecutionListeners()) {
			try {
				testExecutionListener.afterTestMethod(getTestContext());
			}
			catch (Throwable ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Caught exception while allowing TestExecutionListener [" + testExecutionListener +
							"] to process 'after' execution for test: method [" + testMethod + "], instance [" +
							testInstance + "], exception [" + exception + "]", ex);
				}
				if (afterTestMethodException == null) {
					afterTestMethodException = ex;
				}
			}
		}
		if (afterTestMethodException != null) {
			ReflectionUtils.rethrowException(afterTestMethodException);
		}
	}

	/**
	 * Hook for post-processing a test class <em>after</em> execution of all
	 * tests within the class. Should be called after any framework-specific
	 * <em>after class methods</em> (e.g., methods annotated with JUnit's
	 * {@link org.junit.AfterClass @AfterClass}).
	 * <p>Each registered {@link TestExecutionListener} will be given a chance to
	 * post-process the test class. If a listener throws an exception, the
	 * remaining registered listeners will still be called, but the first
	 * exception thrown will be tracked and rethrown after all listeners have
	 * executed. Note that registered listeners will be executed in the opposite
	 * order in which they were registered.
	 * @throws Exception if a registered TestExecutionListener throws an exception
	 * @see #getTestExecutionListeners()
	 */
	public void afterTestClass() throws Exception {
		Class<?> testClass = getTestContext().getTestClass();
		if (logger.isTraceEnabled()) {
			logger.trace("afterTestClass(): class [" + testClass.getName() + "]");
		}
		getTestContext().updateState(null, null, null);

		Throwable afterTestClassException = null;
		// Traverse the TestExecutionListeners in reverse order to ensure proper
		// "wrapper"-style execution of listeners.
		for (TestExecutionListener testExecutionListener : getReversedTestExecutionListeners()) {
			try {
				testExecutionListener.afterTestClass(getTestContext());
			}
			catch (Throwable ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Caught exception while allowing TestExecutionListener [" + testExecutionListener +
							"] to process 'after class' callback for test class [" + testClass + "]", ex);
				}
				if (afterTestClassException == null) {
					afterTestClassException = ex;
				}
			}
		}
		if (afterTestClassException != null) {
			ReflectionUtils.rethrowException(afterTestClassException);
		}
	}

}
