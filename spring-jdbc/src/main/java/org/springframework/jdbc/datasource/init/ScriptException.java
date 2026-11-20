/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
