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
 * Exception that gets thrown when an invalid isolation level is specified,
 * i.e. an isolation level that the transaction manager implementation
 * doesn't support.
 *
 * @author Juergen Hoeller
 * @since 12.05.2003
 */
@SuppressWarnings("serial")
public class InvalidIsolationLevelException extends TransactionUsageException {

	/**
	 * Constructor for InvalidIsolationLevelException.
	 * @param msg the detail message
	 */
	public InvalidIsolationLevelException(String msg) {
		super(msg);
	}

}
