/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context.transaction;

import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.test.context.TestContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.annotation.TransactionManagementConfigurer;
import org.springframework.transaction.interceptor.DelegatingTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Utility methods for working with transactions and data access related beans
 * within the <em>Spring TestContext Framework</em>.
 *
 * <p>Mainly for internal use within the framework.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @author Andreas Ahlenstorf
 * @since 4.1
 */
public abstract class TestContextTransactionUtils {

	/**
	 * Default bean name for a {@link DataSource}: {@code "dataSource"}.
	 */
	public static final String DEFAULT_DATA_SOURCE_NAME = "dataSource";

	/**
	 * Default bean name for a {@link PlatformTransactionManager}:
	 * {@code "transactionManager"}.
	 */
	public static final String DEFAULT_TRANSACTION_MANAGER_NAME = "transactionManager";


	private static final Log logger = LogFactory.getLog(TestContextTransactionUtils.class);


	/**
	 * Retrieve the {@link DataSource} to use for the supplied {@linkplain TestContext
	 * test context}.
	 * <p>The following algorithm is used to retrieve the {@code DataSource} from
	 * the {@link org.springframework.context.ApplicationContext ApplicationContext}
	 * of the supplied test context:
	 * <ol>
	 * <li>Look up the {@code DataSource} by type and name, if the supplied
	 * {@code name} is non-empty, throwing a {@link BeansException} if the named
	 * {@code DataSource} does not exist.
	 * <li>Attempt to look up the single {@code DataSource} by type.
	 * <li>Attempt to look up the <em>primary</em> {@code DataSource} by type.
	 * <li>Attempt to look up the {@code DataSource} by type and the
	 * {@linkplain #DEFAULT_DATA_SOURCE_NAME default data source name}.
	 * </ol>
	 * @param testContext the test context for which the {@code DataSource}
	 * should be retrieved; never {@code null}
	 * @param name the name of the {@code DataSource} to retrieve
	 * (may be {@code null} or <em>empty</em>)
	 * @return the {@code DataSource} to use, or {@code null} if not found
	 * @throws BeansException if an error occurs while retrieving an explicitly
	 * named {@code DataSource}
	 */
	public static @Nullable DataSource retrieveDataSource(TestContext testContext, @Nullable String name) {
		Assert.notNull(testContext, "TestContext must not be null");
		BeanFactory bf = testContext.getApplicationContext().getAutowireCapableBeanFactory();

		try {
			// Look up by type and explicit name
			if (StringUtils.hasText(name)) {
				return bf.getBean(name, DataSource.class);
			}
		}
		catch (BeansException ex) {
			logger.error(String.format("Failed to retrieve DataSource named '%s' for test context %s",
					name, testContext), ex);
			throw ex;
		}

		try {
			if (bf instanceof ListableBeanFactory lbf) {
				// Look up single bean by type
				Map<String, DataSource> dataSources =
						BeanFactoryUtils.beansOfTypeIncludingAncestors(lbf, DataSource.class);
				if (dataSources.size() == 1) {
					return dataSources.values().iterator().next();
				}

				try {
					// look up single bean by type, with support for 'primary' beans
					return bf.getBean(DataSource.class);
				}
				catch (BeansException ex) {
					logBeansException(testContext, ex, PlatformTransactionManager.class);
				}
			}

			// look up by type and default name
			return bf.getBean(DEFAULT_DATA_SOURCE_NAME, DataSource.class);
		}
		catch (BeansException ex) {
			logBeansException(testContext, ex, DataSource.class);
			return null;
		}
	}

