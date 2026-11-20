/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction;

/**
 * Exception that gets thrown when an invalid isolation level is specified,
 * i.e. an isolation level that the transaction manager implementation
 * doesn't support.
 *
 * @author Juergen Hoeller
 * @since 12.05.2003
 */
@SuppressWarnings("serial")
public class InvalidIsolationLevelException extends TransactionUsageException {

	/**
	 * Constructor for InvalidIsolationLevelException.
	 * @param msg the detail message
	 */
	public InvalidIsolationLevelException(String msg) {
		super(msg);
	}

}
