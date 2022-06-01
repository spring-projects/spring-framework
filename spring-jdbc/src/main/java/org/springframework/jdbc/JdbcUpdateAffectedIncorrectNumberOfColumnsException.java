package org.springframework.jdbc;

import org.springframework.dao.IncorrectUpdateSemanticsDataAccessException;
/**
 * Exception thrown when a JDBC update affects an unexpected number of columns.
 * Typically we expect an update to affect a single column, meaning it's an
 * error if it affects multiple columns.
 *
 * @author Peng Yin
 */
@SuppressWarnings("serial")
public class JdbcUpdateAffectedIncorrectNumberOfColumnsException extends IncorrectUpdateSemanticsDataAccessException{
	/** Number of columns that should have been affected. */
	private final int expected;

	/** Number of columns that actually were affected. */
	private final int actual;

	/**
	 * Constructor for JdbcUpdateAffectedIncorrectNumberOfColumnsException.
	 * @param sql the SQL we were trying to execute
	 * @param expected the expected number of columns affected
	 * @param actual the actual number of columns affected
	 */
	public JdbcUpdateAffectedIncorrectNumberOfColumnsException(String sql, int expected, int actual) {
		super("SQL update '" + sql + "' affected " + actual + " columns, not " + expected + " as expected");
		this.expected = expected;
		this.actual = actual;
	}

	/**
	 * Return the number of columns that should have been affected.
	 */
	public int getExpectedColumnsAffected() {
		return this.expected;
	}

	/**
	 * Return the number of columns that have actually been affected.
	 */
	public int getActualColumnsAffected() {
		return this.actual;
	}

	@Override
	public boolean wasDataUpdated() {
		return (getActualColumnsAffected() > 0);
	}

}
