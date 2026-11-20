/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.datasource.init;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Strategy used to populate, initialize, or clean up a database.
 *
 * @author Keith Donald
 * @author Sam Brannen
 * @since 3.0
 * @see ResourceDatabasePopulator
 * @see DatabasePopulatorUtils
 * @see DataSourceInitializer
 */
@FunctionalInterface
public interface DatabasePopulator {

	/**
	 * Populate, initialize, or clean up the database using the provided JDBC
	 * connection.
	 * <p><strong>Warning</strong>: Concrete implementations should not close
	 * the provided {@link Connection}.
	 * <p>Concrete implementations <em>may</em> throw an {@link SQLException} if
	 * an error is encountered but are <em>strongly encouraged</em> to throw a
	 * specific {@link ScriptException} instead. For example, Spring's
	 * {@link ResourceDatabasePopulator} and {@link DatabasePopulatorUtils} wrap
	 * all {@code SQLExceptions} in {@code ScriptExceptions}.
	 * @param connection the JDBC connection to use; already configured and
	 * ready to use; never {@code null}
	 * @throws SQLException if an unrecoverable data access exception occurs
	 * while interacting with the database
	 * @throws ScriptException in all other error cases
	 * @see DatabasePopulatorUtils#execute
	 */
	void populate(Connection connection) throws SQLException, ScriptException;

}
