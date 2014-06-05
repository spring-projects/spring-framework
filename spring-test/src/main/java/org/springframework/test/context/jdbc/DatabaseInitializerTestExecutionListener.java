/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.test.context.jdbc;

import java.lang.reflect.Method;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.jdbc.DatabaseInitializer.ExecutionPhase;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.test.context.transaction.TestContextTransactionUtils;
import org.springframework.test.context.util.TestContextResourceUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ResourceUtils;

/**
 * {@code TestExecutionListener} that provides support for executing SQL scripts
 * configured via the {@link DatabaseInitializer @DatabaseInitializer} annotation.
 *
 * <p>SQL scripts will be executed {@linkplain #beforeTestMethod(TestContext) before}
 * or {@linkplain #afterTestMethod(TestContext) after} execution of the corresponding
 * {@linkplain java.lang.reflect.Method test method}, depending on the configured
 * value of the {@link DatabaseInitializer#requireNewTransaction requireNewTransaction}
 * flag.
 *
 * <h3>Script Resources</h3>
 * <p>For details on default script detection and how explicit script locations
 * are interpreted, see {@link DatabaseInitializer#scripts}.
 *
 * <h3>Required Spring Beans</h3>
 * <p>A {@link DataSource} and {@link PlatformTransactionManager} must be defined
 * as beans in the Spring {@link ApplicationContext} for the corresponding test.
 * Consult the Javadoc for {@link TestContextTransactionUtils#retrieveDataSource}
 * and {@link TestContextTransactionUtils#retrieveTransactionManager} for details
 * on the algorithms used to locate these beans.
 *
 * @author Sam Brannen
 * @since 4.1
 * @see DatabaseInitializer
 * @see DatabaseInitializers
 * @see org.springframework.test.context.transaction.TransactionalTestExecutionListener
 * @see org.springframework.jdbc.datasource.init.ResourceDatabasePopulator
 * @see org.springframework.jdbc.datasource.init.ScriptUtils
 */
public class DatabaseInitializerTestExecutionListener extends AbstractTestExecutionListener {

	private static final Log logger = LogFactory.getLog(DatabaseInitializerTestExecutionListener.class);


	/**
	 * Execute SQL scripts configured via {@link DatabaseInitializer @DatabaseInitializer}
	 * for the supplied {@link TestContext} <em>before</em> the current test method.
	 */
	@Override
	public void beforeTestMethod(TestContext testContext) throws Exception {
		executeDatabaseInitializers(testContext, ExecutionPhase.BEFORE_TEST_METHOD);
	}

	/**
	 * Execute SQL scripts configured via {@link DatabaseInitializer @DatabaseInitializer}
	 * for the supplied {@link TestContext} <em>after</em> the current test method.
	 */
	@Override
	public void afterTestMethod(TestContext testContext) throws Exception {
		executeDatabaseInitializers(testContext, ExecutionPhase.AFTER_TEST_METHOD);
	}

	/**
	 * Execute SQL scripts configured via {@link DatabaseInitializer @DatabaseInitializer}
	 * for the supplied {@link TestContext} and {@link ExecutionPhase}.
	 */
	private void executeDatabaseInitializers(TestContext testContext, ExecutionPhase executionPhase) throws Exception {
		boolean classLevel = false;

		Set<DatabaseInitializer> databaseInitializers = AnnotationUtils.getRepeatableAnnotation(
			testContext.getTestMethod(), DatabaseInitializers.class, DatabaseInitializer.class);
		if (databaseInitializers.isEmpty()) {
			databaseInitializers = AnnotationUtils.getRepeatableAnnotation(testContext.getTestClass(),
				DatabaseInitializers.class, DatabaseInitializer.class);
			if (!databaseInitializers.isEmpty()) {
				classLevel = true;
			}
		}

		for (DatabaseInitializer databaseInitializer : databaseInitializers) {
			executeDatabaseInitializer(databaseInitializer, executionPhase, testContext, classLevel);
		}
	}

