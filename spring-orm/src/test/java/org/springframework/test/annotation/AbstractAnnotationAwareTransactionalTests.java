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

package org.springframework.test.annotation;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import javax.sql.DataSource;

import junit.framework.AssertionFailedError;

import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.AbstractTransactionalDataSourceSpringContextTests;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.util.Assert;

/**
 * <p>
 * Java 5 specific subclass of
 * {@link AbstractTransactionalDataSourceSpringContextTests}, exposing a
 * {@link SimpleJdbcTemplate} and obeying annotations for transaction control.
 * </p>
 * <p>
 * For example, test methods can be annotated with the regular Spring
 * {@link org.springframework.transaction.annotation.Transactional @Transactional}
 * annotation (e.g., to force execution in a read-only transaction) or with the
 * {@link NotTransactional @NotTransactional} annotation to prevent any
 * transaction being created at all. In addition, individual test methods can be
 * annotated with {@link Rollback @Rollback} to override the
 * {@link #isDefaultRollback() default rollback} settings.
 * </p>
 * <p>
 * The following list constitutes all annotations currently supported by
 * AbstractAnnotationAwareTransactionalTests:
 * </p>
 * <ul>
 * <li>{@link DirtiesContext @DirtiesContext}</li>
 * <li>{@link ProfileValueSourceConfiguration @ProfileValueSourceConfiguration}</li>
 * <li>{@link IfProfileValue @IfProfileValue}</li>
 * <li>{@link ExpectedException @ExpectedException}</li>
 * <li>{@link Timed @Timed}</li>
 * <li>{@link Repeat @Repeat}</li>
 * <li>{@link org.springframework.transaction.annotation.Transactional @Transactional}</li>
 * <li>{@link NotTransactional @NotTransactional}</li>
 * <li>{@link Rollback @Rollback}</li>
 * </ul>
 *
 * @author Rod Johnson
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 2.0
 * @deprecated as of Spring 3.0, in favor of using the listener-based test context framework
 * ({@link org.springframework.test.context.junit38.AbstractJUnit38SpringContextTests})
 */
