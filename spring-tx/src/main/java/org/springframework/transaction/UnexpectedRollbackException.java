/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction;

/**
 * Thrown when an attempt to commit a transaction resulted
 * in an unexpected rollback.
 *
 * @author Rod Johnson
 * @since 17.03.2003
 */
@SuppressWarnings("serial")
public class UnexpectedRollbackException extends TransactionException {

	/**
	 * Constructor for UnexpectedRollbackException.
	 * @param msg the detail message
	 */
	public UnexpectedRollbackException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for UnexpectedRollbackException.
	 * @param msg the detail message
	 * @param cause the root cause from the transaction API in use
	 */
	public UnexpectedRollbackException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
