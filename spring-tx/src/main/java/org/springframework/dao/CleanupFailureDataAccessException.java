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

package org.springframework.dao;

/**
 * Exception thrown when we couldn't clean up after a data access operation,
 * but the actual operation went OK.
 *
 * <p>For example, this exception or a subclass might be thrown if a JDBC
 * Connection couldn't be closed after it had been used successfully.
 *
 * <p>Note that data access code might perform resources cleanup in a
 * {@code finally} block and therefore log cleanup failure rather than rethrow it,
 * to keep the original data access exception, if any.
 *
 * @author Rod Johnson
 */
@SuppressWarnings("serial")
public class CleanupFailureDataAccessException extends NonTransientDataAccessException {

	/**
	 * Constructor for CleanupFailureDataAccessException.
	 * @param msg the detail message
	 * @param cause the root cause from the underlying data access API,
	 * such as JDBC
	 */
	public CleanupFailureDataAccessException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
