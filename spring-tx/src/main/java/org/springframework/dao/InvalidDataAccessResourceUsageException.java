/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.dao;

import org.jspecify.annotations.Nullable;

/**
 * Root for exceptions thrown when we use a data access resource incorrectly.
 * Thrown for example on specifying bad SQL when using a RDBMS.
 * Resource-specific subclasses are supplied by concrete data access packages.
 *
 * @author Rod Johnson
 */
@SuppressWarnings("serial")
public class InvalidDataAccessResourceUsageException extends NonTransientDataAccessException {

	/**
	 * Constructor for InvalidDataAccessResourceUsageException.
	 * @param msg the detail message
	 */
	public InvalidDataAccessResourceUsageException(@Nullable String msg) {
		super(msg);
	}

	/**
	 * Constructor for InvalidDataAccessResourceUsageException.
	 * @param msg the detail message
	 * @param cause the root cause from the data access API in use
	 */
	public InvalidDataAccessResourceUsageException(@Nullable String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}

}
