/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.jdbc.core.namedparam;

import org.jspecify.annotations.Nullable;

import org.springframework.jdbc.support.JdbcUtils;

/**
 * Interface that defines common functionality for objects that can
 * offer parameter values for named SQL parameters, serving as argument
 * for {@link NamedParameterJdbcTemplate} operations.
 *
 * <p>This interface allows for the specification of SQL type in addition
 * to parameter values. All parameter values and types are identified by
 * specifying the name of the parameter.
 *
 * <p>Intended to wrap various implementations like a Map or a JavaBean
 * with a consistent interface.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 2.0
 * @see NamedParameterJdbcOperations
 * @see NamedParameterJdbcTemplate
 * @see MapSqlParameterSource
 * @see BeanPropertySqlParameterSource
 */
public interface SqlParameterSource {

	/**
	 * Constant that indicates an unknown (or unspecified) SQL type.
	 * To be returned from {@code getType} when no specific SQL type known.
	 * @see #getSqlType
	 * @see java.sql.Types
	 */
	int TYPE_UNKNOWN = JdbcUtils.TYPE_UNKNOWN;


	/**
	 * Determine whether there is a value for the specified named parameter.
	 * @param paramName the name of the parameter
	 * @return whether there is a value defined
	 */
	boolean hasValue(String paramName);

	/**
	 * Return the parameter value for the requested named parameter.
	 * @param paramName the name of the parameter
	 * @return the value of the specified parameter
	 * @throws IllegalArgumentException if there is no value for the requested parameter
	 */
	@Nullable Object getValue(String paramName) throws IllegalArgumentException;

	/**
	 * Determine the SQL type for the specified named parameter.
	 * @param paramName the name of the parameter
	 * @return the SQL type of the specified parameter,
	 * or {@code TYPE_UNKNOWN} if not known
	 * @see #TYPE_UNKNOWN
	 */
	default int getSqlType(String paramName) {
		return TYPE_UNKNOWN;
	}

	/**
	 * Determine the type name for the specified named parameter.
	 * @param paramName the name of the parameter
	 * @return the type name of the specified parameter,
	 * or {@code null} if not known
	 */
	default @Nullable String getTypeName(String paramName) {
		return null;
	}

	/**
	 * Enumerate all available parameter names if possible.
	 * <p>This is an optional operation, primarily for use with
	 * {@link org.springframework.jdbc.core.simple.SimpleJdbcInsert}
	 * and {@link org.springframework.jdbc.core.simple.SimpleJdbcCall}.
	 * @return the array of parameter names, or {@code null} if not determinable
	 * @since 5.0.3
	 * @see SqlParameterSourceUtils#extractCaseInsensitiveParameterNames
	 */
	default String @Nullable [] getParameterNames() {
		return null;
	}

}
