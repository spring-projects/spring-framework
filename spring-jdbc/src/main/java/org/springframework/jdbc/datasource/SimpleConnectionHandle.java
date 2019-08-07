/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

	/**
	 * This implementation is empty, as we're using a standard
	 * Connection handle that does not have to be released.
	 */
	@Override
	public void releaseConnection(Connection con) {
	}


	@Override
	public String toString() {
		return "SimpleConnectionHandle: " + this.connection;
	}

}
