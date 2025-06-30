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

package org.springframework.jdbc.datasource.init;

import org.jspecify.annotations.Nullable;

import org.springframework.dao.DataAccessException;

/**
 * Root of the hierarchy of data access exceptions that are related to processing
 * of SQL scripts.
 *
 * @author Sam Brannen
 * @since 4.0.3
 */
@SuppressWarnings("serial")
public abstract class ScriptException extends DataAccessException {

	/**
	 * Create a new {@code ScriptException}.
	 * @param message the detail message
	 */
	public ScriptException(String message) {
		super(message);
	}

	/**
	 * Create a new {@code ScriptException}.
	 * @param message the detail message
	 * @param cause the root cause
	 */
	public ScriptException(String message, @Nullable Throwable cause) {
		super(message, cause);
	}

}
