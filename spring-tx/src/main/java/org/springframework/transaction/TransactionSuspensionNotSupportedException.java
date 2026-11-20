/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction;

/**
 * Exception thrown when attempting to suspend an existing transaction
 * but transaction suspension is not supported by the underlying backend.
 *
 * @author Juergen Hoeller
 * @since 1.1
 */
@SuppressWarnings("serial")
public class TransactionSuspensionNotSupportedException extends CannotCreateTransactionException {

	/**
	 * Constructor for TransactionSuspensionNotSupportedException.
	 * @param msg the detail message
	 */
	public TransactionSuspensionNotSupportedException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for TransactionSuspensionNotSupportedException.
	 * @param msg the detail message
	 * @param cause the root cause from the transaction API in use
	 */
	public TransactionSuspensionNotSupportedException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
