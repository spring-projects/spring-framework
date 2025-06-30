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
 * Exception thrown on failure to complete a transaction in serialized mode
 * due to update conflicts.
 *
 * <p>Consider handling the general {@link PessimisticLockingFailureException}
 * instead, semantically including a wider range of locking-related failures.
 *
 * @author Rod Johnson
 * @deprecated as of 6.0.3, in favor of
 * {@link PessimisticLockingFailureException}/{@link CannotAcquireLockException}
 */
@Deprecated(since = "6.0.3")
@SuppressWarnings("serial")
public class CannotSerializeTransactionException extends PessimisticLockingFailureException {

	/**
	 * Constructor for CannotSerializeTransactionException.
	 * @param msg the detail message
	 */
	public CannotSerializeTransactionException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for CannotSerializeTransactionException.
	 * @param msg the detail message
	 * @param cause the root cause from the data access API in use
	 */
	public CannotSerializeTransactionException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
