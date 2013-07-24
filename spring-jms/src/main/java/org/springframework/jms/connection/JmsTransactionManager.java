/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.jms.connection;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.TransactionRolledBackException;

import org.springframework.beans.factory.InitializingBean;
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
 * J2EE-style {@link ConnectionFactory#createConnection()} call with subsequent
 * Session creation. Spring's {@link org.springframework.jms.core.JmsTemplate}
 * will autodetect a thread-bound Session and automatically participate in it.
 *
 * <p>Alternatively, you can allow application code to work with the standard
 * J2EE-style lookup pattern on a ConnectionFactory, for example for legacy code
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

	private ConnectionFactory connectionFactory;


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
	public void setConnectionFactory(ConnectionFactory cf) {
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
	public ConnectionFactory getConnectionFactory() {
		return this.connectionFactory;
	}

	/**
	 * Make sure the ConnectionFactory has been set.
	 */
	public void afterPropertiesSet() {
		if (getConnectionFactory() == null) {
			throw new IllegalArgumentException("Property 'connectionFactory' is required");
		}
	}


	public Object getResourceFactory() {
		return getConnectionFactory();
	}

	protected Object doGetTransaction() {
		JmsTransactionObject txObject = new JmsTransactionObject();
		txObject.setResourceHolder(
				(JmsResourceHolder) TransactionSynchronizationManager.getResource(getConnectionFactory()));
		return txObject;
	}

	protected boolean isExistingTransaction(Object transaction) {
		JmsTransactionObject txObject = (JmsTransactionObject) transaction;
		return (txObject.getResourceHolder() != null);
	}

	protected void doBegin(Object transaction, TransactionDefinition definition) {
		if (definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT) {
			throw new InvalidIsolationLevelException("JMS does not support an isolation level concept");
		}
		JmsTransactionObject txObject = (JmsTransactionObject) transaction;
		Connection con = null;
		Session session = null;
		try {
			con = createConnection();
			session = createSession(con);
			if (logger.isDebugEnabled()) {
				logger.debug("Created JMS transaction on Session [" + session + "] from Connection [" + con + "]");
			}
			txObject.setResourceHolder(new JmsResourceHolder(getConnectionFactory(), con, session));
			txObject.getResourceHolder().setSynchronizedWithTransaction(true);
			int timeout = determineTimeout(definition);
			if (timeout != TransactionDefinition.TIMEOUT_DEFAULT) {
				txObject.getResourceHolder().setTimeoutInSeconds(timeout);
			}
			TransactionSynchronizationManager.bindResource(
					getConnectionFactory(), txObject.getResourceHolder());
		}
		catch (Throwable ex) {
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

	protected Object doSuspend(Object transaction) {
		JmsTransactionObject txObject = (JmsTransactionObject) transaction;
		txObject.setResourceHolder(null);
		return TransactionSynchronizationManager.unbindResource(getConnectionFactory());
	}

	protected void doResume(Object transaction, Object suspendedResources) {
		JmsResourceHolder conHolder = (JmsResourceHolder) suspendedResources;
		TransactionSynchronizationManager.bindResource(getConnectionFactory(), conHolder);
	}

	protected void doCommit(DefaultTransactionStatus status) {
		JmsTransactionObject txObject = (JmsTransactionObject) status.getTransaction();
		Session session = txObject.getResourceHolder().getSession();
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

	protected void doRollback(DefaultTransactionStatus status) {
		JmsTransactionObject txObject = (JmsTransactionObject) status.getTransaction();
		Session session = txObject.getResourceHolder().getSession();
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

	protected void doSetRollbackOnly(DefaultTransactionStatus status) {
		JmsTransactionObject txObject = (JmsTransactionObject) status.getTransaction();
		txObject.getResourceHolder().setRollbackOnly();
	}

	protected void doCleanupAfterCompletion(Object transaction) {
		JmsTransactionObject txObject = (JmsTransactionObject) transaction;
		TransactionSynchronizationManager.unbindResource(getConnectionFactory());
		txObject.getResourceHolder().closeAll();
		txObject.getResourceHolder().clear();
	}


	//-------------------------------------------------------------------------
	// JMS 1.1 factory methods, potentially overridden for JMS 1.0.2
	//-------------------------------------------------------------------------

	/**
	 * Create a JMS Connection via this template's ConnectionFactory.
	 * <p>This implementation uses JMS 1.1 API.
	 * @return the new JMS Connection
	 * @throws javax.jms.JMSException if thrown by JMS API methods
	 */
	protected Connection createConnection() throws JMSException {
		return getConnectionFactory().createConnection();
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
	 * JMS transaction object, representing a JmsResourceHolder.
	 * Used as transaction object by JmsTransactionManager.
	 * @see JmsResourceHolder
	 */
	private static class JmsTransactionObject implements SmartTransactionObject {

		private JmsResourceHolder resourceHolder;

		public void setResourceHolder(JmsResourceHolder resourceHolder) {
			this.resourceHolder = resourceHolder;
		}

		public JmsResourceHolder getResourceHolder() {
			return this.resourceHolder;
		}

		public boolean isRollbackOnly() {
			return this.resourceHolder.isRollbackOnly();
		}

		public void flush() {
			// no-op
		}
	}

}
