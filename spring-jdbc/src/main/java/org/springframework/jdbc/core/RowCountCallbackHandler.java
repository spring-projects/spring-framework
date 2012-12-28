/*
 * Copyright 2002-2006 the original author or authors.
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

import org.springframework.jdbc.support.JdbcUtils;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * Implementation of RowCallbackHandler. Convenient superclass for callback handlers.
 * An instance can only be used once.
 *
 * <p>We can either use this on its own (for example, in a test case, to ensure
 * that our result sets have valid dimensions), or use it as a superclass
 * for callback handlers that actually do something, and will benefit
 * from the dimension information it provides.
 *
 * <p>A usage example with JdbcTemplate:
 *
 * <pre class="code">JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);  // reusable object
 *
 * RowCountCallbackHandler countCallback = new RowCountCallbackHandler();  // not reusable
 * jdbcTemplate.query("select * from user", countCallback);
 * int rowCount = countCallback.getRowCount();</pre>
 *
 * @author Rod Johnson
 * @since May 3, 2001
 */
public class RowCountCallbackHandler implements RowCallbackHandler {

	/** Rows we've seen so far */
	private int rowCount;

	/** Columns we've seen so far */
	private int columnCount;

	/**
	 * Indexed from 0. Type (as in java.sql.Types) for the columns
	 * as returned by ResultSetMetaData object.
	 */
	private int[] columnTypes;

	/**
	 * Indexed from 0. Column name as returned by ResultSetMetaData object.
	 */
	private String[] columnNames;


	/**
	 * Implementation of ResultSetCallbackHandler.
	 * Work out column size if this is the first row, otherwise just count rows.
	 * <p>Subclasses can perform custom extraction or processing
	 * by overriding the {@code processRow(ResultSet, int)} method.
	 * @see #processRow(java.sql.ResultSet, int)
	 */
	public final void processRow(ResultSet rs) throws SQLException {
		if (this.rowCount == 0) {
			ResultSetMetaData rsmd = rs.getMetaData();
			this.columnCount = rsmd.getColumnCount();
			this.columnTypes = new int[this.columnCount];
			this.columnNames = new String[this.columnCount];
			for (int i = 0; i < this.columnCount; i++) {
				this.columnTypes[i] = rsmd.getColumnType(i + 1);
				this.columnNames[i] = JdbcUtils.lookupColumnName(rsmd, i + 1);
			}
			// could also get column names
		}
		processRow(rs, this.rowCount++);
	}

	/**
	 * Subclasses may override this to perform custom extraction
	 * or processing. This class's implementation does nothing.
	 * @param rs ResultSet to extract data from. This method is
	 * invoked for each row
	 * @param rowNum number of the current row (starting from 0)
	 */
	protected void processRow(ResultSet rs, int rowNum) throws SQLException {
	}


	/**
	 * Return the types of the columns as java.sql.Types constants
	 * Valid after processRow is invoked the first time.
	 * @return the types of the columns as java.sql.Types constants.
	 * <b>Indexed from 0 to n-1.</b>
	 */
	public final int[] getColumnTypes() {
		return columnTypes;
	}

	/**
	 * Return the names of the columns.
	 * Valid after processRow is invoked the first time.
	 * @return the names of the columns.
	 * <b>Indexed from 0 to n-1.</b>
	 */
	public final String[] getColumnNames() {
		return columnNames;
	}

	/**
	 * Return the row count of this ResultSet
	 * Only valid after processing is complete
	 * @return the number of rows in this ResultSet
	 */
	public final int getRowCount() {
		return rowCount;
	}

	/**
	 * Return the number of columns in this result set.
	 * Valid once we've seen the first row,
	 * so subclasses can use it during processing
	 * @return the number of columns in this result set
	 */
	public final int getColumnCount() {
		return columnCount;
	}

}
