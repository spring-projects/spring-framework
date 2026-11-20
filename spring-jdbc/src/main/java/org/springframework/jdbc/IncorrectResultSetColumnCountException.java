/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc;

import org.springframework.dao.DataRetrievalFailureException;

/**
 * Data access exception thrown when a result set did not have the correct column count,
 * for example when expecting a single column but getting 0 or more than 1 column.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.dao.IncorrectResultSizeDataAccessException
 */
@SuppressWarnings("serial")
public class IncorrectResultSetColumnCountException extends DataRetrievalFailureException {

	private final int expectedCount;

	private final int actualCount;


	/**
	 * Constructor for IncorrectResultSetColumnCountException.
	 * @param expectedCount the expected column count
	 * @param actualCount the actual column count
	 */
	public IncorrectResultSetColumnCountException(int expectedCount, int actualCount) {
		super("Incorrect column count: expected " + expectedCount + ", actual " + actualCount);
		this.expectedCount = expectedCount;
		this.actualCount = actualCount;
	}

	/**
	 * Constructor for IncorrectResultCountDataAccessException.
	 * @param msg the detail message
	 * @param expectedCount the expected column count
	 * @param actualCount the actual column count
	 */
	public IncorrectResultSetColumnCountException(String msg, int expectedCount, int actualCount) {
		super(msg);
		this.expectedCount = expectedCount;
		this.actualCount = actualCount;
	}


	/**
	 * Return the expected column count.
	 */
	public int getExpectedCount() {
		return this.expectedCount;
	}

	/**
	 * Return the actual column count.
	 */
	public int getActualCount() {
		return this.actualCount;
	}

}
