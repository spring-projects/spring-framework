/*
 * Copyright 2002-2017 the original author or authors.
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
 * Exception thrown on an optimistic locking violation.
 *
 * <p>This exception will be thrown either by O/R mapping tools
 * or by custom DAO implementations. Optimistic locking failure
 * is typically <i>not</i> detected by the database itself.
 *
 * @author Rod Johnson
 * @see PessimisticLockingFailureException
 */
@SuppressWarnings("serial")
public class OptimisticLockingFailureException extends ConcurrencyFailureException {

	/**
	 * Constructor for OptimisticLockingFailureException.
	 * @param msg the detail message
	 */
	public OptimisticLockingFailureException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for OptimisticLockingFailureException.
	 * @param msg the detail message
	 * @param cause the root cause from the data access API in use
	 */
	public OptimisticLockingFailureException(String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}

}
