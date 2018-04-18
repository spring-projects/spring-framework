/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.jdbc.support.rowset;

import org.springframework.jdbc.InvalidResultSetAccessException;

/**
 * Metadata interface for Spring's {@link SqlRowSet}, analogous to JDBC's
 * {@link java.sql.ResultSetMetaData}.
 *
 * <p>The main difference to the standard JDBC ResultSetMetaData is that a
 * {@link java.sql.SQLException} is never thrown here. This allows
 * SqlRowSetMetaData to be used without having to deal with checked exceptions.
 * SqlRowSetMetaData will throw Spring's {@link InvalidResultSetAccessException}
 * instead (when appropriate).
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 1.2
 * @see SqlRowSet#getMetaData()
 * @see java.sql.ResultSetMetaData
 * @see org.springframework.jdbc.InvalidResultSetAccessException
 */
public interface SqlRowSetMetaData {

	/**
	 * Retrieve the catalog name of the table that served as the source for the
	 * specified column.
	 * @param columnIndex the index of the column
	 * @return the catalog name
	 * @see java.sql.ResultSetMetaData#getCatalogName(int)
	 */
	String getCatalogName(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * Retrieve the fully qualified class that the specified column will be mapped to.
	 * @param columnIndex the index of the column
	 * @return the class name as a String
	 * @see java.sql.ResultSetMetaData#getColumnClassName(int)
	 */
	String getColumnClassName(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * Retrieve the number of columns in the RowSet.
	 * @return the number of columns
	 * @see java.sql.ResultSetMetaData#getColumnCount()
	 */
	int getColumnCount() throws InvalidResultSetAccessException;

	/**
	 * Return the column names of the table that the result set represents.
	 * @return the column names
	 */
	String[] getColumnNames() throws InvalidResultSetAccessException;

	/**
	 * Retrieve the maximum width of the designated column.
	 * @param columnIndex the index of the column
	 * @return the width of the column
	 * @see java.sql.ResultSetMetaData#getColumnDisplaySize(int)
	 */
	int getColumnDisplaySize(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * Retrieve the suggested column title for the column specified.
	 * @param columnIndex the index of the column
	 * @return the column title
	 * @see java.sql.ResultSetMetaData#getColumnLabel(int)
	 */
	String getColumnLabel(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * Retrieve the column name for the indicated column.
	 * @param columnIndex the index of the column
	 * @return the column name
	 * @see java.sql.ResultSetMetaData#getColumnName(int)
	 */
	String getColumnName(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * Retrieve the SQL type code for the indicated column.
	 * @param columnIndex the index of the column
	 * @return the SQL type code
	 * @see java.sql.ResultSetMetaData#getColumnType(int)
	 * @see java.sql.Types
	 */
	int getColumnType(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * Retrieve the DBMS-specific type name for the indicated column.
	 * @param columnIndex the index of the column
	 * @return the type name
	 * @see java.sql.ResultSetMetaData#getColumnTypeName(int)
	 */
	String getColumnTypeName(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * Retrieve the precision for the indicated column.
	 * @param columnIndex the index of the column
	 * @return the precision
	 * @see java.sql.ResultSetMetaData#getPrecision(int)
	 */
	int getPrecision(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * Retrieve the scale of the indicated column.
	 * @param columnIndex the index of the column
	 * @return the scale
	 * @see java.sql.ResultSetMetaData#getScale(int)
	 */
	int getScale(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * Retrieve the schema name of the table that served as the source for the
	 * specified column.
	 * @param columnIndex the index of the column
	 * @return the schema name
	 * @see java.sql.ResultSetMetaData#getSchemaName(int)
	 */
	String getSchemaName(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * Retrieve the name of the table that served as the source for the
	 * specified column.
	 * @param columnIndex the index of the column
	 * @return the name of the table
	 * @see java.sql.ResultSetMetaData#getTableName(int)
	 */
	String getTableName(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * Indicate whether the case of the designated column is significant.
	 * @param columnIndex the index of the column
	 * @return true if the case sensitive, false otherwise
	 * @see java.sql.ResultSetMetaData#isCaseSensitive(int)
	 */
	boolean isCaseSensitive(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * Indicate whether the designated column contains a currency value.
	 * @param columnIndex the index of the column
	 * @return true if the value is a currency value, false otherwise
	 * @see java.sql.ResultSetMetaData#isCurrency(int)
	 */
	boolean isCurrency(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * Indicate whether the designated column contains a signed number.
	 * @param columnIndex the index of the column
	 * @return true if the column contains a signed number, false otherwise
	 * @see java.sql.ResultSetMetaData#isSigned(int)
	 */
	boolean isSigned(int columnIndex) throws InvalidResultSetAccessException;

}
