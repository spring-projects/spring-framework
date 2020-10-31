/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.jca.cci;

import java.sql.SQLException;

import org.springframework.dao.InvalidDataAccessResourceUsageException;

/**
 * Exception thrown when a ResultSet has been accessed in an invalid fashion.
 * Such exceptions always have a {@code java.sql.SQLException} root cause.
 *
 * <p>This typically happens when an invalid ResultSet column index or name
 * has been specified.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see javax.resource.cci.ResultSet
 * @deprecated as of 5.3, in favor of specific data access APIs
 * (or native CCI usage if there is no alternative)
 */
@Deprecated
@SuppressWarnings("serial")
public class InvalidResultSetAccessException extends InvalidDataAccessResourceUsageException {

	/**
	 * Constructor for InvalidResultSetAccessException.
	 * @param msg message
	 * @param ex the root cause
	 */
	public InvalidResultSetAccessException(String msg, SQLException ex) {
		super(ex.getMessage(), ex);
	}

}
