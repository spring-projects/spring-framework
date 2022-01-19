/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.jms.listener;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;

import org.springframework.jms.connection.ConnectionFactoryUtils;
import org.springframework.jms.connection.JmsResourceHolder;
import org.springframework.jms.connection.SingleConnectionFactory;
import org.springframework.jms.support.JmsUtils;
import org.springframework.lang.Nullable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.ResourceTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;
import org.springframework.util.Assert;

/**
 * Base class for listener container implementations which are based on polling.
 * Provides support for listener handling based on {@link javax.jms.MessageConsumer},
 * optionally participating in externally managed transactions.
 *
 * <p>This listener container variant is built for repeated polling attempts,
 * each invoking the {@link #receiveAndExecute} method. The MessageConsumer used
 * may be reobtained fo reach attempt or cached in between attempts; this is up
 * to the concrete implementation. The receive timeout for each attempt can be
 * configured through the {@link #setReceiveTimeout "receiveTimeout"} property.
 *
 * <p>The underlying mechanism is based on standard JMS MessageConsumer handling,
 * which is perfectly compatible with both native JMS and JMS in a Java EE environment.
 * Neither the JMS {@code MessageConsumer.setMessageListener} facility  nor the JMS
 * ServerSessionPool facility is required. A further advantage of this approach is
 * full control over the listening process, allowing for custom scaling and throttling
 * and of concurrent message processing (which is up to concrete subclasses).
 *
 * <p>Message reception and listener execution can automatically be wrapped
 * in transactions through passing a Spring
 * {@link org.springframework.transaction.PlatformTransactionManager} into the
 * {@link #setTransactionManager "transactionManager"} property. This will usually
 * be a {@link org.springframework.transaction.jta.JtaTransactionManager} in a
 * Java EE environment, in combination with a JTA-aware JMS ConnectionFactory
 * obtained from JNDI (check your application server's documentation).
 *
 * <p>This base class does not assume any specific mechanism for asynchronous
 * execution of polling invokers. Check out {@link DefaultMessageListenerContainer}
 * for a concrete implementation which is based on Spring's
 * {@link org.springframework.core.task.TaskExecutor} abstraction,
 * including dynamic scaling of concurrent consumers and automatic self recovery.
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 * @see #createListenerConsumer
 * @see #receiveAndExecute
 * @see #setTransactionManager
 */
public abstract class AbstractPollingMessageListenerContainer extends AbstractMessageListenerContainer {

	/**
	 * The default receive timeout: 1000 ms = 1 second.
	 */
	public static final long DEFAULT_RECEIVE_TIMEOUT = 1000;


	private final MessageListenerContainerResourceFactory transactionalResourceFactory =
			new MessageListenerContainerResourceFactory();

	private boolean sessionTransactedCalled = false;

	@Nullable
	private PlatformTransactionManager transactionManager;

	private DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition();

	private long receiveTimeout = DEFAULT_RECEIVE_TIMEOUT;


	@Override
	public void setSessionTransacted(boolean sessionTransacted) {
		super.setSessionTransacted(sessionTransacted);
		this.sessionTransactedCalled = true;
	}

