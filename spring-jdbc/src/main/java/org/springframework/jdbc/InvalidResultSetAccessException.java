/*
 * Copyright 2002-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jdbc;

import java.sql.SQLException;

import org.springframework.dao.InvalidDataAccessResourceUsageException;

/**
 * Exception thrown when a ResultSet has been accessed in an invalid fashion.
 * Such exceptions always have a <code>java.sql.SQLException</code> root cause.
 *
 * <p>This typically happens when an invalid ResultSet column index or name
 * has been specified. Also thrown by disconnected SqlRowSets.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see BadSqlGrammarException
 * @see org.springframework.jdbc.support.rowset.SqlRowSet
 */
public class InvalidResultSetAccessException extends InvalidDataAccessResourceUsageException {

	private String sql;


	/**
	 * Constructor for InvalidResultSetAccessException.
	 * @param task name of current task
	 * @param sql the offending SQL statement
	 * @param ex the root cause
	 */
	public InvalidResultSetAccessException(String task, String sql, SQLException ex) {
		super(task + "; invalid ResultSet access for SQL [" + sql + "]", ex);
		this.sql = sql;
	}

	/**
	 * Constructor for InvalidResultSetAccessException.
	 * @param ex the root cause
	 */
	public InvalidResultSetAccessException(SQLException ex) {
		super(ex.getMessage(), ex);
	}


	/**
	 * Return the wrapped SQLException.
	 */
	public SQLException getSQLException() {
		return (SQLException) getCause();
	}

	/**
	 * Return the SQL that caused the problem.
	 * @return the offending SQL, if known
	 */
	public String getSql() {
		return this.sql;
	}

}
