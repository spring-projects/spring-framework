/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.datasource.embedded;

import javax.sql.DataSource;

/**
 * {@code EmbeddedDatabaseConfigurer} encapsulates the configuration required to
 * create, connect to, and shut down a specific type of embedded database such as
 * HSQL, H2, or Derby.
 *
 * @author Keith Donald
 * @author Sam Brannen
 * @since 3.0
 */
public interface EmbeddedDatabaseConfigurer {

	/**
	 * Configure the properties required to create and connect to the embedded database.
	 * @param properties connection properties to configure
	 * @param databaseName the name of the embedded database
	 */
	void configureConnectionProperties(ConnectionProperties properties, String databaseName);

	/**
	 * Shut down the embedded database instance that backs the supplied {@link DataSource}.
	 * @param dataSource the corresponding {@link DataSource}
	 * @param databaseName the name of the database being shut down
	 */
	void shutdown(DataSource dataSource, String databaseName);

}
