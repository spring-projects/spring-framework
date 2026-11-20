/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.dao;

/**
 * Generic exception thrown when the current process was
 * a deadlock loser, and its transaction rolled back.
 *
 * <p>Consider handling the general {@link PessimisticLockingFailureException}
 * instead, semantically including a wider range of locking-related failures.
 *
 * @author Rod Johnson
 * @deprecated as of 6.0.3, in favor of
 * {@link PessimisticLockingFailureException}/{@link CannotAcquireLockException}
 */
@Deprecated(since = "6.0.3")
@SuppressWarnings("serial")
public class DeadlockLoserDataAccessException extends PessimisticLockingFailureException {

	/**
	 * Constructor for DeadlockLoserDataAccessException.
	 * @param msg the detail message
	 * @param cause the root cause from the data access API in use
	 */
	public DeadlockLoserDataAccessException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
