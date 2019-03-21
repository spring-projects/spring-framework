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

package org.springframework.jdbc.support.rowset;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.springframework.jdbc.InvalidResultSetAccessException;
import org.springframework.lang.Nullable;

/**
 * The default implementation of Spring's {@link SqlRowSetMetaData} interface, wrapping
 * a {@link java.sql.ResultSetMetaData} instance, catching any {@link SQLException}s
 * and translating them to a corresponding Spring {@link InvalidResultSetAccessException}.
 *
 * <p>Used by {@link ResultSetWrappingSqlRowSet}.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 1.2
 * @see ResultSetWrappingSqlRowSet#getMetaData()
 */
public class ResultSetWrappingSqlRowSetMetaData implements SqlRowSetMetaData {

	private final ResultSetMetaData resultSetMetaData;

	@Nullable
	private String[] columnNames;


	/**
	 * Create a new ResultSetWrappingSqlRowSetMetaData object
	 * for the given ResultSetMetaData instance.
	 * @param resultSetMetaData a disconnected ResultSetMetaData instance
	 * to wrap (usually a {@code javax.sql.RowSetMetaData} instance)
	 * @see java.sql.ResultSet#getMetaData
	 * @see javax.sql.RowSetMetaData
	 * @see ResultSetWrappingSqlRowSet#getMetaData
	 */
	public ResultSetWrappingSqlRowSetMetaData(ResultSetMetaData resultSetMetaData) {
		this.resultSetMetaData = resultSetMetaData;
	}


	@Override
	public String getCatalogName(int column) throws InvalidResultSetAccessException {
		try {
			return this.resultSetMetaData.getCatalogName(column);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public String getColumnClassName(int column) throws InvalidResultSetAccessException {
		try {
			return this.resultSetMetaData.getColumnClassName(column);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public int getColumnCount() throws InvalidResultSetAccessException {
		try {
			return this.resultSetMetaData.getColumnCount();
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public String[] getColumnNames() throws InvalidResultSetAccessException {
		if (this.columnNames == null) {
			this.columnNames = new String[getColumnCount()];
			for (int i = 0; i < getColumnCount(); i++) {
				this.columnNames[i] = getColumnName(i + 1);
			}
		}
		return this.columnNames;
	}

	@Override
	public int getColumnDisplaySize(int column) throws InvalidResultSetAccessException {
		try {
			return this.resultSetMetaData.getColumnDisplaySize(column);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public String getColumnLabel(int column) throws InvalidResultSetAccessException {
		try {
			return this.resultSetMetaData.getColumnLabel(column);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public String getColumnName(int column) throws InvalidResultSetAccessException {
		try {
			return this.resultSetMetaData.getColumnName(column);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public int getColumnType(int column) throws InvalidResultSetAccessException {
		try {
			return this.resultSetMetaData.getColumnType(column);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public String getColumnTypeName(int column) throws InvalidResultSetAccessException {
		try {
			return this.resultSetMetaData.getColumnTypeName(column);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public int getPrecision(int column) throws InvalidResultSetAccessException {
		try {
			return this.resultSetMetaData.getPrecision(column);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public int getScale(int column) throws InvalidResultSetAccessException {
		try {
			return this.resultSetMetaData.getScale(column);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public String getSchemaName(int column) throws InvalidResultSetAccessException {
		try {
			return this.resultSetMetaData.getSchemaName(column);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public String getTableName(int column) throws InvalidResultSetAccessException {
		try {
			return this.resultSetMetaData.getTableName(column);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public boolean isCaseSensitive(int column) throws InvalidResultSetAccessException {
		try {
			return this.resultSetMetaData.isCaseSensitive(column);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public boolean isCurrency(int column) throws InvalidResultSetAccessException {
		try {
			return this.resultSetMetaData.isCurrency(column);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public boolean isSigned(int column) throws InvalidResultSetAccessException {
		try {
			return this.resultSetMetaData.isSigned(column);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

}
