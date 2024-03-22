/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.jdbc.core;

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetProvider;

import org.springframework.jdbc.support.rowset.ResultSetWrappingSqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSet;

/**
 * {@link ResultSetExtractor} implementation that returns a Spring {@link SqlRowSet}
 * representation for each given {@link ResultSet}.
 *
 * <p>The default implementation uses a standard JDBC CachedRowSet underneath.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see #newCachedRowSet
 * @see org.springframework.jdbc.support.rowset.SqlRowSet
 * @see JdbcTemplate#queryForRowSet(String)
 * @see javax.sql.rowset.CachedRowSet
 */
public class SqlRowSetResultSetExtractor implements ResultSetExtractor<SqlRowSet> {

	private static final RowSetFactory rowSetFactory;

	static {
		try {
			rowSetFactory = RowSetProvider.newFactory();
		}
		catch (SQLException ex) {
			throw new IllegalStateException("Cannot create RowSetFactory through RowSetProvider", ex);
		}
	}


	@Override
	public SqlRowSet extractData(ResultSet rs) throws SQLException {
		return createSqlRowSet(rs);
	}

	/**
	 * Create a {@link SqlRowSet} that wraps the given {@link ResultSet},
	 * representing its data in a disconnected fashion.
	 * <p>This implementation creates a Spring {@link ResultSetWrappingSqlRowSet}
	 * instance that wraps a standard JDBC {@link CachedRowSet} instance.
	 * Can be overridden to use a different implementation.
	 * @param rs the original ResultSet (connected)
	 * @return the disconnected SqlRowSet
	 * @throws SQLException if thrown by JDBC methods
	 * @see #newCachedRowSet()
	 * @see org.springframework.jdbc.support.rowset.ResultSetWrappingSqlRowSet
	 */
	protected SqlRowSet createSqlRowSet(ResultSet rs) throws SQLException {
		CachedRowSet rowSet = newCachedRowSet();
		rowSet.populate(rs);
		return new ResultSetWrappingSqlRowSet(rowSet);
	}

	/**
	 * Create a new {@link CachedRowSet} instance, to be populated by
	 * the {@code createSqlRowSet} implementation.
	 * <p>The default implementation uses JDBC's {@link RowSetFactory}.
	 * @return a new CachedRowSet instance
	 * @throws SQLException if thrown by JDBC methods
	 * @see #createSqlRowSet
	 * @see RowSetProvider#newFactory()
	 * @see RowSetFactory#createCachedRowSet()
	 */
	protected CachedRowSet newCachedRowSet() throws SQLException {
		return rowSetFactory.createCachedRowSet();
	}

}
