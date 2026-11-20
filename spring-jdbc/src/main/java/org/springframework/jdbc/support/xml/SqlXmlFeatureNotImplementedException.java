/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.support.xml;

import java.sql.Connection;
import java.sql.ResultSet;

import org.springframework.dao.InvalidDataAccessApiUsageException;

/**
 * Exception thrown when the underlying implementation does not support the
 * requested feature of the API.
 *
 * @author Thomas Risberg
 * @since 2.5.5
 * @deprecated as of 6.2, in favor of direct {@link ResultSet#getSQLXML} and
 * {@link Connection#createSQLXML()} usage, possibly in combination with a
 * custom {@link org.springframework.jdbc.support.SqlValue} implementation
 */
@Deprecated(since = "6.2")
@SuppressWarnings("serial")
public class SqlXmlFeatureNotImplementedException extends InvalidDataAccessApiUsageException {

	/**
	 * Constructor for SqlXmlFeatureNotImplementedException.
	 * @param msg the detail message
	 */
	public SqlXmlFeatureNotImplementedException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for SqlXmlFeatureNotImplementedException.
	 * @param msg the detail message
	 * @param cause the root cause from the data access API in use
	 */
	public SqlXmlFeatureNotImplementedException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
