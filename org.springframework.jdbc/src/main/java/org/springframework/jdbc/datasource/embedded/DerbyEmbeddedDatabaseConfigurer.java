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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.derby.impl.io.VFMemoryStorageFactory;
import org.apache.derby.jdbc.EmbeddedDriver;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.util.ClassUtils;

/**
 * {@link EmbeddedDatabaseConfigurer} for the Apache Derby database.
 * 
 * @author Oliver Gierke
 */
final class DerbyEmbeddedDatabaseConfigurer implements EmbeddedDatabaseConfigurer {

	private static final Log logger = LogFactory.getLog(DerbyEmbeddedDatabaseConfigurer.class);
	
	private static final String URL_TEMPLATE = "jdbc:derby:memory:%s;%s";

	// Error codes that indicate successful shutdown
	private static final String SHUTDOWN_CODE = "08006";

	private static DerbyEmbeddedDatabaseConfigurer INSTANCE;

	private DerbyEmbeddedDatabaseConfigurer() {
	}

	/**
	 * Get the singleton {@link DerbyEmbeddedDatabaseConfigurer} instance.
	 * @return the configurer
	 * @throws ClassNotFoundException if Derby is not on the classpath
	 */
	public static synchronized DerbyEmbeddedDatabaseConfigurer getInstance() throws ClassNotFoundException {
		if (INSTANCE == null) {
			// disable log file
			System.setProperty("derby.stream.error.method", 
					DerbyEmbeddedDatabaseConfigurer.class.getName() + ".getNoopOutputStream");
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

	public void shutdown(DataSource dataSource, String databaseName) {
		Connection connection = null;
		try {
			SimpleDriverDataSource shutdownDataSource = new SimpleDriverDataSource();
			shutdownDataSource.setDriverClass(EmbeddedDriver.class);
			shutdownDataSource.setUrl(String.format(URL_TEMPLATE, databaseName, "shutdown=true"));
			connection = shutdownDataSource.getConnection();
		} catch (SQLException e) {
			if (SHUTDOWN_CODE.equals(e.getSQLState())) {
				purgeDatabase(databaseName);
			} else {
				logger.warn("Could not shutdown in-memory Derby database", e);
			}
		} finally {
			JdbcUtils.closeConnection(connection);
		}
	}

	/**
	 * Purges the in-memory database, to prevent it from hanging around after
	 * being shut down
	 * @param databaseName
	 */
	private void purgeDatabase(String databaseName) {
		// TODO: update this code once Derby adds a proper way to remove an in-memory db
		// (see http://wiki.apache.org/db-derby/InMemoryBackEndPrimer for details)
		try {
			VFMemoryStorageFactory.purgeDatabase(new File(databaseName).getCanonicalPath());
		} catch (IOException ioe) {
			logger.warn("Could not purge in-memory Derby database", ioe);
		}
	}


	/**
	 * Returns an {@link OutputStream} that ignores all data given to it.
	 * Used by {@link #getInstance()} to prevent writing to Derby.log file.
	 */
	static OutputStream getNoopOutputStream() {
		return new OutputStream() {
			public void write(int b) throws IOException {
				// ignore the input
			}
		};
	}
	
}