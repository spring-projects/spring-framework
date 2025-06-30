/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
