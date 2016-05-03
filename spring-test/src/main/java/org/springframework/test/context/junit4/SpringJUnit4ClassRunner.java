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

package org.springframework.test.context.junit4;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.internal.runners.model.ReflectiveCallable;
import org.junit.internal.runners.statements.ExpectException;
import org.junit.internal.runners.statements.Fail;
import org.junit.internal.runners.statements.FailOnTimeout;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import org.springframework.test.annotation.ProfileValueUtils;
import org.springframework.test.annotation.TestAnnotationUtils;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.context.junit4.statements.RunAfterTestClassCallbacks;
import org.springframework.test.context.junit4.statements.RunAfterTestMethodCallbacks;
import org.springframework.test.context.junit4.statements.RunBeforeTestClassCallbacks;
import org.springframework.test.context.junit4.statements.RunBeforeTestMethodCallbacks;
import org.springframework.test.context.junit4.statements.SpringFailOnTimeout;
import org.springframework.test.context.junit4.statements.SpringRepeat;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@code SpringJUnit4ClassRunner} is a custom extension of JUnit's
 * {@link BlockJUnit4ClassRunner} which provides functionality of the
 * <em>Spring TestContext Framework</em> to standard JUnit tests by means of the
 * {@link TestContextManager} and associated support classes and annotations.
 *
 * <p>To use this class, simply annotate a JUnit 4 based test class with
 * {@code @RunWith(SpringJUnit4ClassRunner.class)} or {@code @RunWith(SpringRunner.class)}.
 *
 * <p>The following list constitutes all annotations currently supported directly
 * or indirectly by {@code SpringJUnit4ClassRunner}. <em>(Note that additional
 * annotations may be supported by various
 * {@link org.springframework.test.context.TestExecutionListener TestExecutionListener}
 * or {@link org.springframework.test.context.TestContextBootstrapper TestContextBootstrapper}
 * implementations.)</em>
 *
 * <ul>
 * <li>{@link Test#expected() @Test(expected=...)}</li>
 * <li>{@link Test#timeout() @Test(timeout=...)}</li>
 * <li>{@link org.springframework.test.annotation.Timed @Timed}</li>
 * <li>{@link org.springframework.test.annotation.Repeat @Repeat}</li>
 * <li>{@link Ignore @Ignore}</li>
 * <li>{@link org.springframework.test.annotation.ProfileValueSourceConfiguration @ProfileValueSourceConfiguration}</li>
 * <li>{@link org.springframework.test.annotation.IfProfileValue @IfProfileValue}</li>
 * </ul>
 *
 * <p>If you would like to use the Spring TestContext Framework with a runner
 * other than this one, use {@link SpringClassRule} and {@link SpringMethodRule}.
 *
 * <p><strong>NOTE:</strong> As of Spring Framework 4.3, this class requires JUnit 4.12 or higher.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 2.5
 * @see SpringRunner
 * @see TestContextManager
 * @see AbstractJUnit4SpringContextTests
 * @see AbstractTransactionalJUnit4SpringContextTests
 * @see org.springframework.test.context.junit4.rules.SpringClassRule
 * @see org.springframework.test.context.junit4.rules.SpringMethodRule
 */
public class SpringJUnit4ClassRunner extends BlockJUnit4ClassRunner {

	private static final Log logger = LogFactory.getLog(SpringJUnit4ClassRunner.class);

	private static final Method withRulesMethod;

	static {
		if (!ClassUtils.isPresent("org.junit.internal.Throwables", SpringJUnit4ClassRunner.class.getClassLoader())) {
			throw new IllegalStateException("SpringJUnit4ClassRunner requires JUnit 4.12 or higher.");
		}

		withRulesMethod = ReflectionUtils.findMethod(SpringJUnit4ClassRunner.class, "withRules",
				FrameworkMethod.class, Object.class, Statement.class);
		if (withRulesMethod == null) {
			throw new IllegalStateException("SpringJUnit4ClassRunner requires JUnit 4.12 or higher.");
		}
		ReflectionUtils.makeAccessible(withRulesMethod);
	}


	private final TestContextManager testContextManager;


	private static void ensureSpringRulesAreNotPresent(Class<?> testClass) {
		for (Field field : testClass.getFields()) {
			if (SpringClassRule.class.isAssignableFrom(field.getType())) {
				throw new IllegalStateException(String.format("Detected SpringClassRule field in test class [%s], " +
						"but SpringClassRule cannot be used with the SpringJUnit4ClassRunner.", testClass.getName()));
			}
			if (SpringMethodRule.class.isAssignableFrom(field.getType())) {
				throw new IllegalStateException(String.format("Detected SpringMethodRule field in test class [%s], " +
						"but SpringMethodRule cannot be used with the SpringJUnit4ClassRunner.", testClass.getName()));
			}
		}
	}

	/**
	 * Construct a new {@code SpringJUnit4ClassRunner} and initialize a
	 * {@link TestContextManager} to provide Spring testing functionality to
	 * standard JUnit tests.
	 * @param clazz the test class to be run
	 * @see #createTestContextManager(Class)
	 */
	public SpringJUnit4ClassRunner(Class<?> clazz) throws InitializationError {
		super(clazz);
		if (logger.isDebugEnabled()) {
			logger.debug("SpringJUnit4ClassRunner constructor called with [" + clazz + "]");
		}
		ensureSpringRulesAreNotPresent(clazz);
		this.testContextManager = createTestContextManager(clazz);
	}

	/**
	 * Create a new {@link TestContextManager} for the supplied test class.
	 * <p>Can be overridden by subclasses.
	 * @param clazz the test class to be managed
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
	 * Return a description suitable for an ignored test class if the test is
	 * disabled via {@code @IfProfileValue} at the class-level, and
	 * otherwise delegate to the parent implementation.
	 * @see ProfileValueUtils#isTestEnabledInThisEnvironment(Class)
	 */
	@Override
	public Description getDescription() {
		if (!ProfileValueUtils.isTestEnabledInThisEnvironment(getTestClass().getJavaClass())) {
			return Description.createSuiteDescription(getTestClass().getJavaClass());
		}
		return super.getDescription();
	}

	/**
	 * Check whether the test is enabled in the current execution environment.
	 * <p>This prevents classes with a non-matching {@code @IfProfileValue}
	 * annotation from running altogether, even skipping the execution of
	 * {@code prepareTestInstance()} methods in {@code TestExecutionListeners}.
	 * @see ProfileValueUtils#isTestEnabledInThisEnvironment(Class)
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
	 * Wrap the {@link Statement} returned by the parent implementation with a
	 * {@code RunBeforeTestClassCallbacks} statement, thus preserving the
	 * default JUnit functionality while adding support for the Spring TestContext
	 * Framework.
	 * @see RunBeforeTestClassCallbacks
	 */
	@Override
	protected Statement withBeforeClasses(Statement statement) {
		Statement junitBeforeClasses = super.withBeforeClasses(statement);
		return new RunBeforeTestClassCallbacks(junitBeforeClasses, getTestContextManager());
	}

	/**
	 * Wrap the {@link Statement} returned by the parent implementation with a
	 * {@code RunAfterTestClassCallbacks} statement, thus preserving the default
	 * JUnit functionality while adding support for the Spring TestContext Framework.
	 * @see RunAfterTestClassCallbacks
	 */
	@Override
	protected Statement withAfterClasses(Statement statement) {
		Statement junitAfterClasses = super.withAfterClasses(statement);
		return new RunAfterTestClassCallbacks(junitAfterClasses, getTestContextManager());
	}

	/**
	 * Delegate to the parent implementation for creating the test instance and
	 * then allow the {@link #getTestContextManager() TestContextManager} to
	 * prepare the test instance before returning it.
	 * @see TestContextManager#prepareTestInstance
	 */
	@Override
	protected Object createTest() throws Exception {
		Object testInstance = super.createTest();
		getTestContextManager().prepareTestInstance(testInstance);
		return testInstance;
	}

	/**
	 * Perform the same logic as
	 * {@link BlockJUnit4ClassRunner#runChild(FrameworkMethod, RunNotifier)},
	 * except that tests are determined to be <em>ignored</em> by
	 * {@link #isTestMethodIgnored(FrameworkMethod)}.
	 */
	@Override
	protected void runChild(FrameworkMethod frameworkMethod, RunNotifier notifier) {
		Description description = describeChild(frameworkMethod);
		if (isTestMethodIgnored(frameworkMethod)) {
			notifier.fireTestIgnored(description);
		}
		else {
			Statement statement;
			try {
				statement = methodBlock(frameworkMethod);
			}
			catch (Throwable ex) {
				statement = new Fail(ex);
			}
			runLeaf(statement, description, notifier);
		}
	}

	/**
	 * Augment the default JUnit behavior
	 * {@linkplain #withPotentialRepeat with potential repeats} of the entire
	 * execution chain.
	 * <p>Furthermore, support for timeouts has been moved down the execution
	 * chain in order to include execution of {@link org.junit.Before @Before}
	 * and {@link org.junit.After @After} methods within the timed execution.
	 * Note that this differs from the default JUnit behavior of executing
	 * {@code @Before} and {@code @After} methods in the main thread while
	 * executing the actual test method in a separate thread. Thus, the net
	 * effect is that {@code @Before} and {@code @After} methods will be
	 * executed in the same thread as the test method. As a consequence,
	 * JUnit-specified timeouts will work fine in combination with Spring
	 * transactions. However, JUnit-specific timeouts still differ from
	 * Spring-specific timeouts in that the former execute in a separate
	 * thread while the latter simply execute in the main thread (like regular
	 * tests).
	 * @see #possiblyExpectingExceptions(FrameworkMethod, Object, Statement)
	 * @see #withBefores(FrameworkMethod, Object, Statement)
	 * @see #withAfters(FrameworkMethod, Object, Statement)
	 * @see #withRulesReflectively(FrameworkMethod, Object, Statement)
	 * @see #withPotentialRepeat(FrameworkMethod, Object, Statement)
	 * @see #withPotentialTimeout(FrameworkMethod, Object, Statement)
	 */
	@Override
	protected Statement methodBlock(FrameworkMethod frameworkMethod) {
		Object testInstance;
		try {
			testInstance = new ReflectiveCallable() {
				@Override
				protected Object runReflectiveCall() throws Throwable {
					return createTest();
				}
			}.run();
		}
		catch (Throwable ex) {
			return new Fail(ex);
		}

		Statement statement = methodInvoker(frameworkMethod, testInstance);
		statement = possiblyExpectingExceptions(frameworkMethod, testInstance, statement);
		statement = withBefores(frameworkMethod, testInstance, statement);
		statement = withAfters(frameworkMethod, testInstance, statement);
		statement = withRulesReflectively(frameworkMethod, testInstance, statement);
		statement = withPotentialRepeat(frameworkMethod, testInstance, statement);
		statement = withPotentialTimeout(frameworkMethod, testInstance, statement);
		return statement;
	}

	/**
	 * Invoke JUnit's private {@code withRules()} method using reflection.
	 */
	private Statement withRulesReflectively(FrameworkMethod frameworkMethod, Object testInstance, Statement statement) {
		return (Statement) ReflectionUtils.invokeMethod(withRulesMethod, this, frameworkMethod, testInstance, statement);
	}

	/**
	 * Return {@code true} if {@link Ignore @Ignore} is present for the supplied
	 * {@linkplain FrameworkMethod test method} or if the test method is disabled
	 * via {@code @IfProfileValue}.
	 * @see ProfileValueUtils#isTestEnabledInThisEnvironment(Method, Class)
	 */
	protected boolean isTestMethodIgnored(FrameworkMethod frameworkMethod) {
		Method method = frameworkMethod.getMethod();
		return (method.isAnnotationPresent(Ignore.class) ||
				!ProfileValueUtils.isTestEnabledInThisEnvironment(method, getTestClass().getJavaClass()));
	}

	/**
	 * Perform the same logic as
	 * {@link BlockJUnit4ClassRunner#possiblyExpectingExceptions(FrameworkMethod, Object, Statement)}
	 * except that the <em>expected exception</em> is retrieved using
	 * {@link #getExpectedException(FrameworkMethod)}.
	 */
	@Override
	protected Statement possiblyExpectingExceptions(FrameworkMethod frameworkMethod, Object testInstance, Statement next) {
		Class<? extends Throwable> expectedException = getExpectedException(frameworkMethod);
		return (expectedException != null ? new ExpectException(next, expectedException) : next);
	}

	/**
	 * Get the {@code exception} that the supplied {@linkplain FrameworkMethod
	 * test method} is expected to throw.
	 * <p>Supports JUnit's {@link Test#expected() @Test(expected=...)} annotation.
	 * <p>Can be overridden by subclasses.
	 * @return the expected exception, or {@code null} if none was specified
	 */
	protected Class<? extends Throwable> getExpectedException(FrameworkMethod frameworkMethod) {
		Test test = frameworkMethod.getAnnotation(Test.class);
		return (test != null && test.expected() != Test.None.class ? test.expected() : null);
	}

	/**
	 * Perform the same logic as
	 * {@link BlockJUnit4ClassRunner#withPotentialTimeout(FrameworkMethod, Object, Statement)}
	 * but with additional support for Spring's {@code @Timed} annotation.
	 * <p>Supports both Spring's {@link org.springframework.test.annotation.Timed @Timed}
	 * and JUnit's {@link Test#timeout() @Test(timeout=...)} annotations, but not both
	 * simultaneously.
	 * @return either a {@link SpringFailOnTimeout}, a {@link FailOnTimeout},
	 * or the supplied {@link Statement} as appropriate
	 * @see #getSpringTimeout(FrameworkMethod)
	 * @see #getJUnitTimeout(FrameworkMethod)
	 */
	@Override
	@SuppressWarnings("deprecation")
	protected Statement withPotentialTimeout(FrameworkMethod frameworkMethod, Object testInstance, Statement next) {
		Statement statement = null;
		long springTimeout = getSpringTimeout(frameworkMethod);
		long junitTimeout = getJUnitTimeout(frameworkMethod);
		if (springTimeout > 0 && junitTimeout > 0) {
			String msg = String.format("Test method [%s] has been configured with Spring's @Timed(millis=%s) and " +
							"JUnit's @Test(timeout=%s) annotations, but only one declaration of a 'timeout' is " +
							"permitted per test method.", frameworkMethod.getMethod(), springTimeout, junitTimeout);
			logger.error(msg);
			throw new IllegalStateException(msg);
		}
		else if (springTimeout > 0) {
			statement = new SpringFailOnTimeout(next, springTimeout);
		}
		else if (junitTimeout > 0) {
			statement = FailOnTimeout.builder().withTimeout(junitTimeout, TimeUnit.MILLISECONDS).build(next);
		}
		else {
			statement = next;
		}

		return statement;
	}

	/**
	 * Retrieve the configured JUnit {@code timeout} from the {@link Test @Test}
	 * annotation on the supplied {@linkplain FrameworkMethod test method}.
	 * @return the timeout, or {@code 0} if none was specified
	 */
	protected long getJUnitTimeout(FrameworkMethod frameworkMethod) {
		Test test = frameworkMethod.getAnnotation(Test.class);
		return (test != null && test.timeout() > 0 ? test.timeout() : 0);
	}

	/**
	 * Retrieve the configured Spring-specific {@code timeout} from the
	 * {@link org.springframework.test.annotation.Timed @Timed} annotation
	 * on the supplied {@linkplain FrameworkMethod test method}.
	 * @return the timeout, or {@code 0} if none was specified
	 * @see TestAnnotationUtils#getTimeout(Method)
	 */
	protected long getSpringTimeout(FrameworkMethod frameworkMethod) {
		return TestAnnotationUtils.getTimeout(frameworkMethod.getMethod());
	}

	/**
	 * Wrap the {@link Statement} returned by the parent implementation with a
	 * {@code RunBeforeTestMethodCallbacks} statement, thus preserving the
	 * default functionality while adding support for the Spring TestContext
	 * Framework.
	 * @see RunBeforeTestMethodCallbacks
	 */
	@Override
	protected Statement withBefores(FrameworkMethod frameworkMethod, Object testInstance, Statement statement) {
		Statement junitBefores = super.withBefores(frameworkMethod, testInstance, statement);
		return new RunBeforeTestMethodCallbacks(junitBefores, testInstance, frameworkMethod.getMethod(),
				getTestContextManager());
	}

	/**
	 * Wrap the {@link Statement} returned by the parent implementation with a
	 * {@code RunAfterTestMethodCallbacks} statement, thus preserving the
	 * default functionality while adding support for the Spring TestContext
	 * Framework.
	 * @see RunAfterTestMethodCallbacks
	 */
	@Override
	protected Statement withAfters(FrameworkMethod frameworkMethod, Object testInstance, Statement statement) {
		Statement junitAfters = super.withAfters(frameworkMethod, testInstance, statement);
		return new RunAfterTestMethodCallbacks(junitAfters, testInstance, frameworkMethod.getMethod(),
				getTestContextManager());
	}

	/**
	 * Wrap the supplied {@link Statement} with a {@code SpringRepeat} statement.
	 * <p>Supports Spring's {@link org.springframework.test.annotation.Repeat @Repeat}
	 * annotation.
	 * @see TestAnnotationUtils#getRepeatCount(Method)
	 * @see SpringRepeat
	 */
	protected Statement withPotentialRepeat(FrameworkMethod frameworkMethod, Object testInstance, Statement next) {
		return new SpringRepeat(next, frameworkMethod.getMethod());
	}

}
