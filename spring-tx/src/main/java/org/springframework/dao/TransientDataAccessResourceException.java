/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.dao;

/**
 * Data access exception thrown when a resource fails temporarily
 * and the operation can be retried.
 *
 * @author Thomas Risberg
 * @since 2.5
 * @see java.sql.SQLTransientConnectionException
 */
@SuppressWarnings("serial")
public class TransientDataAccessResourceException extends TransientDataAccessException {

	/**
	 * Constructor for TransientDataAccessResourceException.
	 * @param msg the detail message
	 */
	public TransientDataAccessResourceException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for TransientDataAccessResourceException.
	 * @param msg the detail message
	 * @param cause the root cause from the data access API in use
	 */
	public TransientDataAccessResourceException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
