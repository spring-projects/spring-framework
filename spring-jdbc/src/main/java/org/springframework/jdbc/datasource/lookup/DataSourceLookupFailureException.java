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

package org.springframework.jdbc.datasource.lookup;

import org.springframework.dao.NonTransientDataAccessException;

/**
 * Exception to be thrown by a DataSourceLookup implementation,
 * indicating that the specified DataSource could not be obtained.
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("serial")
public class DataSourceLookupFailureException extends NonTransientDataAccessException {

	/**
	 * Constructor for DataSourceLookupFailureException.
	 * @param msg the detail message
	 */
	public DataSourceLookupFailureException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for DataSourceLookupFailureException.
	 * @param msg the detail message
	 * @param cause the root cause (usually from using an underlying
	 * lookup API such as JNDI)
	 */
	public DataSourceLookupFailureException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
