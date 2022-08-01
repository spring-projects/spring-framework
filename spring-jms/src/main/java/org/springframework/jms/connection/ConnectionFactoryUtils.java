/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.jms.connection;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.QueueConnection;
import jakarta.jms.QueueConnectionFactory;
import jakarta.jms.QueueSession;
import jakarta.jms.Session;
import jakarta.jms.TopicConnection;
import jakarta.jms.TopicConnectionFactory;
import jakarta.jms.TopicSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.transaction.support.ResourceHolderSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * Helper class for managing a JMS {@link jakarta.jms.ConnectionFactory}, in particular
 * for obtaining transactional JMS resources for a given ConnectionFactory.
 *
 * <p>Mainly for internal use within the framework. Used by
 * {@link org.springframework.jms.core.JmsTemplate} as well as
 * {@link org.springframework.jms.listener.DefaultMessageListenerContainer}.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see SmartConnectionFactory
 */
public abstract class ConnectionFactoryUtils {

	private static final Log logger = LogFactory.getLog(ConnectionFactoryUtils.class);


	/**
	 * Release the given Connection, stopping it (if necessary) and eventually closing it.
	 * <p>Checks {@link SmartConnectionFactory#shouldStop}, if available.
	 * This is essentially a more sophisticated version of
	 * {@link org.springframework.jms.support.JmsUtils#closeConnection}.
	 * @param con the Connection to release
	 * (if this is {@code null}, the call will be ignored)
	 * @param cf the ConnectionFactory that the Connection was obtained from
	 * (may be {@code null})
	 * @param started whether the Connection might have been started by the application
	 * @see SmartConnectionFactory#shouldStop
	 * @see org.springframework.jms.support.JmsUtils#closeConnection
	 */
	public static void releaseConnection(@Nullable Connection con, @Nullable ConnectionFactory cf, boolean started) {
		if (con == null) {
			return;
		}
		if (started && cf instanceof SmartConnectionFactory && ((SmartConnectionFactory) cf).shouldStop(con)) {
			try {
				con.stop();
			}
			catch (Throwable ex) {
				logger.debug("Could not stop JMS Connection before closing it", ex);
			}
		}
		try {
			con.close();
		}
		catch (Throwable ex) {
			logger.debug("Could not close JMS Connection", ex);
		}
	}

	/**
	 * Return the innermost target Session of the given Session. If the given
	 * Session is a proxy, it will be unwrapped until a non-proxy Session is
	 * found. Otherwise, the passed-in Session will be returned as-is.
	 * @param session the Session proxy to unwrap
	 * @return the innermost target Session, or the passed-in one if no proxy
	 * @see SessionProxy#getTargetSession()
	 */
	public static Session getTargetSession(Session session) {
		Session sessionToUse = session;
		while (sessionToUse instanceof SessionProxy) {
			sessionToUse = ((SessionProxy) sessionToUse).getTargetSession();
		}
		return sessionToUse;
	}



	/**
	 * Determine whether the given JMS Session is transactional, that is,
	 * bound to the current thread by Spring's transaction facilities.
	 * @param session the JMS Session to check
	 * @param cf the JMS ConnectionFactory that the Session originated from
	 * @return whether the Session is transactional
	 */
	public static boolean isSessionTransactional(@Nullable Session session, @Nullable ConnectionFactory cf) {
		if (session == null || cf == null) {
			return false;
		}
		JmsResourceHolder resourceHolder = (JmsResourceHolder) TransactionSynchronizationManager.getResource(cf);
		return (resourceHolder != null && resourceHolder.containsSession(session));
	}


