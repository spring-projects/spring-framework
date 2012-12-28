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

package org.springframework.jdbc.support.nativejdbc;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * {@link NativeJdbcExtractor} implementation that delegates to JDBC 4.0's
 * {@code unwrap} method, as defined by {@link java.sql.Wrapper}.
 * You will typically need to specify a vendor {@link #setConnectionType Connection type}
 * / {@link #setStatementType Statement type} / {@link #setResultSetType ResultSet type}
 * to extract, since JDBC 4.0 only actually unwraps to a given target type.
 *
 * <p>Note: Only use this when actually running against a JDBC 4.0 driver, with a
 * connection pool that supports the JDBC 4.0 API (i.e. at least accepts JDBC 4.0
 * API calls and passes them through to the underlying driver)! Other than that,
 * there is no need for connection pool specific setup. As of JDBC 4.0,
 * NativeJdbcExtractors will typically be implemented for specific drivers
 * instead of for specific pools (e.g. {@link OracleJdbc4NativeJdbcExtractor}).
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see java.sql.Wrapper#unwrap
 * @see SimpleNativeJdbcExtractor
 * @see org.springframework.jdbc.core.JdbcTemplate#setNativeJdbcExtractor
 * @see org.springframework.jdbc.support.lob.OracleLobHandler#setNativeJdbcExtractor
 */
public class Jdbc4NativeJdbcExtractor extends NativeJdbcExtractorAdapter {

	private Class<? extends Connection> connectionType = Connection.class;

	private Class<? extends Statement> statementType = Statement.class;

	private Class<? extends PreparedStatement> preparedStatementType = PreparedStatement.class;

	private Class<? extends CallableStatement> callableStatementType = CallableStatement.class;

	private Class<? extends ResultSet> resultSetType = ResultSet.class;


	/**
	 * Set the vendor's Connection type, e.g. {@code oracle.jdbc.OracleConnection}.
	 */
	public void setConnectionType(Class<? extends Connection> connectionType) {
		this.connectionType = connectionType;
	}

	/**
	 * Set the vendor's Statement type, e.g. {@code oracle.jdbc.OracleStatement}.
	 */
	public void setStatementType(Class<? extends Statement> statementType) {
		this.statementType = statementType;
	}

	/**
	 * Set the vendor's PreparedStatement type, e.g. {@code oracle.jdbc.OraclePreparedStatement}.
	 */
	public void setPreparedStatementType(Class<? extends PreparedStatement> preparedStatementType) {
		this.preparedStatementType = preparedStatementType;
	}

	/**
	 * Set the vendor's CallableStatement type, e.g. {@code oracle.jdbc.OracleCallableStatement}.
	 */
	public void setCallableStatementType(Class<? extends CallableStatement> callableStatementType) {
		this.callableStatementType = callableStatementType;
	}

	/**
	 * Set the vendor's ResultSet type, e.g. {@code oracle.jdbc.OracleResultSet}.
	 */
	public void setResultSetType(Class<? extends ResultSet> resultSetType) {
		this.resultSetType = resultSetType;
	}


	@Override
	protected Connection doGetNativeConnection(Connection con) throws SQLException {
		return con.unwrap(this.connectionType);
	}

	@Override
	public Statement getNativeStatement(Statement stmt) throws SQLException {
		return stmt.unwrap(this.statementType);
	}

	@Override
	public PreparedStatement getNativePreparedStatement(PreparedStatement ps) throws SQLException {
		return ps.unwrap(this.preparedStatementType);
	}

	@Override
	public CallableStatement getNativeCallableStatement(CallableStatement cs) throws SQLException {
		return cs.unwrap(this.callableStatementType);
	}

	@Override
	public ResultSet getNativeResultSet(ResultSet rs) throws SQLException {
		return rs.unwrap(this.resultSetType);
	}

}
