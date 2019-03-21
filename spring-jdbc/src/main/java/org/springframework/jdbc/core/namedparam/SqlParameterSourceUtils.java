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

package org.springframework.jdbc.core.namedparam;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.lang.Nullable;

/**
 * Class that provides helper methods for the use of {@link SqlParameterSource},
 * in particular with {@link NamedParameterJdbcTemplate}.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 2.5
 */
public abstract class SqlParameterSourceUtils {

	/**
	 * Create an array of {@link SqlParameterSource} objects populated with data
	 * from the values passed in (either a {@link Map} or a bean object).
	 * This will define what is included in a batch operation.
	 * @param candidates object array of objects containing the values to be used
	 * @return an array of {@link SqlParameterSource}
	 * @see MapSqlParameterSource
	 * @see BeanPropertySqlParameterSource
	 * @see NamedParameterJdbcTemplate#batchUpdate(String, SqlParameterSource[])
	 */
	@SuppressWarnings("unchecked")
	public static SqlParameterSource[] createBatch(Object... candidates) {
		return createBatch(Arrays.asList(candidates));
	}

	/**
	 * Create an array of {@link SqlParameterSource} objects populated with data
	 * from the values passed in (either a {@link Map} or a bean object).
	 * This will define what is included in a batch operation.
	 * @param candidates collection of objects containing the values to be used
	 * @return an array of {@link SqlParameterSource}
	 * @since 5.0.2
	 * @see MapSqlParameterSource
	 * @see BeanPropertySqlParameterSource
	 * @see NamedParameterJdbcTemplate#batchUpdate(String, SqlParameterSource[])
	 */
	@SuppressWarnings("unchecked")
	public static SqlParameterSource[] createBatch(Collection<?> candidates) {
		SqlParameterSource[] batch = new SqlParameterSource[candidates.size()];
		int i = 0;
		for (Object candidate : candidates) {
			batch[i] = (candidate instanceof Map ? new MapSqlParameterSource((Map<String, ?>) candidate) :
					new BeanPropertySqlParameterSource(candidate));
			i++;
		}
		return batch;
	}

	/**
	 * Create an array of {@link MapSqlParameterSource} objects populated with data from
	 * the values passed in. This will define what is included in a batch operation.
	 * @param valueMaps array of {@link Map} instances containing the values to be used
	 * @return an array of {@link SqlParameterSource}
	 * @see MapSqlParameterSource
	 * @see NamedParameterJdbcTemplate#batchUpdate(String, Map[])
	 */
	public static SqlParameterSource[] createBatch(Map<String, ?>[] valueMaps) {
		SqlParameterSource[] batch = new SqlParameterSource[valueMaps.length];
		for (int i = 0; i < valueMaps.length; i++) {
			batch[i] = new MapSqlParameterSource(valueMaps[i]);
		}
		return batch;
	}

	/**
	 * Create a wrapped value if parameter has type information, plain object if not.
	 * @param source the source of parameter values and type information
	 * @param parameterName the name of the parameter
	 * @return the value object
	 */
	@Nullable
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
		Map<String, String> caseInsensitiveParameterNames = new HashMap<>();
		String[] paramNames = parameterSource.getParameterNames();
		if (paramNames != null) {
			for (String name : paramNames) {
				caseInsensitiveParameterNames.put(name.toLowerCase(), name);
			}
		}
		return caseInsensitiveParameterNames;
	}

}
