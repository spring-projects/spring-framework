/*
 * Copyright 2002-2014 the original author or authors.
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
 * Factory for creating {@link EmbeddedDatabase} instances.
 *
 * <p>Callers are guaranteed that a returned database has been fully initialized
 * and populated.
 *
 * <p>Can be configured:
 * <ul>
 * <li>Call {@link #setDatabaseName(String)} to change the name of the database.
 * <li>Call {@link #setDatabaseType(EmbeddedDatabaseType)} to set the database
 * type if you wish to use one of the supported types.
 * <li>Call {@link #setDatabaseConfigurer(EmbeddedDatabaseConfigurer)} to
 * configure support for your own embedded database type.
 * <li>Call {@link #setDatabasePopulator(DatabasePopulator)} to change the
 * algorithm used to populate the database.
 * <li>Call {@link #setDataSourceFactory(DataSourceFactory)} to change the type
 * of {@link DataSource} used to connect to the database.
 * </ul>
 *
 * <p>Call {@link #getDatabase()} to get the {@link EmbeddedDatabase} instance.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 */
public class EmbeddedDatabaseFactory {

	/**
	 * Default name for an embedded database: &quot;testdb&quot;.
	 */
	public static final String DEFAULT_DATABASE_NAME = "testdb";

	private static Log logger = LogFactory.getLog(EmbeddedDatabaseFactory.class);

	private String databaseName = DEFAULT_DATABASE_NAME;

	private DataSourceFactory dataSourceFactory = new SimpleDriverDataSourceFactory();

	private EmbeddedDatabaseConfigurer databaseConfigurer;

	private DatabasePopulator databasePopulator;

	private DataSource dataSource;


	/**
	 * Set the name of the database.
	 * <p>Defaults to {@value #DEFAULT_DATABASE_NAME}.
	 * @param databaseName name of the embedded database
	 */
	public void setDatabaseName(String databaseName) {
		Assert.hasText(databaseName, "Database name is required");
		this.databaseName = databaseName;
	}

	/**
	 * Set the factory to use to create the {@link DataSource} instance that
	 * connects to the embedded database.
	 * <p>Defaults to {@link SimpleDriverDataSourceFactory}.
	 */
	public void setDataSourceFactory(DataSourceFactory dataSourceFactory) {
		Assert.notNull(dataSourceFactory, "DataSourceFactory is required");
		this.dataSourceFactory = dataSourceFactory;
	}

	/**
	 * Set the type of embedded database to use.
	 * <p>Call this when you wish to configure one of the pre-supported types.
	 * <p>Defaults to HSQL.
	 * @param type the database type
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
	 * Set the strategy that will be used to initialize or populate the embedded
	 * database.
	 * <p>Defaults to {@code null}.
	 */
	public void setDatabasePopulator(DatabasePopulator populator) {
		this.databasePopulator = populator;
	}

	/**
	 * Factory method that returns the {@link EmbeddedDatabase embedded database}
	 * instance, which is also a {@link DataSource}.
	 */
	public EmbeddedDatabase getDatabase() {
		if (this.dataSource == null) {
			initDatabase();
		}
		return new EmbeddedDataSourceProxy(this.dataSource);
	}


	/**
	 * Hook to initialize the embedded database. Subclasses may call this method
	 * to force initialization.
	 * <p>After calling this method, {@link #getDataSource()} returns the
	 * {@link DataSource} providing connectivity to the database.
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
	 * Hook to shutdown the embedded database. Subclasses may call this method
	 * to force shutdown.
	 * <p>After calling, {@link #getDataSource()} returns {@code null}.
	 * <p>Does nothing if no embedded database has been initialized.
	 */
	protected void shutdownDatabase() {
		if (this.dataSource != null) {
			this.databaseConfigurer.shutdown(this.dataSource, this.databaseName);
			this.dataSource = null;
		}
	}

	/**
	 * Hook that gets the {@link DataSource} that provides the connectivity to the
	 * embedded database.
	 * <p>Returns {@code null} if the {@code DataSource} has not been initialized
	 * or if the database has been shut down. Subclasses may call this method to
	 * access the {@code DataSource} instance directly.
	 */
	protected final DataSource getDataSource() {
		return this.dataSource;
	}


	private class EmbeddedDataSourceProxy implements EmbeddedDatabase {

		private final DataSource dataSource;

		public EmbeddedDataSourceProxy(DataSource dataSource) {
			this.dataSource = dataSource;
		}

		@Override
		public Connection getConnection() throws SQLException {
			return this.dataSource.getConnection();
		}

		@Override
		public Connection getConnection(String username, String password) throws SQLException {
			return this.dataSource.getConnection(username, password);
		}

		@Override
		public PrintWriter getLogWriter() throws SQLException {
			return this.dataSource.getLogWriter();
		}

		@Override
		public void setLogWriter(PrintWriter out) throws SQLException {
			this.dataSource.setLogWriter(out);
		}

		@Override
		public int getLoginTimeout() throws SQLException {
			return this.dataSource.getLoginTimeout();
		}

		@Override
		public void setLoginTimeout(int seconds) throws SQLException {
			this.dataSource.setLoginTimeout(seconds);
		}

		@Override
		public <T> T unwrap(Class<T> iface) throws SQLException {
			return this.dataSource.unwrap(iface);
		}

		@Override
		public boolean isWrapperFor(Class<?> iface) throws SQLException {
			return this.dataSource.isWrapperFor(iface);
		}

		// getParentLogger() is required for JDBC 4.1 compatibility
		@Override
		public Logger getParentLogger() {
			return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
		}

		@Override
		public void shutdown() {
			shutdownDatabase();
		}
	}

}
