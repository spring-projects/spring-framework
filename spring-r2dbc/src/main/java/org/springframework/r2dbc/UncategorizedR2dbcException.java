/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.r2dbc;

import io.r2dbc.spi.R2dbcException;

import org.springframework.dao.UncategorizedDataAccessException;
import org.springframework.lang.Nullable;

/**
 * Exception thrown when we can't classify a {@link R2dbcException} into
 * one of our generic data access exceptions.
 *
 * @author Mark Paluch
 * @since 5.3
 */
@SuppressWarnings("serial")
public class UncategorizedR2dbcException extends UncategorizedDataAccessException {

	/** SQL that led to the problem. */
	@Nullable
	private final String sql;


	/**
	 * Constructor for {@code UncategorizedSQLException}.
	 * @param msg the detail message
	 * @param sql the offending SQL statement
	 * @param ex the exception thrown by underlying data access API
	 */
	public UncategorizedR2dbcException(String msg, @Nullable String sql, R2dbcException ex) {
		super(msg, ex);
		this.sql = sql;
	}


	/**
	 * Return the wrapped {@link R2dbcException}.
	 */
	@Nullable
	public R2dbcException getR2dbcException() {
		return (R2dbcException) getCause();
	}

	/**
	 * Return the SQL that led to the problem (if known).
	 */
	@Nullable
	public String getSql() {
		return this.sql;
	}

}
