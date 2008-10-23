/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.jdbc.support.nativejdbc;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * {@link NativeJdbcExtractor} implementation that delegates to JDBC 4.0's
 * <code>unwrap</code> method, as defined by {@link java.sql.Wrapper}.
 *
 * <p>Note: Only use this when actually running against a JDBC 4.0 driver,
 * with a connection pool that supports the JDBC 4.0 API (i.e. at least accepts
 * JDBC 4.0 API calls and passes them through to the underlying driver)!
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see java.sql.Wrapper#unwrap
 * @see SimpleNativeJdbcExtractor
 * @see org.springframework.jdbc.core.JdbcTemplate#setNativeJdbcExtractor
 * @see org.springframework.jdbc.support.lob.OracleLobHandler#setNativeJdbcExtractor
 */
public class Jdbc4NativeJdbcExtractor extends NativeJdbcExtractorAdapter {

	protected Connection doGetNativeConnection(Connection con) throws SQLException {
		return (Connection) con.unwrap(Connection.class);
	}

	public Statement getNativeStatement(Statement stmt) throws SQLException {
		return (Statement) stmt.unwrap(Statement.class);
	}

	public PreparedStatement getNativePreparedStatement(PreparedStatement ps) throws SQLException {
		return (PreparedStatement) ps.unwrap(PreparedStatement.class);
	}

	public CallableStatement getNativeCallableStatement(CallableStatement cs) throws SQLException {
		return (CallableStatement) cs.unwrap(CallableStatement.class);
	}

	public ResultSet getNativeResultSet(ResultSet rs) throws SQLException {
		return (ResultSet) rs.unwrap(ResultSet.class);
	}

}
