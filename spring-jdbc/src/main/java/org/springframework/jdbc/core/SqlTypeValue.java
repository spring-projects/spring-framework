/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.jdbc.core;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.lang.Nullable;

/**
 * Interface to be implemented for setting values for more complex database-specific
 * types not supported by the standard {@code setObject} method. This is
 * effectively an extended variant of {@link org.springframework.jdbc.support.SqlValue}.
 *
 * <p>Implementations perform the actual work of setting the actual values. They must
 * implement the callback method {@code setTypeValue} which can throw SQLExceptions
 * that will be caught and translated by the calling code. This callback method has
 * access to the underlying Connection via the given PreparedStatement object, if that
 * should be needed to create any database-specific objects.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 1.1
 * @see java.sql.Types
 * @see java.sql.PreparedStatement#setObject
 * @see JdbcOperations#update(String, Object[], int[])
 * @see org.springframework.jdbc.support.SqlValue
 */
public interface SqlTypeValue {

	/**
	 * Constant that indicates an unknown (or unspecified) SQL type.
	 * Passed into {@code setTypeValue} if the original operation method
	 * does not specify a SQL type.
	 * @see java.sql.Types
	 * @see JdbcOperations#update(String, Object[])
	 */
	int TYPE_UNKNOWN = JdbcUtils.TYPE_UNKNOWN;


	/**
	 * Set the type value on the given PreparedStatement.
	 * @param ps the PreparedStatement to work on
	 * @param paramIndex the index of the parameter for which we need to set the value
	 * @param sqlType SQL type of the parameter we are setting
	 * @param typeName the type name of the parameter (optional)
	 * @throws SQLException if a SQLException is encountered while setting parameter values
	 * @see java.sql.Types
	 * @see java.sql.PreparedStatement#setObject
	 */
	void setTypeValue(PreparedStatement ps, int paramIndex, int sqlType, @Nullable String typeName)
			throws SQLException;

}
