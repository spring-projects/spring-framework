package org.springframework.jdbc.datasource.embedded;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.SimpleDriverDataSource;

final class SimpleDriverDataSourceFactory implements DataSourceFactory {

	private SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
	
	public ConnectionProperties getConnectionProperties() {
		return new ConnectionProperties() {
			public void setDriverClass(Class<?> driverClass) {
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
		return dataSource;
	}
	
}
