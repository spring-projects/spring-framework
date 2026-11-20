/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.datasource;

import java.sql.Connection;

import org.springframework.util.Assert;

/**
 * Simple implementation of the {@link ConnectionHandle} interface,
 * containing a given JDBC Connection.
 *
 * @author Juergen Hoeller
 * @since 1.1
 */
public class SimpleConnectionHandle implements ConnectionHandle {

	private final Connection connection;


	/**
	 * Create a new SimpleConnectionHandle for the given Connection.
	 * @param connection the JDBC Connection
	 */
	public SimpleConnectionHandle(Connection connection) {
		Assert.notNull(connection, "Connection must not be null");
		this.connection = connection;
	}

	/**
	 * Return the specified Connection as-is.
	 */
	@Override
	public Connection getConnection() {
		return this.connection;
	}


	@Override
	public String toString() {
		return "SimpleConnectionHandle: " + this.connection;
	}

}