	/**
	 * Obtain a JMS Session that is synchronized with the current transaction, if any.
	 * @param cf the ConnectionFactory to obtain a Session for
	 * @param existingCon the existing JMS Connection to obtain a Session for
	 * (may be {@code null})
	 * @param synchedLocalTransactionAllowed whether to allow for a local JMS transaction
	 * that is synchronized with a Spring-managed transaction (where the main transaction
	 * might be a JDBC-based one for a specific DataSource, for example), with the JMS
	 * transaction committing right after the main transaction. If not allowed, the given
	 * ConnectionFactory needs to handle transaction enlistment underneath the covers.
	 * @return the transactional Session, or {@code null} if none found
	 * @throws JMSException in case of JMS failure
	 */
	@Nullable
	public static Session getTransactionalSession(final ConnectionFactory cf,
			@Nullable final Connection existingCon, final boolean synchedLocalTransactionAllowed)
			throws JMSException {

		return doGetTransactionalSession(cf, new ResourceFactory() {
			@Override
			@Nullable
			public Session getSession(JmsResourceHolder holder) {
				return holder.getSession(Session.class, existingCon);
			}
			@Override
			@Nullable
			public Connection getConnection(JmsResourceHolder holder) {
				return (existingCon != null ? existingCon : holder.getConnection());
			}
			@Override
			public Connection createConnection() throws JMSException {
				return cf.createConnection();
			}
			@Override
			public Session createSession(Connection con) throws JMSException {
				return con.createSession(synchedLocalTransactionAllowed, Session.AUTO_ACKNOWLEDGE);
			}
			@Override
			public boolean isSynchedLocalTransactionAllowed() {
				return synchedLocalTransactionAllowed;
			}
		}, true);
	}

	/**
	 * Obtain a JMS QueueSession that is synchronized with the current transaction, if any.
	 * <p>Mainly intended for use with the JMS 1.0.2 API.
	 * @param cf the ConnectionFactory to obtain a Session for
	 * @param existingCon the existing JMS Connection to obtain a Session for
	 * (may be {@code null})
	 * @param synchedLocalTransactionAllowed whether to allow for a local JMS transaction
	 * that is synchronized with a Spring-managed transaction (where the main transaction
	 * might be a JDBC-based one for a specific DataSource, for example), with the JMS
	 * transaction committing right after the main transaction. If not allowed, the given
	 * ConnectionFactory needs to handle transaction enlistment underneath the covers.
	 * @return the transactional Session, or {@code null} if none found
	 * @throws JMSException in case of JMS failure
	 */
	@Nullable
	public static QueueSession getTransactionalQueueSession(final QueueConnectionFactory cf,
			@Nullable final QueueConnection existingCon, final boolean synchedLocalTransactionAllowed)
			throws JMSException {

		return (QueueSession) doGetTransactionalSession(cf, new ResourceFactory() {
			@Override
			@Nullable
			public Session getSession(JmsResourceHolder holder) {
				return holder.getSession(QueueSession.class, existingCon);
			}
			@Override
			@Nullable
			public Connection getConnection(JmsResourceHolder holder) {
				return (existingCon != null ? existingCon : holder.getConnection(QueueConnection.class));
			}
			@Override
			public Connection createConnection() throws JMSException {
				return cf.createQueueConnection();
			}
			@Override
			public Session createSession(Connection con) throws JMSException {
				return ((QueueConnection) con).createQueueSession(synchedLocalTransactionAllowed, Session.AUTO_ACKNOWLEDGE);
			}
			@Override
			public boolean isSynchedLocalTransactionAllowed() {
				return synchedLocalTransactionAllowed;
			}
		}, true);
	}

