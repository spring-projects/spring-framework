/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.core;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * One of the three central callback interfaces used by the JdbcTemplate class.
 * This interface creates a CallableStatement given a connection, provided
 * by the JdbcTemplate class. Implementations are responsible for providing
 * SQL and any necessary parameters.
 *
 * <p>Implementations <i>do not</i> need to concern themselves with
 * SQLExceptions that may be thrown from operations they attempt.
 * The JdbcTemplate class will catch and handle SQLExceptions appropriately.
 *
 * <p>A PreparedStatementCreator should also implement the SqlProvider interface
 * if it is able to provide the SQL it uses for PreparedStatement creation.
 * This allows for better contextual information in case of exceptions.
 *
 * @author Rod Johnson
 * @author Thomas Risberg
 * @see JdbcTemplate#execute(CallableStatementCreator, CallableStatementCallback)
 * @see JdbcTemplate#call
 * @see SqlProvider
 */
@FunctionalInterface
public interface CallableStatementCreator {

	/**
	 * Create a callable statement in this connection. Allows implementations to use
	 * CallableStatements.
	 * @param con the Connection to use to create statement
	 * @return a callable statement
	 * @throws SQLException there is no need to catch SQLExceptions
	 * that may be thrown in the implementation of this method.
	 * The JdbcTemplate class will handle them.
	 */
	CallableStatement createCallableStatement(Connection con) throws SQLException;

}
