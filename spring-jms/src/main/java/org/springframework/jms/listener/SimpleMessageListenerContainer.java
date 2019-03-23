/*
 * Copyright 2002-2015 the original author or authors.
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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;

import org.springframework.jms.support.JmsUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * Message listener container that uses the plain JMS client API's
 * {@code MessageConsumer.setMessageListener()} method to
 * create concurrent MessageConsumers for the specified listeners.
 *
 * <p>This is the simplest form of a message listener container.
 * It creates a fixed number of JMS Sessions to invoke the listener,
 * not allowing for dynamic adaptation to runtime demands. Its main
 * advantage is its low level of complexity and the minimum requirements
 * on the JMS provider: Not even the ServerSessionPool facility is required.
 *
 * <p>See the {@link AbstractMessageListenerContainer} javadoc for details
 * on acknowledge modes and transaction options. Note that this container
 * exposes standard JMS behavior for the default "AUTO_ACKNOWLEDGE" mode:
 * that is, automatic message acknowledgment after listener execution,
 * with no redelivery in case of a user exception thrown but potential
 * redelivery in case of the JVM dying during listener execution.
 *
 * <p>For a different style of MessageListener handling, through looped
 * {@code MessageConsumer.receive()} calls that also allow for
 * transactional reception of messages (registering them with XA transactions),
 * see {@link DefaultMessageListenerContainer}.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see javax.jms.MessageConsumer#setMessageListener
 * @see DefaultMessageListenerContainer
 * @see org.springframework.jms.listener.endpoint.JmsMessageEndpointManager
 */
public class SimpleMessageListenerContainer extends AbstractMessageListenerContainer implements ExceptionListener {

	private boolean connectLazily = false;

	private int concurrentConsumers = 1;

	private Executor taskExecutor;

	private Set<Session> sessions;

	private Set<MessageConsumer> consumers;

	private final Object consumersMonitor = new Object();


	/**
	 * Specify whether to connect lazily, i.e. whether to establish the JMS Connection
	 * and the corresponding Sessions and MessageConsumers as late as possible -
	 * in the start phase of this container.
	 * <p>Default is "false": connecting early, i.e. during the bean initialization phase.
	 * Set this flag to "true" in order to switch to lazy connecting if your target broker
	 * is likely to not have started up yet and you prefer to not even try a connection.
	 * @see #start()
	 * @see #initialize()
	 */
	public void setConnectLazily(boolean connectLazily) {
		this.connectLazily = connectLazily;
	}

	/**
	 * Specify concurrency limits via a "lower-upper" String, e.g. "5-10", or a simple
	 * upper limit String, e.g. "10".
	 * <p>This listener container will always hold on to the maximum number of
	 * consumers {@link #setConcurrentConsumers} since it is unable to scale.
	 * <p>This property is primarily supported for configuration compatibility with
	 * {@link DefaultMessageListenerContainer}. For this local listener container,
	 * generally use {@link #setConcurrentConsumers} instead.
	 */
	@Override
	public void setConcurrency(String concurrency) {
		try {
			int separatorIndex = concurrency.indexOf('-');
			if (separatorIndex != -1) {
				setConcurrentConsumers(Integer.parseInt(concurrency.substring(separatorIndex + 1, concurrency.length())));
			}
			else {
				setConcurrentConsumers(Integer.parseInt(concurrency));
			}
		}
		catch (NumberFormatException ex) {
			throw new IllegalArgumentException("Invalid concurrency value [" + concurrency + "]: only " +
					"single maximum integer (e.g. \"5\") and minimum-maximum combo (e.g. \"3-5\") supported. " +
					"Note that SimpleMessageListenerContainer will effectively ignore the minimum value and " +
					"always keep a fixed number of consumers according to the maximum value.");
		}
	}

