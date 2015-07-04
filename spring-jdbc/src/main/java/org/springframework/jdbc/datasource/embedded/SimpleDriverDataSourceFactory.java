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

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.SimpleDriverDataSource;

/**
 * Creates a {@link SimpleDriverDataSource}.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Aliaksei Kalotkin
 * @since 3.0
 */
final class SimpleDriverDataSourceFactory implements DataSourceFactory {

	private SimpleDriverDataSource dataSource = new SimpleDriverDataSource();

	@Override
	public ConnectionProperties getConnectionProperties() {
		return new SimpleDriverDataSourceAdapter(dataSource);
	}
	
	@Override
	public DataSource getDataSource() {
		return this.dataSource;
	}
	
	/**
	 * Adaptes SimpleDriverDataSource to ConfigurableConnectionProperties.
	 * 
	 * @author Aliaksei Kalotkin
	 * @since 4.3
	 */
	private static class SimpleDriverDataSourceAdapter implements ConfigurableConnectionProperties {
	
		private SimpleDriverDataSource dataSource;
		
		private SimpleDriverDataSourceAdapter(SimpleDriverDataSource dataSource) {
			this.dataSource = dataSource;
		}
		
		/* (non-Javadoc)
		 * @see org.springframework.jdbc.datasource.embedded.ConfigurableConnectionProperties#getProperties()
		 */
		@Override
		public ConnectionProperties getDefaults() {
			return null;
		}
		
		/* (non-Javadoc)
		 * @see org.springframework.jdbc.datasource.embedded.ConfigurableConnectionProperties#getProperties()
		 */
		@Override
		public void setUsername(String username) {
			dataSource.setUsername(username);
		}
		
		/* (non-Javadoc)
		 * @see org.springframework.jdbc.datasource.embedded.ConfigurableConnectionProperties#getProperties()
		 */
		@Override
		public void setUrl(String url) {
			dataSource.setUrl(url);
		}
		
		/* (non-Javadoc)
		 * @see org.springframework.jdbc.datasource.embedded.ConfigurableConnectionProperties#getProperties()
		 */
		@Override
		public void setPassword(String password) {
			dataSource.setPassword(password);
		}
		
		/* (non-Javadoc)
		 * @see org.springframework.jdbc.datasource.embedded.ConfigurableConnectionProperties#getProperties()
		 */
		@Override
		public void setDriverClass(Class<? extends Driver> driverClass) {
			dataSource.setDriverClass(driverClass);
		}
		
		/* (non-Javadoc)
		 * @see org.springframework.jdbc.datasource.embedded.ConfigurableConnectionProperties#getProperties()
		 */
		@Override
		public void setProperties(Properties properties) {
			dataSource.setConnectionProperties(properties);
		}
		
		/* (non-Javadoc)
		 * @see org.springframework.jdbc.datasource.embedded.ConfigurableConnectionProperties#getProperties()
		 */
		@Override
		public void setDefaults(ConfigurableConnectionProperties connectionProperties) {
		}
		
		/* (non-Javadoc)
		 * @see org.springframework.jdbc.datasource.embedded.ConfigurableConnectionProperties#getProperties()
		 */
		@Override
		public String getUsername() {
			return dataSource.getUsername();
		}
		
		/* (non-Javadoc)
		 * @see org.springframework.jdbc.datasource.embedded.ConfigurableConnectionProperties#getProperties()
		 */
		@Override
		public String getUrl() {
			return dataSource.getUrl();
		}
		
		/* (non-Javadoc)
		 * @see org.springframework.jdbc.datasource.embedded.ConfigurableConnectionProperties#getProperties()
		 */
		@Override
		public Properties getProperties() {
			return dataSource.getConnectionProperties();
		}
		
		/* (non-Javadoc)
		 * @see org.springframework.jdbc.datasource.embedded.ConfigurableConnectionProperties#getProperties()
		 */
		@Override
		public String getPassword() {
			return dataSource.getPassword();
		}
		
		/* (non-Javadoc)
		 * @see org.springframework.jdbc.datasource.embedded.ConfigurableConnectionProperties#getProperties()
		 */
		@Override
		public Class<? extends Driver> getDriverClass() {
			return dataSource.getDriver().getClass();
		}
		
	}

}
