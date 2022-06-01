package org.springframework.jdbc;

import org.springframework.dao.DataRetrievalFailureException;

/**
 * Data access exception thrown when a result set did not have the correct row count,
 * for example when expecting a single row but getting 0 or more than 1 rows.
 *
 * @author Peng Yin
 * @since 2.0
 * @see org.springframework.dao.IncorrectResultSizeDataAccessException
 */

@SuppressWarnings("serial")
public class IncorrectResultSetRowCountException extends DataRetrievalFailureException{

	private final int expectedCount;

	private final int actualCount;

	/**
	 * Constructor for IncorrectResultSetRowCountException.
	 * @param expectedCount the expected row count
	 * @param actualCount the actual row count
	 */
	public IncorrectResultSetRowCountException(int expectedCount, int actualCount) {
		super("Incorrect row count: expected " + expectedCount + ", actual " + actualCount);
		this.expectedCount = expectedCount;
		this.actualCount = actualCount;
	}

	/**
	 * Constructor for IncorrectResultCountDataAccessException.
	 * @param msg the detail message
	 * @param expectedCount the expected row count
	 * @param actualCount the actual row count
	 */
	public IncorrectResultSetRowCountException(String msg, int expectedCount, int actualCount) {
		super(msg);
		this.expectedCount = expectedCount;
		this.actualCount = actualCount;
	}


	/**
	 * Return the expected row count.
	 */
	public int getExpectedCount() {
		return this.expectedCount;
	}

	/**
	 * Return the actual row count.
	 */
	public int getActualCount() {
		return this.actualCount;
	}

}
