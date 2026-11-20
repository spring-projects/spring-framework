/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.dao;

import org.jspecify.annotations.Nullable;

/**
 * Data access exception thrown when a resource fails completely:
 * for example, if we can't connect to a database using JDBC.
 *
 * @author Rod Johnson
 * @author Thomas Risberg
 */
@SuppressWarnings("serial")
public class DataAccessResourceFailureException extends NonTransientDataAccessResourceException {

	/**
	 * Constructor for DataAccessResourceFailureException.
	 * @param msg the detail message
	 */
	public DataAccessResourceFailureException(@Nullable String msg) {
		super(msg);
	}

	/**
	 * Constructor for DataAccessResourceFailureException.
	 * @param msg the detail message
	 * @param cause the root cause from the data access API in use
	 */
	public DataAccessResourceFailureException(@Nullable String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}

}
