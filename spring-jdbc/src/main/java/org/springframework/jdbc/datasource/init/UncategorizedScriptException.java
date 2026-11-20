/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.datasource.init;

/**
 * Thrown when we cannot determine anything more specific than "something went
 * wrong while processing an SQL script": for example, a {@link java.sql.SQLException}
 * from JDBC that we cannot pinpoint more precisely.
 *
 * @author Sam Brannen
 * @since 4.0.3
 */
@SuppressWarnings("serial")
public class UncategorizedScriptException extends ScriptException {

	/**
	 * Create a new {@code UncategorizedScriptException}.
	 * @param message detailed message
	 */
	public UncategorizedScriptException(String message) {
		super(message);
	}

	/**
	 * Create a new {@code UncategorizedScriptException}.
	 * @param message detailed message
	 * @param cause the root cause
	 */
	public UncategorizedScriptException(String message, Throwable cause) {
		super(message, cause);
	}

}