	/**
	 * Specify the Spring {@link org.springframework.transaction.PlatformTransactionManager}
	 * to use for transactional wrapping of message reception plus listener execution.
	 * <p>Default is none, not performing any transactional wrapping.
	 * If specified, this will usually be a Spring
	 * {@link org.springframework.transaction.jta.JtaTransactionManager} or one
	 * of its subclasses, in combination with a JTA-aware ConnectionFactory that
	 * this message listener container obtains its Connections from.
	 * <p><b>Note: Consider the use of local JMS transactions instead.</b>
	 * Simply switch the {@link #setSessionTransacted "sessionTransacted"} flag
	 * to "true" in order to use a locally transacted JMS Session for the entire
	 * receive processing, including any Session operations performed by a
	 * {@link SessionAwareMessageListener} (e.g. sending a response message). This
	 * allows for fully synchronized Spring transactions based on local JMS
	 * transactions, similar to what
	 * {@link org.springframework.jms.connection.JmsTransactionManager} provides. Check
	 * {@link AbstractMessageListenerContainer}'s javadoc for
	 * a discussion of transaction choices and message redelivery scenarios.
	 * @see #setSessionTransacted(boolean)
	 * @see org.springframework.transaction.jta.JtaTransactionManager
	 * @see org.springframework.jms.connection.JmsTransactionManager
	 */
	public void setTransactionManager(@Nullable PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	/**
	 * Return the Spring PlatformTransactionManager to use for transactional
	 * wrapping of message reception plus listener execution.
	 */
	@Nullable
	protected final PlatformTransactionManager getTransactionManager() {
		return this.transactionManager;
	}

	/**
	 * Specify the transaction name to use for transactional wrapping.
	 * Default is the bean name of this listener container, if any.
	 * @see org.springframework.transaction.TransactionDefinition#getName()
	 */
	public void setTransactionName(String transactionName) {
		this.transactionDefinition.setName(transactionName);
	}

	/**
	 * Specify the transaction timeout to use for transactional wrapping, in <b>seconds</b>.
	 * Default is none, using the transaction manager's default timeout.
	 * @see org.springframework.transaction.TransactionDefinition#getTimeout()
	 * @see #setReceiveTimeout
	 */
	public void setTransactionTimeout(int transactionTimeout) {
		this.transactionDefinition.setTimeout(transactionTimeout);
	}

	/**
	 * Set the timeout to use for receive calls, in <b>milliseconds</b>.
	 * The default is 1000 ms, that is, 1 second.
	 * <p><b>NOTE:</b> This value needs to be smaller than the transaction
	 * timeout used by the transaction manager (in the appropriate unit,
	 * of course). 0 indicates no timeout at all; however, this is only
	 * feasible if not running within a transaction manager and generally
	 * discouraged since such a listener container cannot cleanly shut down.
	 * A negative value such as -1 indicates a no-wait receive operation.
	 * @see #receiveFromConsumer(MessageConsumer, long)
	 * @see javax.jms.MessageConsumer#receive(long)
	 * @see javax.jms.MessageConsumer#receiveNoWait()
	 * @see javax.jms.MessageConsumer#receive()
	 * @see #setTransactionTimeout
	 */
	public void setReceiveTimeout(long receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}

	/**
	 * Return the receive timeout (ms) configured for this listener container.
	 * @since 4.2
	 */
	protected long getReceiveTimeout() {
		return this.receiveTimeout;
	}


	@Override
	public void initialize() {
		// Set sessionTransacted=true in case of a non-JTA transaction manager.
		if (!this.sessionTransactedCalled &&
				this.transactionManager instanceof ResourceTransactionManager &&
				!TransactionSynchronizationUtils.sameResourceFactory(
						(ResourceTransactionManager) this.transactionManager, obtainConnectionFactory())) {
			super.setSessionTransacted(true);
		}

		// Use bean name as default transaction name.
		if (this.transactionDefinition.getName() == null) {
			String beanName = getBeanName();
			if (beanName != null) {
				this.transactionDefinition.setName(beanName);
			}
		}

		// Proceed with superclass initialization.
		super.initialize();
	}


	/**
	 * Create a MessageConsumer for the given JMS Session,
	 * registering a MessageListener for the specified listener.
	 * @param session the JMS Session to work on
	 * @return the MessageConsumer
	 * @throws javax.jms.JMSException if thrown by JMS methods
	 * @see #receiveAndExecute
	 */
	protected MessageConsumer createListenerConsumer(Session session) throws JMSException {
		Destination destination = getDestination();
		if (destination == null) {
			String destinationName = getDestinationName();
			Assert.state(destinationName != null, "No destination set");
			destination = resolveDestinationName(session, destinationName);
		}
		return createConsumer(session, destination);
	}

	/**
	 * Execute the listener for a message received from the given consumer,
	 * wrapping the entire operation in an external transaction if demanded.
	 * @param session the JMS Session to work on
	 * @param consumer the MessageConsumer to work on
	 * @return whether a message has been received
	 * @throws JMSException if thrown by JMS methods
	 * @see #doReceiveAndExecute
	 */
	protected boolean receiveAndExecute(
			Object invoker, @Nullable Session session, @Nullable MessageConsumer consumer)
			throws JMSException {

		if (this.transactionManager != null) {
			// Execute receive within transaction.
			TransactionStatus status = this.transactionManager.getTransaction(this.transactionDefinition);
			boolean messageReceived;
			try {
				messageReceived = doReceiveAndExecute(invoker, session, consumer, status);
			}
			catch (JMSException | RuntimeException | Error ex) {
				rollbackOnException(this.transactionManager, status, ex);
				throw ex;
			}
			try {
				this.transactionManager.commit(status);
			}
			catch (TransactionException ex) {
				// Propagate transaction system exceptions as infrastructure problems.
				throw ex;
			}
			catch (RuntimeException ex) {
				// Typically a late persistence exception from a listener-used resource
				// -> handle it as listener exception, not as an infrastructure problem.
				// E.g. a database locking failure should not lead to listener shutdown.
				handleListenerException(ex);
			}
			return messageReceived;
		}

		else {
			// Execute receive outside of transaction.
			return doReceiveAndExecute(invoker, session, consumer, null);
		}
	}

	/**
	 * Actually execute the listener for a message received from the given consumer,
	 * fetching all requires resources and invoking the listener.
	 * @param session the JMS Session to work on
	 * @param consumer the MessageConsumer to work on
	 * @param status the TransactionStatus (may be {@code null})
	 * @return whether a message has been received
	 * @throws JMSException if thrown by JMS methods
	 * @see #doExecuteListener(javax.jms.Session, javax.jms.Message)
	 */
	protected boolean doReceiveAndExecute(Object invoker, @Nullable Session session,
			@Nullable MessageConsumer consumer, @Nullable TransactionStatus status) throws JMSException {

		Connection conToClose = null;
		Session sessionToClose = null;
		MessageConsumer consumerToClose = null;
		try {
			Session sessionToUse = session;
			boolean transactional = false;
			if (sessionToUse == null) {
				sessionToUse = ConnectionFactoryUtils.doGetTransactionalSession(
						obtainConnectionFactory(), this.transactionalResourceFactory, true);
				transactional = (sessionToUse != null);
			}
			if (sessionToUse == null) {
				Connection conToUse;
				if (sharedConnectionEnabled()) {
					conToUse = getSharedConnection();
				}
				else {
					conToUse = createConnection();
					conToClose = conToUse;
					conToUse.start();
				}
				sessionToUse = createSession(conToUse);
				sessionToClose = sessionToUse;
			}
			MessageConsumer consumerToUse = consumer;
			if (consumerToUse == null) {
				consumerToUse = createListenerConsumer(sessionToUse);
				consumerToClose = consumerToUse;
			}
			Message message = receiveMessage(consumerToUse);
			if (message != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Received message of type [" + message.getClass() + "] from consumer [" +
							consumerToUse + "] of " + (transactional ? "transactional " : "") + "session [" +
							sessionToUse + "]");
				}
				messageReceived(invoker, sessionToUse);
				boolean exposeResource = (!transactional && isExposeListenerSession() &&
						!TransactionSynchronizationManager.hasResource(obtainConnectionFactory()));
				if (exposeResource) {
					TransactionSynchronizationManager.bindResource(
							obtainConnectionFactory(), new LocallyExposedJmsResourceHolder(sessionToUse));
				}
				try {
					doExecuteListener(sessionToUse, message);
				}
				catch (Throwable ex) {
					if (status != null) {
						if (logger.isDebugEnabled()) {
							logger.debug("Rolling back transaction because of listener exception thrown: " + ex);
						}
						status.setRollbackOnly();
					}
					handleListenerException(ex);
					// Rethrow JMSException to indicate an infrastructure problem
					// that may have to trigger recovery...
					if (ex instanceof JMSException) {
						throw (JMSException) ex;
					}
				}
				finally {
					if (exposeResource) {
						TransactionSynchronizationManager.unbindResource(obtainConnectionFactory());
					}
				}
				// Indicate that a message has been received.
				return true;
			}
			else {
				if (logger.isTraceEnabled()) {
					logger.trace("Consumer [" + consumerToUse + "] of " + (transactional ? "transactional " : "") +
							"session [" + sessionToUse + "] did not receive a message");
				}
				noMessageReceived(invoker, sessionToUse);
				// Nevertheless call commit, in order to reset the transaction timeout (if any).
				if (shouldCommitAfterNoMessageReceived(sessionToUse)) {
					commitIfNecessary(sessionToUse, null);
				}
				// Indicate that no message has been received.
				return false;
			}
		}
		finally {
			JmsUtils.closeMessageConsumer(consumerToClose);
			JmsUtils.closeSession(sessionToClose);
			ConnectionFactoryUtils.releaseConnection(conToClose, getConnectionFactory(), true);
		}
	}

