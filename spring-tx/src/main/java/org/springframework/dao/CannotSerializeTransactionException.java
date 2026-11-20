/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.dao;

/**
 * Exception thrown on failure to complete a transaction in serialized mode
 * due to update conflicts.
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
public class CannotSerializeTransactionException extends PessimisticLockingFailureException {

	/**
	 * Constructor for CannotSerializeTransactionException.
	 * @param msg the detail message
	 */
	public CannotSerializeTransactionException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for CannotSerializeTransactionException.
	 * @param msg the detail message
	 * @param cause the root cause from the data access API in use
	 */
	public CannotSerializeTransactionException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