	/**
	 * Obtain a JMS TopicSession that is synchronized with the current transaction, if any.
	 * <p>Mainly intended for use with the JMS 1.0.2 API.
	 * @param cf the ConnectionFactory to obtain a Session for
	 * @param existingCon the existing JMS Connection to obtain a Session for
	 * (may be {@code null})
	 * @param synchedLocalTransactionAllowed whether to allow for a local JMS transaction
	 * that is synchronized with a Spring-managed transaction (where the main transaction
	 * might be a JDBC-based one for a specific DataSource, for example), with the JMS
	 * transaction committing right after the main transaction. If not allowed, the given
	 * ConnectionFactory needs to handle transaction enlistment underneath the covers.
	 * @return the transactional Session, or {@code null} if none found
	 * @throws JMSException in case of JMS failure
	 */
	@Nullable
	public static TopicSession getTransactionalTopicSession(final TopicConnectionFactory cf,
			@Nullable final TopicConnection existingCon, final boolean synchedLocalTransactionAllowed)
			throws JMSException {

		return (TopicSession) doGetTransactionalSession(cf, new ResourceFactory() {
			@Override
			@Nullable
			public Session getSession(JmsResourceHolder holder) {
				return holder.getSession(TopicSession.class, existingCon);
			}
			@Override
			@Nullable
			public Connection getConnection(JmsResourceHolder holder) {
				return (existingCon != null ? existingCon : holder.getConnection(TopicConnection.class));
			}
			@Override
			public Connection createConnection() throws JMSException {
				return cf.createTopicConnection();
			}
			@Override
			public Session createSession(Connection con) throws JMSException {
				return ((TopicConnection) con).createTopicSession(
						synchedLocalTransactionAllowed, Session.AUTO_ACKNOWLEDGE);
			}
			@Override
			public boolean isSynchedLocalTransactionAllowed() {
				return synchedLocalTransactionAllowed;
			}
		}, true);
	}

	/**
	 * Obtain a JMS Session that is synchronized with the current transaction, if any.
	 * <p>This {@code doGetTransactionalSession} variant always starts the underlying
	 * JMS Connection, assuming that the Session will be used for receiving messages.
	 * @param connectionFactory the JMS ConnectionFactory to bind for
	 * (used as TransactionSynchronizationManager key)
	 * @param resourceFactory the ResourceFactory to use for extracting or creating
	 * JMS resources
	 * @return the transactional Session, or {@code null} if none found
	 * @throws JMSException in case of JMS failure
	 * @see #doGetTransactionalSession(jakarta.jms.ConnectionFactory, ResourceFactory, boolean)
	 */
	@Nullable
	public static Session doGetTransactionalSession(
			ConnectionFactory connectionFactory, ResourceFactory resourceFactory) throws JMSException {

		return doGetTransactionalSession(connectionFactory, resourceFactory, true);
	}

