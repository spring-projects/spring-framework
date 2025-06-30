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
 * Exception thrown on failure to acquire a lock during an update,
 * for example during a "select for update" statement.
 *
 * <p>Consider handling the general {@link PessimisticLockingFailureException}
 * instead, semantically including a wider range of locking-related failures.
 *
 * @author Rod Johnson
 */
@SuppressWarnings("serial")
public class CannotAcquireLockException extends PessimisticLockingFailureException {

	/**
	 * Constructor for CannotAcquireLockException.
	 * @param msg the detail message
	 */
	public CannotAcquireLockException(@Nullable String msg) {
		super(msg);
	}

	/**
	 * Constructor for CannotAcquireLockException.
	 * @param msg the detail message
	 * @param cause the root cause from the data access API in use
	 */
	public CannotAcquireLockException(@Nullable String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}

}
