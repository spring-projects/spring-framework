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
import org.springframework.util.Assert;

/**
 * Returns a {@link EmbeddedDatabase} instance pre-populated with test data. When the database is returned, callers are
 * guaranteed that the database schema and test data will have already been loaded.
 * <p>
 * Can be configured:<br>
 * Call {@link #setDatabaseName(String)} to change the name of the database.<br>
 * Call {@link #setDatabaseType(EmbeddedDatabaseType)} to set the database type if you wish to use one of the supported types.<br>
 * Call {@link #setDatabaseConfigurer(EmbeddedDatabaseConfigurer)} to set a configuration strategy for your own embedded database type.<br>
 * Call {@link #setDatabasePopulator(DatabasePopulator)} to change the algorithm used to populate the database.<br>
 * Call {@link #setDataSourceFactory(DataSourceFactory)} to change the type of DataSource used to connect to the database.<br>
 * Call {@link #getDatabase()} to get the {@link EmbeddedDatabase} instance.<br>
 * 
 * @author Keith Donald
 */
public class EmbeddedDatabaseFactory {

	private static Log logger = LogFactory.getLog(EmbeddedDatabaseFactory.class);

	private String databaseName;

	private DataSourceFactory dataSourceFactory;

	private EmbeddedDatabaseConfigurer databaseConfigurer;

	private DatabasePopulator databasePopulator;

	private DataSource dataSource;

	/**
	 * Creates a default {@link EmbeddedDatabaseFactory}. Calling {@link #getDatabase()} will create a embedded HSQL
	 * database of name 'testdb'.
	 */
	public EmbeddedDatabaseFactory() {
		setDatabaseName("testdb");
		setDatabaseType(EmbeddedDatabaseType.HSQL);
		setDataSourceFactory(new SimpleDriverDataSourceFactory());
	}

	/**
	 * Sets the name of the database. Defaults to 'testdb'.
	 * @param name of the test database
	 */
	public void setDatabaseName(String name) {
		Assert.notNull(name, "The testDatabaseName is required");
		databaseName = name;
	}

	/**
	 * Sets the type of embedded database to use. Call this when you wish to configure one of the pre-supported types.
	 * Defaults to HSQL.
	 * @param type the test database type
	 */
	public void setDatabaseType(EmbeddedDatabaseType type) {
		setDatabaseConfigurer(EmbeddedDatabaseConfigurerFactory.getConfigurer(type));
	}

	/**
	 * Sets the strategy that will be used to configure the embedded database instance. Call this when you wish to use
	 * an embedded database type not already supported.
	 * @param configurer the embedded database configurer
	 */
	public void setDatabaseConfigurer(EmbeddedDatabaseConfigurer configurer) {
		this.databaseConfigurer = configurer;
	}

	/**
	 * Sets the strategy that will be used to populate the embedded database. Defaults to null.
	 * @param populator the database populator
	 */
	public void setDatabasePopulator(DatabasePopulator populator) {
		Assert.notNull(populator, "The DatabasePopulator is required");
		databasePopulator = populator;
	}

	/**
	 * Sets the factory to use to create the DataSource instance that connects to the embedded database. Defaults to
	 * {@link SimpleDriverDataSourceFactory}.
	 * @param dataSourceFactory the data source factory
	 */
	public void setDataSourceFactory(DataSourceFactory dataSourceFactory) {
		Assert.notNull(dataSourceFactory, "The DataSourceFactory is required");
		this.dataSourceFactory = dataSourceFactory;
	}

	// other public methods

	/**
	 * Factory method that returns the embedded database instance.
	 */
	public EmbeddedDatabase getDatabase() {
		if (dataSource == null) {
			initDataSource();
		}
		return new EmbeddedDataSourceProxy(dataSource);
	}

	// subclassing hooks

	protected void initDataSource() {
		// create the embedded database source first
		if (logger.isInfoEnabled()) {
			logger.info("Created embedded database '" + databaseName + "'");
		}
		databaseConfigurer.configureConnectionProperties(dataSourceFactory.getConnectionProperties(), databaseName);
		dataSource = dataSourceFactory.getDataSource();
		if (databasePopulator != null) {
			// now populate the database
			populateDatabase();
		}
	}

	protected DataSource getDataSource() {
		return dataSource;
	}

	protected void shutdownDataSource() {
		if (dataSource != null) {
			databaseConfigurer.shutdown(dataSource);
			dataSource = null;
		}
	}

	// internal helper methods

	private void populateDatabase() {
		Connection connection = JdbcUtils.getConnection(dataSource);
		try {
			databasePopulator.populate(connection);
		} catch (SQLException e) {
			throw new RuntimeException("SQLException occurred populating embedded database", e);
		} finally {
			JdbcUtils.closeConnection(connection);
		}
	}

	private class EmbeddedDataSourceProxy implements EmbeddedDatabase {
		private DataSource dataSource;

		public EmbeddedDataSourceProxy(DataSource dataSource) {
			this.dataSource = dataSource;
		}

		public Connection getConnection() throws SQLException {
			return dataSource.getConnection();
		}

		public Connection getConnection(String username, String password) throws SQLException {
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
			shutdownDataSource();
		}

	}

}