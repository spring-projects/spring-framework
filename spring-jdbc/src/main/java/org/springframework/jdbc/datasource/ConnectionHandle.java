/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.datasource;

import java.sql.Connection;

/**
 * Simple interface to be implemented by handles for a JDBC Connection.
 * Used by JpaDialect, for example.
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see SimpleConnectionHandle
 * @see ConnectionHolder
 */
@FunctionalInterface
public interface ConnectionHandle {

	/**
	 * Fetch the JDBC Connection that this handle refers to.
	 */
	Connection getConnection();

	/**
	 * Release the JDBC Connection that this handle refers to.
	 * <p>The default implementation is empty, assuming that the lifecycle
	 * of the connection is managed externally.
	 * @param con the JDBC Connection to release
	 */
	default void releaseConnection(Connection con) {
	}

}
