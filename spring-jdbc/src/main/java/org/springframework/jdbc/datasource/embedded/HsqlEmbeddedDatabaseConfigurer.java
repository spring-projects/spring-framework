/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.datasource.embedded;

import java.sql.Driver;

import org.jspecify.annotations.Nullable;

import org.springframework.util.ClassUtils;

/**
 * {@link EmbeddedDatabaseConfigurer} for an HSQL embedded database instance.
 *
 * <p>Call {@link #getInstance()} to get the singleton instance of this class.
 *
 * @author Keith Donald
 * @author Oliver Gierke
 * @since 3.0
 */
final class HsqlEmbeddedDatabaseConfigurer extends AbstractEmbeddedDatabaseConfigurer {

	private static @Nullable HsqlEmbeddedDatabaseConfigurer instance;

	private final Class<? extends Driver> driverClass;


	/**
	 * Get the singleton {@link HsqlEmbeddedDatabaseConfigurer} instance.
	 * @return the configurer instance
	 * @throws ClassNotFoundException if HSQL is not on the classpath
	 */
	@SuppressWarnings("unchecked")
	public static synchronized HsqlEmbeddedDatabaseConfigurer getInstance() throws ClassNotFoundException {
		if (instance == null) {
			instance = new HsqlEmbeddedDatabaseConfigurer( (Class<? extends Driver>)
					ClassUtils.forName("org.hsqldb.jdbcDriver", HsqlEmbeddedDatabaseConfigurer.class.getClassLoader()));
		}
		return instance;
	}


	private HsqlEmbeddedDatabaseConfigurer(Class<? extends Driver> driverClass) {
		this.driverClass = driverClass;
	}

	@Override
	public void configureConnectionProperties(ConnectionProperties properties, String databaseName) {
		properties.setDriverClass(this.driverClass);
		properties.setUrl("jdbc:hsqldb:mem:" + databaseName);
		properties.setUsername("sa");
		properties.setPassword("");
	}

}
