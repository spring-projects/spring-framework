/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.jdbc.support;

import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.dao.DataAccessException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Base class for {@link SQLExceptionTranslator} implementations that allow for a
 * fallback to some other {@link SQLExceptionTranslator}, as well as for custom
 * overrides.
 *
 * @author Juergen Hoeller
 * @since 2.5.6
 * @see #doTranslate
 * @see #setFallbackTranslator
 * @see #setCustomTranslator
 */
public abstract class AbstractFallbackSQLExceptionTranslator implements SQLExceptionTranslator {

	/** Logger available to subclasses. */
	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private SQLExceptionTranslator fallbackTranslator;

	@Nullable
	private SQLExceptionTranslator customTranslator;


	/**
	 * Set the fallback translator to use when this translator cannot find a
	 * specific match itself.
	 */
	public void setFallbackTranslator(@Nullable SQLExceptionTranslator fallback) {
		this.fallbackTranslator = fallback;
	}

	/**
	 * Return the fallback exception translator, if any.
	 * @see #setFallbackTranslator
	 */
	@Nullable
	public SQLExceptionTranslator getFallbackTranslator() {
		return this.fallbackTranslator;
	}

	/**
	 * Set a custom exception translator to override any match that this translator
	 * would find. Note that such a custom {@link SQLExceptionTranslator} delegate
	 * is meant to return {@code null} if it does not have an override itself.
	 * @since 6.1
	 */
	public void setCustomTranslator(@Nullable SQLExceptionTranslator customTranslator) {
		this.customTranslator = customTranslator;
	}

	/**
	 * Return a custom exception translator, if any.
	 * @since 6.1
	 * @see #setCustomTranslator
	 */
	@Nullable
	public SQLExceptionTranslator getCustomTranslator() {
		return this.customTranslator;
	}


	/**
	 * Pre-checks the arguments, calls {@link #doTranslate}, and invokes the
	 * {@link #getFallbackTranslator() fallback translator} if necessary.
	 */
	@Override
	@Nullable
	public DataAccessException translate(String task, @Nullable String sql, SQLException ex) {
		Assert.notNull(ex, "Cannot translate a null SQLException");

		SQLExceptionTranslator custom = getCustomTranslator();
		if (custom != null) {
			DataAccessException dae = custom.translate(task, sql, ex);
			if (dae != null) {
				// Custom exception match found.
				return dae;
			}
		}

		DataAccessException dae = doTranslate(task, sql, ex);
		if (dae != null) {
			// Specific exception match found.
			return dae;
		}

		// Looking for a fallback...
		SQLExceptionTranslator fallback = getFallbackTranslator();
		if (fallback != null) {
			return fallback.translate(task, sql, ex);
		}

		return null;
	}

	/**
	 * Template method for actually translating the given exception.
	 * <p>The passed-in arguments will have been pre-checked. Furthermore, this method
	 * is allowed to return {@code null} to indicate that no exception match has
	 * been found and that fallback translation should kick in.
	 * @param task readable text describing the task being attempted
	 * @param sql the SQL query or update that caused the problem (if known)
	 * @param ex the offending {@code SQLException}
	 * @return the DataAccessException, wrapping the {@code SQLException};
	 * or {@code null} if no exception match found
	 */
	@Nullable
	protected abstract DataAccessException doTranslate(String task, @Nullable String sql, SQLException ex);


	/**
	 * Build a message {@code String} for the given {@link java.sql.SQLException}.
	 * <p>To be called by translator subclasses when creating an instance of a generic
	 * {@link org.springframework.dao.DataAccessException} class.
	 * @param task readable text describing the task being attempted
	 * @param sql the SQL statement that caused the problem
	 * @param ex the offending {@code SQLException}
	 * @return the message {@code String} to use
	 */
	protected String buildMessage(String task, @Nullable String sql, SQLException ex) {
		return task + "; " + (sql != null ? ("SQL [" + sql + "]; ") : "") + ex.getMessage();
	}

}
