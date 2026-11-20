/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.r2dbc.connection.init;

import org.springframework.core.io.support.EncodedResource;

/**
 * Thrown by {@link ScriptUtils} if a statement in an SQL script failed when
 * executing it against the target database.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Mark Paluch
 * @since 5.3
 */
@SuppressWarnings("serial")
public class ScriptStatementFailedException extends ScriptException {

	/**
	 * Create a new {@code ScriptStatementFailedException}.
	 * @param stmt the actual SQL statement that failed
	 * @param stmtNumber the statement number in the SQL script (i.e.,
	 * the n<sup>th</sup> statement present in the resource)
	 * @param encodedResource the resource from which the SQL statement was read
	 * @param cause the underlying cause of the failure
	 */
	public ScriptStatementFailedException(String stmt, int stmtNumber, EncodedResource encodedResource, Throwable cause) {
		super(buildErrorMessage(stmt, stmtNumber, encodedResource), cause);
	}


	/**
	 * Build an error message for an SQL script execution failure,
	 * based on the supplied arguments.
	 * @param stmt the actual SQL statement that failed
	 * @param stmtNumber the statement number in the SQL script (i.e.,
	 * the n<sup>th</sup> statement present in the resource)
	 * @param encodedResource the resource from which the SQL statement was read
	 * @return an error message suitable for an exception's <em>detail message</em>
	 * or logging
	 */
	public static String buildErrorMessage(String stmt, int stmtNumber, EncodedResource encodedResource) {
		return String.format("Failed to execute SQL script statement #%s of %s: %s", stmtNumber, encodedResource, stmt);
	}

}
