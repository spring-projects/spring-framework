/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.jca.cci.connection;

import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.ConnectionSpec;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.jca.cci.CannotGetCciConnectionException;
import org.springframework.lang.Nullable;
import org.springframework.transaction.support.ResourceHolderSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * Helper class that provides static methods for obtaining CCI Connections
 * from a {@link javax.resource.cci.ConnectionFactory}. Includes special
 * support for Spring-managed transactional Connections, e.g. managed
 * by {@link CciLocalTransactionManager} or
 * {@link org.springframework.transaction.jta.JtaTransactionManager}.
 *
 * <p>Used internally by {@link org.springframework.jca.cci.core.CciTemplate},
 * Spring's CCI operation objects and the {@link CciLocalTransactionManager}.
 * Can also be used directly in application code.
 *
 * @author Thierry Templier
 * @author Juergen Hoeller
 * @since 1.2
 * @see #getConnection
 * @see #releaseConnection
 * @see CciLocalTransactionManager
 * @see org.springframework.transaction.jta.JtaTransactionManager
 * @see org.springframework.transaction.support.TransactionSynchronizationManager
 */
public abstract class ConnectionFactoryUtils {

	private static final Log logger = LogFactory.getLog(ConnectionFactoryUtils.class);


	/**
	 * Obtain a Connection from the given ConnectionFactory. Translates ResourceExceptions
	 * into the Spring hierarchy of unchecked generic data access exceptions, simplifying
	 * calling code and making any exception that is thrown more meaningful.
	 * <p>Is aware of a corresponding Connection bound to the current thread, for example
	 * when using {@link CciLocalTransactionManager}. Will bind a Connection to the thread
	 * if transaction synchronization is active (e.g. if in a JTA transaction).
	 * @param cf the ConnectionFactory to obtain Connection from
	 * @return a CCI Connection from the given ConnectionFactory
	 * @throws org.springframework.jca.cci.CannotGetCciConnectionException
	 * if the attempt to get a Connection failed
	 * @see #releaseConnection
	 */
	public static Connection getConnection(ConnectionFactory cf) throws CannotGetCciConnectionException {
		return getConnection(cf, null);
	}

	/**
	 * Obtain a Connection from the given ConnectionFactory. Translates ResourceExceptions
	 * into the Spring hierarchy of unchecked generic data access exceptions, simplifying
	 * calling code and making any exception that is thrown more meaningful.
	 * <p>Is aware of a corresponding Connection bound to the current thread, for example
	 * when using {@link CciLocalTransactionManager}. Will bind a Connection to the thread
	 * if transaction synchronization is active (e.g. if in a JTA transaction).
	 * @param cf the ConnectionFactory to obtain Connection from
	 * @param spec the ConnectionSpec for the desired Connection (may be {@code null}).
	 * Note: If this is specified, a new Connection will be obtained for every call,
	 * without participating in a shared transactional Connection.
	 * @return a CCI Connection from the given ConnectionFactory
	 * @throws org.springframework.jca.cci.CannotGetCciConnectionException
	 * if the attempt to get a Connection failed
	 * @see #releaseConnection
	 */
	public static Connection getConnection(ConnectionFactory cf, @Nullable ConnectionSpec spec)
			throws CannotGetCciConnectionException {
		try {
			if (spec != null) {
				Assert.notNull(cf, "No ConnectionFactory specified");
				return cf.getConnection(spec);
			}
			else {
				return doGetConnection(cf);
			}
		}
		catch (ResourceException ex) {
			throw new CannotGetCciConnectionException("Could not get CCI Connection", ex);
		}
	}

