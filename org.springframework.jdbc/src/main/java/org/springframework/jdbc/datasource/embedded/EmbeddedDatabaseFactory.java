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
package org.springframework.jdbc.datasource.embedded;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;

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
 * Returns a {@link EmbeddedDatabase} instance pre-populated with test data.
 * When the database is returned, callers are guaranteed that the database schema and test data will have already been loaded.
 * 
 * Can be configured.
 * Call {@link #setDatabaseName(String)} to change the name of the test database.
 * Call {@link #setDatabaseType(EmbeddedDatabaseType)} to set the test database type.
 * Call {@link #setDatabasePopulator(DatabasePopulator)} to change the algorithm used to populate the database.
 * Call {@link #getDatabase()} to the {@link EmbeddedDatabase} instance.
 */
public class EmbeddedDatabaseFactory {

	private static Log logger = LogFactory.getLog(EmbeddedDatabaseFactory.class);

	private String databaseName;

	private EmbeddedDatabaseConfigurer dataSourceConfigurer;

	private DatabasePopulator databasePopulator;

	private EmbeddedDatabase database;
	
	/**
	 * Creates a new EmbeddedDatabaseFactory that uses the default {@link ResourceDatabasePopulator}.
	 */
	public EmbeddedDatabaseFactory() {
		setDatabaseName("testdb");
		setDatabaseType(EmbeddedDatabaseType.HSQL);
		setDatabasePopulator(new ResourceDatabasePopulator());
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
	public void setDatabaseType(EmbeddedDatabaseType type) {
		Assert.notNull(type, "The TestDatabaseType is required");
		dataSourceConfigurer = EmbeddedDatabaseConfigurerFactory.getConfigurer(type);
	}

	/**
	 * Sets the helper that will be invoked to populate the test database with data.
	 * Defaults a {@link ResourceDatabasePopulator}.
	 * @param populator
	 */
	public void setDatabasePopulator(DatabasePopulator populator) {
		Assert.notNull(populator, "The TestDatabasePopulator is required");
		databasePopulator = populator;
	}
	
	// other public methods

	/**
	 * Factory method that returns the embedded database instance.
	 */
	public EmbeddedDatabase getDatabase() {
		if (database == null) {
			initDatabase();
		}
		return database;
	}
	
	// internal helper methods

	// encapsulates the steps involved in initializing the data source: creating it, and populating it
	private void initDatabase() {
		// create the in-memory database source first
		database = new EmbeddedDataSourceProxy(createDataSource());
		if (logger.isInfoEnabled()) {
			logger.info("Created embedded database '" + databaseName + "'");
		}
		if (databasePopulator != null) {
			// now populate the database
			populateDatabase();
		}
	}

	private DataSource createDataSource() {
		SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
		dataSourceConfigurer.configureConnectionProperties(dataSource, databaseName);
		return dataSource;
	}

	private void populateDatabase() {
		TransactionTemplate template = new TransactionTemplate(new DataSourceTransactionManager(database));
		template.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				databasePopulator.populate(new JdbcTemplate(database));
			}
		});
	}
	
	class EmbeddedDataSourceProxy implements EmbeddedDatabase {
		private DataSource dataSource;

		public EmbeddedDataSourceProxy(DataSource dataSource) {
			this.dataSource = dataSource;
		}

		public Connection getConnection() throws SQLException {
			return dataSource.getConnection();
		}

		public Connection getConnection(String username, String password)
				throws SQLException {
			return dataSource.getConnection(username, password);
		}

		public int getLoginTimeout() throws SQLException {
			return dataSource.getLoginTimeout();
		}

		public PrintWriter getLogWriter() throws SQLException {
			return dataSource.getLogWriter();
		}

		public void setLoginTimeout(int seconds) throws SQLException {
			dataSource.setLoginTimeout(seconds);
		}

		public void setLogWriter(PrintWriter out) throws SQLException {
			dataSource.setLogWriter(out);
		}

		public boolean isWrapperFor(Class<?> iface) throws SQLException {
			return dataSource.isWrapperFor(iface);
		}

		public <T> T unwrap(Class<T> iface) throws SQLException {
			return dataSource.unwrap(iface);
		}
		
		public void shutdown() {
			shutdownDatabase();
		}

	}
	
	private void shutdownDatabase() {
		if (database != null) {
			dataSourceConfigurer.shutdown(database);
			database = null;
		}
	}
	
}