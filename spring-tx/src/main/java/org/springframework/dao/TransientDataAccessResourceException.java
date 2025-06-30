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

/**
 * Data access exception thrown when a resource fails temporarily
 * and the operation can be retried.
 *
 * @author Thomas Risberg
 * @since 2.5
 * @see java.sql.SQLTransientConnectionException
 */
@SuppressWarnings("serial")
public class TransientDataAccessResourceException extends TransientDataAccessException {

	/**
	 * Constructor for TransientDataAccessResourceException.
	 * @param msg the detail message
	 */
	public TransientDataAccessResourceException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for TransientDataAccessResourceException.
	 * @param msg the detail message
	 * @param cause the root cause from the data access API in use
	 */
	public TransientDataAccessResourceException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
