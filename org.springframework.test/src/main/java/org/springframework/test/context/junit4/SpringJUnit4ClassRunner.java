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

package org.springframework.test.context.junit4;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.internal.runners.InitializationError;
import org.junit.internal.runners.JUnit4ClassRunner;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.springframework.test.annotation.ProfileValueUtils;
import org.springframework.test.context.TestContextManager;

/**
 * <p>
 * SpringJUnit4ClassRunner is a custom extension of {@link JUnit4ClassRunner}
 * which provides functionality of the <em>Spring TestContext Framework</em> to
 * standard JUnit 4.5+ tests by means of the {@link TestContextManager} and
 * associated support classes and annotations.
 * </p>
 * <p>
 * The following list constitutes all annotations currently supported directly
 * by SpringJUnit4ClassRunner. <em>(Note that additional annotations
 * may be supported by various
 * {@link org.springframework.test.context.TestExecutionListener
 * TestExecutionListeners})</em>
 * </p>
 * <ul>
 * <li>{@link org.junit.Test#expected() &#064;Test(expected=...)}</li>
 * <li>{@link org.springframework.test.annotation.ExpectedException
 * &#064;ExpectedException}</li>
 * <li>{@link org.junit.Test#timeout() &#064;Test(timeout=...)}</li>
 * <li>{@link org.springframework.test.annotation.Timed &#064;Timed}</li>
 * <li>{@link org.springframework.test.annotation.Repeat &#064;Repeat}</li>
 * <li>{@link org.junit.Ignore &#064;Ignore}</li>
 * <li>
 * {@link org.springframework.test.annotation.ProfileValueSourceConfiguration
 * &#064;ProfileValueSourceConfiguration}</li>
 * <li>{@link org.springframework.test.annotation.IfProfileValue
 * &#064;IfProfileValue}</li>
 * </ul>
 * <p>
 * <b>NOTE:</b> As of Spring 3.0 M1, SpringJUnit4ClassRunner requires JUnit 4.5,
 * while internally still being based on JUnit 4.4 SPI. This will be rewritten
 * based on JUnit 4.5's BlockJUnit4ClassRunner in a later Spring 3.0 release.
 * </p>
 * 
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 2.5
 * @see TestContextManager
 */
public class SpringJUnit4ClassRunner extends JUnit4ClassRunner {

	private static final Log logger = LogFactory.getLog(SpringJUnit4ClassRunner.class);

	private final TestContextManager testContextManager;


	/**
	 * Constructs a new <code>SpringJUnit4ClassRunner</code> and initializes a
	 * {@link TestContextManager} to provide Spring testing functionality to
	 * standard JUnit tests.
	 * 
	 * @param clazz the Class object corresponding to the test class to be run
	 * @see #createTestContextManager(Class)
	 */
	public SpringJUnit4ClassRunner(Class<?> clazz) throws InitializationError {
		super(clazz);
		if (logger.isDebugEnabled()) {
			logger.debug("SpringJUnit4ClassRunner constructor called with [" + clazz + "].");
		}
		this.testContextManager = createTestContextManager(clazz);
	}

	/**
	 * Check whether the test is enabled in the first place. This prevents
	 * classes with a non-matching <code>&#064;IfProfileValue</code> annotation
	 * from running altogether, even skipping the execution of
	 * <code>prepareTestInstance</code> listener methods.
	 * 
	 * @see org.springframework.test.annotation.IfProfileValue
	 * @see org.springframework.test.context.TestExecutionListener
	 */
	@Override
	public void run(RunNotifier notifier) {
		if (!ProfileValueUtils.isTestEnabledInThisEnvironment(getTestClass().getJavaClass())) {
			notifier.fireTestIgnored(getDescription());
			return;
		}
		super.run(notifier);
	}

	/**
	 * Delegates to {@link JUnit4ClassRunner#createTest()} to create the test
	 * instance and then to a {@link TestContextManager} to
	 * {@link TestContextManager#prepareTestInstance(Object) prepare} the test
	 * instance for Spring testing functionality.
	 * 
	 * @see JUnit4ClassRunner#createTest()
	 * @see TestContextManager#prepareTestInstance(Object)
	 */
	@Override
	protected Object createTest() throws Exception {
		Object testInstance = super.createTest();
		getTestContextManager().prepareTestInstance(testInstance);
		return testInstance;
	}

	/**
	 * Creates a new {@link TestContextManager}. Can be overridden by
	 * subclasses.
	 * 
	 * @param clazz the Class object corresponding to the test class to be
	 * managed
	 */
	protected TestContextManager createTestContextManager(Class<?> clazz) {
		return new TestContextManager(clazz);
	}

	/**
	 * Get the {@link TestContextManager} associated with this runner.
	 */
	protected final TestContextManager getTestContextManager() {
		return this.testContextManager;
	}

	/**
	 * Invokes the supplied {@link Method test method} and notifies the supplied
	 * {@link RunNotifier} of the appropriate events.
	 * 
	 * @see #createTest()
	 * @see JUnit4ClassRunner#invokeTestMethod(Method,RunNotifier)
	 */
	@Override
	protected void invokeTestMethod(Method method, RunNotifier notifier) {
		if (logger.isDebugEnabled()) {
			logger.debug("Invoking test method [" + method.toGenericString() + "]");
		}

		// The following is a 1-to-1 copy of the original JUnit 4.4 code, except
		// that we use custom implementations for TestMethod and MethodRoadie.

		Description description = methodDescription(method);
		Object testInstance;
		try {
			testInstance = createTest();
		}
		catch (InvocationTargetException ex) {
			notifier.fireTestFailure(new Failure(description, ex.getCause()));
			return;
		}
		catch (Exception ex) {
			notifier.fireTestFailure(new Failure(description, ex));
			return;
		}

		SpringTestMethod testMethod = new SpringTestMethod(method, getTestClass());
		new SpringMethodRoadie(getTestContextManager(), testInstance, testMethod, notifier, description).run();
	}

}
