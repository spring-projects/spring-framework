/*
 * Copyright 2002-2012 the original author or authors.
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
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * <p>
 * <code>TestContextManager</code> is the main entry point into the
 * <em>Spring TestContext Framework</em>, which provides support for loading and
 * accessing {@link ApplicationContext application contexts}, dependency
 * injection of test instances,
 * {@link org.springframework.transaction.annotation.Transactional
 * transactional} execution of test methods, etc.
 * </p>
 * <p>
 * Specifically, a <code>TestContextManager</code> is responsible for managing a
 * single {@link TestContext} and signaling events to all registered
 * {@link TestExecutionListener TestExecutionListeners} at well defined test
 * execution points:
 * </p>
 * <ul>
 * <li>{@link #beforeTestClass() before test class execution}: prior to any
 * <em>before class methods</em> of a particular testing framework (e.g., JUnit
 * 4's {@link org.junit.BeforeClass &#064;BeforeClass})</li>
 * <li>{@link #prepareTestInstance(Object) test instance preparation}:
 * immediately following instantiation of the test instance</li>
 * <li>{@link #beforeTestMethod(Object,Method) before test method execution}:
 * prior to any <em>before methods</em> of a particular testing framework (e.g.,
 * JUnit 4's {@link org.junit.Before &#064;Before})</li>
 * <li>{@link #afterTestMethod(Object,Method,Throwable) after test method
 * execution}: after any <em>after methods</em> of a particular testing
 * framework (e.g., JUnit 4's {@link org.junit.After &#064;After})</li>
 * <li>{@link #afterTestClass() after test class execution}: after any
 * <em>after class methods</em> of a particular testing framework (e.g., JUnit
 * 4's {@link org.junit.AfterClass &#064;AfterClass})</li>
 * </ul>
 * 
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 2.5
 * @see TestContext
 * @see TestExecutionListeners
 * @see ContextConfiguration
 * @see org.springframework.test.context.transaction.TransactionConfiguration
 */
public class TestContextManager {

	private static final String[] DEFAULT_TEST_EXECUTION_LISTENER_CLASS_NAMES = new String[] {
		"org.springframework.test.context.web.ServletTestExecutionListener",
		"org.springframework.test.context.support.DependencyInjectionTestExecutionListener",
		"org.springframework.test.context.support.DirtiesContextTestExecutionListener",
		"org.springframework.test.context.transaction.TransactionalTestExecutionListener" };

	private static final Log logger = LogFactory.getLog(TestContextManager.class);

	/**
	 * Cache of Spring application contexts. This needs to be static, as tests
	 * may be destroyed and recreated between running individual test methods,
	 * for example with JUnit.
	 */
	static final ContextCache contextCache = new ContextCache();

	private final TestContext testContext;

	private final List<TestExecutionListener> testExecutionListeners = new ArrayList<TestExecutionListener>();


	/**
	 * Delegates to {@link #TestContextManager(Class, String)} with a value of
	 * <code>null</code> for the default <code>ContextLoader</code> class name.
	 */
	public TestContextManager(Class<?> testClass) {
		this(testClass, null);
	}

	/**
	 * Constructs a new <code>TestContextManager</code> for the specified {@link Class test class}
	 * and automatically {@link #registerTestExecutionListeners registers} the
	 * {@link TestExecutionListener TestExecutionListeners} configured for the test class
	 * via the {@link TestExecutionListeners &#064;TestExecutionListeners} annotation.
	 * @param testClass the test class to be managed
	 * @param defaultContextLoaderClassName the name of the default
	 * <code>ContextLoader</code> class to use (may be <code>null</code>)
	 * @see #registerTestExecutionListeners(TestExecutionListener...)
	 * @see #retrieveTestExecutionListeners(Class)
	 */
	public TestContextManager(Class<?> testClass, String defaultContextLoaderClassName) {
		this.testContext = new TestContext(testClass, contextCache, defaultContextLoaderClassName);
		registerTestExecutionListeners(retrieveTestExecutionListeners(testClass));
	}

	/**
	 * Returns the {@link TestContext} managed by this
	 * <code>TestContextManager</code>.
	 */
	protected final TestContext getTestContext() {
		return this.testContext;
	}

