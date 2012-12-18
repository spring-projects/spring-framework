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
 * Abstract base class for {@link DataFieldMaxValueIncrementer} implementations that use
 * a database sequence. Subclasses need to provide the database-specific SQL to use.
 *
 * @author Juergen Hoeller
 * @since 26.02.2004
 * @see #getSequenceQuery
 */
public abstract class AbstractSequenceMaxValueIncrementer extends AbstractDataFieldMaxValueIncrementer {

	/**
	 * Default constructor for bean property style usage.
	 * @see #setDataSource
	 * @see #setIncrementerName
	 */
	public AbstractSequenceMaxValueIncrementer() {
	}

	/**
	 * Convenience constructor.
	 * @param dataSource the DataSource to use
	 * @param incrementerName the name of the sequence/table to use
	 */
	public AbstractSequenceMaxValueIncrementer(DataSource dataSource, String incrementerName) {
		super(dataSource, incrementerName);
	}


	/**
	 * Executes the SQL as specified by {@link #getSequenceQuery()}.
	 */
	@Override
	protected long getNextKey() throws DataAccessException {
		Connection con = DataSourceUtils.getConnection(getDataSource());
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = con.createStatement();
			DataSourceUtils.applyTransactionTimeout(stmt, getDataSource());
			rs = stmt.executeQuery(getSequenceQuery());
			if (rs.next()) {
				return rs.getLong(1);
			}
			else {
				throw new DataAccessResourceFailureException("Sequence query did not return a result");
			}
		}
		catch (SQLException ex) {
			throw new DataAccessResourceFailureException("Could not obtain sequence value", ex);
		}
		finally {
			JdbcUtils.closeResultSet(rs);
			JdbcUtils.closeStatement(stmt);
			DataSourceUtils.releaseConnection(con, getDataSource());
		}
	}

	/**
	 * Return the database-specific query to use for retrieving a sequence value.
	 * <p>The provided SQL is supposed to result in a single row with a single
	 * column that allows for extracting a {@code long} value.
	 */
	protected abstract String getSequenceQuery();

}
