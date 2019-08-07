/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.jdbc.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * One of the two central callback interfaces used by the JdbcTemplate class.
 * This interface creates a PreparedStatement given a connection, provided
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
 * @see JdbcTemplate#execute(PreparedStatementCreator, PreparedStatementCallback)
 * @see JdbcTemplate#query(PreparedStatementCreator, RowCallbackHandler)
 * @see JdbcTemplate#update(PreparedStatementCreator)
 * @see SqlProvider
 */
@FunctionalInterface
public interface PreparedStatementCreator {

	/**
	 * Create a statement in this connection. Allows implementations to use
	 * PreparedStatements. The JdbcTemplate will close the created statement.
	 * @param con Connection to use to create statement
	 * @return a prepared statement
	 * @throws SQLException there is no need to catch SQLExceptions
	 * that may be thrown in the implementation of this method.
	 * The JdbcTemplate class will handle them.
	 */
	PreparedStatement createPreparedStatement(Connection con) throws SQLException;

}
