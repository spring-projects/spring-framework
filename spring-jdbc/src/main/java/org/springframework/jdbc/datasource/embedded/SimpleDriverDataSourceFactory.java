/*
 * Copyright 2002-2012 the original author or authors.
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

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.SimpleDriverDataSource;

/**
 * Creates a {@link SimpleDriverDataSource}.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 */
final class SimpleDriverDataSourceFactory implements DataSourceFactory {

	private final SimpleDriverDataSource dataSource = new SimpleDriverDataSource();

	public ConnectionProperties getConnectionProperties() {
		return new ConnectionProperties() {
			public void setDriverClass(Class<? extends Driver> driverClass) {
				dataSource.setDriverClass(driverClass);
			}

			public void setUrl(String url) {
				dataSource.setUrl(url);
			}

			public void setUsername(String username) {
				dataSource.setUsername(username);
			}

			public void setPassword(String password) {
				dataSource.setPassword(password);
			}
		};
	}

	public DataSource getDataSource() {
		return this.dataSource;
	}

}
