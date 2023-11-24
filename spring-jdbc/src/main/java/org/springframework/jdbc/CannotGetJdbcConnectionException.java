/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.jdbc;

import java.sql.SQLException;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.lang.Nullable;

/**
 * Fatal exception thrown when we can't connect to an RDBMS using JDBC.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class CannotGetJdbcConnectionException extends DataAccessResourceFailureException {

	/**
	 * Constructor for {@code CannotGetJdbcConnectionException}.
	 * @param msg the detail message
	 * @since 5.0
	 */
	public CannotGetJdbcConnectionException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for {@code CannotGetJdbcConnectionException}.
	 * @param msg the detail message
	 * @param ex the root cause SQLException
	 */
	public CannotGetJdbcConnectionException(String msg, @Nullable SQLException ex) {
		super(msg, ex);
	}

	/**
	 * Constructor for {@code CannotGetJdbcConnectionException}.
	 * @param msg the detail message
	 * @param ex the root cause IllegalStateException
	 * @since 5.3.22
	 */
	public CannotGetJdbcConnectionException(String msg, IllegalStateException ex) {
		super(msg, ex);
	}

}
