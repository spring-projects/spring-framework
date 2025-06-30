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

package org.springframework.core.retry;

import java.io.Serial;

/**
 * Exception thrown when a {@link RetryPolicy} has been exhausted.
 *
 * @author Mahmoud Ben Hassine
 * @since 7.0
 * @see RetryOperations
 */
public class RetryException extends Exception {

	@Serial
	private static final long serialVersionUID = 5439915454935047936L;


	/**
	 * Create a new {@code RetryException} for the supplied message.
	 * @param message the detail message
	 */
	public RetryException(String message) {
		super(message);
	}

	/**
	 * Create a new {@code RetryException} for the supplied message and cause.
	 * @param message the detail message
	 * @param cause the root cause
	 */
	public RetryException(String message, Throwable cause) {
		super(message, cause);
	}

}
