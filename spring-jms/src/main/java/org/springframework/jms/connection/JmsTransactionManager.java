/*
 * Copyright 2002-2019 the original author or authors.
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

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.TransactionRolledBackException;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.InvalidIsolationLevelException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.ResourceTransactionManager;
import org.springframework.transaction.support.SmartTransactionObject;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.transaction.PlatformTransactionManager} implementation
 * for a single JMS {@link javax.jms.ConnectionFactory}. Binds a JMS
 * Connection/Session pair from the specified ConnectionFactory to the thread,
 * potentially allowing for one thread-bound Session per ConnectionFactory.
 *
 * <p>This local strategy is an alternative to executing JMS operations within
 * JTA transactions. Its advantage is that it is able to work in any environment,
 * for example a standalone application or a test suite, with any message broker
 * as target. However, this strategy is <i>not</i> able to provide XA transactions,
 * for example in order to share transactions between messaging and database access.
 * A full JTA/XA setup is required for XA transactions, typically using Spring's
 * {@link org.springframework.transaction.jta.JtaTransactionManager} as strategy.
 *
 * <p>Application code is required to retrieve the transactional JMS Session via
 * {@link ConnectionFactoryUtils#getTransactionalSession} instead of a standard
 * Java EE-style {@link ConnectionFactory#createConnection()} call with subsequent
 * Session creation. Spring's {@link org.springframework.jms.core.JmsTemplate}
 * will autodetect a thread-bound Session and automatically participate in it.
 *
 * <p>Alternatively, you can allow application code to work with the standard
 * Java EE-style lookup pattern on a ConnectionFactory, for example for legacy code
 * that is not aware of Spring at all. In that case, define a
 * {@link TransactionAwareConnectionFactoryProxy} for your target ConnectionFactory,
 * which will automatically participate in Spring-managed transactions.
 *
 * <p><b>The use of {@link CachingConnectionFactory} as a target for this
 * transaction manager is strongly recommended.</b> CachingConnectionFactory
 * uses a single JMS Connection for all JMS access in order to avoid the overhead
 * of repeated Connection creation, as well as maintaining a cache of Sessions.
 * Each transaction will then share the same JMS Connection, while still using
 * its own individual JMS Session.
 *
 * <p>The use of a <i>raw</i> target ConnectionFactory would not only be inefficient
 * because of the lack of resource reuse. It might also lead to strange effects
 * when your JMS driver doesn't accept {@code MessageProducer.close()} calls
 * and/or {@code MessageConsumer.close()} calls before {@code Session.commit()},
 * with the latter supposed to commit all the messages that have been sent through the
 * producer handle and received through the consumer handle. As a safe general solution,
 * always pass in a {@link CachingConnectionFactory} into this transaction manager's
 * {@link #setConnectionFactory "connectionFactory"} property.
 *
 * <p>Transaction synchronization is turned off by default, as this manager might
 * be used alongside a datastore-based Spring transaction manager such as the
 * JDBC {@link org.springframework.jdbc.datasource.DataSourceTransactionManager},
 * which has stronger needs for synchronization.
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see ConnectionFactoryUtils#getTransactionalSession
 * @see TransactionAwareConnectionFactoryProxy
 * @see org.springframework.jms.core.JmsTemplate
 */
