/*
 * Copyright 2002-2022 the original author or authors.
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

import org.springframework.lang.Nullable;

/**
 * Exception thrown on various data access concurrency failures.
 *
 * <p>This exception provides subclasses for specific types of failure,
 * in particular optimistic locking versus pessimistic locking.
 *
 * @author Thomas Risberg
 * @since 1.1
 * @see OptimisticLockingFailureException
 * @see PessimisticLockingFailureException
 */
@SuppressWarnings("serial")
public class ConcurrencyFailureException extends TransientDataAccessException {

	/**
	 * Constructor for ConcurrencyFailureException.
	 * @param msg the detail message
	 */
	public ConcurrencyFailureException(@Nullable String msg) {
		super(msg);
	}

	/**
	 * Constructor for ConcurrencyFailureException.
	 * @param msg the detail message
	 * @param cause the root cause from the data access API in use
	 */
	public ConcurrencyFailureException(@Nullable String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}

}
