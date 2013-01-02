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

package org.springframework.jdbc.support;

import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.util.Assert;

/**
 * Base class for {@link SQLExceptionTranslator} implementations that allow for
 * fallback to some other {@link SQLExceptionTranslator}.
 *
 * @author Juergen Hoeller
 * @since 2.5.6
 */
public abstract class AbstractFallbackSQLExceptionTranslator implements SQLExceptionTranslator {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private SQLExceptionTranslator fallbackTranslator;


	/**
	 * Override the default SQL state fallback translator
	 * (typically a {@link SQLStateSQLExceptionTranslator}).
	 */
	public void setFallbackTranslator(SQLExceptionTranslator fallback) {
		this.fallbackTranslator = fallback;
	}

	/**
	 * Return the fallback exception translator, if any.
	 */
	public SQLExceptionTranslator getFallbackTranslator() {
		return this.fallbackTranslator;
	}


	/**
	 * Pre-checks the arguments, calls {@link #doTranslate}, and invokes the
	 * {@link #getFallbackTranslator() fallback translator} if necessary.
	 */
	public DataAccessException translate(String task, String sql, SQLException ex) {
		Assert.notNull(ex, "Cannot translate a null SQLException");
		if (task == null) {
			task = "";
		}
		if (sql == null) {
			sql = "";
		}

		DataAccessException dex = doTranslate(task, sql, ex);
		if (dex != null) {
			// Specific exception match found.
			return dex;
		}
		// Looking for a fallback...
		SQLExceptionTranslator fallback = getFallbackTranslator();
		if (fallback != null) {
			return fallback.translate(task, sql, ex);
		}
		// We couldn't identify it more precisely.
		return new UncategorizedSQLException(task, sql, ex);
	}

	/**
	 * Template method for actually translating the given exception.
	 * <p>The passed-in arguments will have been pre-checked. Furthermore, this method
	 * is allowed to return {@code null} to indicate that no exception match has
	 * been found and that fallback translation should kick in.
	 * @param task readable text describing the task being attempted
	 * @param sql SQL query or update that caused the problem (may be {@code null})
	 * @param ex the offending {@code SQLException}
	 * @return the DataAccessException, wrapping the {@code SQLException};
	 * or {@code null} if no exception match found
	 */
	protected abstract DataAccessException doTranslate(String task, String sql, SQLException ex);


	/**
	 * Build a message {@code String} for the given {@link java.sql.SQLException}.
	 * <p>To be called by translator subclasses when creating an instance of a generic
	 * {@link org.springframework.dao.DataAccessException} class.
	 * @param task readable text describing the task being attempted
	 * @param sql the SQL statement that caused the problem (may be {@code null})
	 * @param ex the offending {@code SQLException}
	 * @return the message {@code String} to use
	 */
	protected String buildMessage(String task, String sql, SQLException ex) {
		return task + "; SQL [" + sql + "]; " + ex.getMessage();
	}

}
