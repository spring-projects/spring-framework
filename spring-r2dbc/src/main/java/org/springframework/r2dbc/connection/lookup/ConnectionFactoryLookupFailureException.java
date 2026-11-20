/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.r2dbc.connection.lookup;

import org.springframework.dao.NonTransientDataAccessException;

/**
 * Exception to be thrown by a {@link ConnectionFactoryLookup} implementation,
 * indicating that the specified {@link io.r2dbc.spi.ConnectionFactory} could
 * not be obtained.
 *
 * @author Mark Paluch
 * @since 5.3
 */
@SuppressWarnings("serial")
public class ConnectionFactoryLookupFailureException extends NonTransientDataAccessException {

	/**
	 * Create a new {@code ConnectionFactoryLookupFailureException}.
	 * @param msg the detail message
	 */
	public ConnectionFactoryLookupFailureException(String msg) {
		super(msg);
	}

	/**
	 * Create a new {@code ConnectionFactoryLookupFailureException}.
	 * @param msg the detail message
	 * @param cause the root cause
	 */
	public ConnectionFactoryLookupFailureException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
