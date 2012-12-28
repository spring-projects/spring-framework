/*
 * Copyright 2002-2006 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.transaction;

/**
 * Superclass for exceptions caused by inappropriate usage of
 * a Spring transaction API.
 *
 * @author Rod Johnson
 * @since 22.03.2003
 */
@SuppressWarnings("serial")
public class TransactionUsageException extends TransactionException {

	/**
	 * Constructor for TransactionUsageException.
	 * @param msg the detail message
	 */
	public TransactionUsageException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for TransactionUsageException.
	 * @param msg the detail message
	 * @param cause the root cause from the transaction API in use
	 */
	public TransactionUsageException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
