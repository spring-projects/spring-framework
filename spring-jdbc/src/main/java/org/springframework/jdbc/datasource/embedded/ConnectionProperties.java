/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.datasource.embedded;

import java.sql.Driver;

/**
 * {@code ConnectionProperties} serves as a simple data container that allows
 * essential JDBC connection properties to be configured consistently,
 * independent of the actual {@link javax.sql.DataSource DataSource}
 * implementation.
 *
 * @author Keith Donald
 * @author Sam Brannen
 * @since 3.0
 * @see DataSourceFactory
 */
public interface ConnectionProperties {

	/**
	 * Set the JDBC driver class to use to connect to the database.
	 * @param driverClass the jdbc driver class
	 */
	void setDriverClass(Class<? extends Driver> driverClass);

	/**
	 * Set the JDBC connection URL for the database.
	 * @param url the connection url
	 */
	void setUrl(String url);

	/**
	 * Set the username to use to connect to the database.
	 * @param username the username
	 */
	void setUsername(String username);

	/**
	 * Set the password to use to connect to the database.
	 * @param password the password
	 */
	void setPassword(String password);

}
