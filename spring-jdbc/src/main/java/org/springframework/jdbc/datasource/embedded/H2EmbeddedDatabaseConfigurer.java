/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.datasource.embedded;

import java.sql.Driver;

import org.jspecify.annotations.Nullable;

import org.springframework.util.ClassUtils;

/**
 * {@link EmbeddedDatabaseConfigurer} for an H2 embedded database instance.
 *
 * <p>Call {@link #getInstance()} to get the singleton instance of this class.
 *
 * @author Oliver Gierke
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 */
final class H2EmbeddedDatabaseConfigurer extends AbstractEmbeddedDatabaseConfigurer {

	private static @Nullable H2EmbeddedDatabaseConfigurer instance;

	private final Class<? extends Driver> driverClass;


	/**
	 * Get the singleton {@code H2EmbeddedDatabaseConfigurer} instance.
	 * @return the configurer instance
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
