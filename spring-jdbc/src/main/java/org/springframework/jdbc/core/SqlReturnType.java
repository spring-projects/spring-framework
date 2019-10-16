/*
 * Copyright 2002-2018 the original author or authors.
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

import java.sql.CallableStatement;
import java.sql.SQLException;

import org.springframework.lang.Nullable;

/**
 * Interface to be implemented for retrieving values for more complex database-specific
 * types not supported by the standard {@code CallableStatement.getObject} method.
 *
 * <p>Implementations perform the actual work of getting the actual values. They must
 * implement the callback method {@code getTypeValue} which can throw SQLExceptions
 * that will be caught and translated by the calling code. This callback method has
 * access to the underlying Connection via the given CallableStatement object, if that
 * should be needed to create any database-specific objects.
 *
 * @author Thomas Risberg
 * @since 1.1
 * @see java.sql.Types
 * @see java.sql.CallableStatement#getObject
 * @see org.springframework.jdbc.object.StoredProcedure#execute(java.util.Map)
 */
public interface SqlReturnType {

	/**
	 * Constant that indicates an unknown (or unspecified) SQL type.
	 * Passed into setTypeValue if the original operation method does
	 * not specify an SQL type.
	 * @see java.sql.Types
	 * @see JdbcOperations#update(String, Object[])
	 */
	int TYPE_UNKNOWN = Integer.MIN_VALUE;


	/**
	 * Get the type value from the specific object.
	 * @param cs the CallableStatement to operate on
	 * @param paramIndex the index of the parameter for which we need to set the value
	 * @param sqlType the SQL type of the parameter we are setting
	 * @param typeName the type name of the parameter (optional)
	 * @return the target value
	 * @throws SQLException if an SQLException is encountered setting parameter values
	 * (that is, there's no need to catch SQLException)
	 * @see java.sql.Types
	 * @see java.sql.CallableStatement#getObject
	 */
	Object getTypeValue(CallableStatement cs, int paramIndex, int sqlType, @Nullable  String typeName)
			throws SQLException;

}