@SuppressWarnings("serial")
public class JmsTransactionManager extends AbstractPlatformTransactionManager
		implements ResourceTransactionManager, InitializingBean {

	@Nullable
	private ConnectionFactory connectionFactory;

	private boolean lazyResourceRetrieval = false;


	/**
	 * Create a new JmsTransactionManager for bean-style usage.
	 * <p>Note: The ConnectionFactory has to be set before using the instance.
	 * This constructor can be used to prepare a JmsTemplate via a BeanFactory,
	 * typically setting the ConnectionFactory via setConnectionFactory.
	 * <p>Turns off transaction synchronization by default, as this manager might
	 * be used alongside a datastore-based Spring transaction manager like
	 * DataSourceTransactionManager, which has stronger needs for synchronization.
	 * Only one manager is allowed to drive synchronization at any point of time.
	 * @see #setConnectionFactory
	 * @see #setTransactionSynchronization
	 */
	public JmsTransactionManager() {
		setTransactionSynchronization(SYNCHRONIZATION_NEVER);
	}

	/**
	 * Create a new JmsTransactionManager, given a ConnectionFactory.
	 * @param connectionFactory the ConnectionFactory to obtain connections from
	 */
	public JmsTransactionManager(ConnectionFactory connectionFactory) {
		this();
		setConnectionFactory(connectionFactory);
		afterPropertiesSet();
	}


	/**
	 * Set the JMS ConnectionFactory that this instance should manage transactions for.
	 */
	public void setConnectionFactory(@Nullable ConnectionFactory cf) {
		if (cf instanceof TransactionAwareConnectionFactoryProxy) {
			// If we got a TransactionAwareConnectionFactoryProxy, we need to perform transactions
			// for its underlying target ConnectionFactory, else JMS access code won't see
			// properly exposed transactions (i.e. transactions for the target ConnectionFactory).
			this.connectionFactory = ((TransactionAwareConnectionFactoryProxy) cf).getTargetConnectionFactory();
		}
		else {
			this.connectionFactory = cf;
		}
	}

	/**
	 * Return the JMS ConnectionFactory that this instance should manage transactions for.
	 */
	@Nullable
	public ConnectionFactory getConnectionFactory() {
		return this.connectionFactory;
	}

	/**
	 * Obtain the ConnectionFactory for actual use.
	 * @return the ConnectionFactory (never {@code null})
	 * @throws IllegalStateException in case of no ConnectionFactory set
	 * @since 5.0
	 */
	protected final ConnectionFactory obtainConnectionFactory() {
		ConnectionFactory connectionFactory = getConnectionFactory();
		Assert.state(connectionFactory != null, "No ConnectionFactory set");
		return connectionFactory;
	}

	/**
	 * Specify whether this transaction manager should lazily retrieve a JMS
	 * Connection and Session on access within a transaction ({@code true}).
	 * By default, it will eagerly create a JMS Connection and Session at
	 * transaction begin ({@code false}).
	 * @since 5.1.6
	 * @see JmsResourceHolder#getConnection()
	 * @see JmsResourceHolder#getSession()
	 */
	public void setLazyResourceRetrieval(boolean lazyResourceRetrieval) {
		this.lazyResourceRetrieval = lazyResourceRetrieval;
	}

	/**
	 * Make sure the ConnectionFactory has been set.
	 */
	@Override
	public void afterPropertiesSet() {
		if (getConnectionFactory() == null) {
			throw new IllegalArgumentException("Property 'connectionFactory' is required");
		}
	}


	@Override
	public Object getResourceFactory() {
		return obtainConnectionFactory();
	}

	@Override
	protected Object doGetTransaction() {
		JmsTransactionObject txObject = new JmsTransactionObject();
		txObject.setResourceHolder(
				(JmsResourceHolder) TransactionSynchronizationManager.getResource(obtainConnectionFactory()));
		return txObject;
	}

	@Override
	protected boolean isExistingTransaction(Object transaction) {
		JmsTransactionObject txObject = (JmsTransactionObject) transaction;
		return txObject.hasResourceHolder();
	}

	@Override
	protected void doBegin(Object transaction, TransactionDefinition definition) {
		if (definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT) {
			throw new InvalidIsolationLevelException("JMS does not support an isolation level concept");
		}

		ConnectionFactory connectionFactory = obtainConnectionFactory();
		JmsTransactionObject txObject = (JmsTransactionObject) transaction;
		Connection con = null;
		Session session = null;
		try {
			JmsResourceHolder resourceHolder;
			if (this.lazyResourceRetrieval) {
				resourceHolder = new LazyJmsResourceHolder(connectionFactory);
			}
			else {
				con = createConnection();
				session = createSession(con);
				if (logger.isDebugEnabled()) {
					logger.debug("Created JMS transaction on Session [" + session + "] from Connection [" + con + "]");
				}
				resourceHolder = new JmsResourceHolder(connectionFactory, con, session);
			}
			resourceHolder.setSynchronizedWithTransaction(true);
			int timeout = determineTimeout(definition);
			if (timeout != TransactionDefinition.TIMEOUT_DEFAULT) {
				resourceHolder.setTimeoutInSeconds(timeout);
			}
			txObject.setResourceHolder(resourceHolder);
			TransactionSynchronizationManager.bindResource(connectionFactory, resourceHolder);
		}
		catch (Throwable ex) {
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
			throw new CannotCreateTransactionException("Could not create JMS transaction", ex);
		}
	}

	@Override
	protected Object doSuspend(Object transaction) {
		JmsTransactionObject txObject = (JmsTransactionObject) transaction;
		txObject.setResourceHolder(null);
		return TransactionSynchronizationManager.unbindResource(obtainConnectionFactory());
	}

	@Override
	protected void doResume(@Nullable Object transaction, Object suspendedResources) {
		TransactionSynchronizationManager.bindResource(obtainConnectionFactory(), suspendedResources);
	}

	@Override
	protected void doCommit(DefaultTransactionStatus status) {
		JmsTransactionObject txObject = (JmsTransactionObject) status.getTransaction();
		Session session = txObject.getResourceHolder().getOriginalSession();
		if (session != null) {
			try {
				if (status.isDebug()) {
					logger.debug("Committing JMS transaction on Session [" + session + "]");
				}
				session.commit();
			}
			catch (TransactionRolledBackException ex) {
				throw new UnexpectedRollbackException("JMS transaction rolled back", ex);
			}
			catch (JMSException ex) {
				throw new TransactionSystemException("Could not commit JMS transaction", ex);
			}
		}
	}

	@Override
	protected void doRollback(DefaultTransactionStatus status) {
		JmsTransactionObject txObject = (JmsTransactionObject) status.getTransaction();
		Session session = txObject.getResourceHolder().getOriginalSession();
		if (session != null) {
			try {
				if (status.isDebug()) {
					logger.debug("Rolling back JMS transaction on Session [" + session + "]");
				}
				session.rollback();
			}
			catch (JMSException ex) {
				throw new TransactionSystemException("Could not roll back JMS transaction", ex);
			}
		}
	}

	@Override
	protected void doSetRollbackOnly(DefaultTransactionStatus status) {
		JmsTransactionObject txObject = (JmsTransactionObject) status.getTransaction();
		txObject.getResourceHolder().setRollbackOnly();
	}

	@Override
	protected void doCleanupAfterCompletion(Object transaction) {
		JmsTransactionObject txObject = (JmsTransactionObject) transaction;
		TransactionSynchronizationManager.unbindResource(obtainConnectionFactory());
		txObject.getResourceHolder().closeAll();
		txObject.getResourceHolder().clear();
	}


	/**
	 * Create a JMS Connection via this template's ConnectionFactory.
	 * <p>This implementation uses JMS 1.1 API.
	 * @return the new JMS Connection
	 * @throws javax.jms.JMSException if thrown by JMS API methods
	 */
	protected Connection createConnection() throws JMSException {
		return obtainConnectionFactory().createConnection();
	}

	/**
	 * Create a JMS Session for the given Connection.
	 * <p>This implementation uses JMS 1.1 API.
	 * @param con the JMS Connection to create a Session for
	 * @return the new JMS Session
	 * @throws javax.jms.JMSException if thrown by JMS API methods
	 */
	protected Session createSession(Connection con) throws JMSException {
		return con.createSession(true, Session.AUTO_ACKNOWLEDGE);
	}


	/**
	 * Lazily initializing variant of {@link JmsResourceHolder},
	 * initializing a JMS Connection and Session on user access.
	 */
	private class LazyJmsResourceHolder extends JmsResourceHolder {

		private boolean connectionInitialized = false;

		private boolean sessionInitialized = false;

		public LazyJmsResourceHolder(@Nullable ConnectionFactory connectionFactory) {
			super(connectionFactory);
		}

		@Override
		@Nullable
		public Connection getConnection() {
			initializeConnection();
			return super.getConnection();
		}

		@Override
		@Nullable
		public <C extends Connection> C getConnection(Class<C> connectionType) {
			initializeConnection();
			return super.getConnection(connectionType);
		}

		@Override
		@Nullable
		public Session getSession() {
			initializeSession();
			return super.getSession();
		}

		@Override
		@Nullable
		public <S extends Session> S getSession(Class<S> sessionType) {
			initializeSession();
			return super.getSession(sessionType);
		}

		@Override
		@Nullable
		public <S extends Session> S getSession(Class<S> sessionType, @Nullable Connection connection) {
			initializeSession();
			return super.getSession(sessionType, connection);
		}

		private void initializeConnection() {
			if (!this.connectionInitialized) {
				try {
					addConnection(createConnection());
				}
				catch (JMSException ex) {
					throw new CannotCreateTransactionException(
							"Failed to lazily initialize JMS Connection for transaction", ex);
				}
				this.connectionInitialized = true;
			}
		}

		private void initializeSession() {
			if (!this.sessionInitialized) {
				Connection con = getConnection();
				Assert.state(con != null, "No transactional JMS Connection");
				try {
					addSession(createSession(con), con);
				}
				catch (JMSException ex) {
					throw new CannotCreateTransactionException(
							"Failed to lazily initialize JMS Session for transaction", ex);
				}
				this.sessionInitialized = true;
			}
		}
	}


	/**
	 * JMS transaction object, representing a JmsResourceHolder.
	 * Used as transaction object by JmsTransactionManager.
	 * @see JmsResourceHolder
	 */
	private static class JmsTransactionObject implements SmartTransactionObject {

		@Nullable
		private JmsResourceHolder resourceHolder;

		public void setResourceHolder(@Nullable JmsResourceHolder resourceHolder) {
			this.resourceHolder = resourceHolder;
		}

		public JmsResourceHolder getResourceHolder() {
			Assert.state(this.resourceHolder != null, "No JmsResourceHolder available");
			return this.resourceHolder;
		}

		public boolean hasResourceHolder() {
			return (this.resourceHolder != null);
		}

		@Override
		public boolean isRollbackOnly() {
			return (this.resourceHolder != null && this.resourceHolder.isRollbackOnly());
		}

		@Override
		public void flush() {
			// no-op
		}
	}

}