@Deprecated
public abstract class AbstractAnnotationAwareTransactionalTests extends
		AbstractTransactionalDataSourceSpringContextTests {

	protected SimpleJdbcTemplate simpleJdbcTemplate;

	private final TransactionAttributeSource transactionAttributeSource = new AnnotationTransactionAttributeSource();

	/**
	 * {@link ProfileValueSource} available to subclasses but primarily intended
	 * for use in {@link #isDisabledInThisEnvironment(Method)}.
	 * <p>Set to {@link SystemProfileValueSource} by default for backwards
	 * compatibility; however, the value may be changed in the
	 * {@link #AbstractAnnotationAwareTransactionalTests(String)} constructor.
	 */
	protected ProfileValueSource profileValueSource = SystemProfileValueSource.getInstance();


	/**
	 * Default constructor for AbstractAnnotationAwareTransactionalTests, which
	 * delegates to {@link #AbstractAnnotationAwareTransactionalTests(String)}.
	 */
	public AbstractAnnotationAwareTransactionalTests() {
		this(null);
	}

	/**
	 * Constructs a new AbstractAnnotationAwareTransactionalTests instance with
	 * the specified JUnit {@code name} and retrieves the configured (or
	 * default) {@link ProfileValueSource}.
	 * @param name the name of the current test
	 * @see ProfileValueUtils#retrieveProfileValueSource(Class)
	 */
	public AbstractAnnotationAwareTransactionalTests(String name) {
		super(name);
		this.profileValueSource = ProfileValueUtils.retrieveProfileValueSource(getClass());
	}


	@Override
	public void setDataSource(DataSource dataSource) {
		super.setDataSource(dataSource);
		// JdbcTemplate will be identically configured
		this.simpleJdbcTemplate = new SimpleJdbcTemplate(this.jdbcTemplate);
	}

	/**
	 * Search for a unique {@link ProfileValueSource} in the supplied
	 * {@link ApplicationContext}. If found, the
	 * {@code profileValueSource} for this test will be set to the unique
	 * {@link ProfileValueSource}.
	 * @param applicationContext the ApplicationContext in which to search for
	 * the ProfileValueSource; may not be {@code null}
	 * @deprecated Use {@link ProfileValueSourceConfiguration @ProfileValueSourceConfiguration} instead.
	 */
	@Deprecated
	protected void findUniqueProfileValueSourceFromContext(ApplicationContext applicationContext) {
		Assert.notNull(applicationContext, "Can not search for a ProfileValueSource in a null ApplicationContext.");
		ProfileValueSource uniqueProfileValueSource = null;
		Map<?, ?> beans = applicationContext.getBeansOfType(ProfileValueSource.class);
		if (beans.size() == 1) {
			uniqueProfileValueSource = (ProfileValueSource) beans.values().iterator().next();
		}
		if (uniqueProfileValueSource != null) {
			this.profileValueSource = uniqueProfileValueSource;
		}
	}

	/**
	 * Overridden to populate transaction definition from annotations.
	 */
	@Override
	public void runBare() throws Throwable {
		// getName will return the name of the method being run.
		if (isDisabledInThisEnvironment(getName())) {
			// Let superclass log that we didn't run the test.
			super.runBare();
			return;
		}

		final Method testMethod = getTestMethod();

		if (isDisabledInThisEnvironment(testMethod)) {
			recordDisabled();
			this.logger.info("**** " + getClass().getName() + "." + getName() + " is disabled in this environment: "
					+ "Total disabled tests=" + getDisabledTestCount());
			return;
		}

		TransactionDefinition explicitTransactionDefinition =
				this.transactionAttributeSource.getTransactionAttribute(testMethod, getClass());
		if (explicitTransactionDefinition != null) {
			this.logger.info("Custom transaction definition [" + explicitTransactionDefinition + "] for test method ["
					+ getName() + "].");
			setTransactionDefinition(explicitTransactionDefinition);
		}
		else if (testMethod.isAnnotationPresent(NotTransactional.class)) {
			// Don't have any transaction...
			preventTransaction();
		}

		// Let JUnit handle execution. We're just changing the state of the test class first.
		runTestTimed(new TestExecutionCallback() {
			@Override
			public void run() throws Throwable {
				try {
					AbstractAnnotationAwareTransactionalTests.super.runBare();
				}
				finally {
					// Mark the context to be blown away if the test was
					// annotated to result in setDirty being invoked
					// automatically.
					if (testMethod.isAnnotationPresent(DirtiesContext.class)) {
						AbstractAnnotationAwareTransactionalTests.this.setDirty();
					}
				}
			}
		}, testMethod);
	}

	/**
	 * Determine if the test for the supplied {@code testMethod} should
	 * run in the current environment.
	 * <p>The default implementation is based on
	 * {@link IfProfileValue @IfProfileValue} semantics.
	 * @param testMethod the test method
	 * @return {@code true} if the test is <em>disabled</em> in the current environment
	 * @see ProfileValueUtils#isTestEnabledInThisEnvironment
	 */
	protected boolean isDisabledInThisEnvironment(Method testMethod) {
		return !ProfileValueUtils.isTestEnabledInThisEnvironment(this.profileValueSource, testMethod, getClass());
	}

	/**
	 * Get the current test method.
	 */
	protected Method getTestMethod() {
		assertNotNull("TestCase.getName() cannot be null", getName());
		Method testMethod = null;
		try {
			// Use same algorithm as JUnit itself to retrieve the test method
			// about to be executed (the method name is returned by getName). It
			// has to be public so we can retrieve it.
			testMethod = getClass().getMethod(getName(), (Class[]) null);
		}
		catch (NoSuchMethodException ex) {
			fail("Method '" + getName() + "' not found");
		}
		if (!Modifier.isPublic(testMethod.getModifiers())) {
			fail("Method '" + getName() + "' should be public");
		}
		return testMethod;
	}

	/**
	 * Determine whether or not to rollback transactions for the current test
	 * by taking into consideration the
	 * {@link #isDefaultRollback() default rollback} flag and a possible
	 * method-level override via the {@link Rollback @Rollback} annotation.
	 * @return the <em>rollback</em> flag for the current test
	 */
	@Override
	protected boolean isRollback() {
		boolean rollback = isDefaultRollback();
		Rollback rollbackAnnotation = getTestMethod().getAnnotation(Rollback.class);
		if (rollbackAnnotation != null) {
			boolean rollbackOverride = rollbackAnnotation.value();
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("Method-level @Rollback(" + rollbackOverride + ") overrides default rollback ["
						+ rollback + "] for test [" + getName() + "].");
			}
			rollback = rollbackOverride;
		}
		else {
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("No method-level @Rollback override: using default rollback [" + rollback
						+ "] for test [" + getName() + "].");
			}
		}
		return rollback;
	}

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

	private void runTest(TestExecutionCallback tec, Method testMethod) throws Throwable {
		ExpectedException expectedExceptionAnnotation = testMethod.getAnnotation(ExpectedException.class);
		boolean exceptionIsExpected = (expectedExceptionAnnotation != null && expectedExceptionAnnotation.value() != null);
		Class<? extends Throwable> expectedException = (exceptionIsExpected ? expectedExceptionAnnotation.value() : null);

		Repeat repeat = testMethod.getAnnotation(Repeat.class);
		int runs = ((repeat != null) && (repeat.value() > 1)) ? repeat.value() : 1;

		for (int i = 0; i < runs; i++) {
			try {
				if (runs > 1 && this.logger != null && this.logger.isInfoEnabled()) {
					this.logger.info("Repetition " + (i + 1) + " of test " + testMethod.getName());
				}
				tec.run();
				if (exceptionIsExpected) {
					fail("Expected exception: " + expectedException.getName());
				}
			}
			catch (Throwable t) {
				if (!exceptionIsExpected) {
					throw t;
				}
				if (!expectedException.isAssignableFrom(t.getClass())) {
					// Wrap the unexpected throwable with an explicit message.
					AssertionFailedError assertionError = new AssertionFailedError("Unexpected exception, expected<" +
							expectedException.getName() + "> but was<" + t.getClass().getName() + ">");
					assertionError.initCause(t);
					throw assertionError;
				}
			}
		}
	}


	private static interface TestExecutionCallback {

		void run() throws Throwable;
	}

}
