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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.derby.jdbc.EmbeddedDriver;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.util.ClassUtils;

/**
 * {@link EmbeddedDatabaseConfigurer} for Apache Derby database.
 * 
 * @author Oliver Gierke
 */
public class DerbyEmbeddedDatabaseConfigurer implements EmbeddedDatabaseConfigurer {

	private static final Log logger = LogFactory.getLog(DerbyEmbeddedDatabaseConfigurer.class);
	
	private static final String URL_TEMPLATE = "jdbc:derby:memory:%s;%s";

	// Error codes that indicate successful shutdown
	private static final String SHUTDOWN_CODE = "08006";

	private static DerbyEmbeddedDatabaseConfigurer INSTANCE;

	private DerbyEmbeddedDatabaseConfigurer() {
	}

	/**
	 * Get the singleton {@link HsqlEmbeddedDatabaseConfigurer} instance.
	 * @return the configurer
	 * @throws ClassNotFoundException if HSQL is not on the classpath
	 */
	public static synchronized DerbyEmbeddedDatabaseConfigurer getInstance() throws ClassNotFoundException {
		if (INSTANCE == null) {
			ClassUtils.forName("org.apache.derby.jdbc.EmbeddedDriver", DerbyEmbeddedDatabaseConfigurer.class
					.getClassLoader());
			INSTANCE = new DerbyEmbeddedDatabaseConfigurer();
		}
		return INSTANCE;
	}

	public void configureConnectionProperties(ConnectionProperties properties, String databaseName) {
		properties.setDriverClass(org.apache.derby.jdbc.EmbeddedDriver.class);
		properties.setUrl(String.format(URL_TEMPLATE, databaseName, "create=true"));
		properties.setUsername("sa");
		properties.setPassword("");
	}

	@Override
	public void shutdown(DataSource dataSource, String databaseName) {
		Connection connection = null;
		try {
			SimpleDriverDataSource shutdownDataSource = new SimpleDriverDataSource();
			shutdownDataSource.setDriverClass(EmbeddedDriver.class);
			shutdownDataSource.setUrl(String.format(URL_TEMPLATE, databaseName, "shutdown=true"));
			connection = shutdownDataSource.getConnection();
		} catch (SQLException e) {
			if (e instanceof SQLNonTransientConnectionException) {
				SQLNonTransientConnectionException ex = (SQLNonTransientConnectionException) e;
				if (!SHUTDOWN_CODE.equals(ex.getSQLState())) {
					logException(e);
				}
			} else {
				logException(e);
			}
		} finally {
			JdbcUtils.closeConnection(connection);
		}
	}

	private void logException(SQLException e) {
		if (logger.isWarnEnabled()) {
			logger.warn("Could not shutdown in-memory Derby database", e);
		}
	}
}
