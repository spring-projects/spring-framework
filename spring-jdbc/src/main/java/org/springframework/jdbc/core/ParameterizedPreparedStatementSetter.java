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

package org.springframework.jdbc.core;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Parameterized callback interface used by the {@link JdbcTemplate} class for batch updates.
 *
 * <p>This interface sets values on a {@link java.sql.PreparedStatement} provided
 * by the JdbcTemplate class, for each of a number of updates in a batch using the
 * same SQL. Implementations are responsible for setting any necessary parameters.
 * SQL with placeholders will already have been supplied.
 *
 * <p>Implementations <i>do not</i> need to concern themselves with SQLExceptions
 * that may be thrown from operations they attempt. The JdbcTemplate class will
 * catch and handle SQLExceptions appropriately.
 *
 * @author Nicolas Fabre
 * @author Thomas Risberg
 * @since 3.1
 * @see JdbcTemplate#batchUpdate(String, java.util.Collection, int, ParameterizedPreparedStatementSetter)
 */
public interface ParameterizedPreparedStatementSetter<T> {

	/**
	 * Set parameter values on the given PreparedStatement.
	 *
	 * @param ps the PreparedStatement to invoke setter methods on
	 * @param argument the object containing the values to be set
	 * @throws SQLException if a SQLException is encountered (i.e. there is no need to catch SQLException)
	 */
	void setValues(PreparedStatement ps, T argument) throws SQLException;

}
