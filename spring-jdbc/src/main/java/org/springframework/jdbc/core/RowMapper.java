/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.core;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jspecify.annotations.Nullable;

/**
 * An interface used by {@link JdbcTemplate} for mapping rows of a
 * {@link java.sql.ResultSet} on a per-row basis. Implementations of this
 * interface perform the actual work of mapping each row to a result object
 * but don't need to worry about exception handling.
 * {@link java.sql.SQLException SQLExceptions} will be caught and handled
 * by the calling {@code JdbcTemplate}.
 *
 * <p>Typically used either for {@code JdbcTemplate}'s query methods or for
 * {@code out} parameters of stored procedures. {@code RowMapper} objects are
 * typically stateless and thus reusable; they are an ideal choice for
 * implementing row-mapping logic in a single place.
 *
 * <p>Alternatively, consider subclassing
 * {@link org.springframework.jdbc.object.MappingSqlQuery} from the
 * {@code jdbc.object} package: instead of working with separate
 * {@code JdbcTemplate} and {@code RowMapper} objects, you can build executable
 * query objects (containing row-mapping logic) in that style.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @param <T> the result type
 * @see JdbcTemplate
 * @see RowCallbackHandler
 * @see ResultSetExtractor
 * @see org.springframework.jdbc.object.MappingSqlQuery
 */
@FunctionalInterface
public interface RowMapper<T extends @Nullable Object> {

	/**
	 * Implementations must implement this method to map each row of data in the
	 * {@code ResultSet}. This method should not call {@code next()} on the
	 * {@code ResultSet}; it is only supposed to map values of the current row.
	 * @param rs the {@code ResultSet} to map (pre-initialized for the current row)
	 * @param rowNum the number of the current row
	 * @return the result object for the current row (may be {@code null})
	 * @throws SQLException if an SQLException is encountered while getting
	 * column values (that is, there's no need to catch SQLException)
	 */
	T mapRow(ResultSet rs, int rowNum) throws SQLException;

}
