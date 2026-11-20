/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.dao;

import org.jspecify.annotations.Nullable;

/**
 * Exception thrown when an attempt to insert or update data
 * results in violation of a primary key or unique constraint.
 * Note that this is not necessarily a purely relational concept;
 * unique primary keys are required by most database types.
 *
 * <p>Consider handling the general {@link DataIntegrityViolationException}
 * instead, semantically including a wider range of constraint violations.
 *
 * @author Thomas Risberg
 */
@SuppressWarnings("serial")
public class DuplicateKeyException extends DataIntegrityViolationException {

	/**
	 * Constructor for DuplicateKeyException.
	 * @param msg the detail message
	 */
	public DuplicateKeyException(@Nullable String msg) {
		super(msg);
	}

	/**
	 * Constructor for DuplicateKeyException.
	 * @param msg the detail message
	 * @param cause the root cause from the data access API in use
	 */
	public DuplicateKeyException(@Nullable String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}

}
