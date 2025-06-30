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
 * Exception thrown when an attempt to insert or update data
 * results in violation of a primary key or unique constraint.
 * Note that this is not necessarily a purely relational concept;
 * unique primary keys are required by most database types.
 *
 * <p>Consider handling the general {@link DataIntegrityViolationException}
 * instead, semantically including a wider range of constraint violations.
 *
 * @author Thomas Risberg
 */
@SuppressWarnings("serial")
public class DuplicateKeyException extends DataIntegrityViolationException {

	/**
	 * Constructor for DuplicateKeyException.
	 * @param msg the detail message
	 */
	public DuplicateKeyException(@Nullable String msg) {
		super(msg);
	}

	/**
	 * Constructor for DuplicateKeyException.
	 * @param msg the detail message
	 * @param cause the root cause from the data access API in use
	 */
	public DuplicateKeyException(@Nullable String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}

}
