/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context.junit4;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.springframework.test.annotation.ProfileValueUtils;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.junit4.statements.RunAfterTestClassCallbacks;
import org.springframework.test.context.junit4.statements.RunBeforeTestClassCallbacks;

/**
 * This runner executes each test method many times, with before/after class methods
 * executed once around.
 * <p>
 * Borrowed from JUnit's {@code Parameterized} runner, and adapted to be able to run the
 * test with a spring-aware runner. In particular, the TestContextManager is created once
 * by this class, in order to be able to invoke beforeTestClass and afterTestClass methods
 * once for every set of parameters.
 * <p>
 * Invoked internally by the {@link SpringJUnit4ClassRunner}.
 * 
 * @since 3.2
 * @author Gaetan Pitteloud
 */
class SpringJUnit4ParameterizedClassRunner extends Suite {

	private final ArrayList<Runner> runners = new ArrayList<Runner>();

	TestContextManager testContextManager;

	/**
	 * Copy-paste from Parameterized inner class, adapted for spring test-context
	 * framework.
	 */
	class TestClassRunnerForParameters extends InternalSpringJUnit4ClassRunner {

		private final Object[] fParameters;

		private final int fParameterSetNumber;

		TestClassRunnerForParameters(Class<?> testClass, Object[] parameters, int i)
				throws InitializationError {
			super(testClass);
			fParameters = parameters;
			fParameterSetNumber = i;
		}

		@Override
		protected TestContextManager createTestContextManager(Class<?> testClass) {
			// already created by wrapping class
			return SpringJUnit4ParameterizedClassRunner.this.testContextManager;
		}

		@Override
		protected Object createTest() throws Exception {
			Object testInstance = getTestClass().getOnlyConstructor().newInstance(fParameters);
			getTestContextManager().prepareTestInstance(testInstance);
			return testInstance;
		}

		@Override
		protected String getName() {
			return String.format("[%s]", fParameterSetNumber);
		}

		@Override
		protected String testName(final FrameworkMethod method) {
			return String.format("%s[%s]", method.getName(), fParameterSetNumber);
		}

		@Override
		protected void validateConstructor(List<Throwable> errors) {
			validateOnlyOneConstructor(errors);
		}

		@Override
		protected Statement classBlock(RunNotifier notifier) {
			return childrenInvoker(notifier);
		}

	}

	public SpringJUnit4ParameterizedClassRunner(Class<?> klass, Method parametersMethod)
			throws InitializationError {
		super(klass, Collections.<Runner> emptyList());
		int paramMethodModifiers = parametersMethod.getModifiers();
		if (!Modifier.isStatic(paramMethodModifiers) || !Modifier.isPublic(paramMethodModifiers)) {
			throw new InitializationError(String.format(
					"%s.%s() must be public and static", getTestClass().getName(),
					parametersMethod.getName()));
		}

		// do not collect parameters nor create manager as the test class will be skipped
		if (!ProfileValueUtils.isTestEnabledInThisEnvironment(getTestClass().getJavaClass())) {
			return;
		}

		testContextManager = new TestContextManager(klass);
		Collection<?> parametersList = getParametersList(parametersMethod);
		int i = 0;
		for (Object testParameters : parametersList) {
			if (testParameters instanceof Object[]) {
				runners.add(new TestClassRunnerForParameters(
						getTestClass().getJavaClass(), (Object[]) testParameters, i++));
			} else {
				throw new InitializationError(String.format(
						"%s.%s() must return a Collection of arrays.",
						getTestClass().getName(), parametersMethod.getName()));
			}
		}

	}

	@Override
	public void run(RunNotifier notifier) {
		if (testContextManager == null) {
			notifier.fireTestIgnored(getDescription());
		} else {
			super.run(notifier);
		}
	}

	/**
	 * Wraps the {@link Statement} returned by the parent implementation with a
	 * {@link RunBeforeTestClassCallbacks} statement, thus preserving the default
	 * functionality but adding support for the Spring TestContext Framework.
	 * 
	 * @see RunBeforeTestClassCallbacks
	 */
	@Override
	protected Statement withBeforeClasses(Statement statement) {
		Statement junitBeforeClasses = super.withBeforeClasses(statement);
		return new RunBeforeTestClassCallbacks(junitBeforeClasses, testContextManager);
	}

	/**
	 * Wraps the {@link Statement} returned by the parent implementation with a
	 * {@link RunAfterTestClassCallbacks} statement, thus preserving the default
	 * functionality but adding support for the Spring TestContext Framework.
	 * 
	 * @see RunAfterTestClassCallbacks
	 */
	@Override
	protected Statement withAfterClasses(Statement statement) {
		Statement junitAfterClasses = super.withAfterClasses(statement);
		return new RunAfterTestClassCallbacks(junitAfterClasses, testContextManager);
	}

	@Override
	protected List<Runner> getChildren() {
		return runners;
	}

	private Collection<?> getParametersList(Method parametersMethod) throws InitializationError {
		try {
			return (Collection<?>) parametersMethod.invoke(null);
		} catch (Exception e) {
			throw new InitializationError(e);
		}
	}

}
