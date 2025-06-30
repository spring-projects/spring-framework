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

package org.springframework.dao;

import org.jspecify.annotations.Nullable;

/**
 * Exception thrown when an attempt to execute an SQL statement fails to map
 * the given data, typically but no limited to an insert or update data
 * results in violation of an integrity constraint. Note that this
 * is not purely a relational concept; integrity constraints such
 * as unique primary keys are required by most database types.
 *
 * <p>Serves as a superclass for more specific exceptions, for example,
 * {@link DuplicateKeyException}. However, it is generally
 * recommended to handle {@code DataIntegrityViolationException}
 * itself instead of relying on specific exception subclasses.
 *
 * @author Rod Johnson
 */
@SuppressWarnings("serial")
public class DataIntegrityViolationException extends NonTransientDataAccessException {

	/**
	 * Constructor for DataIntegrityViolationException.
	 * @param msg the detail message
	 */
	public DataIntegrityViolationException(@Nullable String msg) {
		super(msg);
	}

	/**
	 * Constructor for DataIntegrityViolationException.
	 * @param msg the detail message
	 * @param cause the root cause from the data access API in use
	 */
	public DataIntegrityViolationException(@Nullable String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}

}
