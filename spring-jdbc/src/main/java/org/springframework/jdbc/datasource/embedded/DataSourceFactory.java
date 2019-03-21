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

import javax.sql.DataSource;

/**
 * {@code DataSourceFactory} encapsulates the creation of a particular
 * {@link DataSource} implementation such as a
 * {@link org.springframework.jdbc.datasource.SimpleDriverDataSource
 * SimpleDriverDataSource} or a connection pool such as Apache DBCP or C3P0.
 *
 * <p>Call {@link #getConnectionProperties()} to configure normalized
 * {@code DataSource} properties before calling {@link #getDataSource()} to
 * actually get the configured {@code DataSource} instance.
 *
 * @author Keith Donald
 * @author Sam Brannen
 * @since 3.0
 */
public interface DataSourceFactory {

	/**
	 * Get the {@linkplain ConnectionProperties connection properties} of the
	 * {@link #getDataSource DataSource} to be configured.
	 */
	ConnectionProperties getConnectionProperties();

	/**
	 * Get the {@link DataSource} with the {@linkplain #getConnectionProperties
	 * connection properties} applied.
	 */
	DataSource getDataSource();

}
