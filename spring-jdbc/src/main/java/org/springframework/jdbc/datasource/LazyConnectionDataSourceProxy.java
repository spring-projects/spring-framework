/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.jdbc.datasource;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.Constants;
import org.springframework.lang.Nullable;

/**
 * Proxy for a target DataSource, fetching actual JDBC Connections lazily,
 * i.e. not until first creation of a Statement. Connection initialization
 * properties like auto-commit mode, transaction isolation and read-only mode
 * will be kept and applied to the actual JDBC Connection as soon as an
 * actual Connection is fetched (if ever). Consequently, commit and rollback
 * calls will be ignored if no Statements have been created.
 *
 * <p>This DataSource proxy allows to avoid fetching JDBC Connections from
 * a pool unless actually necessary. JDBC transaction control can happen
 * without fetching a Connection from the pool or communicating with the
 * database; this will be done lazily on first creation of a JDBC Statement.
 *
 * <p><b>If you configure both a LazyConnectionDataSourceProxy and a
 * TransactionAwareDataSourceProxy, make sure that the latter is the outermost
 * DataSource.</b> In such a scenario, data access code will talk to the
 * transaction-aware DataSource, which will in turn work with the
 * LazyConnectionDataSourceProxy.
 *
 * <p>Lazy fetching of physical JDBC Connections is particularly beneficial
 * in a generic transaction demarcation environment. It allows you to demarcate
 * transactions on all methods that could potentially perform data access,
 * without paying a performance penalty if no actual data access happens.
 *
 * <p>This DataSource proxy gives you behavior analogous to JTA and a
 * transactional JNDI DataSource (as provided by the Java EE server), even
 * with a local transaction strategy like DataSourceTransactionManager or
 * HibernateTransactionManager. It does not add value with Spring's
 * JtaTransactionManager as transaction strategy.
 *
 * <p>Lazy fetching of JDBC Connections is also recommended for read-only
 * operations with Hibernate, in particular if the chances of resolving the
 * result in the second-level cache are high. This avoids the need to
 * communicate with the database at all for such read-only operations.
 * You will get the same effect with non-transactional reads, but lazy fetching
 * of JDBC Connections allows you to still perform reads in transactions.
 *
 * <p><b>NOTE:</b> This DataSource proxy needs to return wrapped Connections
 * (which implement the {@link ConnectionProxy} interface) in order to handle
 * lazy fetching of an actual JDBC Connection. Use {@link Connection#unwrap}
 * to retrieve the native JDBC Connection.
 *
 * @author Juergen Hoeller
 * @since 1.1.4
 * @see DataSourceTransactionManager
 */
public class LazyConnectionDataSourceProxy extends DelegatingDataSource {

	/** Constants instance for TransactionDefinition */
	private static final Constants constants = new Constants(Connection.class);

	private static final Log logger = LogFactory.getLog(LazyConnectionDataSourceProxy.class);

	@Nullable
	private Boolean defaultAutoCommit;

	@Nullable
	private Integer defaultTransactionIsolation;


	/**
	 * Create a new LazyConnectionDataSourceProxy.
	 * @see #setTargetDataSource
	 */
	public LazyConnectionDataSourceProxy() {
	}

	/**
	 * Create a new LazyConnectionDataSourceProxy.
	 * @param targetDataSource the target DataSource
	 */
	public LazyConnectionDataSourceProxy(DataSource targetDataSource) {
		setTargetDataSource(targetDataSource);
		afterPropertiesSet();
	}


	/**
	 * Set the default auto-commit mode to expose when no target Connection
	 * has been fetched yet (-> actual JDBC Connection default not known yet).
	 * <p>If not specified, the default gets determined by checking a target
	 * Connection on startup. If that check fails, the default will be determined
	 * lazily on first access of a Connection.
	 * @see java.sql.Connection#setAutoCommit
	 */
	public void setDefaultAutoCommit(boolean defaultAutoCommit) {
		this.defaultAutoCommit = defaultAutoCommit;
	}

	/**
	 * Set the default transaction isolation level to expose when no target Connection
	 * has been fetched yet (-> actual JDBC Connection default not known yet).
	 * <p>This property accepts the int constant value (e.g. 8) as defined in the
	 * {@link java.sql.Connection} interface; it is mainly intended for programmatic
	 * use. Consider using the "defaultTransactionIsolationName" property for setting
	 * the value by name (e.g. "TRANSACTION_SERIALIZABLE").
	 * <p>If not specified, the default gets determined by checking a target
	 * Connection on startup. If that check fails, the default will be determined
	 * lazily on first access of a Connection.
	 * @see #setDefaultTransactionIsolationName
	 * @see java.sql.Connection#setTransactionIsolation
	 */
	public void setDefaultTransactionIsolation(int defaultTransactionIsolation) {
		this.defaultTransactionIsolation = defaultTransactionIsolation;
	}

