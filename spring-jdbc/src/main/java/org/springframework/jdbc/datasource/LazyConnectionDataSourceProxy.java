/*
 * Copyright 2002-2025 the original author or authors.
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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Proxy for a target DataSource, fetching actual JDBC Connections lazily,
 * i.e. not until first creation of a Statement. Connection initialization
 * properties like auto-commit mode, transaction isolation and read-only mode
 * will be kept and applied to the actual JDBC Connection as soon as an actual
 * Connection is fetched (if ever). Consequently, commit and rollback calls will
 * be ignored if no Statements have been created. As of 6.1.2, there is also
 * special support for a {@link #setReadOnlyDataSource read-only DataSource} to use
 * during a read-only transaction, in addition to the regular target DataSource.
 *
 * <p>This DataSource proxy allows to avoid fetching JDBC Connections from
 * a pool unless actually necessary. JDBC transaction control can happen
 * without fetching a Connection from the pool or communicating with the
 * database; this will be done lazily on first creation of a JDBC Statement.
 * As a bonus, this allows for taking the transaction-synchronized read-only
 * flag and/or isolation level into account in a routing DataSource (for example,
 * {@link org.springframework.jdbc.datasource.lookup.IsolationLevelDataSourceRouter}).
 *
 * <p><b>If you configure both a LazyConnectionDataSourceProxy and a
 * TransactionAwareDataSourceProxy, make sure that the latter is the outermost
 * DataSource.</b> In such a scenario, data access code will talk to the
 * transaction-aware DataSource, which will in turn work with the
 * LazyConnectionDataSourceProxy. As of 6.1.2, LazyConnectionDataSourceProxy will
 * initialize its default connection characteristics on first Connection access;
 * to enforce this on startup, call {@link #checkDefaultConnectionProperties()}.
 *
 * <p>Lazy fetching of physical JDBC Connections is particularly beneficial
 * in a generic transaction demarcation environment. It allows you to demarcate
 * transactions on all methods that could potentially perform data access,
 * without paying a performance penalty if no actual data access happens.
 *
 * <p>This DataSource proxy gives you behavior analogous to JTA and a
 * transactional JNDI DataSource (as provided by the Jakarta EE server), even
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
 * <p>As of 6.2.6, this DataSource proxy also suppresses a rollback attempt
 * in case of a timeout where the connection has been closed in the meantime.
 *
 * <p><b>NOTE:</b> This DataSource proxy needs to return wrapped Connections
 * (which implement the {@link ConnectionProxy} interface) in order to handle
 * lazy fetching of an actual JDBC Connection. Use {@link Connection#unwrap}
 * to retrieve the native JDBC Connection.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 1.1.4
 * @see DataSourceTransactionManager
 * @see #setTargetDataSource
 * @see #setReadOnlyDataSource
 */
public class LazyConnectionDataSourceProxy extends DelegatingDataSource {

	/**
	 * Map of constant names to constant values for the isolation constants
	 * defined in {@link java.sql.Connection}.
	 */
	static final Map<String, Integer> constants = Map.of(
			"TRANSACTION_READ_UNCOMMITTED", Connection.TRANSACTION_READ_UNCOMMITTED,
			"TRANSACTION_READ_COMMITTED", Connection.TRANSACTION_READ_COMMITTED,
			"TRANSACTION_REPEATABLE_READ", Connection.TRANSACTION_REPEATABLE_READ,
			"TRANSACTION_SERIALIZABLE", Connection.TRANSACTION_SERIALIZABLE
		);

	private static final Log logger = LogFactory.getLog(LazyConnectionDataSourceProxy.class);

	private @Nullable DataSource readOnlyDataSource;

	private volatile @Nullable Boolean defaultAutoCommit;

	private volatile @Nullable Integer defaultTransactionIsolation;


	/**
	 * Create a new LazyConnectionDataSourceProxy.
	 * @see #setTargetDataSource
	 * @see #setReadOnlyDataSource
	 */
	public LazyConnectionDataSourceProxy() {
	}

	/**
	 * Create a new LazyConnectionDataSourceProxy.
	 * @param targetDataSource the target DataSource
	 * @see #setTargetDataSource
	 */
	public LazyConnectionDataSourceProxy(DataSource targetDataSource) {
		setTargetDataSource(targetDataSource);
		afterPropertiesSet();
	}


