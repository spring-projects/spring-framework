/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction;

/**
 * Exception thrown when an operation is attempted that
 * relies on an existing transaction (such as setting
 * rollback status) and there is no existing transaction.
 * This represents an illegal usage of the transaction API.
 *
 * @author Rod Johnson
 * @since 17.03.2003
 */
@SuppressWarnings("serial")
public class NoTransactionException extends TransactionUsageException {

	/**
	 * Constructor for NoTransactionException.
	 * @param msg the detail message
	 */
	public NoTransactionException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for NoTransactionException.
	 * @param msg the detail message
	 * @param cause the root cause from the transaction API in use
	 */
	public NoTransactionException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
