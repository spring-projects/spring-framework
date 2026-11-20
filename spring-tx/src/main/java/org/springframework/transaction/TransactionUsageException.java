/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction;

/**
 * Superclass for exceptions caused by inappropriate usage of
 * a Spring transaction API.
 *
 * @author Rod Johnson
 * @since 22.03.2003
 */
@SuppressWarnings("serial")
public class TransactionUsageException extends TransactionException {

	/**
	 * Constructor for TransactionUsageException.
	 * @param msg the detail message
	 */
	public TransactionUsageException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for TransactionUsageException.
	 * @param msg the detail message
	 * @param cause the root cause from the transaction API in use
	 */
	public TransactionUsageException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
