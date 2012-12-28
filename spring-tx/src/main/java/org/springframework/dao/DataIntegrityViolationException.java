/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.dao;

/**
 * Exception thrown when an attempt to insert or update data
 * results in violation of an integrity constraint. Note that this
 * is not purely a relational concept; unique primary keys are
 * required by most database types.
 *
 * @author Rod Johnson
 */
@SuppressWarnings("serial")
public class DataIntegrityViolationException extends NonTransientDataAccessException {

	/**
	 * Constructor for DataIntegrityViolationException.
	 * @param msg the detail message
	 */
	public DataIntegrityViolationException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for DataIntegrityViolationException.
	 * @param msg the detail message
	 * @param cause the root cause from the data access API in use
	 */
	public DataIntegrityViolationException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
