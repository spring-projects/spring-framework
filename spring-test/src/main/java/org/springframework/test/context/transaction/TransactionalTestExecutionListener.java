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

package org.springframework.test.context.transaction;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@code TestExecutionListener} that provides support for executing tests
 * within <em>test-managed transactions</em> by honoring Spring's
 * {@link org.springframework.transaction.annotation.Transactional @Transactional}
 * annotation.
 *
 * <h3>Test-managed Transactions</h3>
 * <p><em>Test-managed transactions</em> are transactions that are managed
 * declaratively via this listener or programmatically via
 * {@link TestTransaction}. Such transactions should not be confused with
 * <em>Spring-managed transactions</em> (i.e., those managed directly
 * by Spring within the {@code ApplicationContext} loaded for tests) or
 * <em>application-managed transactions</em> (i.e., those managed
 * programmatically within application code that is invoked via tests).
 * Spring-managed and application-managed transactions will typically
 * participate in test-managed transactions; however, caution should be
 * taken if Spring-managed or application-managed transactions are
 * configured with any propagation type other than
 * {@link org.springframework.transaction.annotation.Propagation#REQUIRED REQUIRED}
 * or {@link org.springframework.transaction.annotation.Propagation#SUPPORTS SUPPORTS}.
 *
 * <h3>Enabling and Disabling Transactions</h3>
 * <p>Annotating a test method with {@code @Transactional} causes the test
 * to be run within a transaction that will, by default, be automatically
 * <em>rolled back</em> after completion of the test. If a test class is
 * annotated with {@code @Transactional}, each test method within that class
 * hierarchy will be run within a transaction. Test methods that are
 * <em>not</em> annotated with {@code @Transactional} (at the class or method
 * level) will not be run within a transaction. Furthermore, tests that
 * <em>are</em> annotated with {@code @Transactional} but have the
 * {@link org.springframework.transaction.annotation.Transactional#propagation propagation}
 * type set to
 * {@link org.springframework.transaction.annotation.Propagation#NOT_SUPPORTED NOT_SUPPORTED}
 * will not be run within a transaction.
 *
 * <h3>Declarative Rollback and Commit Behavior</h3>
 * <p>By default, test transactions will be automatically <em>rolled back</em>
 * after completion of the test; however, transactional commit and rollback
 * behavior can be configured declaratively via the {@link Commit @Commit}
 * and {@link Rollback @Rollback} annotations at the class level and at the
 * method level.
 *
 * <h3>Programmatic Transaction Management</h3>
 * <p>As of Spring Framework 4.1, it is possible to interact with test-managed
 * transactions programmatically via the static methods in {@link TestTransaction}.
 * {@code TestTransaction} may be used within <em>test</em> methods,
 * <em>before</em> methods, and <em>after</em> methods.
 *
 * <h3>Executing Code outside of a Transaction</h3>
 * <p>When executing transactional tests, it is sometimes useful to be able to
 * execute certain <em>set up</em> or <em>tear down</em> code outside of a
 * transaction. {@code TransactionalTestExecutionListener} provides such
 * support for methods annotated with {@link BeforeTransaction @BeforeTransaction}
 * or {@link AfterTransaction @AfterTransaction}. As of Spring Framework 4.3,
 * {@code @BeforeTransaction} and {@code @AfterTransaction} may also be declared
 * on Java 8 based interface default methods.
 *
 * <h3>Configuring a Transaction Manager</h3>
 * <p>{@code TransactionalTestExecutionListener} expects a
 * {@link PlatformTransactionManager} bean to be defined in the Spring
 * {@code ApplicationContext} for the test. In case there are multiple
 * instances of {@code PlatformTransactionManager} within the test's
 * {@code ApplicationContext}, a <em>qualifier</em> may be declared via
 * {@link org.springframework.transaction.annotation.Transactional @Transactional}
 * (e.g., {@code @Transactional("myTxMgr")} or {@code @Transactional(transactionManger = "myTxMgr")},
 * or {@link org.springframework.transaction.annotation.TransactionManagementConfigurer
 * TransactionManagementConfigurer} can be implemented by an
 * {@link org.springframework.context.annotation.Configuration @Configuration}
 * class. See {@link TestContextTransactionUtils#retrieveTransactionManager}
 * for details on the algorithm used to look up a transaction manager in
 * the test's {@code ApplicationContext}.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 2.5
 * @see org.springframework.transaction.annotation.TransactionManagementConfigurer
 * @see org.springframework.transaction.annotation.Transactional
 * @see org.springframework.test.annotation.Commit
 * @see org.springframework.test.annotation.Rollback
 * @see BeforeTransaction
 * @see AfterTransaction
 * @see TestTransaction
 */
public class TransactionalTestExecutionListener extends AbstractTestExecutionListener {

	private static final Log logger = LogFactory.getLog(TransactionalTestExecutionListener.class);

	@SuppressWarnings("deprecation")
	private static final TransactionConfigurationAttributes defaultTxConfigAttributes = new TransactionConfigurationAttributes();

	// Do not require @Transactional test methods to be public.
	protected final TransactionAttributeSource attributeSource = new AnnotationTransactionAttributeSource(false);

	@SuppressWarnings("deprecation")
	private TransactionConfigurationAttributes configurationAttributes;


	/**
	 * Returns {@code 4000}.
	 */
	@Override
	public final int getOrder() {
		return 4000;
	}

	/**
	 * If the test method of the supplied {@linkplain TestContext test context}
	 * is configured to run within a transaction, this method will run
	 * {@link BeforeTransaction @BeforeTransaction} methods and start a new
	 * transaction.
	 * <p>Note that if a {@code @BeforeTransaction} method fails, any remaining
	 * {@code @BeforeTransaction} methods will not be invoked, and a transaction
	 * will not be started.
	 * @see org.springframework.transaction.annotation.Transactional
	 * @see #getTransactionManager(TestContext, String)
	 */
	@Override
	public void beforeTestMethod(final TestContext testContext) throws Exception {
		final Method testMethod = testContext.getTestMethod();
		final Class<?> testClass = testContext.getTestClass();
		Assert.notNull(testMethod, "The test method of the supplied TestContext must not be null");

		TransactionContext txContext = TransactionContextHolder.removeCurrentTransactionContext();
		if (txContext != null) {
			throw new IllegalStateException("Cannot start a new transaction without ending the existing transaction.");
		}

		PlatformTransactionManager tm = null;
		TransactionAttribute transactionAttribute = this.attributeSource.getTransactionAttribute(testMethod, testClass);

		if (transactionAttribute != null) {
			transactionAttribute = TestContextTransactionUtils.createDelegatingTransactionAttribute(testContext,
				transactionAttribute);

			if (logger.isDebugEnabled()) {
				logger.debug("Explicit transaction definition [" + transactionAttribute + "] found for test context " +
						testContext);
			}

			if (transactionAttribute.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NOT_SUPPORTED) {
				return;
			}

			tm = getTransactionManager(testContext, transactionAttribute.getQualifier());

			if (tm == null) {
				throw new IllegalStateException(String.format(
						"Failed to retrieve PlatformTransactionManager for @Transactional test for test context %s.",
						testContext));
			}
		}

		if (tm != null) {
			txContext = new TransactionContext(testContext, tm, transactionAttribute, isRollback(testContext));
			runBeforeTransactionMethods(testContext);
			txContext.startTransaction();
			TransactionContextHolder.setCurrentTransactionContext(txContext);
		}
	}

	/**
	 * If a transaction is currently active for the supplied
	 * {@linkplain TestContext test context}, this method will end the transaction
	 * and run {@link AfterTransaction @AfterTransaction} methods.
	 * <p>{@code @AfterTransaction} methods are guaranteed to be invoked even if
	 * an error occurs while ending the transaction.
	 */
	@Override
	public void afterTestMethod(TestContext testContext) throws Exception {
		Method testMethod = testContext.getTestMethod();
		Assert.notNull(testMethod, "The test method of the supplied TestContext must not be null");

		TransactionContext txContext = TransactionContextHolder.removeCurrentTransactionContext();
		// If there was (or perhaps still is) a transaction...
		if (txContext != null) {
			TransactionStatus transactionStatus = txContext.getTransactionStatus();
			try {
				// If the transaction is still active...
				if ((transactionStatus != null) && !transactionStatus.isCompleted()) {
					txContext.endTransaction();
				}
			}
			finally {
				runAfterTransactionMethods(testContext);
			}
		}
	}

	/**
	 * Run all {@link BeforeTransaction @BeforeTransaction} methods for the
	 * specified {@linkplain TestContext test context}. If one of the methods
	 * fails, however, the caught exception will be rethrown in a wrapped
	 * {@link RuntimeException}, and the remaining methods will <strong>not</strong>
	 * be given a chance to execute.
	 * @param testContext the current test context
	 */
	protected void runBeforeTransactionMethods(TestContext testContext) throws Exception {
		try {
			List<Method> methods = getAnnotatedMethods(testContext.getTestClass(), BeforeTransaction.class);
			Collections.reverse(methods);
			for (Method method : methods) {
				if (logger.isDebugEnabled()) {
					logger.debug("Executing @BeforeTransaction method [" + method + "] for test context " + testContext);
				}
				ReflectionUtils.makeAccessible(method);
				method.invoke(testContext.getTestInstance());
			}
		}
		catch (InvocationTargetException ex) {
			if (logger.isErrorEnabled()) {
				logger.error("Exception encountered while executing @BeforeTransaction methods for test context " +
						testContext + ".", ex.getTargetException());
			}
			ReflectionUtils.rethrowException(ex.getTargetException());
		}
	}

	/**
	 * Run all {@link AfterTransaction @AfterTransaction} methods for the
	 * specified {@linkplain TestContext test context}. If one of the methods
	 * fails, the caught exception will be logged as an error, and the remaining
	 * methods will be given a chance to execute. After all methods have
	 * executed, the first caught exception, if any, will be rethrown.
	 * @param testContext the current test context
	 */
	protected void runAfterTransactionMethods(TestContext testContext) throws Exception {
		Throwable afterTransactionException = null;

		List<Method> methods = getAnnotatedMethods(testContext.getTestClass(), AfterTransaction.class);
		for (Method method : methods) {
			try {
				if (logger.isDebugEnabled()) {
					logger.debug("Executing @AfterTransaction method [" + method + "] for test context " + testContext);
				}
				ReflectionUtils.makeAccessible(method);
				method.invoke(testContext.getTestInstance());
			}
			catch (InvocationTargetException ex) {
				Throwable targetException = ex.getTargetException();
				if (afterTransactionException == null) {
					afterTransactionException = targetException;
				}
				logger.error("Exception encountered while executing @AfterTransaction method [" + method +
						"] for test context " + testContext, targetException);
			}
			catch (Exception ex) {
				if (afterTransactionException == null) {
					afterTransactionException = ex;
				}
				logger.error("Exception encountered while executing @AfterTransaction method [" + method +
						"] for test context " + testContext, ex);
			}
		}

		if (afterTransactionException != null) {
			ReflectionUtils.rethrowException(afterTransactionException);
		}
	}

	/**
	 * Get the {@linkplain PlatformTransactionManager transaction manager} to use
	 * for the supplied {@linkplain TestContext test context} and {@code qualifier}.
	 * <p>Delegates to {@link #getTransactionManager(TestContext)} if the
	 * supplied {@code qualifier} is {@code null} or empty.
	 * @param testContext the test context for which the transaction manager
	 * should be retrieved
	 * @param qualifier the qualifier for selecting between multiple bean matches;
	 * may be {@code null} or empty
	 * @return the transaction manager to use, or {@code null} if not found
	 * @throws BeansException if an error occurs while retrieving the transaction manager
	 * @see #getTransactionManager(TestContext)
	 */
	protected PlatformTransactionManager getTransactionManager(TestContext testContext, String qualifier) {
		// Look up by type and qualifier from @Transactional
		if (StringUtils.hasText(qualifier)) {
			try {
				// Use autowire-capable factory in order to support extended qualifier matching
				// (only exposed on the internal BeanFactory, not on the ApplicationContext).
				BeanFactory bf = testContext.getApplicationContext().getAutowireCapableBeanFactory();

				return BeanFactoryAnnotationUtils.qualifiedBeanOfType(bf, PlatformTransactionManager.class, qualifier);
			}
			catch (RuntimeException ex) {
				if (logger.isWarnEnabled()) {
					logger.warn(String.format(
							"Caught exception while retrieving transaction manager with qualifier '%s' for test context %s",
							qualifier, testContext), ex);
				}
				throw ex;
			}
		}

		// else
		return getTransactionManager(testContext);
	}

	/**
	 * Get the {@linkplain PlatformTransactionManager transaction manager}
	 * to use for the supplied {@linkplain TestContext test context}.
	 * <p>The default implementation simply delegates to
	 * {@link TestContextTransactionUtils#retrieveTransactionManager}.
	 * @param testContext the test context for which the transaction manager
	 * should be retrieved
	 * @return the transaction manager to use, or {@code null} if not found
	 * @throws BeansException if an error occurs while retrieving an explicitly
	 * named transaction manager
	 * @throws IllegalStateException if more than one TransactionManagementConfigurer
	 * exists in the ApplicationContext
	 * @see #getTransactionManager(TestContext, String)
	 */
	protected PlatformTransactionManager getTransactionManager(TestContext testContext) {
		@SuppressWarnings("deprecation")
		String tmName = retrieveConfigurationAttributes(testContext).getTransactionManagerName();
		return TestContextTransactionUtils.retrieveTransactionManager(testContext, tmName);
	}

	/**
	 * Determine whether or not to rollback transactions by default for the
	 * supplied {@linkplain TestContext test context}.
	 * <p>Supports {@link Rollback @Rollback}, {@link Commit @Commit}, or
	 * {@link TransactionConfiguration @TransactionConfiguration} at the
	 * class-level.
	 * @param testContext the test context for which the default rollback flag
	 * should be retrieved
	 * @return the <em>default rollback</em> flag for the supplied test context
	 * @throws Exception if an error occurs while determining the default rollback flag
	 */
	@SuppressWarnings("deprecation")
	protected final boolean isDefaultRollback(TestContext testContext) throws Exception {
		Class<?> testClass = testContext.getTestClass();
		Rollback rollback = AnnotatedElementUtils.findMergedAnnotation(testClass, Rollback.class);
		boolean rollbackPresent = (rollback != null);
		TransactionConfigurationAttributes txConfigAttributes = retrieveConfigurationAttributes(testContext);

		if (rollbackPresent && txConfigAttributes != defaultTxConfigAttributes) {
			throw new IllegalStateException(String.format("Test class [%s] is annotated with both @Rollback "
					+ "and @TransactionConfiguration, but only one is permitted.", testClass.getName()));
		}

		if (rollbackPresent) {
			boolean defaultRollback = rollback.value();
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Retrieved default @Rollback(%s) for test class [%s].", defaultRollback,
					testClass.getName()));
			}
			return defaultRollback;
		}

		// else
		return txConfigAttributes.isDefaultRollback();
	}

	/**
	 * Determine whether or not to rollback transactions for the supplied
	 * {@linkplain TestContext test context} by taking into consideration the
	 * {@linkplain #isDefaultRollback(TestContext) default rollback} flag and a
	 * possible method-level override via the {@link Rollback @Rollback}
	 * annotation.
	 * @param testContext the test context for which the rollback flag
	 * should be retrieved
	 * @return the <em>rollback</em> flag for the supplied test context
	 * @throws Exception if an error occurs while determining the rollback flag
	 */
	protected final boolean isRollback(TestContext testContext) throws Exception {
		boolean rollback = isDefaultRollback(testContext);
		Rollback rollbackAnnotation =
				AnnotatedElementUtils.findMergedAnnotation(testContext.getTestMethod(), Rollback.class);
		if (rollbackAnnotation != null) {
			boolean rollbackOverride = rollbackAnnotation.value();
			if (logger.isDebugEnabled()) {
				logger.debug(String.format(
						"Method-level @Rollback(%s) overrides default rollback [%s] for test context %s.",
						rollbackOverride, rollback, testContext));
			}
			rollback = rollbackOverride;
		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug(String.format(
						"No method-level @Rollback override: using default rollback [%s] for test context %s.",
						rollback, testContext));
			}
		}
		return rollback;
	}

	/**
	 * Get all methods in the supplied {@link Class class} and its superclasses
	 * which are annotated with the supplied {@code annotationType} but
	 * which are not <em>shadowed</em> by methods overridden in subclasses.
	 * <p>Default methods on interfaces are also detected.
	 * @param clazz the class for which to retrieve the annotated methods
	 * @param annotationType the annotation type for which to search
	 * @return all annotated methods in the supplied class and its superclasses
	 * as well as annotated interface default methods
	 */
	private List<Method> getAnnotatedMethods(Class<?> clazz, Class<? extends Annotation> annotationType) {
		return Arrays.stream(ReflectionUtils.getUniqueDeclaredMethods(clazz))
				.filter(method -> AnnotatedElementUtils.hasAnnotation(method, annotationType))
				.collect(Collectors.toList());
	}

	/**
	 * Retrieve the {@link TransactionConfigurationAttributes} for the
	 * supplied {@link TestContext} whose {@linkplain Class test class}
	 * may optionally declare or inherit
	 * {@link TransactionConfiguration @TransactionConfiguration}.
	 * <p>If {@code @TransactionConfiguration} is not present for the
	 * supplied {@code TestContext}, a default instance of
	 * {@code TransactionConfigurationAttributes} will be used instead.
	 * @param testContext the test context for which the configuration
	 * attributes should be retrieved
	 * @return the TransactionConfigurationAttributes instance for this listener,
	 * potentially cached
	 * @see TransactionConfigurationAttributes#TransactionConfigurationAttributes()
	 */
	@SuppressWarnings("deprecation")
	TransactionConfigurationAttributes retrieveConfigurationAttributes(TestContext testContext) {
		if (this.configurationAttributes == null) {
			Class<?> clazz = testContext.getTestClass();

			TransactionConfiguration txConfig =
					AnnotatedElementUtils.findMergedAnnotation(clazz, TransactionConfiguration.class);
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Retrieved @TransactionConfiguration [%s] for test class [%s].",
						txConfig, clazz.getName()));
			}

			TransactionConfigurationAttributes configAttributes = (txConfig == null ? defaultTxConfigAttributes :
					new TransactionConfigurationAttributes(txConfig.transactionManager(), txConfig.defaultRollback()));
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Using TransactionConfigurationAttributes %s for test class [%s].",
						configAttributes, clazz.getName()));
			}
			this.configurationAttributes = configAttributes;
		}
		return this.configurationAttributes;
	}

}