	/**
	 * Specify a variant of the target DataSource to use for read-only transactions.
	 * <p>If available, a Connection from such a read-only DataSource will be lazily
	 * obtained within a Spring-managed transaction that has been marked as read-only.
	 * The {@link Connection#setReadOnly} flag will be left untouched, expecting it
	 * to be pre-configured as a default on the read-only DataSource, avoiding the
	 * overhead of switching it at the beginning and end of every transaction.
	 * Also, the default auto-commit and isolation level settings are expected to
	 * match the default connection properties of the primary target DataSource.
	 * @since 6.1.2
	 * @see #setTargetDataSource
	 * @see #setDefaultAutoCommit
	 * @see #setDefaultTransactionIsolation
	 * @see org.springframework.transaction.TransactionDefinition#isReadOnly()
	 */
	public void setReadOnlyDataSource(@Nullable DataSource readOnlyDataSource) {
		this.readOnlyDataSource = readOnlyDataSource;
	}

	/**
	 * Set the default auto-commit mode to expose when no target Connection
	 * has been fetched yet (when the actual JDBC Connection default is not known yet).
	 * <p>If not specified, the default gets determined by checking lazily on first
	 * access of a Connection.
	 * @see java.sql.Connection#setAutoCommit
	 */
	public void setDefaultAutoCommit(boolean defaultAutoCommit) {
		this.defaultAutoCommit = defaultAutoCommit;
	}

	/**
	 * Set the default transaction isolation level by the name of the corresponding
	 * constant in {@link java.sql.Connection} &mdash; for example,
	 * {@code "TRANSACTION_SERIALIZABLE"}.
	 * @param constantName name of the constant
	 * @see #setDefaultTransactionIsolation
	 * @see java.sql.Connection#TRANSACTION_READ_UNCOMMITTED
	 * @see java.sql.Connection#TRANSACTION_READ_COMMITTED
	 * @see java.sql.Connection#TRANSACTION_REPEATABLE_READ
	 * @see java.sql.Connection#TRANSACTION_SERIALIZABLE
	 */
	public void setDefaultTransactionIsolationName(String constantName) {
		Assert.hasText(constantName, "'constantName' must not be null or blank");
		Integer defaultTransactionIsolation = constants.get(constantName);
		Assert.notNull(defaultTransactionIsolation, "Only transaction isolation constants allowed");
		this.defaultTransactionIsolation = defaultTransactionIsolation;
	}

	/**
	 * Set the default transaction isolation level to expose when no target Connection
	 * has been fetched yet (when the actual JDBC Connection default is not known yet).
	 * <p>This property accepts the int constant value (for example, 8) as defined in the
	 * {@link java.sql.Connection} interface; it is mainly intended for programmatic
	 * use. Consider using the "defaultTransactionIsolationName" property for setting
	 * the value by name (for example, {@code "TRANSACTION_SERIALIZABLE"}).
	 * <p>If not specified, the default gets determined by checking lazily on first
	 * access of a Connection.
	 * @see #setDefaultTransactionIsolationName
	 * @see java.sql.Connection#setTransactionIsolation
	 */
	public void setDefaultTransactionIsolation(int defaultTransactionIsolation) {
		Assert.isTrue(constants.containsValue(defaultTransactionIsolation),
				"Only values of transaction isolation constants allowed");
		this.defaultTransactionIsolation = defaultTransactionIsolation;
	}


	/**
	 * Determine default auto-commit and transaction isolation
	 * via a Connection from the target DataSource, if possible.
	 * @since 6.1.2
	 * @see #checkDefaultConnectionProperties(Connection)
	 */
	public void checkDefaultConnectionProperties() {
		if (this.defaultAutoCommit == null || this.defaultTransactionIsolation == null) {
			try {
				try (Connection con = obtainTargetDataSource().getConnection()) {
					checkDefaultConnectionProperties(con);
				}
			}
			catch (SQLException ex) {
				logger.debug("Could not retrieve default auto-commit and transaction isolation settings", ex);
			}
		}
	}

