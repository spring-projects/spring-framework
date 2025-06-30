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

package org.springframework.dao;

import org.jspecify.annotations.Nullable;

/**
 * Root of the hierarchy of data access exceptions that are considered transient -
 * where a previously failed operation might be able to succeed when the operation
 * is retried without any intervention by application-level functionality.
 *
 * @author Thomas Risberg
 * @since 2.5
 * @see java.sql.SQLTransientException
 */
@SuppressWarnings("serial")
public abstract class TransientDataAccessException extends DataAccessException {

	/**
	 * Constructor for TransientDataAccessException.
	 * @param msg the detail message
	 */
	public TransientDataAccessException(@Nullable String msg) {
		super(msg);
	}

	/**
	 * Constructor for TransientDataAccessException.
	 * @param msg the detail message
	 * @param cause the root cause (usually from using an underlying
	 * data access API such as JDBC)
	 */
	public TransientDataAccessException(@Nullable String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}

}
