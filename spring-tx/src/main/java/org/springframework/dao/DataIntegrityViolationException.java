/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.dao;

import org.jspecify.annotations.Nullable;

/**
 * Exception thrown when an attempt to execute an SQL statement fails to map
 * the given data, typically but no limited to an insert or update data
 * results in violation of an integrity constraint. Note that this
 * is not purely a relational concept; integrity constraints such
 * as unique primary keys are required by most database types.
 *
 * <p>Serves as a superclass for more specific exceptions, for example,
 * {@link DuplicateKeyException}. However, it is generally
 * recommended to handle {@code DataIntegrityViolationException}
 * itself instead of relying on specific exception subclasses.
 *
 * @author Rod Johnson
 */
@SuppressWarnings("serial")
public class DataIntegrityViolationException extends NonTransientDataAccessException {

	/**
	 * Constructor for DataIntegrityViolationException.
	 * @param msg the detail message
	 */
	public DataIntegrityViolationException(@Nullable String msg) {
		super(msg);
	}

	/**
	 * Constructor for DataIntegrityViolationException.
	 * @param msg the detail message
	 * @param cause the root cause from the data access API in use
	 */
	public DataIntegrityViolationException(@Nullable String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}

}
