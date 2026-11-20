/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.dao;

import org.jspecify.annotations.Nullable;

/**
 * Exception thrown on failure to acquire a lock during an update,
 * for example during a "select for update" statement.
 *
 * <p>Consider handling the general {@link PessimisticLockingFailureException}
 * instead, semantically including a wider range of locking-related failures.
 *
 * @author Rod Johnson
 */
@SuppressWarnings("serial")
public class CannotAcquireLockException extends PessimisticLockingFailureException {

	/**
	 * Constructor for CannotAcquireLockException.
	 * @param msg the detail message
	 */
	public CannotAcquireLockException(@Nullable String msg) {
		super(msg);
	}

	/**
	 * Constructor for CannotAcquireLockException.
	 * @param msg the detail message
	 * @param cause the root cause from the data access API in use
	 */
	public CannotAcquireLockException(@Nullable String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}

}
