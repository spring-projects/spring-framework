/*
 * Copyright 2002-2023 the original author or authors.
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

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * An interface used by {@link JdbcTemplate} and
 * {@link org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
 * NamedParameterJdbcTemplate} for processing rows of a
 * {@link java.sql.ResultSet} on a per-row basis. Implementations of
 * this interface perform the actual work of processing each row
 * but don't need to worry about exception handling.
 * {@link java.sql.SQLException SQLExceptions} will be caught and handled
 * by the calling {@code JdbcTemplate} or {@code NamedParameterJdbcTemplate}.
 *
 * <p>In contrast to a {@link ResultSetExtractor}, a {@code RowCallbackHandler}
 * object is typically stateful: it keeps the result state within the
 * object, to be available for later inspection. See
 * {@link RowCountCallbackHandler} for a usage example.
 *
 * <p>Consider using a {@link RowMapper} instead if you need to map
 * exactly one result object per row, assembling them into a List.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see JdbcTemplate
 * @see org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate NamedParameterJdbcTemplate
 * @see RowMapper
 * @see ResultSetExtractor
 * @see RowCountCallbackHandler
 */
@FunctionalInterface
public interface RowCallbackHandler {

	/**
	 * Implementations must implement this method to process each row of data
	 * in the {@link ResultSet}. This method should not call {@code next()} on
	 * the {@code ResultSet}; it is only supposed to extract values of the current
	 * row.
	 * <p>Exactly what the implementation chooses to do is up to it:
	 * a trivial implementation might simply count rows, while another
	 * implementation might build an XML document.
	 * @param rs the {@code ResultSet} to process (pre-initialized for the current row)
	 * @throws SQLException if an {@code SQLException} is encountered getting
	 * column values (that is, there's no need to catch {@code SQLException})
	 */
	void processRow(ResultSet rs) throws SQLException;

}
