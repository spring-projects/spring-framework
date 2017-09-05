/*
 * Copyright 2002-2017 the original author or authors.
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

import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * {@link EmbeddedDatabaseConfigurer} for an H2 embedded database instance.
 * <p>Call {@link #getInstance()} to get the singleton instance of this class.
 *
 * @author Oliver Gierke
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 */
final class H2EmbeddedDatabaseConfigurer extends AbstractEmbeddedDatabaseConfigurer {

	@Nullable
	private static H2EmbeddedDatabaseConfigurer instance;

	private final Class<? extends Driver> driverClass;


	/**
	 * Get the singleton {@code H2EmbeddedDatabaseConfigurer} instance.
	 * @return the configurer
	 * @throws ClassNotFoundException if H2 is not on the classpath
	 */
	@SuppressWarnings("unchecked")
	public static synchronized H2EmbeddedDatabaseConfigurer getInstance() throws ClassNotFoundException {
		if (instance == null) {
			instance = new H2EmbeddedDatabaseConfigurer( (Class<? extends Driver>)
					ClassUtils.forName("org.h2.Driver", H2EmbeddedDatabaseConfigurer.class.getClassLoader()));
		}
		return instance;
	}


	private H2EmbeddedDatabaseConfigurer(Class<? extends Driver> driverClass) {
		this.driverClass = driverClass;
	}

	@Override
	public void configureConnectionProperties(ConnectionProperties properties, String databaseName) {
		properties.setDriverClass(this.driverClass);
		properties.setUrl(String.format("jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false", databaseName));
		properties.setUsername("sa");
		properties.setPassword("");
	}

}