	/**
	 * Execute the SQL scripts configured via the supplied
	 * {@link DatabaseInitializer @DatabaseInitializer} for the given
	 * {@link ExecutionPhase} and {@link TestContext}.
	 *
	 * <p>Special care must be taken in order to properly support the
	 * {@link DatabaseInitializer#requireNewTransaction requireNewTransaction}
	 * flag.
	 *
	 * @param databaseInitializer the {@code @DatabaseInitializer} to parse
	 * @param executionPhase the current execution phase
	 * @param testContext the current {@code TestContext}
	 * @param classLevel {@code true} if {@link DatabaseInitializer @DatabaseInitializer}
	 * was declared at the class level
	 */
	@SuppressWarnings("serial")
	private void executeDatabaseInitializer(DatabaseInitializer databaseInitializer, ExecutionPhase executionPhase,
			TestContext testContext, boolean classLevel) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Processing %s for execution phase [%s] and test context %s.",
				databaseInitializer, executionPhase, testContext));
		}

		if (executionPhase != databaseInitializer.executionPhase()) {
			return;
		}

		final ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
		populator.setSqlScriptEncoding(databaseInitializer.encoding());
		populator.setSeparator(databaseInitializer.separator());
		populator.setCommentPrefix(databaseInitializer.commentPrefix());
		populator.setBlockCommentStartDelimiter(databaseInitializer.blockCommentStartDelimiter());
		populator.setBlockCommentEndDelimiter(databaseInitializer.blockCommentEndDelimiter());
		populator.setContinueOnError(databaseInitializer.continueOnError());
		populator.setIgnoreFailedDrops(databaseInitializer.ignoreFailedDrops());

		String[] scripts = getScripts(databaseInitializer, testContext, classLevel);
		scripts = TestContextResourceUtils.convertToClasspathResourcePaths(testContext.getTestClass(), scripts);
		populator.setScripts(TestContextResourceUtils.convertToResources(testContext.getApplicationContext(), scripts));
		if (logger.isDebugEnabled()) {
			logger.debug("Executing SQL scripts: " + ObjectUtils.nullSafeToString(scripts));
		}

		final DataSource dataSource = TestContextTransactionUtils.retrieveDataSource(testContext,
			databaseInitializer.dataSource());
		final PlatformTransactionManager transactionManager = TestContextTransactionUtils.retrieveTransactionManager(
			testContext, databaseInitializer.transactionManager());

		int propagation = databaseInitializer.requireNewTransaction() ? TransactionDefinition.PROPAGATION_REQUIRES_NEW
				: TransactionDefinition.PROPAGATION_REQUIRED;

		TransactionAttribute transactionAttribute = TestContextTransactionUtils.createDelegatingTransactionAttribute(
			testContext, new DefaultTransactionAttribute(propagation));

		new TransactionTemplate(transactionManager, transactionAttribute).execute(new TransactionCallbackWithoutResult() {

			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				populator.execute(dataSource);
			};
		});
	}

	private String[] getScripts(DatabaseInitializer databaseInitializer, TestContext testContext, boolean classLevel) {
		String[] scripts = databaseInitializer.scripts();
		String[] value = databaseInitializer.value();
		boolean scriptsDeclared = !ObjectUtils.isEmpty(scripts);
		boolean valueDeclared = !ObjectUtils.isEmpty(value);

		if (valueDeclared && scriptsDeclared) {
			String elementType = (classLevel ? "class" : "method");
			String elementName = (classLevel ? testContext.getTestClass().getName()
					: testContext.getTestMethod().toString());
			String msg = String.format("Test %s [%s] has been configured with @DatabaseInitializer's 'value' [%s] "
					+ "and 'scripts' [%s] attributes. Only one declaration of SQL script "
					+ "paths is permitted per @DatabaseInitializer annotation.", elementType, elementName,
				ObjectUtils.nullSafeToString(value), ObjectUtils.nullSafeToString(scripts));
			logger.error(msg);
			throw new IllegalStateException(msg);
		}
		if (valueDeclared) {
			scripts = value;
		}
		if (ObjectUtils.isEmpty(scripts)) {
			scripts = new String[] { detectDefaultScript(testContext, classLevel) };
		}
		return scripts;
	}

	/**
	 * Detect a default SQL script by implementing the algorithm defined in
	 * {@link DatabaseInitializer#scripts}.
	 */
	private String detectDefaultScript(TestContext testContext, boolean classLevel) {
		Class<?> clazz = testContext.getTestClass();
		Method method = testContext.getTestMethod();
		String elementType = (classLevel ? "class" : "method");
		String elementName = (classLevel ? clazz.getName() : method.toString());

		String resourcePath = ClassUtils.convertClassNameToResourcePath(clazz.getName());
		if (!classLevel) {
			resourcePath += "." + method.getName();
		}
		resourcePath += ".sql";

		String prefixedResourcePath = ResourceUtils.CLASSPATH_URL_PREFIX + resourcePath;
		ClassPathResource classPathResource = new ClassPathResource(resourcePath);

		if (classPathResource.exists()) {
			if (logger.isInfoEnabled()) {
				logger.info(String.format("Detected default SQL script \"%s\" for test %s [%s]", prefixedResourcePath,
					elementType, elementName));
			}
			return prefixedResourcePath;
		}
		else {
			String msg = String.format("Could not detect default SQL script for test %s [%s]: "
					+ "%s does not exist. Either declare scripts via @DatabaseInitializer or make the "
					+ "default SQL script available.", elementType, elementName, classPathResource);
			logger.error(msg);
			throw new IllegalStateException(msg);
		}
	}

}
