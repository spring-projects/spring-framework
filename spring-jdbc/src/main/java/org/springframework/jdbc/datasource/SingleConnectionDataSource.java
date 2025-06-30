/*
 * Copyright 2002-present the original author or authors.
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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Implementation of {@link SmartDataSource} that wraps a single JDBC Connection
 * which is not closed after use. Obviously, this is not multi-threading capable.
 *
 * <p>Note that at shutdown, someone should close the underlying Connection
 * via the {@code close()} method. Client code will never call close
 * on the Connection handle if it is SmartDataSource-aware (for example, uses
 * {@code DataSourceUtils.releaseConnection}).
 *
 * <p>If client code will call {@code close()} in the assumption of a pooled
 * Connection, like when using persistence tools, set "suppressClose" to "true".
 * This will return a close-suppressing proxy instead of the physical Connection.
 *
 * <p>This is primarily intended for testing. For example, it enables easy testing
 * outside an application server, for code that expects to work on a DataSource.
 * In contrast to {@link DriverManagerDataSource}, it reuses the same Connection
 * all the time, avoiding excessive creation of physical Connections.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #getConnection()
 * @see java.sql.Connection#close()
 * @see DataSourceUtils#releaseConnection
 */
public class SingleConnectionDataSource extends DriverManagerDataSource
		implements SmartDataSource, AutoCloseable, DisposableBean {

	/** Create a close-suppressing proxy? */
	private boolean suppressClose;

	/** Explicit rollback before close? */
	private boolean rollbackBeforeClose;

	/** Override auto-commit state? */
	private @Nullable Boolean autoCommit;

	/** Wrapped Connection. */
	private @Nullable Connection target;

	/** Proxy Connection. */
	private @Nullable Connection connection;

	/** Lifecycle lock for the shared Connection. */
	private final Lock connectionLock = new ReentrantLock();


	/**
	 * Constructor for bean-style configuration.
	 */
	public SingleConnectionDataSource() {
	}

	/**
	 * Create a new SingleConnectionDataSource with the given standard
	 * DriverManager parameters.
	 * @param url the JDBC URL to use for accessing the DriverManager
	 * @param username the JDBC username to use for accessing the DriverManager
	 * @param password the JDBC password to use for accessing the DriverManager
	 * @param suppressClose if the returned Connection should be a
	 * close-suppressing proxy or the physical Connection
	 * @see java.sql.DriverManager#getConnection(String, String, String)
	 */
	public SingleConnectionDataSource(String url, String username, String password, boolean suppressClose) {
		super(url, username, password);
		this.suppressClose = suppressClose;
	}

	/**
	 * Create a new SingleConnectionDataSource with the given standard
	 * DriverManager parameters.
	 * @param url the JDBC URL to use for accessing the DriverManager
	 * @param suppressClose if the returned Connection should be a
	 * close-suppressing proxy or the physical Connection
	 * @see java.sql.DriverManager#getConnection(String, String, String)
	 */
	public SingleConnectionDataSource(String url, boolean suppressClose) {
		super(url);
		this.suppressClose = suppressClose;
	}

	/**
	 * Create a new SingleConnectionDataSource with a given Connection.
	 * @param target underlying target Connection
	 * @param suppressClose if the Connection should be wrapped with a Connection that
	 * suppresses {@code close()} calls (to allow for normal {@code close()}
	 * usage in applications that expect a pooled Connection but do not know our
	 * SmartDataSource interface)
	 */
	public SingleConnectionDataSource(Connection target, boolean suppressClose) {
		Assert.notNull(target, "Connection must not be null");
		this.target = target;
		this.suppressClose = suppressClose;
		this.connection = (suppressClose ? getCloseSuppressingConnectionProxy(target) : target);
	}


	/**
	 * Specify whether the returned Connection should be a close-suppressing proxy
	 * or the physical Connection.
	 */
	public void setSuppressClose(boolean suppressClose) {
		this.suppressClose = suppressClose;
	}

	/**
	 * Return whether the returned Connection will be a close-suppressing proxy
	 * or the physical Connection.
	 */
	protected boolean isSuppressClose() {
		return this.suppressClose;
	}

	/**
	 * Specify whether the shared Connection should be explicitly rolled back
	 * before close (if not in auto-commit mode).
	 * <p>This is recommended for the Oracle JDBC driver in testing scenarios.
	 * @since 6.1.2
	 */
	public void setRollbackBeforeClose(boolean rollbackBeforeClose) {
		this.rollbackBeforeClose = rollbackBeforeClose;
	}

	/**
	 * Return whether the shared Connection should be explicitly rolled back
	 * before close (if not in auto-commit mode).
	 * @since 6.1.2
	 */
	protected boolean isRollbackBeforeClose() {
		return this.rollbackBeforeClose;
	}

	/**
	 * Specify whether the returned Connection's "autoCommit" setting should be overridden.
	 */
	public void setAutoCommit(boolean autoCommit) {
		this.autoCommit = autoCommit;
	}

	/**
	 * Return whether the returned Connection's "autoCommit" setting should be overridden.
	 * @return the "autoCommit" value, or {@code null} if none to be applied
	 */
	protected @Nullable Boolean getAutoCommitValue() {
		return this.autoCommit;
	}


	@Override
	@SuppressWarnings("NullAway") // Dataflow analysis limitation
	public Connection getConnection() throws SQLException {
		this.connectionLock.lock();
		try {
			if (this.connection == null) {
				// No underlying Connection -> lazy init via DriverManager.
				initConnection();
			}
			if (this.connection.isClosed()) {
				throw new SQLException(
						"Connection was closed in SingleConnectionDataSource. Check that user code checks " +
						"shouldClose() before closing Connections, or set 'suppressClose' to 'true'");
			}
			return this.connection;
		}
		finally {
			this.connectionLock.unlock();
		}
	}

	/**
	 * Specifying a custom username and password doesn't make sense
	 * with a single Connection. Returns the single Connection if given
	 * the same username and password; throws an SQLException else.
	 */
	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		if (ObjectUtils.nullSafeEquals(username, getUsername()) &&
				ObjectUtils.nullSafeEquals(password, getPassword())) {
			return getConnection();
		}
		else {
			throw new SQLException("SingleConnectionDataSource does not support custom username and password");
		}
	}

	/**
	 * This is a single Connection: Do not close it when returning to the "pool".
	 */
	@Override
	public boolean shouldClose(Connection con) {
		this.connectionLock.lock();
		try {
			return (con != this.connection && con != this.target);
		}
		finally {
			this.connectionLock.unlock();
		}
	}

	/**
	 * Close the underlying Connection.
	 * The provider of this DataSource needs to care for proper shutdown.
	 * <p>As this class implements {@link AutoCloseable}, it can be used
	 * with a try-with-resource statement.
	 * @since 6.1.2
	 */
	@Override
	public void close() {
		destroy();
	}

	/**
	 * Close the underlying Connection.
	 * The provider of this DataSource needs to care for proper shutdown.
	 * <p>As this bean implements {@link DisposableBean}, a bean factory
	 * will automatically invoke this on destruction of the bean.
	 */
	@Override
	public void destroy() {
		this.connectionLock.lock();
		try {
			if (this.target != null) {
				closeConnection(this.target);
			}
		}
		finally {
			this.connectionLock.unlock();
		}
	}


	/**
	 * Initialize the underlying Connection via the DriverManager.
	 */
	public void initConnection() throws SQLException {
		if (getUrl() == null) {
			throw new IllegalStateException("'url' property is required for lazily initializing a Connection");
		}
		this.connectionLock.lock();
		try {
			if (this.target != null) {
				closeConnection(this.target);
			}
			this.target = getConnectionFromDriver(getUsername(), getPassword());
			prepareConnection(this.target);
			if (logger.isDebugEnabled()) {
				logger.debug("Established shared JDBC Connection: " + this.target);
			}
			this.connection = (isSuppressClose() ? getCloseSuppressingConnectionProxy(this.target) : this.target);
		}
		finally {
			this.connectionLock.unlock();
		}
	}

	/**
	 * Reset the underlying shared Connection, to be reinitialized on next access.
	 */
	public void resetConnection() {
		this.connectionLock.lock();
		try {
			if (this.target != null) {
				closeConnection(this.target);
			}
			this.target = null;
			this.connection = null;
		}
		finally {
			this.connectionLock.unlock();
		}
	}

	/**
	 * Prepare the given Connection before it is exposed.
	 * <p>The default implementation applies the auto-commit flag, if necessary.
	 * Can be overridden in subclasses.
	 * @param con the Connection to prepare
	 * @see #setAutoCommit
	 */
	protected void prepareConnection(Connection con) throws SQLException {
		Boolean autoCommit = getAutoCommitValue();
		if (autoCommit != null && con.getAutoCommit() != autoCommit) {
			con.setAutoCommit(autoCommit);
		}
	}

	/**
	 * Close the underlying shared Connection.
	 * @since 6.1.2
	 */
	protected void closeConnection(Connection con) {
		if (isRollbackBeforeClose()) {
			try {
				if (!con.getAutoCommit()) {
					con.rollback();
				}
			}
			catch (Throwable ex) {
				logger.info("Could not roll back shared JDBC Connection before close", ex);
			}
		}
		try {
			con.close();
		}
		catch (Throwable ex) {
			logger.info("Could not close shared JDBC Connection", ex);
		}
	}

	/**
	 * Wrap the given Connection with a proxy that delegates every method call to it
	 * but suppresses close calls.
	 * @param target the original Connection to wrap
	 * @return the wrapped Connection
	 */
	protected Connection getCloseSuppressingConnectionProxy(Connection target) {
		return (Connection) Proxy.newProxyInstance(
				ConnectionProxy.class.getClassLoader(),
				new Class<?>[] {ConnectionProxy.class},
				new CloseSuppressingInvocationHandler(target));
	}


	/**
	 * Invocation handler that suppresses close calls on JDBC Connections.
	 */
	private static class CloseSuppressingInvocationHandler implements InvocationHandler {

		private final Connection target;

		public CloseSuppressingInvocationHandler(Connection target) {
			this.target = target;
		}

		@Override
		public @Nullable Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// Invocation on ConnectionProxy interface coming in...

			return switch (method.getName()) {
				// Only consider equal when proxies are identical.
				case "equals" -> (proxy == args[0]);
				// Use hashCode of Connection proxy.
				case "hashCode" -> System.identityHashCode(proxy);
				// Handle close method: don't pass the call on.
				case "close" -> null;
				case "isClosed" -> this.target.isClosed();
				// Handle getTargetConnection method: return underlying Connection.
				case "getTargetConnection" -> this.target;
				case "unwrap" -> (((Class<?>) args[0]).isInstance(proxy) ? proxy : this.target.unwrap((Class<?>) args[0]));
				case "isWrapperFor" -> (((Class<?>) args[0]).isInstance(proxy) || this.target.isWrapperFor((Class<?>) args[0]));
				default -> {
					try {
						// Invoke method on target Connection.
						yield method.invoke(this.target, args);
					}
					catch (InvocationTargetException ex) {
						throw ex.getTargetException();
					}
				}
			};
		}
	}

}
