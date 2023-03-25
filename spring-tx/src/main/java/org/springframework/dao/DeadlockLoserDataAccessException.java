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

/**
 * Generic exception thrown when the current process was
 * a deadlock loser, and its transaction rolled back.
 *
 * <p>Consider handling the general {@link PessimisticLockingFailureException}
 * instead, semantically including a wider range of locking-related failures.
 *
 * @author Rod Johnson
 */
@SuppressWarnings("serial")
public class DeadlockLoserDataAccessException extends PessimisticLockingFailureException {

	/**
	 * Constructor for DeadlockLoserDataAccessException.
	 * @param msg the detail message
	 * @param cause the root cause from the data access API in use
	 */
	public DeadlockLoserDataAccessException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
