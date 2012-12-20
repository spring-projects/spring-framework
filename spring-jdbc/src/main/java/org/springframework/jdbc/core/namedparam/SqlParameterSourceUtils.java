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

package org.springframework.jdbc.core.namedparam;

import java.util.HashMap;
import java.util.Map;
import org.springframework.jdbc.core.SqlParameterValue;

/**
 * Class that provides helper methods for the use of {@link SqlParameterSource}
 * with <code>SimpleJdbc</code> classes.
 *
 * @author Thomas Risberg
 * @since 2.5
 */
public class SqlParameterSourceUtils {

	/**
	 * Create an array of MapSqlParameterSource objects populated with data from the
	 * values passed in. This will define what is included in a batch operation.
	 * @param valueMaps array of Maps containing the values to be used
	 * @return an array of SqlParameterSource
	 */
	public static SqlParameterSource[] createBatch(Map<String, ?>[] valueMaps) {
		MapSqlParameterSource[] batch = new MapSqlParameterSource[valueMaps.length];
		for (int i = 0; i < valueMaps.length; i++) {
			Map<String, ?> valueMap = valueMaps[i];
			batch[i] = new MapSqlParameterSource(valueMap);
		}
		return batch;
	}

	/**
	 * Create an array of BeanPropertySqlParameterSource objects populated with data
	 * from the values passed in. This will define what is included in a batch operation.
	 * @param beans object array of beans containing the values to be used
	 * @return an array of SqlParameterSource
	 */
	public static SqlParameterSource[] createBatch(Object[] beans) {
		BeanPropertySqlParameterSource[] batch = new BeanPropertySqlParameterSource[beans.length];
		for (int i = 0; i < beans.length; i++) {
			Object bean = beans[i];
			batch[i] = new BeanPropertySqlParameterSource(bean);
		}
		return batch;
	}

	/**
	 * Create a wrapped value if parameter has type information, plain object if not.
	 * @param source the source of paramer values and type information
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
 * @param parameterSource the source of paramer names
 * @return the Map that can be used for case insensitive matching of parameter names
 */
	public static Map<String, String> extractCaseInsensitiveParameterNames(SqlParameterSource parameterSource) {
		Map<String, String> caseInsensitiveParameterNames = new HashMap<String, String>();
		if (parameterSource instanceof BeanPropertySqlParameterSource) {
			String[] propertyNames = ((BeanPropertySqlParameterSource)parameterSource).getReadablePropertyNames();
			for (int i = 0; i < propertyNames.length; i++) {
				String name = propertyNames[i];
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
