/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.core;

/**
 * Represents a returned {@link java.sql.ResultSet} from a stored procedure call.
 *
 * <p>A {@link ResultSetExtractor}, {@link RowCallbackHandler} or {@link RowMapper}
 * must be provided to handle any returned rows.
 *
 * <p>Returned {@link java.sql.ResultSet ResultSets} - like all stored procedure
 * parameters - must have names.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 */
public class SqlReturnResultSet extends ResultSetSupportingSqlParameter {

	/**
	 * Create a new instance of the {@link SqlReturnResultSet} class.
	 * @param name the name of the parameter, as used in input and output maps
	 * @param extractor the {@link ResultSetExtractor} to use for parsing the {@link java.sql.ResultSet}
	 */
	public SqlReturnResultSet(String name, ResultSetExtractor<?> extractor) {
		super(name, 0, extractor);
	}

	/**
	 * Create a new instance of the {@link SqlReturnResultSet} class.
	 * @param name the name of the parameter, as used in input and output maps
	 * @param handler the {@link RowCallbackHandler} to use for parsing the {@link java.sql.ResultSet}
	 */
	public SqlReturnResultSet(String name, RowCallbackHandler handler) {
		super(name, 0, handler);
	}

	/**
	 * Create a new instance of the {@link SqlReturnResultSet} class.
	 * @param name the name of the parameter, as used in input and output maps
	 * @param mapper the {@link RowMapper} to use for parsing the {@link java.sql.ResultSet}
	 */
	public SqlReturnResultSet(String name, RowMapper<?> mapper) {
		super(name, 0, mapper);
	}


	/**
	 * This implementation always returns {@code true}.
	 */
	@Override
	public boolean isResultsParameter() {
		return true;
	}

}
