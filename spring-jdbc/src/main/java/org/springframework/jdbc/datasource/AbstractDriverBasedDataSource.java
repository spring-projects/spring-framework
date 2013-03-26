/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.jdbc.datasource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.springframework.util.Assert;

/**
 * Abstract base class for JDBC {@link javax.sql.DataSource} implementations
 * that operate on a JDBC {@link java.sql.Driver}.
 *
 * @author Juergen Hoeller
 * @since 2.5.5
 * @see SimpleDriverDataSource
 * @see DriverManagerDataSource
 */
public abstract class AbstractDriverBasedDataSource extends AbstractDataSource {

	private String url;

	private String username;

	private String password;

	private Properties connectionProperties;


	/**
	 * Set the JDBC URL to use for connecting through the Driver.
	 * @see java.sql.Driver#connect(String, java.util.Properties)
	 */
	public void setUrl(String url) {
		Assert.hasText(url, "Property 'url' must not be empty");
		this.url = url.trim();
	}

	/**
	 * Return the JDBC URL to use for connecting through the Driver.
	 */
	public String getUrl() {
		return this.url;
	}

	/**
	 * Set the JDBC username to use for connecting through the Driver.
	 * @see java.sql.Driver#connect(String, java.util.Properties)
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Return the JDBC username to use for connecting through the Driver.
	 */
	public String getUsername() {
		return this.username;
	}

	/**
	 * Set the JDBC password to use for connecting through the Driver.
	 * @see java.sql.Driver#connect(String, java.util.Properties)
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Return the JDBC password to use for connecting through the Driver.
	 */
	public String getPassword() {
		return this.password;
	}

	/**
	 * Specify arbitrary connection properties as key/value pairs,
	 * to be passed to the Driver.
	 * <p>Can also contain "user" and "password" properties. However,
	 * any "username" and "password" bean properties specified on this
	 * DataSource will override the corresponding connection properties.
	 * @see java.sql.Driver#connect(String, java.util.Properties)
	 */
	public void setConnectionProperties(Properties connectionProperties) {
		this.connectionProperties = connectionProperties;
	}

	/**
	 * Return the connection properties to be passed to the Driver, if any.
	 */
	public Properties getConnectionProperties() {
		return this.connectionProperties;
	}


	/**
	 * This implementation delegates to {@code getConnectionFromDriver},
	 * using the default username and password of this DataSource.
	 * @see #getConnectionFromDriver(String, String)
	 * @see #setUsername
	 * @see #setPassword
	 */
	public Connection getConnection() throws SQLException {
		return getConnectionFromDriver(getUsername(), getPassword());
	}

	/**
	 * This implementation delegates to {@code getConnectionFromDriver},
	 * using the given username and password.
	 * @see #getConnectionFromDriver(String, String)
	 */
	public Connection getConnection(String username, String password) throws SQLException {
		return getConnectionFromDriver(username, password);
	}


	/**
	 * Build properties for the Driver, including the given username and password (if any),
	 * and obtain a corresponding Connection.
	 * @param username the name of the user
	 * @param password the password to use
	 * @return the obtained Connection
	 * @throws SQLException in case of failure
	 * @see java.sql.Driver#connect(String, java.util.Properties)
	 */
	protected Connection getConnectionFromDriver(String username, String password) throws SQLException {
		Properties mergedProps = new Properties();
		Properties connProps = getConnectionProperties();
		if (connProps != null) {
			mergedProps.putAll(connProps);
		}
		if (username != null) {
			mergedProps.setProperty("user", username);
		}
		if (password != null) {
			mergedProps.setProperty("password", password);
		}
		return getConnectionFromDriver(mergedProps);
	}

	/**
	 * Obtain a Connection using the given properties.
	 * <p>Template method to be implemented by subclasses.
	 * @param props the merged connection properties
	 * @return the obtained Connection
	 * @throws SQLException in case of failure
	 */
	protected abstract Connection getConnectionFromDriver(Properties props) throws SQLException;

}
