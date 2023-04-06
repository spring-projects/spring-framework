/*
 * Copyright 2002-2022 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Abstract base class for {@link SqlParameterSource} implementations.
 * Provides registration of SQL types per parameter and a friendly
 * {@link #toString() toString} representation enumerating all parameters for
 * a {@code SqlParameterSource} implementing {@link #getParameterNames()}.
 * Concrete subclasses must implement {@link #hasValue} and {@link #getValue}.
 *
 * @author Juergen Hoeller
 * @author Jens Schauder
 * @since 2.0
 * @see #hasValue(String)
 * @see #getValue(String)
 * @see #getParameterNames()
 */
public abstract class AbstractSqlParameterSource implements SqlParameterSource {

	private final Map<String, Integer> sqlTypes = new HashMap<>();

	private final Map<String, String> typeNames = new HashMap<>();


	/**
	 * Register an SQL type for the given parameter.
	 * @param paramName the name of the parameter
	 * @param sqlType the SQL type of the parameter
	 */
	public void registerSqlType(String paramName, int sqlType) {
		Assert.notNull(paramName, "Parameter name must not be null");
		this.sqlTypes.put(paramName, sqlType);
	}

	/**
	 * Register an SQL type for the given parameter.
	 * @param paramName the name of the parameter
	 * @param typeName the type name of the parameter
	 */
	public void registerTypeName(String paramName, String typeName) {
		Assert.notNull(paramName, "Parameter name must not be null");
		this.typeNames.put(paramName, typeName);
	}

	/**
	 * Return the SQL type for the given parameter, if registered.
	 * @param paramName the name of the parameter
	 * @return the SQL type of the parameter,
	 * or {@code TYPE_UNKNOWN} if not registered
	 */
	@Override
	public int getSqlType(String paramName) {
		Assert.notNull(paramName, "Parameter name must not be null");
		return this.sqlTypes.getOrDefault(paramName, TYPE_UNKNOWN);
	}

	/**
	 * Return the type name for the given parameter, if registered.
	 * @param paramName the name of the parameter
	 * @return the type name of the parameter,
	 * or {@code null} if not registered
	 */
	@Override
	@Nullable
	public String getTypeName(String paramName) {
		Assert.notNull(paramName, "Parameter name must not be null");
		return this.typeNames.get(paramName);
	}


	/**
	 * Enumerate the parameter names and values with their corresponding SQL type if available,
	 * or just return the simple {@code SqlParameterSource} implementation class name otherwise.
	 * @since 5.2
	 * @see #getParameterNames()
	 */
	@Override
	public String toString() {
		String[] parameterNames = getParameterNames();
		if (parameterNames != null) {
			StringJoiner result = new StringJoiner(", ", getClass().getSimpleName() + " {", "}");
			for (String parameterName : parameterNames) {
				Object value = getValue(parameterName);
				if (value instanceof SqlParameterValue sqlParameterValue) {
					value = sqlParameterValue.getValue();
				}
				String typeName = getTypeName(parameterName);
				if (typeName == null) {
					int sqlType = getSqlType(parameterName);
					if (sqlType != TYPE_UNKNOWN) {
						typeName = JdbcUtils.resolveTypeName(sqlType);
						if (typeName == null) {
							typeName = String.valueOf(sqlType);
						}
					}
				}
				StringBuilder entry = new StringBuilder();
				entry.append(parameterName).append('=').append(value);
				if (typeName != null) {
					entry.append(" (type:").append(typeName).append(')');
				}
				result.add(entry);
			}
			return result.toString();
		}
		else {
			return getClass().getSimpleName();
		}
	}

}