	/**
	 * Actually obtain a CCI Connection from the given ConnectionFactory.
	 * Same as {@link #getConnection}, but throwing the original ResourceException.
	 * <p>Is aware of a corresponding Connection bound to the current thread, for example
	 * when using {@link CciLocalTransactionManager}. Will bind a Connection to the thread
	 * if transaction synchronization is active (e.g. if in a JTA transaction).
	 * <p>Directly accessed by {@link TransactionAwareConnectionFactoryProxy}.
	 * @param cf the ConnectionFactory to obtain Connection from
	 * @return a CCI Connection from the given ConnectionFactory
	 * @throws ResourceException if thrown by CCI API methods
	 * @see #doReleaseConnection
	 */
	public static Connection doGetConnection(ConnectionFactory cf) throws ResourceException {
		Assert.notNull(cf, "No ConnectionFactory specified");

		ConnectionHolder conHolder = (ConnectionHolder) TransactionSynchronizationManager.getResource(cf);
		if (conHolder != null) {
			return conHolder.getConnection();
		}

		logger.debug("Opening CCI Connection");
		Connection con = cf.getConnection();

		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			conHolder = new ConnectionHolder(con);
			conHolder.setSynchronizedWithTransaction(true);
			TransactionSynchronizationManager.registerSynchronization(new ConnectionSynchronization(conHolder, cf));
			TransactionSynchronizationManager.bindResource(cf, conHolder);
		}

		return con;
	}

	/**
	 * Determine whether the given JCA CCI Connection is transactional, that is,
	 * bound to the current thread by Spring's transaction facilities.
	 * @param con the Connection to check
	 * @param cf the ConnectionFactory that the Connection was obtained from
	 * (may be {@code null})
	 * @return whether the Connection is transactional
	 */
	public static boolean isConnectionTransactional(Connection con, @Nullable ConnectionFactory cf) {
		if (cf == null) {
			return false;
		}
		ConnectionHolder conHolder = (ConnectionHolder) TransactionSynchronizationManager.getResource(cf);
		return (conHolder != null && conHolder.getConnection() == con);
	}

	/**
	 * Close the given Connection, obtained from the given ConnectionFactory,
	 * if it is not managed externally (that is, not bound to the thread).
	 * @param con the Connection to close if necessary
	 * (if this is {@code null}, the call will be ignored)
	 * @param cf the ConnectionFactory that the Connection was obtained from
	 * (can be {@code null})
	 * @see #getConnection
	 */
	public static void releaseConnection(@Nullable Connection con, @Nullable ConnectionFactory cf) {
		try {
			doReleaseConnection(con, cf);
		}
		catch (ResourceException ex) {
			logger.debug("Could not close CCI Connection", ex);
		}
		catch (Throwable ex) {
			// We don't trust the CCI driver: It might throw RuntimeException or Error.
			logger.debug("Unexpected exception on closing CCI Connection", ex);
		}
	}

	/**
	 * Actually close the given Connection, obtained from the given ConnectionFactory.
	 * Same as {@link #releaseConnection}, but throwing the original ResourceException.
	 * <p>Directly accessed by {@link TransactionAwareConnectionFactoryProxy}.
	 * @param con the Connection to close if necessary
	 * (if this is {@code null}, the call will be ignored)
	 * @param cf the ConnectionFactory that the Connection was obtained from
	 * (can be {@code null})
	 * @throws ResourceException if thrown by JCA CCI methods
	 * @see #doGetConnection
	 */
	public static void doReleaseConnection(@Nullable Connection con, @Nullable ConnectionFactory cf)
			throws ResourceException {

		if (con == null || isConnectionTransactional(con, cf)) {
			return;
		}
		con.close();
	}


	/**
	 * Callback for resource cleanup at the end of a non-native CCI transaction
	 * (e.g. when participating in a JTA transaction).
	 */
	private static class ConnectionSynchronization
			extends ResourceHolderSynchronization<ConnectionHolder, ConnectionFactory> {

		public ConnectionSynchronization(ConnectionHolder connectionHolder, ConnectionFactory connectionFactory) {
			super(connectionHolder, connectionFactory);
		}

		@Override
		protected void releaseResource(ConnectionHolder resourceHolder, ConnectionFactory resourceKey) {
			releaseConnection(resourceHolder.getConnection(), resourceKey);
		}
	}

}
