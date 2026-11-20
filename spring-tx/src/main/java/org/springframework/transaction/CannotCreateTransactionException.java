/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction;

/**
 * Exception thrown when a transaction can't be created using an
 * underlying transaction API such as JTA.
 *
 * @author Rod Johnson
 * @since 17.03.2003
 */
@SuppressWarnings("serial")
public class CannotCreateTransactionException extends TransactionException {

	/**
	 * Constructor for CannotCreateTransactionException.
	 * @param msg the detail message
	 */
	public CannotCreateTransactionException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for CannotCreateTransactionException.
	 * @param msg the detail message
	 * @param cause the root cause from the transaction API in use
	 */
	public CannotCreateTransactionException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
