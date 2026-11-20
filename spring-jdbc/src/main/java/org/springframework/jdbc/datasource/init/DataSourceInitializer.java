/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.datasource.init;

import javax.sql.DataSource;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * Used to {@linkplain #setDatabasePopulator set up} a database during
 * initialization and {@link #setDatabaseCleaner clean up} a database during
 * destruction.
 *
 * @author Dave Syer
 * @author Sam Brannen
 * @since 3.0
 * @see DatabasePopulator
 */
public class DataSourceInitializer implements InitializingBean, DisposableBean {

	private @Nullable DataSource dataSource;

	private @Nullable DatabasePopulator databasePopulator;

	private @Nullable DatabasePopulator databaseCleaner;

	private boolean enabled = true;


	/**
	 * The {@link DataSource} for the database to populate when this component
	 * is initialized and to clean up when this component is shut down.
	 * <p>This property is mandatory with no default provided.
	 * @param dataSource the DataSource
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * Set the {@link DatabasePopulator} to execute during the bean initialization phase,
	 * if any.
	 * @param databasePopulator the {@code DatabasePopulator} to use during initialization
	 * @see #setDatabaseCleaner
	 */
	public void setDatabasePopulator(@Nullable DatabasePopulator databasePopulator) {
		this.databasePopulator = databasePopulator;
	}

	/**
	 * Set the {@link DatabasePopulator} to execute during the bean destruction phase,
	 * if any, cleaning up the database and leaving it in a known state for others.
	 * @param databaseCleaner the {@code DatabasePopulator} to use during destruction
	 * @see #setDatabasePopulator
	 */
	public void setDatabaseCleaner(@Nullable DatabasePopulator databaseCleaner) {
		this.databaseCleaner = databaseCleaner;
	}

	/**
	 * Flag to explicitly enable or disable the {@linkplain #setDatabasePopulator
	 * database populator} and {@linkplain #setDatabaseCleaner database cleaner}.
	 * @param enabled {@code true} if the database populator and database cleaner
	 * should be called on startup and shutdown, respectively
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}


	/**
	 * Use the {@linkplain #setDatabasePopulator database populator} to set up
	 * the database.
	 */
	@Override
	public void afterPropertiesSet() {
		execute(this.databasePopulator);
	}

	/**
	 * Use the {@linkplain #setDatabaseCleaner database cleaner} to clean up the
	 * database.
	 */
	@Override
	public void destroy() {
		execute(this.databaseCleaner);
	}

	private void execute(@Nullable DatabasePopulator populator) {
		Assert.state(this.dataSource != null, "DataSource must be set");
		if (this.enabled && populator != null) {
			DatabasePopulatorUtils.execute(populator, this.dataSource);
		}
	}

}
