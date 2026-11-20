/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.dao;

import org.jspecify.annotations.Nullable;

/**
 * Exception thrown on incorrect usage of the API, such as failing to
 * "compile" a query object that needed compilation before execution.
 *
 * <p>This represents a problem in our Java data access framework,
 * not the underlying data access infrastructure.
 *
 * @author Rod Johnson
 */
@SuppressWarnings("serial")
public class InvalidDataAccessApiUsageException extends NonTransientDataAccessException {

	/**
	 * Constructor for InvalidDataAccessApiUsageException.
	 * @param msg the detail message
	 */
	public InvalidDataAccessApiUsageException(@Nullable String msg) {
		super(msg);
	}

	/**
	 * Constructor for InvalidDataAccessApiUsageException.
	 * @param msg the detail message
	 * @param cause the root cause from the data access API in use
	 */
	public InvalidDataAccessApiUsageException(@Nullable String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}

}
