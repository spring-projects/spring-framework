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
package org.springframework.test.jdbc;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

/**
 * Returns a {@link DataSource} that connects to an in-memory test database pre-populated with test data.
 * When the DataSource is returned, callers are guaranteed that the database schema and test data will have already been loaded.
 * 
 * Can be configured.
 * Call {@link #setDatabaseName(String)} to change the name of the test database.
 * Call {@link #setDatabaseType(TestDatabaseType)} to set the test database type.
 * Call {@link #setDatabasePopulator(TestDatabasePopulator)} to change the algorithm used to populate the test database.
 */
public class TestDataSourceFactory {

	private static Log logger = LogFactory.getLog(TestDataSourceFactory.class);

	private String databaseName;

	private TestDataSourceConfigurer dataSourceConfigurer;

	private TestDatabasePopulator databasePopulator;

	private DataSource dataSource;
	
	/**
	 * Creates a new TestDataSourceFactory that uses the default {@link ResourceTestDatabasePopulator}.
	 */
	public TestDataSourceFactory() {
		setDatabaseName("testdb");
		setDatabaseType(TestDatabaseType.HSQL);
		setDatabasePopulator(new ResourceTestDatabasePopulator());
	}
	
	/**
	 * Sets the name of the test database.
	 * Defaults to 'testdb'.
	 * @param name of the test database
	 */
	public void setDatabaseName(String name) {
		Assert.notNull(name, "The testDatabaseName is required");
		databaseName = name;
	}
	
	/**
	 * Sets the type of test database to use.
	 * Defaults to HSQL.
	 * @param type the test database type
	 */
	public void setDatabaseType(TestDatabaseType type) {
		Assert.notNull(type, "The TestDatabaseType is required");
		dataSourceConfigurer = TestDataSourceConfigurerFactory.getConfigurer(type);
	}

	/**
	 * Sets the helper that will be invoked to populate the test database with data.
	 * Defaults a {@link ResourceTestDatabasePopulator}.
	 * @param populator
	 */
	public void setDatabasePopulator(TestDatabasePopulator populator) {
		Assert.notNull(populator, "The TestDatabasePopulator is required");
		databasePopulator = populator;
	}
	
	// other public methods

	/**
	 * The factory method that returns the {@link DataSource} that connects to the test database.
	 */
	public DataSource getDataSource() {
		if (dataSource == null) {
			initDataSource();
		}
		return dataSource;
	}
	
	/**
	 * Destroy the test database.
	 * Does nothing if the database has not been initialized.
	 */
	public void destroyDataSource() {
		if (dataSource != null) {
			dataSourceConfigurer.shutdown(dataSource);
			dataSource = null;
		}
	}
	
	// internal helper methods

	// encapsulates the steps involved in initializing the data source: creating it, and populating it
	private void initDataSource() {
		// create the in-memory database source first
		dataSource = createDataSource();
		if (logger.isInfoEnabled()) {
			logger.info("Created in-memory test database '" + databaseName + "'");
		}
		if (databasePopulator != null) {
			// now populate the database
			populateDatabase();
		}
	}

	protected DataSource createDataSource() {
		SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
		dataSourceConfigurer.configureConnectionProperties(dataSource, databaseName);
		return dataSource;
	}

	private void populateDatabase() {
		TransactionTemplate template = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
		template.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				databasePopulator.populate(new JdbcTemplate(dataSource));
			}
		});
	}
}