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

package org.springframework.transaction;

/**
 * Exception thrown when attempting to work with a nested transaction
 * but nested transactions are not supported by the underlying backend.
 *
 * @author Juergen Hoeller
 * @since 1.1
 */
@SuppressWarnings("serial")
public class NestedTransactionNotSupportedException extends CannotCreateTransactionException {

	/**
	 * Constructor for NestedTransactionNotSupportedException.
	 * @param msg the detail message
	 */
	public NestedTransactionNotSupportedException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for NestedTransactionNotSupportedException.
	 * @param msg the detail message
	 * @param cause the root cause from the transaction API in use
	 */
	public NestedTransactionNotSupportedException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
