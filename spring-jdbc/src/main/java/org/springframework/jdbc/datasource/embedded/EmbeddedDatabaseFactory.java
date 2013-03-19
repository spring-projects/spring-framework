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

package org.springframework.jdbc.datasource.embedded;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.util.Assert;

/**
 * Creates a {@link EmbeddedDatabase} instance. Callers are guaranteed that the returned database has been fully
 * initialized and populated.
 *
 * <p>Can be configured:<br>
 * Call {@link #setDatabaseName(String)} to change the name of the database.<br>
 * Call {@link #setDatabaseType(EmbeddedDatabaseType)} to set the database type if you wish to use one of the supported types.<br>
 * Call {@link #setDatabaseConfigurer(EmbeddedDatabaseConfigurer)} to configure support for your own embedded database type.<br>
 * Call {@link #setDatabasePopulator(DatabasePopulator)} to change the algorithm used to populate the database.<br>
 * Call {@link #setDataSourceFactory(DataSourceFactory)} to change the type of DataSource used to connect to the database.<br>
 * Call {@link #getDatabase()} to get the {@link EmbeddedDatabase} instance.<br>
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 */
public class EmbeddedDatabaseFactory {

	private static Log logger = LogFactory.getLog(EmbeddedDatabaseFactory.class);

	private String databaseName = "testdb";

	private DataSourceFactory dataSourceFactory = new SimpleDriverDataSourceFactory();

	private EmbeddedDatabaseConfigurer databaseConfigurer;

	private DatabasePopulator databasePopulator;

	private DataSource dataSource;


	/**
	 * Set the name of the database. Defaults to "testdb".
	 * @param databaseName name of the test database
	 */
	public void setDatabaseName(String databaseName) {
		Assert.notNull(databaseName, "Database name is required");
		this.databaseName = databaseName;
	}

	/**
	 * Set the factory to use to create the DataSource instance that connects to the embedded database.
	 * <p>Defaults to {@link SimpleDriverDataSourceFactory}.
	 */
	public void setDataSourceFactory(DataSourceFactory dataSourceFactory) {
		Assert.notNull(dataSourceFactory, "DataSourceFactory is required");
		this.dataSourceFactory = dataSourceFactory;
	}

	/**
	 * Set the type of embedded database to use. Call this when you wish to configure
	 * one of the pre-supported types. Defaults to HSQL.
	 * @param type the test database type
	 */
	public void setDatabaseType(EmbeddedDatabaseType type) {
		this.databaseConfigurer = EmbeddedDatabaseConfigurerFactory.getConfigurer(type);
	}

	/**
	 * Set the strategy that will be used to configure the embedded database instance.
	 * <p>Call this when you wish to use an embedded database type not already supported.
	 */
	public void setDatabaseConfigurer(EmbeddedDatabaseConfigurer configurer) {
		this.databaseConfigurer = configurer;
	}

	/**
	 * Set the strategy that will be used to populate the embedded database. Defaults to null.
	 * @see org.springframework.jdbc.datasource.init.DataSourceInitializer#setDatabasePopulator
	 */
	public void setDatabasePopulator(DatabasePopulator populator) {
		this.databasePopulator = populator;
	}

	/**
	 * Factory method that returns the embedded database instance.
	 */
	public EmbeddedDatabase getDatabase() {
		if (this.dataSource == null) {
			initDatabase();
		}
		return new EmbeddedDataSourceProxy(this.dataSource);
	}


	/**
	 * Hook to initialize the embedded database. Subclasses may call to force initialization. After calling this method,
	 * {@link #getDataSource()} returns the DataSource providing connectivity to the db.
	 */
	protected void initDatabase() {
		// Create the embedded database source first
		if (logger.isInfoEnabled()) {
			logger.info("Creating embedded database '" + this.databaseName + "'");
		}
		if (this.databaseConfigurer == null) {
			this.databaseConfigurer = EmbeddedDatabaseConfigurerFactory.getConfigurer(EmbeddedDatabaseType.HSQL);
		}
		this.databaseConfigurer.configureConnectionProperties(
				this.dataSourceFactory.getConnectionProperties(), this.databaseName);
		this.dataSource = this.dataSourceFactory.getDataSource();

		// Now populate the database
		if (this.databasePopulator != null) {
			try {
				DatabasePopulatorUtils.execute(this.databasePopulator, this.dataSource);
			}
			catch (RuntimeException ex) {
				// failed to populate, so leave it as not initialized
				shutdownDatabase();
				throw ex;
			}
		}
	}

	/**
	 * Hook to shutdown the embedded database. Subclasses may call to force shutdown.
	 * After calling, {@link #getDataSource()} returns null. Does nothing if no embedded database has been initialized.
	 */
	protected void shutdownDatabase() {
		if (this.dataSource != null) {
			this.databaseConfigurer.shutdown(this.dataSource, this.databaseName);
			this.dataSource = null;
		}
	}

	/**
	 * Hook that gets the DataSource that provides the connectivity to the embedded database.
	 * <p>Returns {@code null} if the DataSource has not been initialized or the database
	 * has been shut down. Subclasses may call to access the DataSource instance directly.
	 */
	protected final DataSource getDataSource() {
		return this.dataSource;
	}


	private class EmbeddedDataSourceProxy implements EmbeddedDatabase {

		private final DataSource dataSource;

		public EmbeddedDataSourceProxy(DataSource dataSource) {
			this.dataSource = dataSource;
		}

		public Connection getConnection() throws SQLException {
			return this.dataSource.getConnection();
		}

		public Connection getConnection(String username, String password) throws SQLException {
			return this.dataSource.getConnection(username, password);
		}

		public PrintWriter getLogWriter() throws SQLException {
			return this.dataSource.getLogWriter();
		}

		public void setLogWriter(PrintWriter out) throws SQLException {
			this.dataSource.setLogWriter(out);
		}

		public int getLoginTimeout() throws SQLException {
			return this.dataSource.getLoginTimeout();
		}

		public void setLoginTimeout(int seconds) throws SQLException {
			this.dataSource.setLoginTimeout(seconds);
		}

		public <T> T unwrap(Class<T> iface) throws SQLException {
			return this.dataSource.unwrap(iface);
		}

		public boolean isWrapperFor(Class<?> iface) throws SQLException {
			return this.dataSource.isWrapperFor(iface);
		}

		// getParentLogger() is required for JDBC 4.1 compatibility
		public Logger getParentLogger() {
			return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
		}

		public void shutdown() {
			shutdownDatabase();
		}
	}

}
