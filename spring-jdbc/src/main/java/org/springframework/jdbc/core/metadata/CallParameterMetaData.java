/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.jdbc.core.metadata;

import java.sql.DatabaseMetaData;

import org.springframework.lang.Nullable;

/**
 * Holder of meta-data for a specific parameter that is used for call processing.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 2.5
 * @see GenericCallMetaDataProvider
 */
public class CallParameterMetaData {

	private final boolean function;

	@Nullable
	private final String parameterName;

	private final int parameterType;

	private final int sqlType;

	@Nullable
	private final String typeName;

	private final boolean nullable;


	/**
	 * Constructor taking all the properties except the function marker.
	 */
	@Deprecated
	public CallParameterMetaData(
			@Nullable String columnName, int columnType, int sqlType, @Nullable String typeName, boolean nullable) {

		this(false, columnName, columnType, sqlType, typeName, nullable);
	}

	/**
	 * Constructor taking all the properties including the function marker.
	 * @since 5.2.9
	 */
	public CallParameterMetaData(boolean function, @Nullable String columnName, int columnType,
			int sqlType, @Nullable String typeName, boolean nullable) {

		this.function = function;
		this.parameterName = columnName;
		this.parameterType = columnType;
		this.sqlType = sqlType;
		this.typeName = typeName;
		this.nullable = nullable;
	}


	/**
	 * Return whether this parameter is declared in a function.
	 * @since 5.2.9
	 */
	public boolean isFunction() {
		return this.function;
	}

	/**
	 * Return the parameter name.
	 */
	@Nullable
	public String getParameterName() {
		return this.parameterName;
	}

	/**
	 * Return the parameter type.
	 */
	public int getParameterType() {
		return this.parameterType;
	}

	/**
	 * Determine whether the declared parameter qualifies as a 'return' parameter
	 * for our purposes: type {@link DatabaseMetaData#procedureColumnReturn} or
	 * {@link DatabaseMetaData#procedureColumnResult}, or in case of a function,
	 * {@link DatabaseMetaData#functionReturn}.
	 * @since 4.3.15
	 */
	public boolean isReturnParameter() {
		return (this.function ? this.parameterType == DatabaseMetaData.functionReturn :
				(this.parameterType == DatabaseMetaData.procedureColumnReturn ||
						this.parameterType == DatabaseMetaData.procedureColumnResult));
	}

	/**
	 * Return the parameter SQL type.
	 */
	public int getSqlType() {
		return this.sqlType;
	}

	/**
	 * Return the parameter type name.
	 */
	@Nullable
	public String getTypeName() {
		return this.typeName;
	}

	/**
	 * Return whether the parameter is nullable.
	 */
	public boolean isNullable() {
		return this.nullable;
	}

}
