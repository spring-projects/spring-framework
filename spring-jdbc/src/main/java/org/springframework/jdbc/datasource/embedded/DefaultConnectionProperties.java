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

import org.springframework.util.StringUtils;


/**
 * Default implementation.
 * 
 * @author Aliaksei Kalotkin
 * @since 4.3
 */
public class DefaultConnectionProperties implements ConfigurableConnectionProperties {

	private Class<? extends Driver> driverClass;
	private String url;
	private String username;
	private String password;
	private Properties properties = new Properties();
	private ConfigurableConnectionProperties defaults;

	/**
	 * Default constructor.
	 */
	public DefaultConnectionProperties() {
	}

	/* (non-Javadoc)
	 * @see org.springframework.jdbc.datasource.embedded.ConnectionProperties#getDriverClass()
	 */
	@Override
	public Class<? extends Driver> getDriverClass() {
		Class<? extends Driver> driverClass = this.driverClass;
		if (driverClass == null && defaults != null) {
			driverClass = defaults.getDriverClass();
		}
		return driverClass;
	}

	/* (non-Javadoc)
	 * @see org.springframework.jdbc.datasource.embedded.ConnectionProperties#getPassword()
	 */
	@Override
	public String getPassword() {
		String password = this.password;
		if (StringUtils.isEmpty(password) && defaults != null) {
			password = defaults.getPassword();
		}
		return password;
	}

	/* (non-Javadoc)
	 * @see org.springframework.jdbc.datasource.embedded.ConnectionProperties#getProperties()
	 */
	@Override
	public Properties getProperties() {
		if (defaults != null) {
			Properties properties = new Properties();
			properties.putAll(defaults.getProperties());
			properties.putAll(this.properties);
			return properties;
		} else {
			return properties;
		}
	}

	/* (non-Javadoc)
	 * @see org.springframework.jdbc.datasource.embedded.ConnectionProperties#getUrl()
	 */
	@Override
	public String getUrl() {
		String url = this.url;
		if (StringUtils.isEmpty(url) && defaults != null) {
			url = defaults.getUrl();
		}
		return url;
	}

	/* (non-Javadoc)
	 * @see org.springframework.jdbc.datasource.embedded.ConnectionProperties#getUsername()
	 */
	@Override
	public String getUsername() {
		String username = this.username;
		if (StringUtils.isEmpty(username) && defaults != null) {
			username = defaults.getUsername();
		}
		return username;
	}

	/* (non-Javadoc)
	 * @see org.springframework.jdbc.datasource.embedded.ConnectionProperties#setDriverClass(java.lang.Class)
	 */
	@Override
	public void setDriverClass(Class<? extends Driver> driverClass) {
		this.driverClass = driverClass;
	}

	/* (non-Javadoc)
	 * @see org.springframework.jdbc.datasource.embedded.ConnectionProperties#setPassword(java.lang.String)
	 */
	@Override
	public void setPassword(String password) {
		this.password = password;
	}

	/* (non-Javadoc)
	 * @see org.springframework.jdbc.datasource.embedded.ConnectionProperties#setProperties(java.util.Properties)
	 */
	@Override
	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	/* (non-Javadoc)
	 * @see org.springframework.jdbc.datasource.embedded.ConnectionProperties#setUrl(java.lang.String)
	 */
	@Override
	public void setUrl(String url) {
		this.url = url;
	}

	/* (non-Javadoc)
	 * @see org.springframework.jdbc.datasource.embedded.ConnectionProperties#setUsername(java.lang.String)
	 */
	@Override
	public void setUsername(String username) {
		this.username = username;
	}

	/* (non-Javadoc)
	 * @see org.springframework.jdbc.datasource.embedded.ConnectionProperties#override(org.springframework.jdbc.datasource.embedded.ConnectionProperties)
	 */
	@Override
	public void setDefaults(ConfigurableConnectionProperties defaults) {
		this.defaults = defaults;
	}

	/* (non-Javadoc)
	 * @see org.springframework.jdbc.datasource.embedded.ConfigurableConnectionProperties#getDefaults()
	 */
	@Override
	public ConnectionProperties getDefaults() {
		return this.defaults;
	}
}
