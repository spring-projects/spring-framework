/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.dao;

import org.jspecify.annotations.Nullable;

/**
 * Exception thrown on various data access concurrency failures.
 *
 * <p>This exception provides subclasses for specific types of failure,
 * in particular optimistic locking versus pessimistic locking.
 *
 * @author Thomas Risberg
 * @since 1.1
 * @see OptimisticLockingFailureException
 * @see PessimisticLockingFailureException
 */
@SuppressWarnings("serial")
public class ConcurrencyFailureException extends TransientDataAccessException {

	/**
	 * Constructor for ConcurrencyFailureException.
	 * @param msg the detail message
	 */
	public ConcurrencyFailureException(@Nullable String msg) {
		super(msg);
	}

	/**
	 * Constructor for ConcurrencyFailureException.
	 * @param msg the detail message
	 * @param cause the root cause from the data access API in use
	 */
	public ConcurrencyFailureException(@Nullable String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}

}
