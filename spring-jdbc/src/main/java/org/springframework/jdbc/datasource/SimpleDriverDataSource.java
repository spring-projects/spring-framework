/*
 * Copyright 2002-2015 the original author or authors.
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
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

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
 * <p>In a J2EE container, it is recommended to use a JNDI DataSource provided by
 * the container. Such a DataSource can be exposed as a DataSource bean in a Spring
 * ApplicationContext via {@link org.springframework.jndi.JndiObjectFactoryBean},
 * for seamless switching to and from a local DataSource bean like this class.
 *
 * <p>If you need a "real" connection pool outside of a J2EE container, consider
 * <a href="http://commons.apache.org/proper/commons-dbcp">Apache Commons DBCP</a>
 * or <a href="http://sourceforge.net/projects/c3p0">C3P0</a>.
 * Commons DBCP's BasicDataSource and C3P0's ComboPooledDataSource are full
 * connection pool beans, supporting the same basic properties as this class
 * plus specific settings (such as minimal/maximal pool size etc).
 *
 * @author Juergen Hoeller
 * @since 2.5.5
 * @see DriverManagerDataSource
 */
public class SimpleDriverDataSource extends AbstractDriverBasedDataSource {

	private Driver driver;


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
	 * @param conProps JDBC connection properties
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
	public void setDriver(Driver driver) {
		this.driver = driver;
	}

	/**
	 * Return the JDBC Driver instance to use.
	 */
	public Driver getDriver() {
		return this.driver;
	}


	@Override
	protected Connection getConnectionFromDriver(Properties props) throws SQLException {
		Driver driver = getDriver();
		String url = getUrl();
		Assert.notNull(driver, "Driver must not be null");
		if (logger.isDebugEnabled()) {
			logger.debug("Creating new JDBC Driver Connection to [" + url + "]");
		}
		return driver.connect(url, props);
	}

}
