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

import java.lang.reflect.Method;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.springframework.util.ReflectionUtils;

/**
 * Implementation of the {@link NativeJdbcExtractor} interface for JBoss,
 * supporting JBoss Application Server 3.2.4+. As of Spring 3.1.1, it also
 * supports JBoss 7.
 *
 * <p>Returns the underlying native Connection, Statement, etc to
 * application code instead of JBoss' wrapper implementations.
 * The returned JDBC classes can then safely be cast, e.g. to
 * {@code oracle.jdbc.OracleConnection}.
 *
 * <p>This NativeJdbcExtractor can be set just to <i>allow</i> working with
 * a JBoss connection pool: If a given object is not a JBoss wrapper,
 * it will be returned as-is.
 *
 * @author Juergen Hoeller
 * @since 03.01.2004
 * @see org.jboss.resource.adapter.jdbc.WrappedConnection#getUnderlyingConnection
 * @see org.jboss.resource.adapter.jdbc.WrappedStatement#getUnderlyingStatement
 * @see org.jboss.resource.adapter.jdbc.WrappedResultSet#getUnderlyingResultSet
 */
public class JBossNativeJdbcExtractor extends NativeJdbcExtractorAdapter {

	// JBoss 7
	private static final String JBOSS_JCA_PREFIX = "org.jboss.jca.adapters.jdbc.";

	// JBoss <= 6
	private static final String JBOSS_RESOURCE_PREFIX = "org.jboss.resource.adapter.jdbc.";


	private Class<?> wrappedConnectionClass;

	private Class<?> wrappedStatementClass;

	private Class<?> wrappedResultSetClass;

	private Method getUnderlyingConnectionMethod;

	private Method getUnderlyingStatementMethod;

	private Method getUnderlyingResultSetMethod;


	/**
	 * This constructor retrieves JBoss JDBC wrapper classes,
	 * so we can get the underlying vendor connection using reflection.
	 */
	public JBossNativeJdbcExtractor() {
		String prefix = JBOSS_JCA_PREFIX;
		try {
			// trying JBoss 7 jca package first...
			this.wrappedConnectionClass = getClass().getClassLoader().loadClass(prefix + "WrappedConnection");
		}
		catch (ClassNotFoundException ex) {
			// JBoss 7 jca package not found -> try traditional resource package.
			prefix = JBOSS_RESOURCE_PREFIX;
			try {
				this.wrappedConnectionClass = getClass().getClassLoader().loadClass(prefix + "WrappedConnection");
			}
			catch (ClassNotFoundException ex2) {
				throw new IllegalStateException("Could not initialize JBossNativeJdbcExtractor: neither JBoss 7's [" +
						JBOSS_JCA_PREFIX + ".WrappedConnection] nor traditional JBoss [" + JBOSS_RESOURCE_PREFIX +
						".WrappedConnection] found");
			}
		}
		try {
			this.wrappedStatementClass = getClass().getClassLoader().loadClass(prefix + "WrappedStatement");
			this.wrappedResultSetClass = getClass().getClassLoader().loadClass(prefix + "WrappedResultSet");
			this.getUnderlyingConnectionMethod =
				this.wrappedConnectionClass.getMethod("getUnderlyingConnection", (Class[]) null);
			this.getUnderlyingStatementMethod =
				this.wrappedStatementClass.getMethod("getUnderlyingStatement", (Class[]) null);
			this.getUnderlyingResultSetMethod =
				this.wrappedResultSetClass.getMethod("getUnderlyingResultSet", (Class[]) null);
		}
		catch (Exception ex) {
			throw new IllegalStateException(
					"Could not initialize JBossNativeJdbcExtractor because of missing JBoss API methods/classes: " + ex);
		}
	}


	/**
	 * Retrieve the Connection via JBoss' {@code getUnderlyingConnection} method.
	 */
	@Override
	protected Connection doGetNativeConnection(Connection con) throws SQLException {
		if (this.wrappedConnectionClass.isAssignableFrom(con.getClass())) {
			return (Connection) ReflectionUtils.invokeJdbcMethod(this.getUnderlyingConnectionMethod, con);
		}
		return con;
	}

	/**
	 * Retrieve the Connection via JBoss' {@code getUnderlyingStatement} method.
	 */
	@Override
	public Statement getNativeStatement(Statement stmt) throws SQLException {
		if (this.wrappedStatementClass.isAssignableFrom(stmt.getClass())) {
			return (Statement) ReflectionUtils.invokeJdbcMethod(this.getUnderlyingStatementMethod, stmt);
		}
		return stmt;
	}

	/**
	 * Retrieve the Connection via JBoss' {@code getUnderlyingStatement} method.
	 */
	@Override
	public PreparedStatement getNativePreparedStatement(PreparedStatement ps) throws SQLException {
		return (PreparedStatement) getNativeStatement(ps);
	}

	/**
	 * Retrieve the Connection via JBoss' {@code getUnderlyingStatement} method.
	 */
	@Override
	public CallableStatement getNativeCallableStatement(CallableStatement cs) throws SQLException {
		return (CallableStatement) getNativeStatement(cs);
	}

	/**
	 * Retrieve the Connection via JBoss' {@code getUnderlyingResultSet} method.
	 */
	@Override
	public ResultSet getNativeResultSet(ResultSet rs) throws SQLException {
		if (this.wrappedResultSetClass.isAssignableFrom(rs.getClass())) {
			return (ResultSet) ReflectionUtils.invokeJdbcMethod(this.getUnderlyingResultSetMethod, rs);
		}
		return rs;
	}

}
