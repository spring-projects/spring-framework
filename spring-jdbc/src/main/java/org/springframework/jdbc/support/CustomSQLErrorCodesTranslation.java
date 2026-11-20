/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.support;

import org.jspecify.annotations.Nullable;

import org.springframework.dao.DataAccessException;
import org.springframework.util.StringUtils;

/**
 * JavaBean for holding custom JDBC error codes translation for a particular
 * database. The "exceptionClass" property defines which exception will be
 * thrown for the list of error codes specified in the errorCodes property.
 *
 * @author Thomas Risberg
 * @since 1.1
 * @see SQLErrorCodeSQLExceptionTranslator
 */
public class CustomSQLErrorCodesTranslation {

	private String[] errorCodes = new String[0];

	private @Nullable Class<?> exceptionClass;


	/**
	 * Set the SQL error codes to match.
	 */
	public void setErrorCodes(String... errorCodes) {
		this.errorCodes = StringUtils.sortStringArray(errorCodes);
	}

	/**
	 * Return the SQL error codes to match.
	 */
	public String[] getErrorCodes() {
		return this.errorCodes;
	}

	/**
	 * Set the exception class for the specified error codes.
	 */
	public void setExceptionClass(@Nullable Class<?> exceptionClass) {
		if (exceptionClass != null && !DataAccessException.class.isAssignableFrom(exceptionClass)) {
			throw new IllegalArgumentException("Invalid exception class [" + exceptionClass +
					"]: needs to be a subclass of [org.springframework.dao.DataAccessException]");
		}
		this.exceptionClass = exceptionClass;
	}

	/**
	 * Return the exception class for the specified error codes.
	 */
	public @Nullable Class<?> getExceptionClass() {
		return this.exceptionClass;
	}

}
