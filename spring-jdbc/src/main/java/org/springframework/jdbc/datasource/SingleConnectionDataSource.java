/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.jdbc.datasource;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Implementation of {@link SmartDataSource} that wraps a single JDBC Connection
 * which is not closed after use. Obviously, this is not multi-threading capable.
 *
 * <p>Note that at shutdown, someone should close the underlying Connection
 * via the <code>close()</code> method. Client code will never call close
 * on the Connection handle if it is SmartDataSource-aware (e.g. uses
 * <code>DataSourceUtils.releaseConnection</code>).
 *
 * <p>If client code will call <code>close()</code> in the assumption of a pooled
 * Connection, like when using persistence tools, set "suppressClose" to "true".
 * This will return a close-suppressing proxy instead of the physical Connection.
 * Be aware that you will not be able to cast this to a native
 * <code>OracleConnection</code> or the like anymore; you need to use a
 * {@link org.springframework.jdbc.support.nativejdbc.NativeJdbcExtractor} then.
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
 * @see org.springframework.jdbc.support.nativejdbc.NativeJdbcExtractor
 */
public class SingleConnectionDataSource extends DriverManagerDataSource
		implements SmartDataSource, DisposableBean {

	/** Create a close-suppressing proxy? */
	private boolean suppressClose;

	/** Override auto-commit state? */
	private Boolean autoCommit;

	/** Wrapped Connection */
	private Connection target;

	/** Proxy Connection */
	private Connection connection;

	/** Synchronization monitor for the shared Connection */
	private final Object connectionMonitor = new Object();


	/**
	 * Constructor for bean-style configuration.
	 */
	public SingleConnectionDataSource() {
	}

	/**
	 * Create a new SingleConnectionDataSource with the given standard
	 * DriverManager parameters.
	 * @param driverClassName the JDBC driver class name
	 * @param url the JDBC URL to use for accessing the DriverManager
	 * @param username the JDBC username to use for accessing the DriverManager
	 * @param password the JDBC password to use for accessing the DriverManager
	 * @param suppressClose if the returned Connection should be a
	 * close-suppressing proxy or the physical Connection
	 * @deprecated since Spring 2.5. Driver parameter usage is generally not recommended
	 * for a SingleConnectionDataSource. If you insist on using driver parameters
	 * directly, set up the Driver class manually before invoking this DataSource.
	 * @see java.sql.DriverManager#getConnection(String, String, String)
	 */
	@Deprecated
	public SingleConnectionDataSource(
			String driverClassName, String url, String username, String password, boolean suppressClose) {

		super(driverClassName, url, username, password);
		this.suppressClose = suppressClose;
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
	 * suppresses <code>close()</code> calls (to allow for normal <code>close()</code>
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
	 * Set whether the returned Connection should be a close-suppressing proxy
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
	 * Set whether the returned Connection's "autoCommit" setting should be overridden.
	 */
	public void setAutoCommit(boolean autoCommit) {
		this.autoCommit = (autoCommit);
	}

	/**
	 * Return whether the returned Connection's "autoCommit" setting should be overridden.
	 * @return the "autoCommit" value, or <code>null</code> if none to be applied
	 */
	protected Boolean getAutoCommitValue() {
		return this.autoCommit;
	}


	@Override
	public Connection getConnection() throws SQLException {
		synchronized (this.connectionMonitor) {
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
	}

	/**
	 * Specifying a custom username and password doesn't make sense
	 * with a single Connection. Returns the single Connection if given
	 * the same username and password; throws a SQLException else.
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
	public boolean shouldClose(Connection con) {
		synchronized (this.connectionMonitor) {
			return (con != this.connection && con != this.target);
		}
	}

	/**
	 * Close the underlying Connection.
	 * The provider of this DataSource needs to care for proper shutdown.
	 * <p>As this bean implements DisposableBean, a bean factory will
	 * automatically invoke this on destruction of its cached singletons.
	 */
	public void destroy() {
		synchronized (this.connectionMonitor) {
			closeConnection();
		}
	}


	/**
	 * Initialize the underlying Connection via the DriverManager.
	 */
	public void initConnection() throws SQLException {
		if (getUrl() == null) {
			throw new IllegalStateException("'url' property is required for lazily initializing a Connection");
		}
		synchronized (this.connectionMonitor) {
			closeConnection();
			this.target = getConnectionFromDriver(getUsername(), getPassword());
			prepareConnection(this.target);
			if (logger.isInfoEnabled()) {
				logger.info("Established shared JDBC Connection: " + this.target);
			}
			this.connection = (isSuppressClose() ? getCloseSuppressingConnectionProxy(this.target) : this.target);
		}
	}

	/**
	 * Reset the underlying shared Connection, to be reinitialized on next access.
	 */
	public void resetConnection() {
		synchronized (this.connectionMonitor) {
			closeConnection();
			this.target = null;
			this.connection = null;
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
	 */
	private void closeConnection() {
		if (this.target != null) {
			try {
				this.target.close();
			}
			catch (Throwable ex) {
				logger.warn("Could not close shared JDBC Connection", ex);
			}
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
				new Class[] {ConnectionProxy.class},
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

		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// Invocation on ConnectionProxy interface coming in...

			if (method.getName().equals("equals")) {
				// Only consider equal when proxies are identical.
				return (proxy == args[0]);
			}
			else if (method.getName().equals("hashCode")) {
				// Use hashCode of Connection proxy.
				return System.identityHashCode(proxy);
			}
			else if (method.getName().equals("unwrap")) {
				if (((Class) args[0]).isInstance(proxy)) {
					return proxy;
				}
			}
			else if (method.getName().equals("isWrapperFor")) {
				if (((Class) args[0]).isInstance(proxy)) {
					return true;
				}
			}
			else if (method.getName().equals("close")) {
				// Handle close method: don't pass the call on.
				return null;
			}
			else if (method.getName().equals("isClosed")) {
				return false;
			}
			else if (method.getName().equals("getTargetConnection")) {
				// Handle getTargetConnection method: return underlying Connection.
				return this.target;
			}

			// Invoke method on target Connection.
			try {
				return method.invoke(this.target, args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}
	}

}