	/**
	 * Set the default transaction isolation level by the name of the corresponding
	 * constant in {@link java.sql.Connection}, e.g. "TRANSACTION_SERIALIZABLE".
	 * @param constantName name of the constant
	 * @see #setDefaultTransactionIsolation
	 * @see java.sql.Connection#TRANSACTION_READ_UNCOMMITTED
	 * @see java.sql.Connection#TRANSACTION_READ_COMMITTED
	 * @see java.sql.Connection#TRANSACTION_REPEATABLE_READ
	 * @see java.sql.Connection#TRANSACTION_SERIALIZABLE
	 */
	public void setDefaultTransactionIsolationName(String constantName) {
		setDefaultTransactionIsolation(constants.asNumber(constantName).intValue());
	}


	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();

		// Determine default auto-commit and transaction isolation
		// via a Connection from the target DataSource, if possible.
		if (this.defaultAutoCommit == null || this.defaultTransactionIsolation == null) {
			try {
				Connection con = obtainTargetDataSource().getConnection();
				try {
					checkDefaultConnectionProperties(con);
				}
				finally {
					con.close();
				}
			}
			catch (SQLException ex) {
				logger.warn("Could not retrieve default auto-commit and transaction isolation settings", ex);
			}
		}
	}

	/**
	 * Check the default connection properties (auto-commit, transaction isolation),
	 * keeping them to be able to expose them correctly without fetching an actual
	 * JDBC Connection from the target DataSource.
	 * <p>This will be invoked once on startup, but also for each retrieval of a
	 * target Connection. If the check failed on startup (because the database was
	 * down), we'll lazily retrieve those settings.
	 * @param con the Connection to use for checking
	 * @throws SQLException if thrown by Connection methods
	 */
	protected synchronized void checkDefaultConnectionProperties(Connection con) throws SQLException {
		if (this.defaultAutoCommit == null) {
			this.defaultAutoCommit = con.getAutoCommit();
		}
		if (this.defaultTransactionIsolation == null) {
			this.defaultTransactionIsolation = con.getTransactionIsolation();
		}
	}

	/**
	 * Expose the default auto-commit value.
	 */
	@Nullable
	protected Boolean defaultAutoCommit() {
		return this.defaultAutoCommit;
	}

	/**
	 * Expose the default transaction isolation value.
	 */
	@Nullable
	protected Integer defaultTransactionIsolation() {
		return this.defaultTransactionIsolation;
	}


	/**
	 * Return a Connection handle that lazily fetches an actual JDBC Connection
	 * when asked for a Statement (or PreparedStatement or CallableStatement).
	 * <p>The returned Connection handle implements the ConnectionProxy interface,
	 * allowing to retrieve the underlying target Connection.
	 * @return a lazy Connection handle
	 * @see ConnectionProxy#getTargetConnection()
	 */
	@Override
	public Connection getConnection() throws SQLException {
		return (Connection) Proxy.newProxyInstance(
				ConnectionProxy.class.getClassLoader(),
				new Class<?>[] {ConnectionProxy.class},
				new LazyConnectionInvocationHandler());
	}

	/**
	 * Return a Connection handle that lazily fetches an actual JDBC Connection
	 * when asked for a Statement (or PreparedStatement or CallableStatement).
	 * <p>The returned Connection handle implements the ConnectionProxy interface,
	 * allowing to retrieve the underlying target Connection.
	 * @param username the per-Connection username
	 * @param password the per-Connection password
	 * @return a lazy Connection handle
	 * @see ConnectionProxy#getTargetConnection()
	 */
	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return (Connection) Proxy.newProxyInstance(
				ConnectionProxy.class.getClassLoader(),
				new Class<?>[] {ConnectionProxy.class},
				new LazyConnectionInvocationHandler(username, password));
	}


	/**
	 * Invocation handler that defers fetching an actual JDBC Connection
	 * until first creation of a Statement.
	 */
	private class LazyConnectionInvocationHandler implements InvocationHandler {

		@Nullable
		private String username;

		@Nullable
		private String password;

		@Nullable
		private Boolean autoCommit;

		@Nullable
		private Integer transactionIsolation;

		private boolean readOnly = false;

		private boolean closed = false;

		@Nullable
		private Connection target;

		public LazyConnectionInvocationHandler() {
			this.autoCommit = defaultAutoCommit();
			this.transactionIsolation = defaultTransactionIsolation();
		}

		public LazyConnectionInvocationHandler(String username, String password) {
			this();
			this.username = username;
			this.password = password;
		}

		@Override
		@Nullable
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// Invocation on ConnectionProxy interface coming in...

			if (method.getName().equals("equals")) {
				// We must avoid fetching a target Connection for "equals".
				// Only consider equal when proxies are identical.
				return (proxy == args[0]);
			}
			else if (method.getName().equals("hashCode")) {
				// We must avoid fetching a target Connection for "hashCode",
				// and we must return the same hash code even when the target
				// Connection has been fetched: use hashCode of Connection proxy.
				return System.identityHashCode(proxy);
			}
			else if (method.getName().equals("unwrap")) {
				if (((Class<?>) args[0]).isInstance(proxy)) {
					return proxy;
				}
			}
			else if (method.getName().equals("isWrapperFor")) {
				if (((Class<?>) args[0]).isInstance(proxy)) {
					return true;
				}
			}
			else if (method.getName().equals("getTargetConnection")) {
				// Handle getTargetConnection method: return underlying connection.
				return getTargetConnection(method);
			}

			if (!hasTargetConnection()) {
				// No physical target Connection kept yet ->
				// resolve transaction demarcation methods without fetching
				// a physical JDBC Connection until absolutely necessary.

				if (method.getName().equals("toString")) {
					return "Lazy Connection proxy for target DataSource [" + getTargetDataSource() + "]";
				}
				else if (method.getName().equals("getAutoCommit")) {
					if (this.autoCommit != null) {
						return this.autoCommit;
					}
					// Else fetch actual Connection and check there,
					// because we didn't have a default specified.
				}
				else if (method.getName().equals("setAutoCommit")) {
					this.autoCommit = (Boolean) args[0];
					return null;
				}
				else if (method.getName().equals("getTransactionIsolation")) {
					if (this.transactionIsolation != null) {
						return this.transactionIsolation;
					}
					// Else fetch actual Connection and check there,
					// because we didn't have a default specified.
				}
				else if (method.getName().equals("setTransactionIsolation")) {
					this.transactionIsolation = (Integer) args[0];
					return null;
				}
				else if (method.getName().equals("isReadOnly")) {
					return this.readOnly;
				}
				else if (method.getName().equals("setReadOnly")) {
					this.readOnly = (Boolean) args[0];
					return null;
				}
				else if (method.getName().equals("commit")) {
					// Ignore: no statements created yet.
					return null;
				}
				else if (method.getName().equals("rollback")) {
					// Ignore: no statements created yet.
					return null;
				}
				else if (method.getName().equals("getWarnings")) {
					return null;
				}
				else if (method.getName().equals("clearWarnings")) {
					return null;
				}
				else if (method.getName().equals("close")) {
					// Ignore: no target connection yet.
					this.closed = true;
					return null;
				}
				else if (method.getName().equals("isClosed")) {
					return this.closed;
				}
				else if (this.closed) {
					// Connection proxy closed, without ever having fetched a
					// physical JDBC Connection: throw corresponding SQLException.
					throw new SQLException("Illegal operation: connection is closed");
				}
			}

			// Target Connection already fetched,
			// or target Connection necessary for current operation ->
			// invoke method on target connection.
			try {
				return method.invoke(getTargetConnection(method), args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}

		/**
		 * Return whether the proxy currently holds a target Connection.
		 */
		private boolean hasTargetConnection() {
			return (this.target != null);
		}

		/**
		 * Return the target Connection, fetching it and initializing it if necessary.
		 */
		private Connection getTargetConnection(Method operation) throws SQLException {
			if (this.target == null) {
				// No target Connection held -> fetch one.
				if (logger.isDebugEnabled()) {
					logger.debug("Connecting to database for operation '" + operation.getName() + "'");
				}

				// Fetch physical Connection from DataSource.
				this.target = (this.username != null) ?
						obtainTargetDataSource().getConnection(this.username, this.password) :
						obtainTargetDataSource().getConnection();

				// If we still lack default connection properties, check them now.
				checkDefaultConnectionProperties(this.target);

				// Apply kept transaction settings, if any.
				if (this.readOnly) {
					try {
						this.target.setReadOnly(true);
					}
					catch (Exception ex) {
						// "read-only not supported" -> ignore, it's just a hint anyway
						logger.debug("Could not set JDBC Connection read-only", ex);
					}
				}
				if (this.transactionIsolation != null &&
						!this.transactionIsolation.equals(defaultTransactionIsolation())) {
					this.target.setTransactionIsolation(this.transactionIsolation);
				}
				if (this.autoCommit != null && this.autoCommit != this.target.getAutoCommit()) {
					this.target.setAutoCommit(this.autoCommit);
				}
			}

			else {
				// Target Connection already held -> return it.
				if (logger.isDebugEnabled()) {
					logger.debug("Using existing database connection for operation '" + operation.getName() + "'");
				}
			}

			return this.target;
		}
	}

}
