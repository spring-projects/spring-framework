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

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.SimpleDriverDataSource;

/**
 * Encapsulates the configuration required to create, connect to, and shutdown a specific type of embedded database such as HSQLdb or H2.
 * Create a subclass for each database type we wish to support.
 * 
 * @see EmbeddedDatabaseConfigurerFactory
 */
abstract class EmbeddedDatabaseConfigurer {
	
	/**
	 * Configure the properties required to create and connect to the embedded database instance.
	 * @param dataSource the data source to configure
	 * @param databaseName the name of the test database
	 */
	public abstract void configureConnectionProperties(SimpleDriverDataSource dataSource, String databaseName);

	/**
	 * Shutdown the embedded database instance that backs dataSource.
	 * @param dataSource the data source
	 */
	public abstract void shutdown(DataSource dataSource);
	
}