	/**
	 * Check the default connection properties (auto-commit, transaction isolation),
	 * keeping them to be able to expose them correctly without fetching an actual
	 * JDBC Connection from the target DataSource later on.
	 * @param con the Connection to use for checking
	 * @throws SQLException if thrown by Connection methods
	 */
	protected void checkDefaultConnectionProperties(Connection con) throws SQLException {
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
	protected @Nullable Boolean defaultAutoCommit() {
		return this.defaultAutoCommit;
	}

	/**
	 * Expose the default transaction isolation value.
	 */
	protected @Nullable Integer defaultTransactionIsolation() {
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
		checkDefaultConnectionProperties();
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
		checkDefaultConnectionProperties();
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

		private @Nullable String username;

		private @Nullable String password;

		private @Nullable Boolean autoCommit;

		private @Nullable Integer transactionIsolation;

		private boolean readOnly = false;

		private int holdability = ResultSet.CLOSE_CURSORS_AT_COMMIT;

		private boolean closed = false;

		private @Nullable Connection target;

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
		public @Nullable Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// Invocation on ConnectionProxy interface coming in...

			switch (method.getName()) {
				case "equals" -> {
					// We must avoid fetching a target Connection for "equals".
					// Only consider equal when proxies are identical.
					return (proxy == args[0]);
				}
				case "hashCode" -> {
					// We must avoid fetching a target Connection for "hashCode",
					// and we must return the same hash code even when the target
					// Connection has been fetched: use hashCode of Connection proxy.
					return System.identityHashCode(proxy);
				}
				case "getTargetConnection" -> {
					// Handle getTargetConnection method: return underlying connection.
					return getTargetConnection(method);
				}
				case "unwrap" -> {
					if (((Class<?>) args[0]).isInstance(proxy)) {
						return proxy;
					}
				}
				case "isWrapperFor" -> {
					if (((Class<?>) args[0]).isInstance(proxy)) {
						return true;
					}
				}
			}

			if (!hasTargetConnection()) {
				// No physical target Connection kept yet ->
				// resolve transaction demarcation methods without fetching
				// a physical JDBC Connection until absolutely necessary.

				switch (method.getName()) {
					case "toString" -> {
						return "Lazy Connection proxy for target DataSource [" + getTargetDataSource() + "]";
					}
					case "getAutoCommit" -> {
						if (this.autoCommit != null) {
							return this.autoCommit;
						}
						// Else fetch actual Connection and check there,
						// because we didn't have a default specified.
					}
					case "setAutoCommit" -> {
						this.autoCommit = (Boolean) args[0];
						return null;
					}
					case "getTransactionIsolation" -> {
						if (this.transactionIsolation != null) {
							return this.transactionIsolation;
						}
						// Else fetch actual Connection and check there,
						// because we didn't have a default specified.
					}
					case "setTransactionIsolation" -> {
						this.transactionIsolation = (Integer) args[0];
						return null;
					}
					case "isReadOnly" -> {
						return this.readOnly;
					}
					case "setReadOnly" -> {
						this.readOnly = (Boolean) args[0];
						return null;
					}
					case "getHoldability" -> {
						return this.holdability;
					}
					case "setHoldability" -> {
						this.holdability = (Integer) args[0];
						return null;
					}
					case "commit", "rollback" -> {
						// Ignore: no statements created yet.
						return null;
					}
					case "getWarnings", "clearWarnings" -> {
						// Ignore: no warnings to expose yet.
						return null;
					}
					case "close" -> {
						// Ignore: no target connection yet.
						this.closed = true;
						return null;
					}
					case "isClosed" -> {
						return this.closed;
					}
					default -> {
						if (this.closed) {
							// Connection proxy closed, without ever having fetched a
							// physical JDBC Connection: throw corresponding SQLException.
							throw new SQLException("Illegal operation: connection is closed");
						}
					}
				}
			}

			if (readOnlyDataSource != null && "setReadOnly".equals(method.getName())) {
				// Suppress setReadOnly reset call in case of dedicated read-only DataSource
				return null;
			}


			// Target Connection already fetched, or target Connection necessary for current operation
			// -> invoke method on target connection.
			try {
				Connection conToUse = getTargetConnection(method);

				if ("rollback".equals(method.getName()) && conToUse.isClosed()) {
					// Connection closed in the meantime, probably due to a resource timeout. Since a
					// rollback attempt typically happens right before close, we leniently suppress it.
					return null;
				}

				return method.invoke(conToUse, args);
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
				if (logger.isTraceEnabled()) {
					logger.trace("Connecting to database for operation '" + operation.getName() + "'");
				}

				// Fetch physical Connection from DataSource.
				DataSource dataSource = getDataSourceToUse();
				this.target = (this.username != null ? dataSource.getConnection(this.username, this.password) :
						dataSource.getConnection());
				if (this.target == null) {
					throw new IllegalStateException("DataSource returned null from getConnection(): " + dataSource);
				}

				// Apply kept transaction settings, if any.
				if (this.readOnly && readOnlyDataSource == null) {
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
				if (this.autoCommit != null && this.autoCommit != defaultAutoCommit()) {
					this.target.setAutoCommit(this.autoCommit);
				}
			}

			else {
				// Target Connection already held -> return it.
				if (logger.isTraceEnabled()) {
					logger.trace("Using existing database connection for operation '" + operation.getName() + "'");
				}
			}

			return this.target;
		}

		private DataSource getDataSourceToUse() {
			return (this.readOnly && readOnlyDataSource != null ? readOnlyDataSource : obtainTargetDataSource());
		}
	}

}
