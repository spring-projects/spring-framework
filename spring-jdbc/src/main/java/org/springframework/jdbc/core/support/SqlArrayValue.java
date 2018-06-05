/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.jdbc.core.support;

import java.sql.Array;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Default {@link AbstractSqlArrayValue} implementation that uses
 * {@link Connection#createArrayOf(String, Object[])} to create an
 * {@link Array}.
 *
 * @author Philippe Marschall
 * @since 5.1
 * @see Connection#createArrayOf(String, Object[])
 */
public class SqlArrayValue extends AbstractSqlArrayValue {

	private final Object[] elements;

	private final String typeName;

	/**
	 * Constructor that takes two parameters, the array type and and
	 * the array of values passed in to the statement.
	 * @param typeName the type name
	 * @param elements the array containing the values
	 */
	public SqlArrayValue(String typeName, Object... elements) {
		this.elements = elements;
		this.typeName = typeName;
	}

	@Override
	protected Array createArray(Connection conn) throws SQLException {
		return conn.createArrayOf(typeName, elements);
	}

}
