/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.datasource.embedded;

/**
 * An {@link EmbeddedDatabaseConfigurer} delegate that can be used to customize
 * the embedded database.
 *
 * @author Stephane Nicoll
 * @since 6.2
 */
public class EmbeddedDatabaseConfigurerDelegate extends AbstractEmbeddedDatabaseConfigurer {

	private final EmbeddedDatabaseConfigurer target;

	public EmbeddedDatabaseConfigurerDelegate(EmbeddedDatabaseConfigurer target) {
		this.target = target;
	}

	@Override
	public void configureConnectionProperties(ConnectionProperties properties, String databaseName) {
		this.target.configureConnectionProperties(properties, databaseName);
	}

}
