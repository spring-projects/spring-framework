/*
 * Copyright 2002-present the original author or authors.
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

import java.sql.SQLWarning;

import org.jspecify.annotations.Nullable;

import org.springframework.dao.UncategorizedDataAccessException;

/**
 * Exception thrown when we're not ignoring {@link java.sql.SQLWarning SQLWarnings}.
 *
 * <p>If an SQLWarning is reported, the operation completed, so we will need
 * to explicitly roll it back if we're not happy when looking at the warning.
 * We might choose to ignore (and log) the warning, or to wrap and throw it
 * in the shape of this SQLWarningException instead.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.jdbc.core.JdbcTemplate#setIgnoreWarnings
 */
@SuppressWarnings("serial")
public class SQLWarningException extends UncategorizedDataAccessException {

	/**
	 * Constructor for SQLWarningException.
	 * @param msg the detail message
	 * @param ex the JDBC warning
	 */
	public SQLWarningException(String msg, SQLWarning ex) {
		super(msg, ex);
	}


	/**
	 * Return the underlying {@link SQLWarning}.
	 * @since 5.3.29
	 */
	public @Nullable SQLWarning getSQLWarning() {
		return (SQLWarning) getCause();
	}

	/**
	 * Return the underlying {@link SQLWarning}.
	 * @deprecated as of 5.3.29, in favor of {@link #getSQLWarning()}
	 */
	@Deprecated(since = "5.3.29")
	public @Nullable SQLWarning SQLWarning() {
		return getSQLWarning();
	}

}
