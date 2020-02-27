/*
 * Copyright 2002-2014 the original author or authors.
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
