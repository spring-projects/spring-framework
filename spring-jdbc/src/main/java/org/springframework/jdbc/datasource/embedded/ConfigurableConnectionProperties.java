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

package org.springframework.jdbc.datasource.embedded;

import java.sql.Driver;
import java.util.Properties;


/**
 * Extension over ConnectionProperties provides possibility to configure 
 * embedded database with custom properties.
 * 
 * @author Aliaksei Kalotkin
 * @since 4.3
 */
public interface ConfigurableConnectionProperties extends ConnectionProperties {

	/**
	 * Set additional connection properties (like DB_CLOSE_DELAY=-1 for H2).
	 * @param properties connection properties
	 */
	void setProperties(Properties properties);
	
	/**
	 * Get the JDBC driver class to use to connection to the database.
	 * @return JDBC driver class
	 */
	Class<? extends Driver> getDriverClass();
	
	/**
	 * Get the JDBC connection URL for the database.
	 * @return the connection url
	 */
	String getUrl();
	
	/**
	 * Get the username to use to connect to the database.
	 * @return the username
	 */
	String getUsername();
	
	/**
	 * Get the password to use to connect to the database.
	 * @return the password
	 */
	String getPassword();
	
	/**
	 * Get additional connection properties like (DB_CLOSE_DELAY=-1 for H2)
	 * @return additional connection properties
	 */
	Properties getProperties();
	
	/**
	 * Set default connection properties
	 * @param connectionProperties default connection properties
	 */
	public void setDefaults(ConfigurableConnectionProperties connectionProperties);
	
	/**
	 * Get default connection properties.
	 * @return default connection properties
	 */
	public ConnectionProperties getDefaults();
}
