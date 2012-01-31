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
 * Encapsulates the creation of a particular DataSource implementation, such as a
 * {@link SimpleDriverDataSource} or connection pool such as Apache DBCP or C3P0.
 *
 * <p>Call {@link #getConnectionProperties()} to configure normalized DataSource properties
 * before calling {@link #getDataSource()} to actually get the configured DataSource instance.
 *
 * @author Keith Donald
 * @since 3.0
 */
public interface DataSourceFactory {

	/**
	 * Allows properties of the DataSource to be configured.
	 */
	ConnectionProperties getConnectionProperties();

	/**
	 * Returns the DataSource with the connection properties applied.
	 */
	DataSource getDataSource();

}
