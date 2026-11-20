/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.datasource.embedded;

import javax.sql.DataSource;

/**
 * {@code DataSourceFactory} encapsulates the creation of a particular
 * {@link DataSource} implementation such as a non-pooling
 * {@link org.springframework.jdbc.datasource.SimpleDriverDataSource}
 * or a HikariCP pool setup in the shape of a {@code HikariDataSource}.
 *
 * <p>Call {@link #getConnectionProperties()} to configure normalized
 * {@code DataSource} properties before calling {@link #getDataSource()}
 * to actually get the configured {@code DataSource} instance.
 *
 * @author Keith Donald
 * @author Sam Brannen
 * @since 3.0
 */
public interface DataSourceFactory {

	/**
	 * Get the {@linkplain ConnectionProperties connection properties}
	 * of the {@link #getDataSource DataSource} to be configured.
	 */
	ConnectionProperties getConnectionProperties();

	/**
	 * Get the {@link DataSource} with the
	 * {@linkplain #getConnectionProperties connection properties} applied.
	 */
	DataSource getDataSource();

}
