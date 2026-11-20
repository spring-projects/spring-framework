/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.r2dbc;

import io.r2dbc.spi.R2dbcException;
import org.jspecify.annotations.Nullable;

import org.springframework.dao.InvalidDataAccessResourceUsageException;

/**
 * Exception thrown when SQL specified is invalid. Such exceptions always have a
 * {@link io.r2dbc.spi.R2dbcException} root cause.
 *
 * <p>It would be possible to have subclasses for no such table, no such column etc.
 * A custom R2dbcExceptionTranslator could create such more specific exceptions,
 * without affecting code using this class.
 *
 * @author Mark Paluch
 * @since 5.3
 */
@SuppressWarnings("serial")
public class BadSqlGrammarException extends InvalidDataAccessResourceUsageException {

	private final String sql;


	/**
	 * Constructor for BadSqlGrammarException.
	 * @param task name of current task
	 * @param sql the offending SQL statement
	 * @param ex the root cause
	 */
	public BadSqlGrammarException(String task, String sql, R2dbcException ex) {
		super(task + "; bad SQL grammar [" + sql + "]", ex);
		this.sql = sql;
	}


	/**
	 * Return the wrapped {@link R2dbcException}.
	 */
	public @Nullable R2dbcException getR2dbcException() {
		return (R2dbcException) getCause();
	}

	/**
	 * Return the SQL that caused the problem.
	 */
	public String getSql() {
		return this.sql;
	}

}
