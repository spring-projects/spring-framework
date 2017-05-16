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
	 * Configure the properties required to create and connect to the embedded
	 * database instance.
	 * @param properties connection properties to configure
	 * @param databaseName the name of the embedded database
	 */
	void configureConnectionProperties(ConnectionProperties properties, String databaseName);

	/**
	 * Shut down the embedded database instance that backs the supplied {@link DataSource}.
	 * @param dataSource the data source
	 * @param databaseName the name of the database being shut down
	 */
	void shutdown(DataSource dataSource, String databaseName);

}
