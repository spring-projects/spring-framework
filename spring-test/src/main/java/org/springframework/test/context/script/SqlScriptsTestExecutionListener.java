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

package org.springframework.test.context.script;

import java.io.IOException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * <code>TestExecutionListener</code> which provides support for executing SQL scripts before each test method using
 * {@link SqlScripts} annotation.
 *
 * @author Tadaya Tsuyukubo
 * @see SqlScripts
 * @since 3.2
 */
public class SqlScriptsTestExecutionListener extends AbstractTestExecutionListener {

	private static final String SLASH = "/";

	private static final Log logger = LogFactory.getLog(SqlScriptsTestExecutionListener.class);

	@Override
	public void beforeTestMethod(TestContext testContext) throws Exception {

		final Method testMethod = testContext.getTestMethod();
		Assert.notNull(testMethod, "The test method of the supplied TestContext must not be null");

		final SqlScripts sqlScripts = getSqlScripts(testContext);
		if (sqlScripts == null) {
			return; // not annotated
		}

		final DataSource dataSource;
		final String dsQualifier = sqlScripts.dataSource();
		if (StringUtils.hasLength(dsQualifier)) {
			dataSource = testContext.getApplicationContext().getBean(dsQualifier, DataSource.class);
		}
		else {
			dataSource = testContext.getApplicationContext().getBean(DataSource.class);
		}

		final Resource[] scripts = getScriptsAsResources(testContext, sqlScripts.value());

		final Connection connection;
		if (sqlScripts.withinTransaction()) {
			// use spring's transaction management.
			connection = DataSourceUtils.getConnection(dataSource);
		}
		else {
			// simply use connection from datasource.
			// depends on the datasource implementation, this operation may be under transaction management.
			connection = dataSource.getConnection();
		}

		try {
			final ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
			populator.setScripts(scripts);
			populator.populate(connection);
		}
		finally {
			if (sqlScripts.withinTransaction()) {
				DataSourceUtils.releaseConnection(connection, dataSource);
			}
			else {
				JdbcUtils.closeConnection(connection);
			}
		}

	}

	/**
	 * Resolve given sql files as resources.
	 *
	 * Pattern matching also supported.
	 *
	 * @param testContext the current test context
	 * @param scripts sql files
	 * @return array of resolved resources
	 * @throws IOException in case of I/O errors
	 */
	private Resource[] getScriptsAsResources(TestContext testContext, String[] scripts) throws IOException {
		final List<Resource> list = new ArrayList<Resource>();
		final String[] modifiedLocations = modifyLocations(testContext.getTestClass(), scripts);

		for (String script : modifiedLocations) {
			Resource[] resources = testContext.getApplicationContext().getResources(script);
			list.addAll(Arrays.asList(resources));
		}
		return list.toArray(new Resource[list.size()]);
	}

	/**
	 * TODO: currently this method is a copy from {@link org.springframework.test.context.support.AbstractContextLoader#modifyLocations(Class,
	 * String...)}.
	 *
	 * In order to match the script-resource-directory with @ContextConfiguration's semantics, here needs access to the
	 * ContextLoader (and ContextConfigurationAttributes for user specified loader) which is encapsulated in TestContext
	 * class and not visible from TestExecutionListener.
	 *
	 * @param clazz the class with which the locations are associated
	 * @param locations the resource locations to be modified
	 * @return an array of modified application context resource locations
	 */
	protected String[] modifyLocations(Class<?> clazz, String... locations) {

		String[] modifiedLocations = new String[locations.length];
		for (int i = 0; i < locations.length; i++) {
			String path = locations[i];
			if (path.startsWith(SLASH)) {
				modifiedLocations[i] = ResourceUtils.CLASSPATH_URL_PREFIX + path;
			}
			else if (!ResourcePatternUtils.isUrl(path)) {
				modifiedLocations[i] = ResourceUtils.CLASSPATH_URL_PREFIX + SLASH +
						StringUtils.cleanPath(ClassUtils.classPackageAsResourcePath(clazz) + SLASH + path);
			}
			else {
				modifiedLocations[i] = StringUtils.cleanPath(path);
			}
		}
		return modifiedLocations;
	}

	/**
	 * Resolve appropriate {@link SqlScripts} annotation.
	 *
	 * @param testContext the current test context
	 * @return null if not annotated.
	 */
	private SqlScripts getSqlScripts(TestContext testContext) {
		final Method testMethod = testContext.getTestMethod();
		Assert.notNull(testMethod, "The test method of the supplied TestContext must not be null");

		final Class<?> testClass = testContext.getTestClass();
		final boolean isClassAnnotated = testClass.isAnnotationPresent(SqlScripts.class);
		final boolean isMethodAnnotated = testMethod.isAnnotationPresent(SqlScripts.class);

		if (isMethodAnnotated) {
			if (isClassAnnotated) {
				logger.debug("Using Method-level annotation exists. Overriding Class-level annotation.");
			}
			else {
				logger.debug("Using Method-level annotation");
			}
			return testMethod.getAnnotation(SqlScripts.class);
		}
		else if (isClassAnnotated) {
			logger.debug("Using Class-level annotation");
			return testClass.getAnnotation(SqlScripts.class);
		}

		return null;
	}

}
