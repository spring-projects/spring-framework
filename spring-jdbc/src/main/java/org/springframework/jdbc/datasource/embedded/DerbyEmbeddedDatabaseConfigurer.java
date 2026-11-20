/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.datasource.embedded;

import java.io.OutputStream;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.logging.LogFactory;
import org.apache.derby.jdbc.EmbeddedDriver;
import org.jspecify.annotations.Nullable;

/**
 * {@link EmbeddedDatabaseConfigurer} for the Apache Derby database.
 *
 * <p>Call {@link #getInstance()} to get the singleton instance of this class.
 *
 * @author Oliver Gierke
 * @author Juergen Hoeller
 * @since 3.0
 * @deprecated Apache Derby is retired, switch to H2 or HSQLDB
 */
@Deprecated(forRemoval = true, since = "7.1")
final class DerbyEmbeddedDatabaseConfigurer implements EmbeddedDatabaseConfigurer {

	private static final String URL_TEMPLATE = "jdbc:derby:memory:%s;%s";

	private static @Nullable DerbyEmbeddedDatabaseConfigurer instance;


	/**
	 * Get the singleton {@link DerbyEmbeddedDatabaseConfigurer} instance.
	 * @return the configurer instance
	 */
	public static synchronized DerbyEmbeddedDatabaseConfigurer getInstance() {
		if (instance == null) {
			// disable log file
			System.setProperty("derby.stream.error.method",
					OutputStream.class.getName() + ".nullOutputStream");
			instance = new DerbyEmbeddedDatabaseConfigurer();
		}
		return instance;
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
					String.format(URL_TEMPLATE, databaseName, "drop=true"), new Properties());
		}
		catch (SQLException ex) {
			// Error code that indicates successful shutdown
			if (!"08006".equals(ex.getSQLState())) {
				LogFactory.getLog(getClass()).warn("Could not shut down embedded Derby database", ex);
			}
		}
	}

}