	/**
	 * Register the supplied {@link TestExecutionListener TestExecutionListeners}
	 * by appending them to the set of listeners used by this <code>TestContextManager</code>.
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
	 * registered for this <code>TestContextManager</code>.
	 * <p>Allows for modifications, e.g. adding a listener to the beginning of the list.
	 * However, make sure to keep the list stable while actually executing tests.
	 */
	public final List<TestExecutionListener> getTestExecutionListeners() {
		return this.testExecutionListeners;
	}

	/**
	 * Get a copy of the {@link TestExecutionListener TestExecutionListeners}
	 * registered for this <code>TestContextManager</code> in reverse order.
	 */
	private List<TestExecutionListener> getReversedTestExecutionListeners() {
		List<TestExecutionListener> listenersReversed = new ArrayList<TestExecutionListener>(
			getTestExecutionListeners());
		Collections.reverse(listenersReversed);
		return listenersReversed;
	}

	/**
	 * Retrieve an array of newly instantiated {@link TestExecutionListener TestExecutionListeners}
	 * for the specified {@link Class class}. If {@link TestExecutionListeners &#064;TestExecutionListeners}
	 * is not <em>present</em> on the supplied class, the default listeners will be returned.
	 * <p>Note that the {@link TestExecutionListeners#inheritListeners() inheritListeners} flag of
	 * {@link TestExecutionListeners &#064;TestExecutionListeners} will be taken into consideration.
	 * Specifically, if the <code>inheritListeners</code> flag is set to <code>true</code>, listeners
	 * defined in the annotated class will be appended to the listeners defined in superclasses.
	 * @param clazz the test class for which the listeners should be retrieved
	 * @return an array of TestExecutionListeners for the specified class
	 */
	private TestExecutionListener[] retrieveTestExecutionListeners(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		Class<TestExecutionListeners> annotationType = TestExecutionListeners.class;
		List<Class<? extends TestExecutionListener>> classesList = new ArrayList<Class<? extends TestExecutionListener>>();
		Class<?> declaringClass = AnnotationUtils.findAnnotationDeclaringClass(annotationType, clazz);
		boolean defaultListeners = false;

		// Use defaults?
		if (declaringClass == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("@TestExecutionListeners is not present for class [" + clazz + "]: using defaults.");
			}
			classesList.addAll(getDefaultTestExecutionListenerClasses());
			defaultListeners = true;
		} else {
			// Traverse the class hierarchy...
			while (declaringClass != null) {
				TestExecutionListeners testExecutionListeners = declaringClass.getAnnotation(annotationType);
				if (logger.isTraceEnabled()) {
					logger.trace("Retrieved @TestExecutionListeners [" + testExecutionListeners
							+ "] for declaring class [" + declaringClass + "].");
				}

				Class<? extends TestExecutionListener>[] valueListenerClasses = testExecutionListeners.value();
				Class<? extends TestExecutionListener>[] listenerClasses = testExecutionListeners.listeners();
				if (!ObjectUtils.isEmpty(valueListenerClasses) && !ObjectUtils.isEmpty(listenerClasses)) {
					String msg = String.format(
						"Test class [%s] has been configured with @TestExecutionListeners' 'value' [%s] "
								+ "and 'listeners' [%s] attributes. Use one or the other, but not both.",
						declaringClass, ObjectUtils.nullSafeToString(valueListenerClasses),
						ObjectUtils.nullSafeToString(listenerClasses));
					logger.error(msg);
					throw new IllegalStateException(msg);
				} else if (!ObjectUtils.isEmpty(valueListenerClasses)) {
					listenerClasses = valueListenerClasses;
				}

				if (listenerClasses != null) {
					classesList.addAll(0, Arrays.<Class<? extends TestExecutionListener>> asList(listenerClasses));
				}
				declaringClass = (testExecutionListeners.inheritListeners() ? AnnotationUtils.findAnnotationDeclaringClass(
					annotationType, declaringClass.getSuperclass()) : null);
			}
		}

