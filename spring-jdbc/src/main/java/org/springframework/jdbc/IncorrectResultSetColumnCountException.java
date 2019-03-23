/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.jdbc;

import org.springframework.dao.DataRetrievalFailureException;

/**
 * Data access exception thrown when a result set did not have the correct column count,
 * for example when expecting a single column but getting 0 or more than 1 columns.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.dao.IncorrectResultSizeDataAccessException
 */
@SuppressWarnings("serial")
public class IncorrectResultSetColumnCountException extends DataRetrievalFailureException {

	private int expectedCount;

	private int actualCount;


	/**
	 * Constructor for IncorrectResultSetColumnCountException.
	 * @param expectedCount the expected column count
	 * @param actualCount the actual column count
	 */
	public IncorrectResultSetColumnCountException(int expectedCount, int actualCount) {
		super("Incorrect column count: expected " + expectedCount + ", actual " + actualCount);
		this.expectedCount = expectedCount;
		this.actualCount = actualCount;
	}

	/**
	 * Constructor for IncorrectResultCountDataAccessException.
	 * @param msg the detail message
	 * @param expectedCount the expected column count
	 * @param actualCount the actual column count
	 */
	public IncorrectResultSetColumnCountException(String msg, int expectedCount, int actualCount) {
		super(msg);
		this.expectedCount = expectedCount;
		this.actualCount = actualCount;
	}


	/**
	 * Return the expected column count.
	 */
	public int getExpectedCount() {
		return this.expectedCount;
	}

	/**
	 * Return the actual column count.
	 */
	public int getActualCount() {
		return this.actualCount;
	}

}
