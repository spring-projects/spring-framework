/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.datasource.embedded;

import java.sql.Driver;

import org.springframework.util.ClassUtils;

/**
 * {@link EmbeddedDatabaseConfigurer} for an H2 embedded database instance
 * with auto-commit disabled.
 *
 * @author Sam Brannen
 * @since 5.3.11
 */
public class AutoCommitDisabledH2EmbeddedDatabaseConfigurer extends AbstractEmbeddedDatabaseConfigurer {

	private final Class<? extends Driver> driverClass;


	@SuppressWarnings("unchecked")
	public AutoCommitDisabledH2EmbeddedDatabaseConfigurer() throws Exception {
		this.driverClass = (Class<? extends Driver>) ClassUtils.forName("org.h2.Driver",
			AutoCommitDisabledH2EmbeddedDatabaseConfigurer.class.getClassLoader());
	}

	@Override
	public void configureConnectionProperties(ConnectionProperties properties, String databaseName) {
		String url = String.format("jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false;AUTOCOMMIT=false", databaseName);

		properties.setDriverClass(this.driverClass);
		properties.setUrl(url);
		properties.setUsername("sa");
		properties.setPassword("");
	}

}
