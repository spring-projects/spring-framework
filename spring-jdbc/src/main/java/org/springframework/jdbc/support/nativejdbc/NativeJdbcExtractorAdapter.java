/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.jdbc.support.nativejdbc;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.springframework.jdbc.datasource.DataSourceUtils;

/**
 * Abstract adapter class for the {@link NativeJdbcExtractor} interface,
 * for simplified implementation of basic extractors.
 * Basically returns the passed-in JDBC objects on all methods.
 *
 * <p>{@code getNativeConnection} checks for a ConnectionProxy chain,
 * for example from a TransactionAwareDataSourceProxy, before delegating to
 * {@code doGetNativeConnection} for actual unwrapping. You can override
 * either of the two for a specific connection pool, but the latter is
 * recommended to participate in ConnectionProxy unwrapping.
 *
 * <p>{@code getNativeConnection} also applies a fallback if the first
 * native extraction process failed, that is, returned the same Connection as
 * passed in. It assumes that some additional proxying is going in this case:
 * Hence, it retrieves the underlying native Connection from the DatabaseMetaData
 * via {@code conHandle.getMetaData().getConnection()} and retries the native
 * extraction process based on that Connection handle. This works, for example,
 * for the Connection proxies exposed by Hibernate 3.1's {@code Session.connection()}.
 *
 * <p>The {@code getNativeConnectionFromStatement} method is implemented
 * to simply delegate to {@code getNativeConnection} with the Statement's
 * Connection. This is what most extractor implementations will stick to,
 * unless there's a more efficient version for a specific pool.
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see #getNativeConnection
 * @see #getNativeConnectionFromStatement
 * @see org.springframework.jdbc.datasource.ConnectionProxy
 */
public abstract class NativeJdbcExtractorAdapter implements NativeJdbcExtractor {

	/**
	 * Return {@code false} by default.
	 */
	@Override
	public boolean isNativeConnectionNecessaryForNativeStatements() {
		return false;
	}

	/**
	 * Return {@code false} by default.
	 */
	@Override
	public boolean isNativeConnectionNecessaryForNativePreparedStatements() {
		return false;
	}

	/**
	 * Return {@code false} by default.
	 */
	@Override
	public boolean isNativeConnectionNecessaryForNativeCallableStatements() {
		return false;
	}

	/**
	 * Check for a ConnectionProxy chain, then delegate to doGetNativeConnection.
	 * <p>ConnectionProxy is used by Spring's TransactionAwareDataSourceProxy
	 * and LazyConnectionDataSourceProxy. The target connection behind it is
	 * typically one from a local connection pool, to be unwrapped by the
	 * doGetNativeConnection implementation of a concrete subclass.
	 * @see #doGetNativeConnection
	 * @see org.springframework.jdbc.datasource.ConnectionProxy
	 * @see org.springframework.jdbc.datasource.DataSourceUtils#getTargetConnection
	 * @see org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy
	 * @see org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy
	 */
	@Override
	public Connection getNativeConnection(Connection con) throws SQLException {
		if (con == null) {
			return null;
		}
		Connection targetCon = DataSourceUtils.getTargetConnection(con);
		Connection nativeCon = doGetNativeConnection(targetCon);
		if (nativeCon == targetCon) {
			// We haven't received a different Connection, so we'll assume that there's
			// some additional proxying going on. Let's check whether we get something
			// different back from the DatabaseMetaData.getConnection() call.
			DatabaseMetaData metaData = targetCon.getMetaData();
			// The following check is only really there for mock Connections
			// which might not carry a DatabaseMetaData instance.
			if (metaData != null) {
				Connection metaCon = metaData.getConnection();
				if (metaCon != null && metaCon != targetCon) {
					// We've received a different Connection there:
					// Let's retry the native extraction process with it.
					nativeCon = doGetNativeConnection(metaCon);
				}
			}
		}
		return nativeCon;
	}

	/**
	 * Not able to unwrap: return passed-in Connection.
	 */
	protected Connection doGetNativeConnection(Connection con) throws SQLException {
		return con;
	}

	/**
	 * Retrieve the Connection via the Statement's Connection.
	 * @see #getNativeConnection
	 * @see Statement#getConnection
	 */
	@Override
	public Connection getNativeConnectionFromStatement(Statement stmt) throws SQLException {
		if (stmt == null) {
			return null;
		}
		return getNativeConnection(stmt.getConnection());
	}

	/**
	 * Not able to unwrap: return passed-in Statement.
	 */
	@Override
	public Statement getNativeStatement(Statement stmt) throws SQLException {
		return stmt;
	}

	/**
	 * Not able to unwrap: return passed-in PreparedStatement.
	 */
	@Override
	public PreparedStatement getNativePreparedStatement(PreparedStatement ps) throws SQLException {
		return ps;
	}

	/**
	 * Not able to unwrap: return passed-in CallableStatement.
	 */
	@Override
	public CallableStatement getNativeCallableStatement(CallableStatement cs) throws SQLException {
		return cs;
	}

	/**
	 * Not able to unwrap: return passed-in ResultSet.
	 */
	@Override
	public ResultSet getNativeResultSet(ResultSet rs) throws SQLException {
		return rs;
	}

}