	/**
	 * This implementation checks whether the Session is externally synchronized.
	 * In this case, the Session is not locally transacted, despite the listener
	 * container's "sessionTransacted" flag being set to "true".
	 * @see org.springframework.jms.connection.JmsResourceHolder
	 */
	@Override
	protected boolean isSessionLocallyTransacted(Session session) {
		if (!super.isSessionLocallyTransacted(session)) {
			return false;
		}
		JmsResourceHolder resourceHolder =
				(JmsResourceHolder) TransactionSynchronizationManager.getResource(obtainConnectionFactory());
		return (resourceHolder == null || resourceHolder instanceof LocallyExposedJmsResourceHolder ||
				!resourceHolder.containsSession(session));
	}

	/**
	 * Determine whether to trigger a commit after no message has been received.
	 * This is a good idea on any modern-day JMS provider.
	 * @param session the current JMS Session which received no message
	 * @return whether to call {@link #commitIfNecessary} on the given Session
	 */
	protected boolean shouldCommitAfterNoMessageReceived(Session session) {
		return true;
	}

	/**
	 * Perform a rollback, handling rollback exceptions properly.
	 * @param status object representing the transaction
	 * @param ex the thrown listener exception or error
	 */
	private void rollbackOnException(PlatformTransactionManager manager, TransactionStatus status, Throwable ex) {
		logger.debug("Initiating transaction rollback on listener exception", ex);
		try {
			manager.rollback(status);
		}
		catch (RuntimeException ex2) {
			logger.error("Listener exception overridden by rollback exception", ex);
			throw ex2;
		}
		catch (Error err) {
			logger.error("Listener exception overridden by rollback error", ex);
			throw err;
		}
	}

