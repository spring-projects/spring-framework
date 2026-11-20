/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.support;

import org.springframework.core.NestedCheckedException;

/**
 * Exception indicating that something went wrong during JDBC meta-data lookup.
 *
 * <p>This is a checked exception since we want it to be caught, logged and
 * handled rather than cause the application to fail. Failure to read JDBC
 * meta-data is usually not a fatal problem.
 *
 * @author Thomas Risberg
 * @since 1.0.1
 */
@SuppressWarnings("serial")
public class MetaDataAccessException extends NestedCheckedException {

	/**
	 * Constructor for MetaDataAccessException.
	 * @param msg the detail message
	 */
	public MetaDataAccessException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for MetaDataAccessException.
	 * @param msg the detail message
	 * @param cause the root cause from the data access API in use
	 */
	public MetaDataAccessException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
