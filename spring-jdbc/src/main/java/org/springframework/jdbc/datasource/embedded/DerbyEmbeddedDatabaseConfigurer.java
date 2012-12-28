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

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.derby.impl.io.VFMemoryStorageFactory;
import org.apache.derby.jdbc.EmbeddedDriver;

/**
 * {@link EmbeddedDatabaseConfigurer} for the Apache Derby database.
 *
 * @author Oliver Gierke
 * @author Juergen Hoeller
 * @since 3.0
 */
final class DerbyEmbeddedDatabaseConfigurer implements EmbeddedDatabaseConfigurer {

	private static final Log logger = LogFactory.getLog(DerbyEmbeddedDatabaseConfigurer.class);

	private static final String URL_TEMPLATE = "jdbc:derby:memory:%s;%s";

	// Error code that indicates successful shutdown
	private static final String SHUTDOWN_CODE = "08006";

	private static final boolean IS_AT_LEAST_DOT_SIX = new EmbeddedDriver().getMinorVersion() >= 6;

	private static final String SHUTDOWN_COMMAND =
			String.format("%s=true", IS_AT_LEAST_DOT_SIX ? "drop" : "shutdown");

	private static DerbyEmbeddedDatabaseConfigurer INSTANCE;


	/**
	 * Get the singleton {@link DerbyEmbeddedDatabaseConfigurer} instance.
	 * @return the configurer
	 * @throws ClassNotFoundException if Derby is not on the classpath
	 */
	public static synchronized DerbyEmbeddedDatabaseConfigurer getInstance() throws ClassNotFoundException {
		if (INSTANCE == null) {
			// disable log file
			System.setProperty("derby.stream.error.method",
					OutputStreamFactory.class.getName() + ".getNoopOutputStream");
			INSTANCE = new DerbyEmbeddedDatabaseConfigurer();
		}
		return INSTANCE;
	}

	private DerbyEmbeddedDatabaseConfigurer() {
	}

	@Override
	public void configureConnectionProperties(ConnectionProperties properties, String databaseName) {
		properties.setDriverClass(EmbeddedDriver.class);
		properties.setUrl(String.format(URL_TEMPLATE, databaseName, "create=true"));
		properties.setUsername("sa");
		properties.setPassword("");
	}

	@Override
	public void shutdown(DataSource dataSource, String databaseName) {
		try {
			new EmbeddedDriver().connect(
					String.format(URL_TEMPLATE, databaseName, SHUTDOWN_COMMAND), new Properties());
		}
		catch (SQLException ex) {
			if (!SHUTDOWN_CODE.equals(ex.getSQLState())) {
				logger.warn("Could not shutdown in-memory Derby database", ex);
				return;
			}
			if (!IS_AT_LEAST_DOT_SIX) {
				// Explicitly purge the in-memory database, to prevent it
				// from hanging around after being shut down.
				try {
					VFMemoryStorageFactory.purgeDatabase(new File(databaseName).getCanonicalPath());
				}
				catch (IOException ex2) {
					logger.warn("Could not purge in-memory Derby database", ex2);
				}
			}
		}
	}

}
