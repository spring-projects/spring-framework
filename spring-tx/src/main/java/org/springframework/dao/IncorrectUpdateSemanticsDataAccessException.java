/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.dao;

/**
 * Data access exception thrown when something unintended appears to have
 * happened with an update, but the transaction hasn't already been rolled back.
 * Thrown, for example, when we wanted to update 1 row in an RDBMS but actually
 * updated 3.
 *
 * @author Rod Johnson
 */
@SuppressWarnings("serial")
public class IncorrectUpdateSemanticsDataAccessException extends InvalidDataAccessResourceUsageException {

	/**
	 * Constructor for IncorrectUpdateSemanticsDataAccessException.
	 * @param msg the detail message
	 */
	public IncorrectUpdateSemanticsDataAccessException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for IncorrectUpdateSemanticsDataAccessException.
	 * @param msg the detail message
	 * @param cause the root cause from the underlying API, such as JDBC
	 */
	public IncorrectUpdateSemanticsDataAccessException(String msg, Throwable cause) {
		super(msg, cause);
	}


	/**
	 * Return whether data was updated.
	 * If this method returns {@code false}, there is nothing to roll back.
	 * <p>The default implementation always returns {@code true}.
	 * This can be overridden in subclasses.
	 */
	public boolean wasDataUpdated() {
		return true;
	}

}
