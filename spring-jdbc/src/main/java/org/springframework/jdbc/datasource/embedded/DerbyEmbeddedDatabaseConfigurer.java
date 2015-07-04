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

import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.logging.LogFactory;
import org.apache.derby.jdbc.EmbeddedDriver;

/**
 * {@link EmbeddedDatabaseConfigurer} for the Apache Derby database 10.6+.
 * <p>Call {@link #getInstance()} to get the singleton instance of this class.
 *
 * @author Oliver Gierke
 * @author Juergen Hoeller
 * @author Aliaksei Kalotkin
 * @since 3.0
 */
final class DerbyEmbeddedDatabaseConfigurer extends AbstractEmbeddedDatabaseConfigurer {

	private static final String URL_TEMPLATE = "jdbc:derby:memory:%s;%s";

	private static DerbyEmbeddedDatabaseConfigurer instance;
	
	/**
	 * Database url.
	 */
	private String url;


	/**
	 * Get the singleton {@link DerbyEmbeddedDatabaseConfigurer} instance.
	 * @return the configurer
	 * @throws ClassNotFoundException if Derby is not on the classpath
	 */
	public static synchronized DerbyEmbeddedDatabaseConfigurer getInstance() throws ClassNotFoundException {
		if (instance == null) {
			// disable log file
			System.setProperty("derby.stream.error.method",
					OutputStreamFactory.class.getName() + ".getNoopOutputStream");
			instance = new DerbyEmbeddedDatabaseConfigurer();
		}
		return instance;
	}


	private DerbyEmbeddedDatabaseConfigurer() {
		super(EmbeddedDriver.class);
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.jdbc.datasource.embedded.AbstractEmbeddedDatabaseConfigurer#configureDefaultProperties(java.lang.String)
	 */
	@Override
	protected void configureDefaultProperties(String databaseName) {
		defaultProperties.setDriverClass(driverClass);
		defaultProperties.setUrl(String.format(URL_TEMPLATE, databaseName, "create=true"));
		defaultProperties.setUsername("sa");
		defaultProperties.setPassword("");
	}

	@Override
	public void configureConnectionProperties(ConnectionProperties properties, String databaseName) {
		super.configureConnectionProperties(properties, databaseName);
		if (properties instanceof ConfigurableConnectionProperties) {
			this.url = ((ConfigurableConnectionProperties) properties).getUrl();
		} else {
			this.url = defaultProperties.getUrl();
		}
	}

	@Override
	public void shutdown(DataSource dataSource, String databaseName) {
		try {
			Properties shutdownProperties = new Properties();
			shutdownProperties.put("shutdown", "true");
			new EmbeddedDriver().connect(url, shutdownProperties);
		}
		catch (SQLException ex) {
			// Error code that indicates successful shutdown
			if (!"08006".equals(ex.getSQLState())) {
				LogFactory.getLog(getClass()).warn("Could not shutdown in-memory Derby database", ex);
			}
		}
	}
}
