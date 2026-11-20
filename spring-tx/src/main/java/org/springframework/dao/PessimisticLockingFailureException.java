/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.dao;

import org.jspecify.annotations.Nullable;

/**
 * Exception thrown on a pessimistic locking violation.
 * Thrown by Spring's SQLException translation mechanism
 * if a corresponding database error is encountered.
 *
 * <p>Serves as a superclass for more specific exceptions, for example,
 * {@link CannotAcquireLockException}. However, it is generally
 * recommended to handle {@code PessimisticLockingFailureException}
 * itself instead of relying on specific exception subclasses.
 *
 * @author Thomas Risberg
 * @since 1.2
 * @see OptimisticLockingFailureException
 */
@SuppressWarnings("serial")
public class PessimisticLockingFailureException extends ConcurrencyFailureException {

	/**
	 * Constructor for PessimisticLockingFailureException.
	 * @param msg the detail message
	 */
	public PessimisticLockingFailureException(@Nullable String msg) {
		super(msg);
	}

	/**
	 * Constructor for PessimisticLockingFailureException.
	 * @param msg the detail message
	 * @param cause the root cause from the data access API in use
	 */
	public PessimisticLockingFailureException(@Nullable String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}

}
