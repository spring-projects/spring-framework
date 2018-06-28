/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.test.context.junit4.rules;

import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.junit4.statements.ProfileValueChecker;
import org.springframework.test.context.junit4.statements.RunAfterTestMethodCallbacks;
import org.springframework.test.context.junit4.statements.RunBeforeTestMethodCallbacks;
import org.springframework.test.context.junit4.statements.RunPrepareTestInstanceCallbacks;
import org.springframework.test.context.junit4.statements.SpringFailOnTimeout;
import org.springframework.test.context.junit4.statements.SpringRepeat;

/**
 * {@code SpringMethodRule} is a custom JUnit 4 {@link MethodRule} that
 * supports instance-level and method-level features of the
 * <em>Spring TestContext Framework</em> in standard JUnit tests by means
 * of the {@link TestContextManager} and associated support classes and
 * annotations.
 *
 * <p>In contrast to the {@link org.springframework.test.context.junit4.SpringJUnit4ClassRunner
 * SpringJUnit4ClassRunner}, Spring's rule-based JUnit support has the advantage
 * that it is independent of any {@link org.junit.runner.Runner Runner} and
 * can therefore be combined with existing alternative runners like JUnit's
 * {@code Parameterized} or third-party runners such as the {@code MockitoJUnitRunner}.
 *
 * <p>In order to achieve the same functionality as the {@code SpringJUnit4ClassRunner},
 * however, a {@code SpringMethodRule} must be combined with a {@link SpringClassRule},
 * since {@code SpringMethodRule} only supports the instance-level and method-level
 * features of the {@code SpringJUnit4ClassRunner}.
 *
 * <h3>Example Usage</h3>
 * <pre><code> public class ExampleSpringIntegrationTest {
 *
 *    &#064;ClassRule
 *    public static final SpringClassRule springClassRule = new SpringClassRule();
 *
 *    &#064;Rule
 *    public final SpringMethodRule springMethodRule = new SpringMethodRule();
 *
 *    // ...
 * }</code></pre>
 *
 * <p>The following list constitutes all annotations currently supported directly
 * or indirectly by {@code SpringMethodRule}. <em>(Note that additional annotations
 * may be supported by various
 * {@link org.springframework.test.context.TestExecutionListener TestExecutionListener} or
 * {@link org.springframework.test.context.TestContextBootstrapper TestContextBootstrapper}
 * implementations.)</em>
 *
 * <ul>
 * <li>{@link org.springframework.test.annotation.Timed @Timed}</li>
 * <li>{@link org.springframework.test.annotation.Repeat @Repeat}</li>
 * <li>{@link org.springframework.test.annotation.ProfileValueSourceConfiguration @ProfileValueSourceConfiguration}</li>
 * <li>{@link org.springframework.test.annotation.IfProfileValue @IfProfileValue}</li>
 * </ul>
 *
 * <p><strong>NOTE:</strong> As of Spring Framework 4.3, this class requires JUnit 4.12 or higher.
 *
 * <p><strong>WARNING:</strong> Due to the shortcomings of JUnit rules, the
 * {@code SpringMethodRule} does <strong>not</strong> support the
 * {@code beforeTestExecution()} and {@code afterTestExecution()} callbacks of the
 * {@link org.springframework.test.context.TestExecutionListener TestExecutionListener}
 * API.
 *
 * @author Sam Brannen
 * @author Philippe Marschall
 * @since 4.2
 * @see #apply(Statement, FrameworkMethod, Object)
 * @see SpringClassRule
 * @see org.springframework.test.context.TestContextManager
 * @see org.springframework.test.context.junit4.SpringJUnit4ClassRunner
 */
public class SpringMethodRule implements MethodRule {

	private static final Log logger = LogFactory.getLog(SpringMethodRule.class);


