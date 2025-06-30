/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jdbc.datasource;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeanUtils;
import org.springframework.util.Assert;

/**
 * Simple implementation of the standard JDBC {@link javax.sql.DataSource} interface,
 * configuring a plain old JDBC {@link java.sql.Driver} via bean properties, and
 * returning a new {@link java.sql.Connection} from every {@code getConnection} call.
 *
 * <p><b>NOTE: This class is not an actual connection pool; it does not actually
 * pool Connections.</b> It just serves as simple replacement for a full-blown
 * connection pool, implementing the same standard interface, but creating new
 * Connections on every call.
 *
 * <p>In a Jakarta EE container, it is recommended to use a JNDI DataSource provided by
 * the container. Such a DataSource can be exposed as a DataSource bean in a Spring
 * ApplicationContext via {@link org.springframework.jndi.JndiObjectFactoryBean},
 * for seamless switching to and from a local DataSource bean like this class.
 *
 * <p>This {@code SimpleDriverDataSource} class was originally designed alongside
 * <a href="https://commons.apache.org/proper/commons-dbcp">Apache Commons DBCP</a>
 * and <a href="https://sourceforge.net/projects/c3p0">C3P0</a>, featuring bean-style
 * {@code BasicDataSource}/{@code ComboPooledDataSource} classes with configuration
 * properties for local resource setups. For a modern JDBC connection pool, consider
 * <a href="https://github.com/brettwooldridge/HikariCP">HikariCP</a> instead,
 * exposing a corresponding {@code HikariDataSource} instance to the application.
 *
 * @author Juergen Hoeller
 * @since 2.5.5
 * @see DriverManagerDataSource
 */
public class SimpleDriverDataSource extends AbstractDriverBasedDataSource {

	private @Nullable Driver driver;


	/**
	 * Constructor for bean-style configuration.
	 */
	public SimpleDriverDataSource() {
	}

	/**
	 * Create a new DriverManagerDataSource with the given standard Driver parameters.
	 * @param driver the JDBC Driver object
	 * @param url the JDBC URL to use for accessing the DriverManager
	 * @see java.sql.Driver#connect(String, java.util.Properties)
	 */
	public SimpleDriverDataSource(Driver driver, String url) {
		setDriver(driver);
		setUrl(url);
	}

	/**
	 * Create a new DriverManagerDataSource with the given standard Driver parameters.
	 * @param driver the JDBC Driver object
	 * @param url the JDBC URL to use for accessing the DriverManager
	 * @param username the JDBC username to use for accessing the DriverManager
	 * @param password the JDBC password to use for accessing the DriverManager
	 * @see java.sql.Driver#connect(String, java.util.Properties)
	 */
	public SimpleDriverDataSource(Driver driver, String url, String username, String password) {
		setDriver(driver);
		setUrl(url);
		setUsername(username);
		setPassword(password);
	}

	/**
	 * Create a new DriverManagerDataSource with the given standard Driver parameters.
	 * @param driver the JDBC Driver object
	 * @param url the JDBC URL to use for accessing the DriverManager
	 * @param conProps the JDBC connection properties
	 * @see java.sql.Driver#connect(String, java.util.Properties)
	 */
	public SimpleDriverDataSource(Driver driver, String url, Properties conProps) {
		setDriver(driver);
		setUrl(url);
		setConnectionProperties(conProps);
	}


	/**
	 * Specify the JDBC Driver implementation class to use.
	 * <p>An instance of this Driver class will be created and held
	 * within the SimpleDriverDataSource.
	 * @see #setDriver
	 */
	public void setDriverClass(Class<? extends Driver> driverClass) {
		this.driver = BeanUtils.instantiateClass(driverClass);
	}

	/**
	 * Specify the JDBC Driver instance to use.
	 * <p>This allows for passing in a shared, possibly pre-configured
	 * Driver instance.
	 * @see #setDriverClass
	 */
	public void setDriver(@Nullable Driver driver) {
		this.driver = driver;
	}

	/**
	 * Return the JDBC Driver instance to use.
	 */
	public @Nullable Driver getDriver() {
		return this.driver;
	}


	@Override
	protected Connection getConnectionFromDriver(Properties props) throws SQLException {
		Driver driver = getDriver();
		Assert.state(driver != null, "Driver has not been set");
		String url = getUrl();
		if (logger.isDebugEnabled()) {
			logger.debug("Creating new JDBC Driver Connection to [" + url + "]");
		}
		return driver.connect(url, props);
	}

}
