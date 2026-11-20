/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction;

import org.springframework.core.NestedRuntimeException;

/**
 * Superclass for all transaction exceptions.
 *
 * @author Rod Johnson
 * @since 17.03.2003
 */
@SuppressWarnings("serial")
public abstract class TransactionException extends NestedRuntimeException {

	/**
	 * Constructor for TransactionException.
	 * @param msg the detail message
	 */
	public TransactionException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for TransactionException.
	 * @param msg the detail message
	 * @param cause the root cause from the transaction API in use
	 */
	public TransactionException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
