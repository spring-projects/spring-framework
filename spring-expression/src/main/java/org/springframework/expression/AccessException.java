/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.expression;

/**
 * An AccessException is thrown by an accessor if it has an unexpected problem.
 *
 * @author Andy Clement
 * @since 3.0
 */
@SuppressWarnings("serial")
public class AccessException extends Exception {

	/**
	 * Create an AccessException with a specific message and cause.
	 * @param message the message
	 * @param cause the cause
	 */
	public AccessException(String message, Exception cause) {
		super(message, cause);
	}

	/**
	 * Create an AccessException with a specific message.
	 * @param message the message
	 */
	public AccessException(String message) {
		super(message);
	}

}
