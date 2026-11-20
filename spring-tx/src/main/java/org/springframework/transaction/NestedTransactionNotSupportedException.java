/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction;

/**
 * Exception thrown when attempting to work with a nested transaction
 * but nested transactions are not supported by the underlying backend.
 *
 * @author Juergen Hoeller
 * @since 1.1
 */
@SuppressWarnings("serial")
public class NestedTransactionNotSupportedException extends CannotCreateTransactionException {

	/**
	 * Constructor for NestedTransactionNotSupportedException.
	 * @param msg the detail message
	 */
	public NestedTransactionNotSupportedException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for NestedTransactionNotSupportedException.
	 * @param msg the detail message
	 * @param cause the root cause from the transaction API in use
	 */
	public NestedTransactionNotSupportedException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
