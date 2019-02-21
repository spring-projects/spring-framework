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

package org.springframework.jdbc.core;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;

import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedCaseInsensitiveMap;

/**
 * {@link RowMapper} implementation that creates a {@code java.util.Map}
 * for each row, representing all columns as key-value pairs: one
 * entry for each column, with the column name as key.
 *
 * <p>The Map implementation to use and the key to use for each column
 * in the column Map can be customized through overriding
 * {@link #createColumnMap} and {@link #getColumnKey}, respectively.
 *
 * <p><b>Note:</b> By default, ColumnMapRowMapper will try to build a linked Map
 * with case-insensitive keys, to preserve column order as well as allow any
 * casing to be used for column names. This requires Commons Collections on the
 * classpath (which will be autodetected). Else, the fallback is a standard linked
 * HashMap, which will still preserve column order but requires the application
 * to specify the column names in the same casing as exposed by the driver.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see JdbcTemplate#queryForList(String)
 * @see JdbcTemplate#queryForMap(String)
 */
public class ColumnMapRowMapper implements RowMapper<Map<String, Object>> {

	@Override
	public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
		ResultSetMetaData rsmd = rs.getMetaData();
		int columnCount = rsmd.getColumnCount();
		Map<String, Object> mapOfColumnValues = createColumnMap(columnCount);
		for (int i = 1; i <= columnCount; i++) {
			String column = JdbcUtils.lookupColumnName(rsmd, i);
			mapOfColumnValues.putIfAbsent(getColumnKey(column), getColumnValue(rs, i));
		}
		return mapOfColumnValues;
	}

	/**
	 * Create a Map instance to be used as column map.
	 * <p>By default, a linked case-insensitive Map will be created.
	 * @param columnCount the column count, to be used as initial
	 * capacity for the Map
	 * @return the new Map instance
	 * @see org.springframework.util.LinkedCaseInsensitiveMap
	 */
	protected Map<String, Object> createColumnMap(int columnCount) {
		return new LinkedCaseInsensitiveMap<>(columnCount);
	}

	/**
	 * Determine the key to use for the given column in the column Map.
	 * @param columnName the column name as returned by the ResultSet
	 * @return the column key to use
	 * @see java.sql.ResultSetMetaData#getColumnName
	 */
	protected String getColumnKey(String columnName) {
		return columnName;
	}

	/**
	 * Retrieve a JDBC object value for the specified column.
	 * <p>The default implementation uses the {@code getObject} method.
	 * Additionally, this implementation includes a "hack" to get around Oracle
	 * returning a non standard object for their TIMESTAMP datatype.
	 * @param rs is the ResultSet holding the data
	 * @param index is the column index
	 * @return the Object returned
	 * @see org.springframework.jdbc.support.JdbcUtils#getResultSetValue
	 */
	@Nullable
	protected Object getColumnValue(ResultSet rs, int index) throws SQLException {
		return JdbcUtils.getResultSetValue(rs, index);
	}

}
