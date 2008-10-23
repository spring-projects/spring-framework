/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.jdbc.core.metadata;

/**
 * Holder of metadata for a specific parameter that is used for call processing.
 *
 * @author Thomas Risberg
 * @since 2.5
 */
public class CallParameterMetaData {
	private String parameterName;
	private int parameterType;
	private int sqlType;
	private String typeName;
	private boolean nullable;

	/**
	 * Constructor taking all the properties
	 */
	public CallParameterMetaData(String columnName, int columnType, int sqlType, String typeName, boolean nullable) {
		this.parameterName = columnName;
		this.parameterType = columnType;
		this.sqlType = sqlType;
		this.typeName = typeName;
		this.nullable = nullable;
	}


	/**
	 * Get the parameter name.
	 */
	public String getParameterName() {
		return parameterName;
	}

	/**
	 * Get the parameter type.
	 */
	public int getParameterType() {
		return parameterType;
	}

	/**
	 * Get the parameter SQL type.
	 */
	public int getSqlType() {
		return sqlType;
	}

	/**
	 * Get the parameter type name.
	 */
	public String getTypeName() {
		return typeName;
	}

	/**
	 * Get whether the parameter is nullable.
	 */
	public boolean isNullable() {
		return nullable;
	}
}