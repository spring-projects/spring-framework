/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.core.retry;

import java.io.Serial;

/**
 * Exception class for exhausted retries.
 *
 * @author Mahmoud Ben Hassine
 * @since 7.0
 * @see RetryOperations
 */
public class RetryException extends Exception {

	@Serial
	private static final long serialVersionUID = 5439915454935047936L;

	/**
	 * Create a new exception with a message.
	 * @param message the exception's message
	 */
	public RetryException(String message) {
		super(message);
	}

	/**
	 * Create a new exception with a message and a cause.
	 * @param message the exception's message
	 * @param cause the exception's cause
	 */
	public RetryException(String message, Throwable cause) {
		super(message, cause);
	}

}