		List<TestExecutionListener> listeners = new ArrayList<TestExecutionListener>(classesList.size());
		for (Class<? extends TestExecutionListener> listenerClass : classesList) {
			try {
				listeners.add(BeanUtils.instantiateClass(listenerClass));
			} catch (NoClassDefFoundError err) {
				if (defaultListeners) {
					if (logger.isDebugEnabled()) {
						logger.debug("Could not instantiate default TestExecutionListener class ["
								+ listenerClass.getName()
								+ "]. Specify custom listener classes or make the default listener classes available.");
					}
				} else {
					throw err;
				}
			}
		}
		return listeners.toArray(new TestExecutionListener[listeners.size()]);
	}

	/**
	 * Determine the default {@link TestExecutionListener} classes.
	 */
	@SuppressWarnings("unchecked")
	protected Set<Class<? extends TestExecutionListener>> getDefaultTestExecutionListenerClasses() {
		Set<Class<? extends TestExecutionListener>> defaultListenerClasses = new LinkedHashSet<Class<? extends TestExecutionListener>>();
		for (String className : DEFAULT_TEST_EXECUTION_LISTENER_CLASS_NAMES) {
			try {
				defaultListenerClasses.add((Class<? extends TestExecutionListener>) getClass().getClassLoader().loadClass(
					className));
			} catch (Throwable t) {
				if (logger.isDebugEnabled()) {
					logger.debug("Could not load default TestExecutionListener class [" + className
							+ "]. Specify custom listener classes or make the default listener classes available.", t);
				}
			}
		}
		return defaultListenerClasses;
	}

	/**
	 * Hook for pre-processing a test class <em>before</em> execution of any
	 * tests within the class. Should be called prior to any framework-specific
	 * <em>before class methods</em> (e.g., methods annotated with JUnit's
	 * {@link org.junit.BeforeClass &#064;BeforeClass}).
	 * <p>An attempt will be made to give each registered
	 * {@link TestExecutionListener} a chance to pre-process the test class
	 * execution. If a listener throws an exception, however, the remaining
	 * registered listeners will <strong>not</strong> be called.
	 * @throws Exception if a registered TestExecutionListener throws an
	 * exception
	 * @see #getTestExecutionListeners()
	 */
	public void beforeTestClass() throws Exception {
		final Class<?> testClass = getTestContext().getTestClass();
		if (logger.isTraceEnabled()) {
			logger.trace("beforeTestClass(): class [" + testClass + "]");
		}
		getTestContext().updateState(null, null, null);

		for (TestExecutionListener testExecutionListener : getTestExecutionListeners()) {
			try {
				testExecutionListener.beforeTestClass(getTestContext());
			} catch (Exception ex) {
				logger.warn("Caught exception while allowing TestExecutionListener [" + testExecutionListener
						+ "] to process 'before class' callback for test class [" + testClass + "]", ex);
				throw ex;
			}
		}
	}

	/**
	 * Hook for preparing a test instance prior to execution of any individual
	 * test methods, for example for injecting dependencies, etc. Should be
	 * called immediately after instantiation of the test instance.
	 * <p>The managed {@link TestContext} will be updated with the supplied
	 * <code>testInstance</code>.
	 * <p>An attempt will be made to give each registered
	 * {@link TestExecutionListener} a chance to prepare the test instance. If a
	 * listener throws an exception, however, the remaining registered listeners
	 * will <strong>not</strong> be called.
	 * @param testInstance the test instance to prepare (never <code>null</code>)
	 * @throws Exception if a registered TestExecutionListener throws an exception
	 * @see #getTestExecutionListeners()
	 */
	public void prepareTestInstance(Object testInstance) throws Exception {
		Assert.notNull(testInstance, "testInstance must not be null");
		if (logger.isTraceEnabled()) {
			logger.trace("prepareTestInstance(): instance [" + testInstance + "]");
		}
		getTestContext().updateState(testInstance, null, null);

		for (TestExecutionListener testExecutionListener : getTestExecutionListeners()) {
			try {
				testExecutionListener.prepareTestInstance(getTestContext());
			} catch (Exception ex) {
				logger.error("Caught exception while allowing TestExecutionListener [" + testExecutionListener
						+ "] to prepare test instance [" + testInstance + "]", ex);
				throw ex;
			}
		}
	}

	/**
	 * Hook for pre-processing a test <em>before</em> execution of the supplied
	 * {@link Method test method}, for example for setting up test fixtures,
	 * starting a transaction, etc. Should be called prior to any
	 * framework-specific <em>before methods</em> (e.g., methods annotated with
	 * JUnit's {@link org.junit.Before &#064;Before}).
	 * <p>The managed {@link TestContext} will be updated with the supplied
	 * <code>testInstance</code> and <code>testMethod</code>.
	 * <p>An attempt will be made to give each registered
	 * {@link TestExecutionListener} a chance to pre-process the test method
	 * execution. If a listener throws an exception, however, the remaining
	 * registered listeners will <strong>not</strong> be called.
	 * @param testInstance the current test instance (never <code>null</code>)
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
			} catch (Exception ex) {
				logger.warn("Caught exception while allowing TestExecutionListener [" + testExecutionListener
						+ "] to process 'before' execution of test method [" + testMethod + "] for test instance ["
						+ testInstance + "]", ex);
				throw ex;
			}
		}
	}

	/**
	 * Hook for post-processing a test <em>after</em> execution of the supplied
	 * {@link Method test method}, for example for tearing down test fixtures,
	 * ending a transaction, etc. Should be called after any framework-specific
	 * <em>after methods</em> (e.g., methods annotated with JUnit's
	 * {@link org.junit.After &#064;After}).
	 * <p>The managed {@link TestContext} will be updated with the supplied
	 * <code>testInstance</code>, <code>testMethod</code>, and
	 * <code>exception</code>.
	 * <p>Each registered {@link TestExecutionListener} will be given a chance to
	 * post-process the test method execution. If a listener throws an
	 * exception, the remaining registered listeners will still be called, but
	 * the first exception thrown will be tracked and rethrown after all
	 * listeners have executed. Note that registered listeners will be executed
	 * in the opposite order in which they were registered.
	 * @param testInstance the current test instance (never <code>null</code>)
	 * @param testMethod the test method which has just been executed on the
	 * test instance
	 * @param exception the exception that was thrown during execution of the
	 * test method or by a TestExecutionListener, or <code>null</code> if none
	 * was thrown
	 * @throws Exception if a registered TestExecutionListener throws an exception
	 * @see #getTestExecutionListeners()
	 */
	public void afterTestMethod(Object testInstance, Method testMethod, Throwable exception) throws Exception {
		Assert.notNull(testInstance, "testInstance must not be null");
		if (logger.isTraceEnabled()) {
			logger.trace("afterTestMethod(): instance [" + testInstance + "], method [" + testMethod + "], exception ["
					+ exception + "]");
		}
		getTestContext().updateState(testInstance, testMethod, exception);

		Exception afterTestMethodException = null;
		// Traverse the TestExecutionListeners in reverse order to ensure proper
		// "wrapper"-style execution of listeners.
		for (TestExecutionListener testExecutionListener : getReversedTestExecutionListeners()) {
			try {
				testExecutionListener.afterTestMethod(getTestContext());
			} catch (Exception ex) {
				logger.warn("Caught exception while allowing TestExecutionListener [" + testExecutionListener
						+ "] to process 'after' execution for test: method [" + testMethod + "], instance ["
						+ testInstance + "], exception [" + exception + "]", ex);
				if (afterTestMethodException == null) {
					afterTestMethodException = ex;
				}
			}
		}
		if (afterTestMethodException != null) {
			throw afterTestMethodException;
		}
	}

	/**
	 * Hook for post-processing a test class <em>after</em> execution of all
	 * tests within the class. Should be called after any framework-specific
	 * <em>after class methods</em> (e.g., methods annotated with JUnit's
	 * {@link org.junit.AfterClass &#064;AfterClass}).
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
		final Class<?> testClass = getTestContext().getTestClass();
		if (logger.isTraceEnabled()) {
			logger.trace("afterTestClass(): class [" + testClass + "]");
		}
		getTestContext().updateState(null, null, null);

		Exception afterTestClassException = null;
		// Traverse the TestExecutionListeners in reverse order to ensure proper
		// "wrapper"-style execution of listeners.
		for (TestExecutionListener testExecutionListener : getReversedTestExecutionListeners()) {
			try {
				testExecutionListener.afterTestClass(getTestContext());
			} catch (Exception ex) {
				logger.warn("Caught exception while allowing TestExecutionListener [" + testExecutionListener
						+ "] to process 'after class' callback for test class [" + testClass + "]", ex);
				if (afterTestClassException == null) {
					afterTestClassException = ex;
				}
			}
		}
		if (afterTestClassException != null) {
			throw afterTestClassException;
		}
	}

}