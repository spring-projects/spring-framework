/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc;

import org.springframework.dao.IncorrectUpdateSemanticsDataAccessException;

/**
 * Exception thrown when a JDBC update affects an unexpected number of rows.
 * Typically, we expect an update to affect a single row, meaning it is an
 * error if it affects multiple rows.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class JdbcUpdateAffectedIncorrectNumberOfRowsException extends IncorrectUpdateSemanticsDataAccessException {

	/** Number of rows that should have been affected. */
	private final int expected;

	/** Number of rows that actually were affected. */
	private final int actual;


	/**
	 * Constructor for JdbcUpdateAffectedIncorrectNumberOfRowsException.
	 * @param sql the SQL we were trying to execute
	 * @param expected the expected number of rows affected
	 * @param actual the actual number of rows affected
	 */
	public JdbcUpdateAffectedIncorrectNumberOfRowsException(String sql, int expected, int actual) {
		super("SQL update '" + sql + "' affected " + actual + " rows, not " + expected + " as expected");
		this.expected = expected;
		this.actual = actual;
	}


	/**
	 * Return the number of rows that should have been affected.
	 */
	public int getExpectedRowsAffected() {
		return this.expected;
	}

	/**
	 * Return the number of rows that have actually been affected.
	 */
	public int getActualRowsAffected() {
		return this.actual;
	}

	@Override
	public boolean wasDataUpdated() {
		return (getActualRowsAffected() > 0);
	}

}
