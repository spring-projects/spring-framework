/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.r2dbc.connection.init;

import io.r2dbc.spi.ConnectionFactory;
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
 * @author Mark Paluch
 * @since 5.3
 * @see DatabasePopulator
 */
public class ConnectionFactoryInitializer implements InitializingBean, DisposableBean {

	private @Nullable ConnectionFactory connectionFactory;

	private @Nullable DatabasePopulator databasePopulator;

	private @Nullable DatabasePopulator databaseCleaner;

	private boolean enabled = true;


	/**
	 * The {@link ConnectionFactory} for the database to populate when this
	 * component is initialized and to clean up when this component is shut down.
	 * <p>This property is mandatory with no default provided.
	 * @param connectionFactory the R2DBC {@link ConnectionFactory}
	 */
	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	/**
	 * Set the {@link DatabasePopulator} to execute during the bean initialization phase.
	 * @param databasePopulator the {@link DatabasePopulator} to use during initialization
	 * @see #setDatabaseCleaner
	 */
	public void setDatabasePopulator(DatabasePopulator databasePopulator) {
		this.databasePopulator = databasePopulator;
	}

	/**
	 * Set the {@link DatabasePopulator} to execute during the bean destruction
	 * phase, cleaning up the database and leaving it in a known state for others.
	 * @param databaseCleaner the {@code DatabasePopulator} to use during destruction
	 * @see #setDatabasePopulator
	 */
	public void setDatabaseCleaner(DatabasePopulator databaseCleaner) {
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
		Assert.state(this.connectionFactory != null, "ConnectionFactory must be set");
		if (this.enabled && populator != null) {
			populator.populate(this.connectionFactory).block();
		}
	}

}
