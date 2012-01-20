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

import java.sql.Driver;

import org.springframework.util.ClassUtils;

/**
 * Initializes an HSQL embedded database instance.
 * Call {@link #getInstance()} to get the singleton instance of this class.
 *
 * @author Keith Donald
 * @author Oliver Gierke
 * @since 3.0
 */
final class HsqlEmbeddedDatabaseConfigurer extends AbstractEmbeddedDatabaseConfigurer {

	private static HsqlEmbeddedDatabaseConfigurer INSTANCE;

	private final Class<? extends Driver> driverClass;

	/**
	 * Get the singleton {@link HsqlEmbeddedDatabaseConfigurer} instance.
	 * @return the configurer
	 * @throws ClassNotFoundException if HSQL is not on the classpath
	 */
	@SuppressWarnings("unchecked")
	public static synchronized HsqlEmbeddedDatabaseConfigurer getInstance() throws ClassNotFoundException {
		if (INSTANCE == null) {
			INSTANCE = new HsqlEmbeddedDatabaseConfigurer(
					(Class<? extends Driver>) ClassUtils.forName("org.hsqldb.jdbcDriver", HsqlEmbeddedDatabaseConfigurer.class.getClassLoader()));
		}
		return INSTANCE;
	}

	private HsqlEmbeddedDatabaseConfigurer(Class<? extends Driver> driverClass) {
		this.driverClass = driverClass;
	}

	public void configureConnectionProperties(ConnectionProperties properties, String databaseName) {
		properties.setDriverClass(this.driverClass);
		properties.setUrl("jdbc:hsqldb:mem:" + databaseName);
		properties.setUsername("sa");
		properties.setPassword("");
	}

}