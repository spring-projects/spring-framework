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
 * Interface for extracting native JDBC objects from wrapped objects coming from
 * connection pools. This is necessary to allow for casting to native implementations
 * like {@code OracleConnection} or {@code OracleResultSet} in application
 * code, for example to create Blobs or to access vendor-specific features.
 *
 * <p>Note: Setting a custom {@code NativeJdbcExtractor} is just necessary
 * if you intend to cast to database-specific implementations like
 * {@code OracleConnection} or {@code OracleResultSet}.
 * Otherwise, any wrapped JDBC object will be fine, with no need for unwrapping.
 *
 * <p>Note: To be able to support any pool's strategy of native ResultSet wrapping,
 * it is advisable to get both the native Statement <i>and</i> the native ResultSet
 * via this extractor. Some pools just allow to unwrap the Statement, some just to
 * unwrap the ResultSet - the above strategy will cover both. It is typically
 * <i>not</i> necessary to unwrap the Connection to retrieve a native ResultSet.
 *
 * <p>When working with a simple connection pool that wraps Connections but not
 * Statements, a {@link SimpleNativeJdbcExtractor} is often sufficient. However,
 * some pools (like Jakarta's Commons DBCP) wrap <i>all</i> JDBC objects that they
 * return: Therefore, you need to use a specific {@code NativeJdbcExtractor}
 * (like {@link CommonsDbcpNativeJdbcExtractor}) with them.
 *
 * <p>{@link org.springframework.jdbc.core.JdbcTemplate} can properly apply a
 * {@code NativeJdbcExtractor} if specified, unwrapping all JDBC objects
 * that it creates. Note that this is just necessary if you intend to cast to
 * native implementations in your data access code.
 *
 * <p>{@link org.springframework.jdbc.support.lob.OracleLobHandler},
 * the Oracle-specific implementation of Spring's
 * {@link org.springframework.jdbc.support.lob.LobHandler} interface, requires a
 * {@code NativeJdbcExtractor} for obtaining the native {@code OracleConnection}.
 * This is also necessary for other Oracle-specific features that you may want
 * to leverage in your applications, such as Oracle InterMedia.
 *
 * @author Juergen Hoeller
 * @since 25.08.2003
 * @see SimpleNativeJdbcExtractor
 * @see CommonsDbcpNativeJdbcExtractor
 * @see org.springframework.jdbc.core.JdbcTemplate#setNativeJdbcExtractor
 * @see org.springframework.jdbc.support.lob.OracleLobHandler#setNativeJdbcExtractor
 */
public interface NativeJdbcExtractor {

	/**
	 * Return whether it is necessary to work on the native Connection to
	 * receive native Statements.
	 * <p>This should be true if the connection pool does not allow to extract
	 * the native JDBC objects from its Statement wrapper but supports a way
	 * to retrieve the native JDBC Connection. This way, applications can
	 * still receive native Statements and ResultSet via working on the
	 * native JDBC Connection.
	 */
	boolean isNativeConnectionNecessaryForNativeStatements();

	/**
	 * Return whether it is necessary to work on the native Connection to
	 * receive native PreparedStatements.
	 * <p>This should be true if the connection pool does not allow to extract
	 * the native JDBC objects from its PreparedStatement wrappers but
	 * supports a way to retrieve the native JDBC Connection. This way,
	 * applications can still receive native Statements and ResultSet via
	 * working on the native JDBC Connection.
	 */
	boolean isNativeConnectionNecessaryForNativePreparedStatements();

	/**
	 * Return whether it is necessary to work on the native Connection to
	 * receive native CallableStatements.
	 * <p>This should be true if the connection pool does not allow to extract
	 * the native JDBC objects from its CallableStatement wrappers but
	 * supports a way to retrieve the native JDBC Connection. This way,
	 * applications can still receive native Statements and ResultSet via
	 * working on the native JDBC Connection.
	 */
	boolean isNativeConnectionNecessaryForNativeCallableStatements();

	/**
	 * Retrieve the underlying native JDBC Connection for the given Connection.
	 * Supposed to return the given Connection if not capable of unwrapping.
	 * @param con the Connection handle, potentially wrapped by a connection pool
	 * @return the underlying native JDBC Connection, if possible;
	 * else, the original Connection
	 * @throws SQLException if thrown by JDBC methods
	 */
	Connection getNativeConnection(Connection con) throws SQLException;

	/**
	 * Retrieve the underlying native JDBC Connection for the given Statement.
	 * Supposed to return the {@code Statement.getConnection()} if not
	 * capable of unwrapping.
	 * <p>Having this extra method allows for more efficient unwrapping if data
	 * access code already has a Statement. {@code Statement.getConnection()}
	 * often returns the native JDBC Connection even if the Statement itself
	 * is wrapped by a pool.
	 * @param stmt the Statement handle, potentially wrapped by a connection pool
	 * @return the underlying native JDBC Connection, if possible;
	 * else, the original Connection
	 * @throws SQLException if thrown by JDBC methods
	 * @see java.sql.Statement#getConnection()
	 */
	Connection getNativeConnectionFromStatement(Statement stmt) throws SQLException;

	/**
	 * Retrieve the underlying native JDBC Statement for the given Statement.
	 * Supposed to return the given Statement if not capable of unwrapping.
	 * @param stmt the Statement handle, potentially wrapped by a connection pool
	 * @return the underlying native JDBC Statement, if possible;
	 * else, the original Statement
	 * @throws SQLException if thrown by JDBC methods
	 */
	Statement getNativeStatement(Statement stmt) throws SQLException;

	/**
	 * Retrieve the underlying native JDBC PreparedStatement for the given statement.
	 * Supposed to return the given PreparedStatement if not capable of unwrapping.
	 * @param ps the PreparedStatement handle, potentially wrapped by a connection pool
	 * @return the underlying native JDBC PreparedStatement, if possible;
	 * else, the original PreparedStatement
	 * @throws SQLException if thrown by JDBC methods
	 */
	PreparedStatement getNativePreparedStatement(PreparedStatement ps) throws SQLException;

	/**
	 * Retrieve the underlying native JDBC CallableStatement for the given statement.
	 * Supposed to return the given CallableStatement if not capable of unwrapping.
	 * @param cs the CallableStatement handle, potentially wrapped by a connection pool
	 * @return the underlying native JDBC CallableStatement, if possible;
	 * else, the original CallableStatement
	 * @throws SQLException if thrown by JDBC methods
	 */
	CallableStatement getNativeCallableStatement(CallableStatement cs) throws SQLException;

	/**
	 * Retrieve the underlying native JDBC ResultSet for the given statement.
	 * Supposed to return the given ResultSet if not capable of unwrapping.
	 * @param rs the ResultSet handle, potentially wrapped by a connection pool
	 * @return the underlying native JDBC ResultSet, if possible;
	 * else, the original ResultSet
	 * @throws SQLException if thrown by JDBC methods
	 */
	ResultSet getNativeResultSet(ResultSet rs) throws SQLException;

}