	/**
	 * Apply <em>instance-level</em> and <em>method-level</em> features of
	 * the <em>Spring TestContext Framework</em> to the supplied {@code base}
	 * statement.
	 * <p>Specifically, this method invokes the
	 * {@link TestContextManager#prepareTestInstance prepareTestInstance()},
	 * {@link TestContextManager#beforeTestMethod beforeTestMethod()}, and
	 * {@link TestContextManager#afterTestMethod afterTestMethod()} methods
	 * on the {@code TestContextManager}, potentially with Spring timeouts
	 * and repetitions.
	 * <p>In addition, this method checks whether the test is enabled in
	 * the current execution environment. This prevents methods with a
	 * non-matching {@code @IfProfileValue} annotation from running altogether,
	 * even skipping the execution of {@code prepareTestInstance()} methods
	 * in {@code TestExecutionListeners}.
	 * @param base the base {@code Statement} that this rule should be applied to
	 * @param frameworkMethod the method which is about to be invoked on the test instance
	 * @param testInstance the current test instance
	 * @return a statement that wraps the supplied {@code base} with instance-level
	 * and method-level features of the Spring TestContext Framework
	 * @see #withBeforeTestMethodCallbacks
	 * @see #withAfterTestMethodCallbacks
	 * @see #withPotentialRepeat
	 * @see #withPotentialTimeout
	 * @see #withTestInstancePreparation
	 * @see #withProfileValueCheck
	 */
	@Override
	public Statement apply(Statement base, FrameworkMethod frameworkMethod, Object testInstance) {
		Method testMethod = frameworkMethod.getMethod();
		if (logger.isDebugEnabled()) {
			logger.debug("Applying SpringMethodRule to test method [" + testMethod + "]");
		}
		Class<?> testClass = testInstance.getClass();
		TestContextManager testContextManager = SpringClassRule.getTestContextManager(testClass);

		Statement statement = base;
		statement = withBeforeTestMethodCallbacks(statement, testMethod, testInstance, testContextManager);
		statement = withAfterTestMethodCallbacks(statement, testMethod, testInstance, testContextManager);
		statement = withTestInstancePreparation(statement, testInstance, testContextManager);
		statement = withPotentialRepeat(statement, testMethod, testInstance);
		statement = withPotentialTimeout(statement, testMethod, testInstance);
		statement = withProfileValueCheck(statement, testMethod, testInstance);
		return statement;
	}

	/**
	 * Wrap the supplied {@link Statement} with a {@code RunBeforeTestMethodCallbacks} statement.
	 * @see RunBeforeTestMethodCallbacks
	 */
	private Statement withBeforeTestMethodCallbacks(Statement next, Method testMethod,
			Object testInstance, TestContextManager testContextManager) {

		return new RunBeforeTestMethodCallbacks(
				next, testInstance, testMethod, testContextManager);
	}

	/**
	 * Wrap the supplied {@link Statement} with a {@code RunAfterTestMethodCallbacks} statement.
	 * @see RunAfterTestMethodCallbacks
	 */
	private Statement withAfterTestMethodCallbacks(Statement next, Method testMethod,
			Object testInstance, TestContextManager testContextManager) {

		return new RunAfterTestMethodCallbacks(
				next, testInstance, testMethod, testContextManager);
	}

	/**
	 * Wrap the supplied {@link Statement} with a {@code RunPrepareTestInstanceCallbacks} statement.
	 * @see RunPrepareTestInstanceCallbacks
	 */
	private Statement withTestInstancePreparation(
			Statement next, Object testInstance, TestContextManager testContextManager) {

		return new RunPrepareTestInstanceCallbacks(next, testInstance, testContextManager);
	}

	/**
	 * Wrap the supplied {@link Statement} with a {@code SpringRepeat} statement.
	 * <p>Supports Spring's {@link org.springframework.test.annotation.Repeat @Repeat}
	 * annotation.
	 * @see SpringRepeat
	 */
	private Statement withPotentialRepeat(Statement next, Method testMethod, Object testInstance) {
		return new SpringRepeat(next, testMethod);
	}

	/**
	 * Wrap the supplied {@link Statement} with a {@code SpringFailOnTimeout} statement.
	 * <p>Supports Spring's {@link org.springframework.test.annotation.Timed @Timed}
	 * annotation.
	 * @see SpringFailOnTimeout
	 */
	private Statement withPotentialTimeout(Statement next, Method testMethod, Object testInstance) {
		return new SpringFailOnTimeout(next, testMethod);
	}

	/**
	 * Wrap the supplied {@link Statement} with a {@code ProfileValueChecker} statement.
	 * @see ProfileValueChecker
	 */
	private Statement withProfileValueCheck(Statement next, Method testMethod, Object testInstance) {
		return new ProfileValueChecker(next, testInstance.getClass(), testMethod);
	}

}