	/**
	 * Receive a message from the given consumer.
	 * @param consumer the MessageConsumer to use
	 * @return the Message, or {@code null} if none
	 * @throws JMSException if thrown by JMS methods
	 */
	@Nullable
	protected Message receiveMessage(MessageConsumer consumer) throws JMSException {
		return receiveFromConsumer(consumer, getReceiveTimeout());
	}

	/**
	 * Template method that gets called right when a new message has been received,
	 * before attempting to process it. Allows subclasses to react to the event
	 * of an actual incoming message, for example adapting their consumer count.
	 * @param invoker the invoker object (passed through)
	 * @param session the receiving JMS Session
	 */
	protected void messageReceived(Object invoker, Session session) {
	}

	/**
	 * Template method that gets called when <i>no</i> message has been received,
	 * before returning to the receive loop again. Allows subclasses to react to
	 * the event of no incoming message, for example marking the invoker as idle.
	 * @param invoker the invoker object (passed through)
	 * @param session the receiving JMS Session
	 */
	protected void noMessageReceived(Object invoker, Session session) {
	}

	/**
	 * Fetch an appropriate Connection from the given JmsResourceHolder.
	 * <p>This implementation accepts any JMS 1.1 Connection.
	 * @param holder the JmsResourceHolder
	 * @return an appropriate Connection fetched from the holder,
	 * or {@code null} if none found
	 */
	@Nullable
	protected Connection getConnection(JmsResourceHolder holder) {
		return holder.getConnection();
	}

	/**
	 * Fetch an appropriate Session from the given JmsResourceHolder.
	 * <p>This implementation accepts any JMS 1.1 Session.
	 * @param holder the JmsResourceHolder
	 * @return an appropriate Session fetched from the holder,
	 * or {@code null} if none found
	 */
	@Nullable
	protected Session getSession(JmsResourceHolder holder) {
		return holder.getSession();
	}


	/**
	 * ResourceFactory implementation that delegates to this listener container's protected callback methods.
	 */
	private class MessageListenerContainerResourceFactory implements ConnectionFactoryUtils.ResourceFactory {

		@Override
		@Nullable
		public Connection getConnection(JmsResourceHolder holder) {
			return AbstractPollingMessageListenerContainer.this.getConnection(holder);
		}

		@Override
		@Nullable
		public Session getSession(JmsResourceHolder holder) {
			return AbstractPollingMessageListenerContainer.this.getSession(holder);
		}

		@Override
		public Connection createConnection() throws JMSException {
			if (AbstractPollingMessageListenerContainer.this.sharedConnectionEnabled()) {
				Connection sharedCon = AbstractPollingMessageListenerContainer.this.getSharedConnection();
				return new SingleConnectionFactory(sharedCon).createConnection();
			}
			else {
				return AbstractPollingMessageListenerContainer.this.createConnection();
			}
		}

		@Override
		public Session createSession(Connection con) throws JMSException {
			return AbstractPollingMessageListenerContainer.this.createSession(con);
		}

		@Override
		public boolean isSynchedLocalTransactionAllowed() {
			return AbstractPollingMessageListenerContainer.this.isSessionTransacted();
		}
	}

}
