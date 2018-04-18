/*
 * Copyright 2002-2017 the original author or authors.
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

import org.springframework.lang.Nullable;

/**
 * Object to represent a SQL parameter value, including parameter meta-data
 * such as the SQL type and the scale for numeric values.
 *
 * <p>Designed for use with {@link JdbcTemplate}'s operations that take an array of
 * argument values: Each such argument value may be a {@code SqlParameterValue},
 * indicating the SQL type (and optionally the scale) instead of letting the
 * template guess a default type. Note that this only applies to the operations with
 * a 'plain' argument array, not to the overloaded variants with an explicit type array.
 *
 * @author Juergen Hoeller
 * @since 2.0.5
 * @see java.sql.Types
 * @see JdbcTemplate#query(String, Object[], ResultSetExtractor)
 * @see JdbcTemplate#query(String, Object[], RowCallbackHandler)
 * @see JdbcTemplate#query(String, Object[], RowMapper)
 * @see JdbcTemplate#update(String, Object[])
 */
public class SqlParameterValue extends SqlParameter {

	@Nullable
	private final Object value;


	/**
	 * Create a new SqlParameterValue, supplying the SQL type.
	 * @param sqlType SQL type of the parameter according to {@code java.sql.Types}
	 * @param value the value object
	 */
	public SqlParameterValue(int sqlType, @Nullable Object value) {
		super(sqlType);
		this.value = value;
	}

	/**
	 * Create a new SqlParameterValue, supplying the SQL type.
	 * @param sqlType SQL type of the parameter according to {@code java.sql.Types}
	 * @param typeName the type name of the parameter (optional)
	 * @param value the value object
	 */
	public SqlParameterValue(int sqlType, @Nullable String typeName, @Nullable Object value) {
		super(sqlType, typeName);
		this.value = value;
	}

	/**
	 * Create a new SqlParameterValue, supplying the SQL type.
	 * @param sqlType SQL type of the parameter according to {@code java.sql.Types}
	 * @param scale the number of digits after the decimal point
	 * (for DECIMAL and NUMERIC types)
	 * @param value the value object
	 */
	public SqlParameterValue(int sqlType, int scale, @Nullable Object value) {
		super(sqlType, scale);
		this.value = value;
	}

	/**
	 * Create a new SqlParameterValue based on the given SqlParameter declaration.
	 * @param declaredParam the declared SqlParameter to define a value for
	 * @param value the value object
	 */
	public SqlParameterValue(SqlParameter declaredParam, @Nullable Object value) {
		super(declaredParam);
		this.value = value;
	}


	/**
	 * Return the value object that this parameter value holds.
	 */
	@Nullable
	public Object getValue() {
		return this.value;
	}

}
