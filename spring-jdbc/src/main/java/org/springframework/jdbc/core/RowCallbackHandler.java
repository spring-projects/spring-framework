/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
