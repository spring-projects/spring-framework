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

package org.springframework.jdbc.support.incrementer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.JdbcUtils;

/**
 * Abstract base class for {@link DataFieldMaxValueIncrementer} implementations
 * which are based on identity columns in a sequence-like table.
 *
 * @author Juergen Hoeller
 * @author Thomas Risberg
 * @since 4.1.2
 */
public abstract class AbstractIdentityColumnMaxValueIncrementer extends AbstractColumnMaxValueIncrementer {

	private boolean deleteSpecificValues = false;

	/** The current cache of values */
	private long[] valueCache;

	/** The next id to serve from the value cache */
	private int nextValueIndex = -1;


	/**
	 * Default constructor for bean property style usage.
	 * @see #setDataSource
	 * @see #setIncrementerName
	 * @see #setColumnName
	 */
	public AbstractIdentityColumnMaxValueIncrementer() {
	}

	public AbstractIdentityColumnMaxValueIncrementer(DataSource dataSource, String incrementerName, String columnName) {
		super(dataSource, incrementerName, columnName);
	}


	/**
	 * Specify whether to delete the entire range below the current maximum key value
	 * ({@code false} - the default), or the specifically generated values ({@code true}).
	 * The former mode will use a where range clause whereas the latter will use an in
	 * clause starting with the lowest value minus 1, just preserving the maximum value.
	 */
	public void setDeleteSpecificValues(boolean deleteSpecificValues) {
		this.deleteSpecificValues = deleteSpecificValues;
	}

	/**
	 * Return whether to delete the entire range below the current maximum key value
	 * ({@code false} - the default), or the specifically generated values ({@code true}).
	 */
	public boolean isDeleteSpecificValues() {
		return this.deleteSpecificValues;
	}


	@Override
	protected synchronized long getNextKey() throws DataAccessException {
		if (this.nextValueIndex < 0 || this.nextValueIndex >= getCacheSize()) {
			/*
			* Need to use straight JDBC code because we need to make sure that the insert and select
			* are performed on the same connection (otherwise we can't be sure that @@identity
			* returns the correct value)
			*/
			Connection con = DataSourceUtils.getConnection(getDataSource());
			Statement stmt = null;
			try {
				stmt = con.createStatement();
				DataSourceUtils.applyTransactionTimeout(stmt, getDataSource());
				this.valueCache = new long[getCacheSize()];
				this.nextValueIndex = 0;
				for (int i = 0; i < getCacheSize(); i++) {
					stmt.executeUpdate(getIncrementStatement());
					ResultSet rs = stmt.executeQuery(getIdentityStatement());
					try {
						if (!rs.next()) {
							throw new DataAccessResourceFailureException("Identity statement failed after inserting");
						}
						this.valueCache[i] = rs.getLong(1);
					}
					finally {
						JdbcUtils.closeResultSet(rs);
					}
				}
				stmt.executeUpdate(getDeleteStatement(this.valueCache));
			}
			catch (SQLException ex) {
				throw new DataAccessResourceFailureException("Could not increment identity", ex);
			}
			finally {
				JdbcUtils.closeStatement(stmt);
				DataSourceUtils.releaseConnection(con, getDataSource());
			}
		}
		return this.valueCache[this.nextValueIndex++];
	}


	/**
	 * Statement to use to increment the "sequence" value.
	 * @return the SQL statement to use
	 */
	protected abstract String getIncrementStatement();

	/**
	 * Statement to use to obtain the current identity value.
	 * @return the SQL statement to use
	 */
	protected abstract String getIdentityStatement();

	/**
	 * Statement to use to clean up "sequence" values.
	 * <p>The default implementation either deletes the entire range below
	 * the current maximum value, or the specifically generated values
	 * (starting with the lowest minus 1, just preserving the maximum value)
	 * - according to the {@link #isDeleteSpecificValues()} setting.
	 * @param values the currently generated key values
	 * (the number of values corresponds to {@link #getCacheSize()})
	 * @return the SQL statement to use
	 */
	protected String getDeleteStatement(long[] values) {
		StringBuilder sb = new StringBuilder(64);
		sb.append("delete from ").append(getIncrementerName()).append(" where ").append(getColumnName());
		if (isDeleteSpecificValues()) {
			sb.append(" in (").append(values[0] - 1);
			for (int i = 0; i < values.length - 1; i++) {
				sb.append(", ").append(values[i]);
			}
			sb.append(")");
		}
		else {
			long maxValue = values[values.length - 1];
			sb.append(" < ").append(maxValue);
		}
		return sb.toString();
	}

}