	/**
	 * Specify the number of concurrent consumers to create. Default is 1.
	 * <p>Raising the number of concurrent consumers is recommendable in order
	 * to scale the consumption of messages coming in from a queue. However,
	 * note that any ordering guarantees are lost once multiple consumers are
	 * registered. In general, stick with 1 consumer for low-volume queues.
	 * <p><b>Do not raise the number of concurrent consumers for a topic.</b>
	 * This would lead to concurrent consumption of the same message,
	 * which is hardly ever desirable.
	 */
	public void setConcurrentConsumers(int concurrentConsumers) {
		Assert.isTrue(concurrentConsumers > 0, "'concurrentConsumers' value must be at least 1 (one)");
		this.concurrentConsumers = concurrentConsumers;
	}

	/**
	 * Set the Spring TaskExecutor to use for executing the listener once
	 * a message has been received by the provider.
	 * <p>Default is none, that is, to run in the JMS provider's own receive thread,
	 * blocking the provider's receive endpoint while executing the listener.
	 * <p>Specify a TaskExecutor for executing the listener in a different thread,
	 * rather than blocking the JMS provider, usually integrating with an existing
	 * thread pool. This allows to keep the number of concurrent consumers low (1)
	 * while still processing messages concurrently (decoupled from receiving!).
	 * <p><b>NOTE: Specifying a TaskExecutor for listener execution affects
	 * acknowledgement semantics.</b> Messages will then always get acknowledged
	 * before listener execution, with the underlying Session immediately reused
	 * for receiving the next message. Using this in combination with a transacted
	 * session or with client acknowledgement will lead to unspecified results!
	 * <p><b>NOTE: Concurrent listener execution via a TaskExecutor will lead
	 * to concurrent processing of messages that have been received by the same
	 * underlying Session.</b> As a consequence, it is not recommended to use
	 * this setting with a {@link SessionAwareMessageListener}, at least not
	 * if the latter performs actual work on the given Session. A standard
	 * {@link javax.jms.MessageListener} will work fine, in general.
	 * @see #setConcurrentConsumers
	 * @see org.springframework.core.task.SimpleAsyncTaskExecutor
	 * @see org.springframework.scheduling.commonj.WorkManagerTaskExecutor
	 */
	public void setTaskExecutor(Executor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	@Override
	protected void validateConfiguration() {
		super.validateConfiguration();
		if (isSubscriptionDurable() && this.concurrentConsumers != 1) {
			throw new IllegalArgumentException("Only 1 concurrent consumer supported for durable subscription");
		}
	}


	//-------------------------------------------------------------------------
	// Implementation of AbstractMessageListenerContainer's template methods
	//-------------------------------------------------------------------------

	/**
	 * Always use a shared JMS Connection.
	 */
	@Override
	protected final boolean sharedConnectionEnabled() {
		return true;
	}

	/**
	 * Creates the specified number of concurrent consumers,
	 * in the form of a JMS Session plus associated MessageConsumer.
	 * @see #createListenerConsumer
	 */
	@Override
	protected void doInitialize() throws JMSException {
		if (!this.connectLazily) {
			try {
				establishSharedConnection();
			}
			catch (JMSException ex) {
				logger.debug("Could not connect on initialization - registering message consumers lazily", ex);
				return;
			}
			initializeConsumers();
		}
	}

	/**
	 * Re-initializes this container's JMS message consumers,
	 * if not initialized already.
	 */
	@Override
	protected void doStart() throws JMSException {
		super.doStart();
		initializeConsumers();
	}

	/**
	 * Registers this listener container as JMS ExceptionListener on the shared connection.
	 */
	@Override
	protected void prepareSharedConnection(Connection connection) throws JMSException {
		super.prepareSharedConnection(connection);
		connection.setExceptionListener(this);
	}

	/**
	 * JMS ExceptionListener implementation, invoked by the JMS provider in
	 * case of connection failures. Re-initializes this listener container's
	 * shared connection and its sessions and consumers.
	 * @param ex the reported connection exception
	 */
	@Override
	public void onException(JMSException ex) {
		// First invoke the user-specific ExceptionListener, if any.
		invokeExceptionListener(ex);

		// Now try to recover the shared Connection and all consumers...
		if (logger.isInfoEnabled()) {
			logger.info("Trying to recover from JMS Connection exception: " + ex);
		}
		try {
			synchronized (this.consumersMonitor) {
				this.sessions = null;
				this.consumers = null;
			}
			refreshSharedConnection();
			initializeConsumers();
			logger.info("Successfully refreshed JMS Connection");
		}
		catch (JMSException recoverEx) {
			logger.debug("Failed to recover JMS Connection", recoverEx);
			logger.error("Encountered non-recoverable JMSException", ex);
		}
	}

	/**
	 * Initialize the JMS Sessions and MessageConsumers for this container.
	 * @throws JMSException in case of setup failure
	 */
	protected void initializeConsumers() throws JMSException {
		// Register Sessions and MessageConsumers.
		synchronized (this.consumersMonitor) {
			if (this.consumers == null) {
				this.sessions = new HashSet<Session>(this.concurrentConsumers);
				this.consumers = new HashSet<MessageConsumer>(this.concurrentConsumers);
				Connection con = getSharedConnection();
				for (int i = 0; i < this.concurrentConsumers; i++) {
					Session session = createSession(con);
					MessageConsumer consumer = createListenerConsumer(session);
					this.sessions.add(session);
					this.consumers.add(consumer);
				}
			}
		}
	}

	/**
	 * Create a MessageConsumer for the given JMS Session,
	 * registering a MessageListener for the specified listener.
	 * @param session the JMS Session to work on
	 * @return the MessageConsumer
	 * @throws JMSException if thrown by JMS methods
	 * @see #executeListener
	 */
	protected MessageConsumer createListenerConsumer(final Session session) throws JMSException {
		Destination destination = getDestination();
		if (destination == null) {
			destination = resolveDestinationName(session, getDestinationName());
		}
		MessageConsumer consumer = createConsumer(session, destination);

		if (this.taskExecutor != null) {
			consumer.setMessageListener(new MessageListener() {
				@Override
				public void onMessage(final Message message) {
					taskExecutor.execute(new Runnable() {
						@Override
						public void run() {
							processMessage(message, session);
						}
					});
				}
			});
		}
		else {
			consumer.setMessageListener(new MessageListener() {
				@Override
				public void onMessage(Message message) {
					processMessage(message, session);
				}
			});
		}

		return consumer;
	}

	/**
	 * Process a message received from the provider.
	 * <p>Executes the listener, exposing the current JMS Session as
	 * thread-bound resource (if "exposeListenerSession" is "true").
	 * @param message the received JMS Message
	 * @param session the JMS Session to operate on
	 * @see #executeListener
	 * @see #setExposeListenerSession
	 */
	protected void processMessage(Message message, Session session) {
		boolean exposeResource = isExposeListenerSession();
		if (exposeResource) {
			TransactionSynchronizationManager.bindResource(
					getConnectionFactory(), new LocallyExposedJmsResourceHolder(session));
		}
		try {
			executeListener(session, message);
		}
		finally {
			if (exposeResource) {
				TransactionSynchronizationManager.unbindResource(getConnectionFactory());
			}
		}
	}

	/**
	 * Destroy the registered JMS Sessions and associated MessageConsumers.
	 */
	@Override
	protected void doShutdown() throws JMSException {
		synchronized (this.consumersMonitor) {
			if (this.consumers != null) {
				logger.debug("Closing JMS MessageConsumers");
				for (MessageConsumer consumer : this.consumers) {
					JmsUtils.closeMessageConsumer(consumer);
				}
				logger.debug("Closing JMS Sessions");
				for (Session session : this.sessions) {
					JmsUtils.closeSession(session);
				}
			}
		}
	}

}
