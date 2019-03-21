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

package org.springframework.jdbc.core.namedparam;

import java.util.HashMap;
import java.util.Map;

import org.springframework.jdbc.core.SqlParameterValue;

/**
 * Class that provides helper methods for the use of {@link SqlParameterSource},
 * in particular with {@link NamedParameterJdbcTemplate}.
 *
 * @author Thomas Risberg
 * @since 2.5
 */
public class SqlParameterSourceUtils {

	/**
	 * Create an array of {@link MapSqlParameterSource} objects populated with data from
	 * the values passed in. This will define what is included in a batch operation.
	 * @param valueMaps array of {@link Map} instances containing the values to be used
	 * @return an array of {@link SqlParameterSource}
	 * @see MapSqlParameterSource
	 * @see NamedParameterJdbcTemplate#batchUpdate(String, Map[])
	 */
	public static SqlParameterSource[] createBatch(Map<String, ?>[] valueMaps) {
		MapSqlParameterSource[] batch = new MapSqlParameterSource[valueMaps.length];
		for (int i = 0; i < valueMaps.length; i++) {
			batch[i] = new MapSqlParameterSource(valueMaps[i]);
		}
		return batch;
	}

	/**
	 * Create an array of {@link BeanPropertySqlParameterSource} objects populated with data
	 * from the values passed in. This will define what is included in a batch operation.
	 * @param beans object array of beans containing the values to be used
	 * @return an array of {@link SqlParameterSource}
	 * @see BeanPropertySqlParameterSource
	 * @see NamedParameterJdbcTemplate#batchUpdate(String, SqlParameterSource[])
	 */
	public static SqlParameterSource[] createBatch(Object[] beans) {
		BeanPropertySqlParameterSource[] batch = new BeanPropertySqlParameterSource[beans.length];
		for (int i = 0; i < beans.length; i++) {
			batch[i] = new BeanPropertySqlParameterSource(beans[i]);
		}
		return batch;
	}

	/**
	 * Create a wrapped value if parameter has type information, plain object if not.
	 * @param source the source of parameter values and type information
	 * @param parameterName the name of the parameter
	 * @return the value object
	 */
	public static Object getTypedValue(SqlParameterSource source, String parameterName) {
		int sqlType = source.getSqlType(parameterName);
		if (sqlType != SqlParameterSource.TYPE_UNKNOWN) {
			if (source.getTypeName(parameterName) != null) {
				return new SqlParameterValue(sqlType, source.getTypeName(parameterName), source.getValue(parameterName));
			}
			else {
				return new SqlParameterValue(sqlType, source.getValue(parameterName));
			}
		}
		else {
			return source.getValue(parameterName);
		}
	}

	/**
	 * Create a Map of case insensitive parameter names together with the original name.
	 * @param parameterSource the source of parameter names
	 * @return the Map that can be used for case insensitive matching of parameter names
	 */
	public static Map<String, String> extractCaseInsensitiveParameterNames(SqlParameterSource parameterSource) {
		Map<String, String> caseInsensitiveParameterNames = new HashMap<String, String>();
		if (parameterSource instanceof BeanPropertySqlParameterSource) {
			String[] propertyNames = ((BeanPropertySqlParameterSource) parameterSource).getReadablePropertyNames();
			for (String name : propertyNames) {
				caseInsensitiveParameterNames.put(name.toLowerCase(), name);
			}
		}
		else if (parameterSource instanceof MapSqlParameterSource) {
			for (String name : ((MapSqlParameterSource) parameterSource).getValues().keySet()) {
				caseInsensitiveParameterNames.put(name.toLowerCase(), name);
			}
		}
		return caseInsensitiveParameterNames;
	}

}
