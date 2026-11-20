/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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

	@Override
	public ConnectionProperties getConnectionProperties() {
		return new ConnectionProperties() {
			@Override
			public void setDriverClass(Class<? extends Driver> driverClass) {
				dataSource.setDriverClass(driverClass);
			}

			@Override
			public void setUrl(String url) {
				dataSource.setUrl(url);
			}

			@Override
			public void setUsername(String username) {
				dataSource.setUsername(username);
			}

			@Override
			public void setPassword(String password) {
				dataSource.setPassword(password);
			}
		};
	}

	@Override
	public DataSource getDataSource() {
		return this.dataSource;
	}

}