	/**
	 * Obtain a JMS Session that is synchronized with the current transaction, if any.
	 * @param connectionFactory the JMS ConnectionFactory to bind for
	 * (used as TransactionSynchronizationManager key)
	 * @param resourceFactory the ResourceFactory to use for extracting or creating
	 * JMS resources
	 * @param startConnection whether the underlying JMS Connection approach should be
	 * started in order to allow for receiving messages. Note that a reused Connection
	 * may already have been started before, even if this flag is {@code false}.
	 * @return the transactional Session, or {@code null} if none found
	 * @throws JMSException in case of JMS failure
	 */
	@Nullable
	public static Session doGetTransactionalSession(
			ConnectionFactory connectionFactory, ResourceFactory resourceFactory, boolean startConnection)
			throws JMSException {

		Assert.notNull(connectionFactory, "ConnectionFactory must not be null");
		Assert.notNull(resourceFactory, "ResourceFactory must not be null");

		JmsResourceHolder resourceHolder =
				(JmsResourceHolder) TransactionSynchronizationManager.getResource(connectionFactory);
		if (resourceHolder != null) {
			Session session = resourceFactory.getSession(resourceHolder);
			if (session != null) {
				if (startConnection) {
					Connection con = resourceFactory.getConnection(resourceHolder);
					if (con != null) {
						con.start();
					}
				}
				return session;
			}
			if (resourceHolder.isFrozen()) {
				return null;
			}
		}
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			return null;
		}
		JmsResourceHolder resourceHolderToUse = resourceHolder;
		if (resourceHolderToUse == null) {
			resourceHolderToUse = new JmsResourceHolder(connectionFactory);
		}
		Connection con = resourceFactory.getConnection(resourceHolderToUse);
		Session session = null;
		try {
			boolean isExistingCon = (con != null);
			if (!isExistingCon) {
				con = resourceFactory.createConnection();
				resourceHolderToUse.addConnection(con);
			}
			session = resourceFactory.createSession(con);
			resourceHolderToUse.addSession(session, con);
			if (startConnection) {
				con.start();
			}
		}
		catch (JMSException ex) {
			if (session != null) {
				try {
					session.close();
				}
				catch (Throwable ex2) {
					// ignore
				}
			}
			if (con != null) {
				try {
					con.close();
				}
				catch (Throwable ex2) {
					// ignore
				}
			}
			throw ex;
		}
		if (resourceHolderToUse != resourceHolder) {
			TransactionSynchronizationManager.registerSynchronization(
					new JmsResourceSynchronization(resourceHolderToUse, connectionFactory,
							resourceFactory.isSynchedLocalTransactionAllowed()));
			resourceHolderToUse.setSynchronizedWithTransaction(true);
			TransactionSynchronizationManager.bindResource(connectionFactory, resourceHolderToUse);
		}
		return session;
	}


	/**
	 * Callback interface for resource creation.
	 * Serving as argument for the {@code doGetTransactionalSession} method.
	 */
	public interface ResourceFactory {

		/**
		 * Fetch an appropriate Session from the given JmsResourceHolder.
		 * @param holder the JmsResourceHolder
		 * @return an appropriate Session fetched from the holder,
		 * or {@code null} if none found
		 */
		@Nullable
		Session getSession(JmsResourceHolder holder);

		/**
		 * Fetch an appropriate Connection from the given JmsResourceHolder.
		 * @param holder the JmsResourceHolder
		 * @return an appropriate Connection fetched from the holder,
		 * or {@code null} if none found
		 */
		@Nullable
		Connection getConnection(JmsResourceHolder holder);

		/**
		 * Create a new JMS Connection for registration with a JmsResourceHolder.
		 * @return the new JMS Connection
		 * @throws JMSException if thrown by JMS API methods
		 */
		Connection createConnection() throws JMSException;

		/**
		 * Create a new JMS Session for registration with a JmsResourceHolder.
		 * @param con the JMS Connection to create a Session for
		 * @return the new JMS Session
		 * @throws JMSException if thrown by JMS API methods
		 */
		Session createSession(Connection con) throws JMSException;

		/**
		 * Return whether to allow for a local JMS transaction that is synchronized with
		 * a Spring-managed transaction (where the main transaction might be a JDBC-based
		 * one for a specific DataSource, for example), with the JMS transaction
		 * committing right after the main transaction.
		 * @return whether to allow for synchronizing a local JMS transaction
		 */
		boolean isSynchedLocalTransactionAllowed();
	}


	/**
	 * Callback for resource cleanup at the end of a non-native JMS transaction
	 * (e.g. when participating in a JtaTransactionManager transaction).
	 * @see org.springframework.transaction.jta.JtaTransactionManager
	 */
	private static class JmsResourceSynchronization extends ResourceHolderSynchronization<JmsResourceHolder, Object> {

		private final boolean transacted;

		public JmsResourceSynchronization(JmsResourceHolder resourceHolder, Object resourceKey, boolean transacted) {
			super(resourceHolder, resourceKey);
			this.transacted = transacted;
		}

		@Override
		protected boolean shouldReleaseBeforeCompletion() {
			return !this.transacted;
		}

		@Override
		protected void processResourceAfterCommit(JmsResourceHolder resourceHolder) {
			try {
				resourceHolder.commitAll();
			}
			catch (JMSException ex) {
				throw new SynchedLocalTransactionFailedException("Local JMS transaction failed to commit", ex);
			}
		}

		@Override
		protected void releaseResource(JmsResourceHolder resourceHolder, Object resourceKey) {
			resourceHolder.closeAll();
		}
	}

}
