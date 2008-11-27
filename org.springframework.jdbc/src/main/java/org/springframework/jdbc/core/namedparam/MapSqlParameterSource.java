/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.jdbc.core.namedparam;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.util.Assert;

/**
 * {@link SqlParameterSource} implementation that holds a given Map of parameters.
 *
 * <p>This class is intended for passing in a simple Map of parameter values
 * to the methods of the {@link NamedParameterJdbcTemplate} class.
 *
 * <p>The <code>addValue</code> methods on this class will make adding several
 * values easier. The methods return a reference to the {@link MapSqlParameterSource}
 * itself, so you can chain several method calls together within a single statement.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 2.0
 * @see #addValue(String, Object)
 * @see #addValue(String, Object, int)
 * @see #registerSqlType
 * @see NamedParameterJdbcTemplate
 */
public class MapSqlParameterSource extends AbstractSqlParameterSource {

	private final Map<String, Object> values = new HashMap<String, Object>();


	/**
	 * Create an empty MapSqlParameterSource,
	 * with values to be added via <code>addValue</code>.
	 * @see #addValue(String, Object)
	 */
	public MapSqlParameterSource() {
	}

	/**
	 * Create a new MapSqlParameterSource, with one value
	 * comprised of the supplied arguments.
	 * @param paramName the name of the parameter
	 * @param value the value of the parameter
	 * @see #addValue(String, Object)
	 */
	public MapSqlParameterSource(String paramName, Object value) {
		addValue(paramName, value);
	}

	/**
	 * Create a new MapSqlParameterSource based on a Map.
	 * @param values a Map holding existing parameter values (can be <code>null</code>)
	 */
	public MapSqlParameterSource(Map<String, ?> values) {
		addValues(values);
	}


	/**
	 * Add a parameter to this parameter source.
	 * @param paramName the name of the parameter
	 * @param value the value of the parameter
	 * @return a reference to this parameter source,
	 * so it's possible to chain several calls together
	 */
	public MapSqlParameterSource addValue(String paramName, Object value) {
		Assert.notNull(paramName, "Parameter name must not be null");
		this.values.put(paramName, value);
		if (value instanceof SqlParameterValue) {
			registerSqlType(paramName, ((SqlParameterValue) value).getSqlType());
		}
		return this;
	}

	/**
	 * Add a parameter to this parameter source.
	 * @param paramName the name of the parameter
	 * @param value the value of the parameter
	 * @param sqlType the SQL type of the parameter
	 * @return a reference to this parameter source,
	 * so it's possible to chain several calls together
	 */
	public MapSqlParameterSource addValue(String paramName, Object value, int sqlType) {
		Assert.notNull(paramName, "Parameter name must not be null");
		this.values.put(paramName, value);
		registerSqlType(paramName, sqlType);
		return this;
	}

	/**
	 * Add a parameter to this parameter source.
	 * @param paramName the name of the parameter
	 * @param value the value of the parameter
	 * @param sqlType the SQL type of the parameter
	 * @param typeName the type name of the parameter
	 * @return a reference to this parameter source,
	 * so it's possible to chain several calls together
	 */
	public MapSqlParameterSource addValue(String paramName, Object value, int sqlType, String typeName) {
		Assert.notNull(paramName, "Parameter name must not be null");
		this.values.put(paramName, value);
		registerSqlType(paramName, sqlType);
		registerTypeName(paramName, typeName);
		return this;
	}

	/**
	 * Add a Map of parameters to this parameter source.
	 * @param values a Map holding existing parameter values (can be <code>null</code>)
	 * @return a reference to this parameter source,
	 * so it's possible to chain several calls together
	 */
	public MapSqlParameterSource addValues(Map<String, ?> values) {
		if (values != null) {
			for (Map.Entry<String, ?> entry : values.entrySet()) {
				this.values.put(entry.getKey(), entry.getValue());
				if (entry.getValue() instanceof SqlParameterValue) {
					SqlParameterValue value = (SqlParameterValue) entry.getValue();
					registerSqlType(entry.getKey(), value.getSqlType());
				}
			}
		}
		return this;
	}

	/**
	 * Expose the current parameter values as read-only Map.
	 */
	public Map<String, Object> getValues() {
		return Collections.unmodifiableMap(this.values);
	}


	public boolean hasValue(String paramName) {
		return this.values.containsKey(paramName);
	}

	public Object getValue(String paramName) {
		if (!hasValue(paramName)) {
			throw new IllegalArgumentException("No value registered for key '" + paramName + "'");
		}
		return this.values.get(paramName);
	}

}
