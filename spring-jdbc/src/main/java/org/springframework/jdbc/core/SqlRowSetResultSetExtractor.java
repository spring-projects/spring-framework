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

package org.springframework.jdbc.core;

import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetProvider;

import org.springframework.core.JdkVersion;
import org.springframework.jdbc.support.rowset.ResultSetWrappingSqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSet;

/**
 * {@link ResultSetExtractor} implementation that returns a Spring {@link SqlRowSet}
 * representation for each given {@link ResultSet}.
 *
 * <p>The default implementation uses a standard JDBC CachedRowSet underneath.
 * This means that JDBC RowSet support needs to be available at runtime:
 * by default, Sun's <code>com.sun.rowset.CachedRowSetImpl</code> class on Java 5 and 6,
 * or the <code>javax.sql.rowset.RowSetProvider</code> mechanism on Java 7 / JDBC 4.1.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see #newCachedRowSet
 * @see org.springframework.jdbc.support.rowset.SqlRowSet
 * @see JdbcTemplate#queryForRowSet(String)
 * @see javax.sql.rowset.CachedRowSet
 */
public class SqlRowSetResultSetExtractor implements ResultSetExtractor<SqlRowSet> {

	private static final CachedRowSetFactory cachedRowSetFactory;

	static {
		ClassLoader cl = SqlRowSetResultSetExtractor.class.getClassLoader();
		try {
			Class<?> rowSetProviderClass = cl.loadClass("javax.sql.rowset.RowSetProvider");
			Method newFactory = rowSetProviderClass.getMethod("newFactory");
			rowSetFactory = ReflectionUtils.invokeMethod(newFactory, null);
			createCachedRowSet = rowSetFactory.getClass().getMethod("createCachedRowSet");
		}
		else {
			// JDBC 4.1 API not available - fall back to Sun CachedRowSetImpl
			cachedRowSetFactory = new SunCachedRowSetFactory();
		}
	}


	public SqlRowSet extractData(ResultSet rs) throws SQLException {
		return createSqlRowSet(rs);
	}

	/**
	 * Create a SqlRowSet that wraps the given ResultSet,
	 * representing its data in a disconnected fashion.
	 * <p>This implementation creates a Spring ResultSetWrappingSqlRowSet
	 * instance that wraps a standard JDBC CachedRowSet instance.
	 * Can be overridden to use a different implementation.
	 * @param rs the original ResultSet (connected)
	 * @return the disconnected SqlRowSet
	 * @throws SQLException if thrown by JDBC methods
	 * @see #newCachedRowSet
	 * @see org.springframework.jdbc.support.rowset.ResultSetWrappingSqlRowSet
	 */
	protected SqlRowSet createSqlRowSet(ResultSet rs) throws SQLException {
		CachedRowSet rowSet = newCachedRowSet();
		rowSet.populate(rs);
		return new ResultSetWrappingSqlRowSet(rowSet);
	}

	/**
	 * Create a new CachedRowSet instance, to be populated by
	 * the <code>createSqlRowSet</code> implementation.
	 * <p>The default implementation uses JDBC 4.1's RowSetProvider
	 * when running on JDK 7 or higher, falling back to Sun's
	 * <code>com.sun.rowset.CachedRowSetImpl</code> class on older JDKs.
	 * @return a new CachedRowSet instance
	 * @throws SQLException if thrown by JDBC methods
	 * @see #createSqlRowSet
	 */
	protected CachedRowSet newCachedRowSet() throws SQLException {
		return cachedRowSetFactory.createCachedRowSet();
	}


	/**
	 * Internal strategy interface for the creation of CachedRowSet instances.
	 */
	private interface CachedRowSetFactory {

		CachedRowSet createCachedRowSet() throws SQLException;
	}


	/**
	 * Inner class to avoid a hard dependency on JDBC 4.1 RowSetProvider class.
	 */
	private static class StandardCachedRowSetFactory implements CachedRowSetFactory {

		private final RowSetFactory rowSetFactory;

		public StandardCachedRowSetFactory() {
			try {
				this.rowSetFactory = RowSetProvider.newFactory();
			}
			catch (SQLException ex) {
				throw new IllegalStateException("Cannot create RowSetFactory through RowSetProvider", ex);
			}
		}

		public CachedRowSet createCachedRowSet() throws SQLException {
			return this.rowSetFactory.createCachedRowSet();
		}
	}


	/**
	 * Inner class to avoid a hard dependency on Sun's CachedRowSetImpl class.
	 */
	private static class SunCachedRowSetFactory implements CachedRowSetFactory {

		public CachedRowSet createCachedRowSet() throws SQLException {
			return new com.sun.rowset.CachedRowSetImpl();
		}
	}

}
