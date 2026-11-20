/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.dao;

import org.jspecify.annotations.Nullable;

/**
 * Exception thrown on mismatch between Java type and database type:
 * for example on an attempt to set an object of the wrong type
 * in an RDBMS column.
 *
 * @author Rod Johnson
 */
@SuppressWarnings("serial")
public class TypeMismatchDataAccessException extends InvalidDataAccessResourceUsageException {

	/**
	 * Constructor for TypeMismatchDataAccessException.
	 * @param msg the detail message
	 */
	public TypeMismatchDataAccessException(@Nullable String msg) {
		super(msg);
	}

	/**
	 * Constructor for TypeMismatchDataAccessException.
	 * @param msg the detail message
	 * @param cause the root cause from the data access API in use
	 */
	public TypeMismatchDataAccessException(@Nullable String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}

}
