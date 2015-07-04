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
 * Wraps properties from: EmbeddedDatabaseFactory (configuration properties), 
 * EmbeddedDataBaseConfigurer (default properties) and DataSourceFactory (datasource properties).
 * 
 * 
 * @author Aliaksei Kalotkin
 * @since 4.3
 */
class ConnectionPropertiesWrapper implements ConfigurableConnectionProperties {

	private ConfigurableConnectionProperties configurationProperties;
	private ConfigurableConnectionProperties defaultProperties;
	private ConnectionProperties dataSourceProperties;
	
	/**
	 * 
	 */
	public ConnectionPropertiesWrapper(ConnectionProperties dataSourceProperties, ConfigurableConnectionProperties configurationProperties) {
		this.configurationProperties = configurationProperties;
		this.dataSourceProperties = dataSourceProperties;
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.jdbc.datasource.embedded.ConnectionProperties#setDriverClass(java.lang.Class)
	 */
	@Override
	public void setDriverClass(Class<? extends Driver> driverClass) {
		dataSourceProperties.setDriverClass(driverClass);
	}

	/* (non-Javadoc)
	 * @see org.springframework.jdbc.datasource.embedded.ConnectionProperties#setUrl(java.lang.String)
	 */
	@Override
	public void setUrl(String url) {
		dataSourceProperties.setUrl(url);
	}

	/* (non-Javadoc)
	 * @see org.springframework.jdbc.datasource.embedded.ConnectionProperties#setUsername(java.lang.String)
	 */
	@Override
	public void setUsername(String username) {
		dataSourceProperties.setUsername(username);
	}

	/* (non-Javadoc)
	 * @see org.springframework.jdbc.datasource.embedded.ConnectionProperties#setPassword(java.lang.String)
	 */
	@Override
	public void setPassword(String password) {
		dataSourceProperties.setPassword(password);
	}

	/* (non-Javadoc)
	 * @see org.springframework.jdbc.datasource.embedded.ConfigurableConnectionProperties#setProperties(java.util.Properties)
	 */
	@Override
	public void setProperties(Properties properties) {
		if (dataSourceProperties instanceof ConfigurableConnectionProperties) {
			((ConfigurableConnectionProperties) dataSourceProperties).setProperties(properties);
		}
	}

	/* (non-Javadoc)
	 * @see org.springframework.jdbc.datasource.embedded.ConfigurableConnectionProperties#getDriverClass()
	 */
	@Override
	public Class<? extends Driver> getDriverClass() {
		return configurationProperties.getDriverClass();
	}

	/* (non-Javadoc)
	 * @see org.springframework.jdbc.datasource.embedded.ConfigurableConnectionProperties#getUrl()
	 */
	@Override
	public String getUrl() {
		return configurationProperties.getUrl();
	}

	/* (non-Javadoc)
	 * @see org.springframework.jdbc.datasource.embedded.ConfigurableConnectionProperties#getUsername()
	 */
	@Override
	public String getUsername() {
		return configurationProperties.getUsername();
	}

	/* (non-Javadoc)
	 * @see org.springframework.jdbc.datasource.embedded.ConfigurableConnectionProperties#getPassword()
	 */
	@Override
	public String getPassword() {
		return configurationProperties.getPassword();
	}

	/* (non-Javadoc)
	 * @see org.springframework.jdbc.datasource.embedded.ConfigurableConnectionProperties#getProperties()
	 */
	@Override
	public Properties getProperties() {
		return configurationProperties.getProperties();
	}

	/* (non-Javadoc)
	 * @see org.springframework.jdbc.datasource.embedded.ConfigurableConnectionProperties#getProperties()
	 */
	public ConnectionProperties getDefaults() {
		return defaultProperties;
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.jdbc.datasource.embedded.ConfigurableConnectionProperties#getProperties()
	 */
	public void setDefaults(ConfigurableConnectionProperties defaults) {
		this.defaultProperties = defaults;
		configurationProperties.setDefaults(defaultProperties);
		setDriverClass(configurationProperties.getDriverClass());
		setPassword(configurationProperties.getPassword());
		setUrl(configurationProperties.getUrl());
		setUsername(configurationProperties.getUsername());
		setProperties(configurationProperties.getProperties());
	}
}
