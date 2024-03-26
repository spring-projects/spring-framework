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
 * Exception thrown on a pessimistic locking violation.
 * Thrown by Spring's SQLException translation mechanism
 * if a corresponding database error is encountered.
 *
 * <p>Serves as a superclass for more specific exceptions, e.g.
 * {@link CannotAcquireLockException}. However, it is generally
 * recommended to handle {@code PessimisticLockingFailureException}
 * itself instead of relying on specific exception subclasses.
 *
 * @author Thomas Risberg
 * @since 1.2
 * @see OptimisticLockingFailureException
 */
@SuppressWarnings("serial")
public class PessimisticLockingFailureException extends ConcurrencyFailureException {

	/**
	 * Constructor for PessimisticLockingFailureException.
	 * @param msg the detail message
	 */
	public PessimisticLockingFailureException(@Nullable String msg) {
		super(msg);
	}

	/**
	 * Constructor for PessimisticLockingFailureException.
	 * @param msg the detail message
	 * @param cause the root cause from the data access API in use
	 */
	public PessimisticLockingFailureException(@Nullable String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}

}