	/**
	 * Retrieve the {@linkplain PlatformTransactionManager transaction manager}
	 * to use for the supplied {@linkplain TestContext test context}.
	 * <p>The following algorithm is used to retrieve the transaction manager
	 * from the {@link org.springframework.context.ApplicationContext ApplicationContext}
	 * of the supplied test context:
	 * <ol>
	 * <li>Look up the transaction manager by type and explicit name, if the supplied
	 * {@code name} is non-empty, throwing a {@link BeansException} if the named
	 * transaction manager does not exist.
	 * <li>Attempt to look up the transaction manager via a
	 * {@link TransactionManagementConfigurer}, if present.
	 * <li>Attempt to look up the single transaction manager by type.
	 * <li>Attempt to look up the <em>primary</em> transaction manager by type.
	 * <li>Attempt to look up the transaction manager by type and the
	 * {@linkplain #DEFAULT_TRANSACTION_MANAGER_NAME default transaction manager
	 * name}.
	 * </ol>
	 * @param testContext the test context for which the transaction manager
	 * should be retrieved; never {@code null}
	 * @param name the name of the transaction manager to retrieve
	 * (may be {@code null} or <em>empty</em>)
	 * @return the transaction manager to use, or {@code null} if not found
	 * @throws BeansException if an error occurs while retrieving an explicitly
	 * named transaction manager
	 * @throws IllegalStateException if more than one TransactionManagementConfigurer
	 * exists in the ApplicationContext
	 */
	public static @Nullable PlatformTransactionManager retrieveTransactionManager(TestContext testContext, @Nullable String name) {
		Assert.notNull(testContext, "TestContext must not be null");
		BeanFactory bf = testContext.getApplicationContext().getAutowireCapableBeanFactory();

		try {
			// Look up by type and explicit name
			if (StringUtils.hasText(name)) {
				return bf.getBean(name, PlatformTransactionManager.class);
			}
		}
		catch (BeansException ex) {
			logger.error(String.format("Failed to retrieve transaction manager named '%s' for test context %s",
					name, testContext), ex);
			throw ex;
		}

		try {
			if (bf instanceof ListableBeanFactory lbf) {
				// Look up single TransactionManagementConfigurer
				Map<String, TransactionManagementConfigurer> configurers =
						BeanFactoryUtils.beansOfTypeIncludingAncestors(lbf, TransactionManagementConfigurer.class);
				Assert.state(configurers.size() <= 1,
						"Only one TransactionManagementConfigurer may exist in the ApplicationContext");
				if (configurers.size() == 1) {
					TransactionManager tm = configurers.values().iterator().next().annotationDrivenTransactionManager();
					Assert.state(tm instanceof PlatformTransactionManager, () ->
						"Transaction manager specified via TransactionManagementConfigurer " +
						"is not a PlatformTransactionManager: " + tm);
					return (PlatformTransactionManager) tm;
				}

				// Look up single bean by type
				Map<String, PlatformTransactionManager> txMgrs =
						BeanFactoryUtils.beansOfTypeIncludingAncestors(lbf, PlatformTransactionManager.class);
				if (txMgrs.size() == 1) {
					return txMgrs.values().iterator().next();
				}

				try {
					// Look up single bean by type, with support for 'primary' beans
					return bf.getBean(PlatformTransactionManager.class);
				}
				catch (BeansException ex) {
					logBeansException(testContext, ex, PlatformTransactionManager.class);
				}
			}

			// look up by type and default name
			return bf.getBean(DEFAULT_TRANSACTION_MANAGER_NAME, PlatformTransactionManager.class);
		}
		catch (BeansException ex) {
			logBeansException(testContext, ex, PlatformTransactionManager.class);
			return null;
		}
	}

	private static void logBeansException(TestContext testContext, BeansException ex, Class<?> beanType) {
		if (logger.isTraceEnabled()) {
			logger.trace("Caught exception while retrieving %s for test context %s"
					.formatted(beanType.getSimpleName(), testContext), ex);
		}
	}

	/**
	 * Create a delegating {@link TransactionAttribute} for the supplied target
	 * {@link TransactionAttribute} and {@link TestContext}, using the names of
	 * the test class and test method to build the name of the transaction.
	 * @param testContext the {@code TestContext} upon which to base the name
	 * @param targetAttribute the {@code TransactionAttribute} to delegate to
	 * @return the delegating {@code TransactionAttribute}
	 */
	public static TransactionAttribute createDelegatingTransactionAttribute(
			TestContext testContext, TransactionAttribute targetAttribute) {

		return createDelegatingTransactionAttribute(testContext, targetAttribute, true);
	}

	/**
	 * Create a delegating {@link TransactionAttribute} for the supplied target
	 * {@link TransactionAttribute} and {@link TestContext}, using the names of
	 * the test class and test method (if requested) to build the name of the
	 * transaction.
	 * @param testContext the {@code TestContext} upon which to base the name
	 * @param targetAttribute the {@code TransactionAttribute} to delegate to
	 * @param includeMethodName {@code true} if the test method's name should be
	 * included in the name of the transaction
	 * @return the delegating {@code TransactionAttribute}
	 * @since 6.1
	 */
	public static TransactionAttribute createDelegatingTransactionAttribute(
			TestContext testContext, TransactionAttribute targetAttribute, boolean includeMethodName) {

		Assert.notNull(testContext, "TestContext must not be null");
		Assert.notNull(targetAttribute, "Target TransactionAttribute must not be null");
		return new TestContextTransactionAttribute(targetAttribute, testContext, includeMethodName);
	}


	@SuppressWarnings("serial")
	private static class TestContextTransactionAttribute extends DelegatingTransactionAttribute {

		private final String name;

		public TestContextTransactionAttribute(
				TransactionAttribute targetAttribute, TestContext testContext, boolean includeMethodName) {

			super(targetAttribute);

			String name = testContext.getTestClass().getName();
			if (includeMethodName) {
				name += "." + testContext.getTestMethod().getName();
			}
			this.name = name;
		}

		@Override
		public @Nullable String getName() {
			return this.name;
		}
	}

}
