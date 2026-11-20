/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction;

/**
 * Exception to be thrown when a transaction has timed out.
 *
 * <p>Thrown by Spring's local transaction strategies if the deadline
 * for a transaction has been reached when an operation is attempted,
 * according to the timeout specified for the given transaction.
 *
 * <p>Beyond such checks before each transactional operation, Spring's
 * local transaction strategies will also pass appropriate timeout values
 * to resource operations (for example to JDBC Statements, letting the JDBC
 * driver respect the timeout). Such operations will usually throw native
 * resource exceptions (for example, JDBC SQLExceptions) if their operation
 * timeout has been exceeded, to be converted to Spring's DataAccessException
 * in the respective DAO (which might use Spring's JdbcTemplate, for example).
 *
 * <p>In a JTA environment, it is up to the JTA transaction coordinator
 * to apply transaction timeouts. Usually, the corresponding JTA-aware
 * connection pool will perform timeout checks and throw corresponding
 * native resource exceptions (for example, JDBC SQLExceptions).
 *
 * @author Juergen Hoeller
 * @since 1.1.5
 * @see org.springframework.transaction.support.ResourceHolderSupport#getTimeToLiveInMillis
 * @see java.sql.Statement#setQueryTimeout
 * @see java.sql.SQLException
 */
@SuppressWarnings("serial")
public class TransactionTimedOutException extends TransactionException {

	/**
	 * Constructor for TransactionTimedOutException.
	 * @param msg the detail message
	 */
	public TransactionTimedOutException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for TransactionTimedOutException.
	 * @param msg the detail message
	 * @param cause the root cause from the transaction API in use
	 */
	public TransactionTimedOutException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
