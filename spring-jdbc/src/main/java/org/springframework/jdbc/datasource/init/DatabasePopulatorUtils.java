/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.datasource.init;

import java.sql.Connection;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.util.Assert;

/**
 * Utility methods for executing a {@link DatabasePopulator}.
 *
 * @author Juergen Hoeller
 * @author Oliver Gierke
 * @author Sam Brannen
 * @since 3.1
 */
public abstract class DatabasePopulatorUtils {

	/**
	 * Execute the given {@link DatabasePopulator} against the given {@link DataSource}.
	 * <p>The {@link Connection} for the supplied {@code DataSource} will be
	 * {@linkplain Connection#commit() committed} if it is not configured for
	 * {@link Connection#getAutoCommit() auto-commit} and is not
	 * {@linkplain DataSourceUtils#isConnectionTransactional transactional}.
	 * @param populator the {@code DatabasePopulator} to execute
	 * @param dataSource the {@code DataSource} to execute against
	 * @throws DataAccessException if an error occurs, specifically a {@link ScriptException}
	 * @see DataSourceUtils#isConnectionTransactional(Connection, DataSource)
	 */
	public static void execute(DatabasePopulator populator, DataSource dataSource) throws DataAccessException {
		Assert.notNull(populator, "DatabasePopulator must not be null");
		Assert.notNull(dataSource, "DataSource must not be null");
		try {
			Connection connection = DataSourceUtils.getConnection(dataSource);
			try {
				populator.populate(connection);
				if (!connection.getAutoCommit() && !DataSourceUtils.isConnectionTransactional(connection, dataSource)) {
					connection.commit();
				}
			}
			finally {
				DataSourceUtils.releaseConnection(connection, dataSource);
			}
		}
		catch (ScriptException ex) {
			throw ex;
		}
		catch (Throwable ex) {
			throw new UncategorizedScriptException("Failed to execute database script", ex);
		}
	}

}
