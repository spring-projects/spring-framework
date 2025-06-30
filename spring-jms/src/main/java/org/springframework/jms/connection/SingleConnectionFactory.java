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

package org.springframework.jms.connection;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.ExceptionListener;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.QueueConnection;
import jakarta.jms.QueueConnectionFactory;
import jakarta.jms.Session;
import jakarta.jms.TopicConnection;
import jakarta.jms.TopicConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.Lifecycle;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * A JMS ConnectionFactory adapter that returns the same Connection
 * from all {@link #createConnection()} calls, and ignores calls to
 * {@link jakarta.jms.Connection#close()}. According to the JMS Connection
 * model, this is perfectly thread-safe (in contrast to, for example, JDBC). The
 * shared Connection can be automatically recovered in case of an Exception.
 *
 * <p>You can either pass in a specific JMS Connection directly or let this
 * factory lazily create a Connection via a given target ConnectionFactory.
 * This factory generally works with JMS 1.1 as well as the JMS 1.0.2 API.
 *
 * <p>Note that when using the JMS 1.0.2 API, this ConnectionFactory will switch
 * into queue/topic mode according to the JMS API methods used at runtime:
 * {@code createQueueConnection} and {@code createTopicConnection} will
 * lead to queue/topic mode, respectively; generic {@code createConnection}
 * calls will lead to a JMS 1.1 connection which is able to serve both modes.
 *
 * <p>As of Spring Framework 5, this class supports JMS 2.0 {@code JMSContext}
 * calls and therefore requires the JMS 2.0 API to be present at runtime.
 * It may nevertheless run against a JMS 1.1 driver (bound to the JMS 2.0 API)
 * as long as no actual JMS 2.0 calls are triggered by the application's setup.
 *
 * <p>Useful for testing and standalone environments in order to keep using the
 * same Connection for multiple {@link org.springframework.jms.core.JmsTemplate}
 * calls, without having a pooling ConnectionFactory underneath. This may span
 * any number of transactions, even concurrently executing transactions.
 *
 * <p>Note that Spring's message listener containers support the use of
 * a shared Connection within each listener container instance. Using
 * SingleConnectionFactory in combination only really makes sense for
 * sharing a single JMS Connection <i>across multiple listener containers</i>.
 *
 * @author Juergen Hoeller
 * @author Mark Pollack
 * @since 1.1
 * @see org.springframework.jms.core.JmsTemplate
 * @see org.springframework.jms.listener.SimpleMessageListenerContainer
 * @see org.springframework.jms.listener.DefaultMessageListenerContainer#setCacheLevel
 */
public class SingleConnectionFactory implements ConnectionFactory, QueueConnectionFactory,
		TopicConnectionFactory, ExceptionListener, InitializingBean, DisposableBean, Lifecycle {

	protected final Log logger = LogFactory.getLog(getClass());

	private @Nullable ConnectionFactory targetConnectionFactory;

	private @Nullable String clientId;

	private @Nullable ExceptionListener exceptionListener;

	private boolean reconnectOnException = false;

	/** The target Connection. */
	private @Nullable Connection connection;

	/** A hint whether to create a queue or topic connection. */
	private @Nullable Boolean pubSubMode;

	/** An internal aggregator allowing for per-connection ExceptionListeners. */
	private @Nullable AggregatedExceptionListener aggregatedExceptionListener;

	/** Whether the shared Connection has been started. */
	private int startedCount = 0;

	/** Lifecycle lock for the shared Connection. */
	private final Lock connectionLock = new ReentrantLock();


	/**
	 * Create a new SingleConnectionFactory for bean-style usage.
	 * @see #setTargetConnectionFactory
	 */
	public SingleConnectionFactory() {
	}

	/**
	 * Create a new SingleConnectionFactory that always returns the given Connection.
	 * @param targetConnection the single Connection
	 */
	public SingleConnectionFactory(Connection targetConnection) {
		Assert.notNull(targetConnection, "Target Connection must not be null");
		this.connection = targetConnection;
	}

	/**
	 * Create a new SingleConnectionFactory that always returns a single Connection
	 * that it will lazily create via the given target ConnectionFactory.
	 * @param targetConnectionFactory the target ConnectionFactory
	 */
	public SingleConnectionFactory(ConnectionFactory targetConnectionFactory) {
		Assert.notNull(targetConnectionFactory, "Target ConnectionFactory must not be null");
		this.targetConnectionFactory = targetConnectionFactory;
	}


	/**
	 * Set the target ConnectionFactory which will be used to lazily
	 * create a single Connection.
	 */
	public void setTargetConnectionFactory(@Nullable ConnectionFactory targetConnectionFactory) {
		this.targetConnectionFactory = targetConnectionFactory;
	}

	/**
	 * Return the target ConnectionFactory which will be used to lazily
	 * create a single Connection, if any.
	 */
	public @Nullable ConnectionFactory getTargetConnectionFactory() {
		return this.targetConnectionFactory;
	}

	/**
	 * Specify a JMS client ID for the single Connection created and exposed
	 * by this ConnectionFactory.
	 * <p>Note that client IDs need to be unique among all active Connections
	 * of the underlying JMS provider. Furthermore, a client ID can only be
	 * assigned if the original ConnectionFactory hasn't already assigned one.
	 * @see jakarta.jms.Connection#setClientID
	 * @see #setTargetConnectionFactory
	 */
	public void setClientId(@Nullable String clientId) {
		this.clientId = clientId;
	}

	/**
	 * Return a JMS client ID for the single Connection created and exposed
	 * by this ConnectionFactory, if any.
	 */
	protected @Nullable String getClientId() {
		return this.clientId;
	}

	/**
	 * Specify an JMS ExceptionListener implementation that should be
	 * registered with the single Connection created by this factory.
	 * @see #setReconnectOnException
	 */
	public void setExceptionListener(@Nullable ExceptionListener exceptionListener) {
		this.exceptionListener = exceptionListener;
	}

	/**
	 * Return the JMS ExceptionListener implementation that should be registered
	 * with the single Connection created by this factory, if any.
	 */
	protected @Nullable ExceptionListener getExceptionListener() {
		return this.exceptionListener;
	}

	/**
	 * Specify whether the single Connection should be reset (to be subsequently renewed)
	 * when a JMSException is reported by the underlying Connection.
	 * <p>Default is "false". Switch this to "true" to automatically trigger
	 * recovery based on your JMS provider's exception notifications.
	 * <p>Internally, this will lead to a special JMS ExceptionListener
	 * (this SingleConnectionFactory itself) being registered with the
	 * underlying Connection. This can also be combined with a
	 * user-specified ExceptionListener, if desired.
	 * @see #setExceptionListener
	 */
	public void setReconnectOnException(boolean reconnectOnException) {
		this.reconnectOnException = reconnectOnException;
	}

	/**
	 * Return whether the single Connection should be renewed when
	 * a JMSException is reported by the underlying Connection.
	 */
	protected boolean isReconnectOnException() {
		return this.reconnectOnException;
	}

	/**
	 * Make sure a Connection or ConnectionFactory has been set.
	 */
	@Override
	public void afterPropertiesSet() {
		if (this.connection == null && getTargetConnectionFactory() == null) {
			throw new IllegalArgumentException("Target Connection or ConnectionFactory is required");
		}
	}


	@Override
	public Connection createConnection() throws JMSException {
		return getSharedConnectionProxy(getConnection());
	}

	@Override
	public Connection createConnection(String username, String password) throws JMSException {
		throw new jakarta.jms.IllegalStateException(
				"SingleConnectionFactory does not support custom username and password");
	}

	@Override
	public QueueConnection createQueueConnection() throws JMSException {
		Connection con;
		this.connectionLock.lock();
		try {
			this.pubSubMode = Boolean.FALSE;
			con = createConnection();
		}
		finally {
			this.connectionLock.unlock();
		}
		if (!(con instanceof QueueConnection queueConnection)) {
			throw new jakarta.jms.IllegalStateException(
					"This SingleConnectionFactory does not hold a QueueConnection but rather: " + con);
		}
		return queueConnection;
	}

	@Override
	public QueueConnection createQueueConnection(String username, String password) throws JMSException {
		throw new jakarta.jms.IllegalStateException(
				"SingleConnectionFactory does not support custom username and password");
	}

	@Override
	public TopicConnection createTopicConnection() throws JMSException {
		Connection con;
		this.connectionLock.lock();
		try {
			this.pubSubMode = Boolean.TRUE;
			con = createConnection();
		}
		finally {
			this.connectionLock.unlock();
		}
		if (!(con instanceof TopicConnection topicConnection)) {
			throw new jakarta.jms.IllegalStateException(
					"This SingleConnectionFactory does not hold a TopicConnection but rather: " + con);
		}
		return topicConnection;
	}

	@Override
	public TopicConnection createTopicConnection(String username, String password) throws JMSException {
		throw new jakarta.jms.IllegalStateException(
				"SingleConnectionFactory does not support custom username and password");
	}

	@Override
	public JMSContext createContext() {
		return obtainTargetConnectionFactory().createContext();
	}

	@Override
	public JMSContext createContext(String userName, String password) {
		return obtainTargetConnectionFactory().createContext(userName, password);
	}

	@Override
	public JMSContext createContext(String userName, String password, int sessionMode) {
		return obtainTargetConnectionFactory().createContext(userName, password, sessionMode);
	}

	@Override
	public JMSContext createContext(int sessionMode) {
		return obtainTargetConnectionFactory().createContext(sessionMode);
	}

	private ConnectionFactory obtainTargetConnectionFactory() {
		ConnectionFactory target = getTargetConnectionFactory();
		Assert.state(target != null, "'targetConnectionFactory' is required");
		return target;
	}


	/**
	 * Obtain an initialized shared Connection.
	 * @return the Connection (never {@code null})
	 * @throws jakarta.jms.JMSException if thrown by JMS API methods
	 * @see #initConnection()
	 */
	@SuppressWarnings("NullAway") // Dataflow analysis limitation
	protected Connection getConnection() throws JMSException {
		this.connectionLock.lock();
		try {
			if (this.connection == null) {
				initConnection();
			}
			return this.connection;
		}
		finally {
			this.connectionLock.unlock();
		}
	}

	/**
	 * Exception listener callback that renews the underlying single Connection.
	 * @see #resetConnection()
	 */
	@Override
	public void onException(JMSException ex) {
		logger.info("Encountered a JMSException - resetting the underlying JMS Connection", ex);
		resetConnection();
	}

	/**
	 * Close the underlying shared connection.
	 * The provider of this ConnectionFactory needs to care for proper shutdown.
	 * <p>As this bean implements DisposableBean, a bean factory will
	 * automatically invoke this on destruction of its cached singletons.
	 * @see #resetConnection()
	 */
	@Override
	public void destroy() {
		resetConnection();
	}

	/**
	 * Initialize the underlying shared connection on start.
	 * @since 6.1
	 * @see #initConnection()
	 */
	@Override
	public void start() {
		try {
			initConnection();
		}
		catch (JMSException ex) {
			logger.info("Start attempt failed for shared JMS Connection", ex);
		}
	}

	/**
	 * Reset the underlying shared connection on stop.
	 * @since 6.1
	 * @see #resetConnection()
	 */
	@Override
	public void stop() {
		resetConnection();
	}

	/**
	 * Check whether there is currently an underlying connection.
	 * @since 6.1
	 * @see #start()
	 * @see #stop()
	 */
	@Override
	public boolean isRunning() {
		this.connectionLock.lock();
		try {
			return (this.connection != null);
		}
		finally {
			this.connectionLock.unlock();
		}
	}


	/**
	 * Initialize the underlying shared Connection.
	 * <p>Closes and reinitializes the Connection if an underlying
	 * Connection is present already.
	 * @throws jakarta.jms.JMSException if thrown by JMS API methods
	 * @see #prepareConnection
	 */
	public void initConnection() throws JMSException {
		if (getTargetConnectionFactory() == null) {
			throw new IllegalStateException(
					"'targetConnectionFactory' is required for lazily initializing a Connection");
		}
		this.connectionLock.lock();
		try {
			if (this.connection != null) {
				closeConnection(this.connection);
			}
			// Create new (method local) connection, which is later assigned to instance connection
			//  - prevention to hold instance connection without exception listener, in case when
			//    some subsequent methods (after creation of connection) throw JMSException
			Connection con = doCreateConnection();
			try {
				prepareConnection(con);
				this.connection = con;
			}
			catch (JMSException ex) {
				// Attempt to close new (not used) connection to release possible resources
				try {
					con.close();
				}
				catch(Throwable th) {
					logger.debug("Could not close newly obtained JMS Connection that failed to prepare", th);
				}
				throw ex;
			}
			if (this.startedCount > 0) {
				this.connection.start();
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Established shared JMS Connection: " + this.connection);
			}
		}
		finally {
			this.connectionLock.unlock();
		}
	}

	/**
	 * Create a JMS Connection via this template's ConnectionFactory.
	 * @return the new JMS Connection
	 * @throws jakarta.jms.JMSException if thrown by JMS API methods
	 */
	protected Connection doCreateConnection() throws JMSException {
		ConnectionFactory cf = getTargetConnectionFactory();
		if (Boolean.FALSE.equals(this.pubSubMode) && cf instanceof QueueConnectionFactory queueFactory) {
			return queueFactory.createQueueConnection();
		}
		else if (Boolean.TRUE.equals(this.pubSubMode) && cf instanceof TopicConnectionFactory topicFactory) {
			return topicFactory.createTopicConnection();
		}
		else {
			return obtainTargetConnectionFactory().createConnection();
		}
	}

	/**
	 * Prepare the given Connection before it is exposed.
	 * <p>The default implementation applies ExceptionListener and client id.
	 * Can be overridden in subclasses.
	 * @param con the Connection to prepare
	 * @throws JMSException if thrown by JMS API methods
	 * @see #setExceptionListener
	 * @see #setReconnectOnException
	 */
	protected void prepareConnection(Connection con) throws JMSException {
		if (getClientId() != null) {
			con.setClientID(getClientId());
		}
		if (this.aggregatedExceptionListener != null) {
			con.setExceptionListener(this.aggregatedExceptionListener);
		}
		else if (getExceptionListener() != null || isReconnectOnException()) {
			ExceptionListener listenerToUse = getExceptionListener();
			if (isReconnectOnException()) {
				this.aggregatedExceptionListener = new AggregatedExceptionListener();
				this.aggregatedExceptionListener.delegates.add(this);
				if (listenerToUse != null) {
					this.aggregatedExceptionListener.delegates.add(listenerToUse);
				}
				listenerToUse = this.aggregatedExceptionListener;
			}
			con.setExceptionListener(listenerToUse);
		}
	}

	/**
	 * Template method for obtaining a (potentially cached) Session.
	 * <p>The default implementation always returns {@code null}.
	 * Subclasses may override this for exposing specific Session handles,
	 * possibly delegating to {@link #createSession} for the creation of raw
	 * Session objects that will then get wrapped and returned from here.
	 * @param con the JMS Connection to operate on
	 * @param mode the Session acknowledgement mode
	 * ({@code Session.TRANSACTED} or one of the common modes)
	 * @return the Session to use, or {@code null} to indicate
	 * creation of a raw standard Session
	 * @throws JMSException if thrown by the JMS API
	 */
	protected @Nullable Session getSession(Connection con, Integer mode) throws JMSException {
		return null;
	}

	/**
	 * Create a default Session for this ConnectionFactory,
	 * adapting to JMS 1.0.2 style queue/topic mode if necessary.
	 * @param con the JMS Connection to operate on
	 * @param mode the Session acknowledgement mode
	 * ({@code Session.TRANSACTED} or one of the common modes)
	 * @return the newly created Session
	 * @throws JMSException if thrown by the JMS API
	 */
	protected Session createSession(Connection con, Integer mode) throws JMSException {
		// Determine JMS API arguments...
		boolean transacted = (mode == Session.SESSION_TRANSACTED);
		int ackMode = (transacted ? Session.AUTO_ACKNOWLEDGE : mode);
		// Now actually call the appropriate JMS factory method...
		if (Boolean.FALSE.equals(this.pubSubMode) && con instanceof QueueConnection queueConnection) {
			return queueConnection.createQueueSession(transacted, ackMode);
		}
		else if (Boolean.TRUE.equals(this.pubSubMode) && con instanceof TopicConnection topicConnection) {
			return topicConnection.createTopicSession(transacted, ackMode);
		}
		else {
			return con.createSession(transacted, ackMode);
		}
	}

	/**
	 * Reset the underlying shared Connection, to be reinitialized on next access.
	 * @see #closeConnection
	 */
	public void resetConnection() {
		this.connectionLock.lock();
		try {
			if (this.connection != null) {
				closeConnection(this.connection);
			}
			this.connection = null;
		}
		finally {
			this.connectionLock.unlock();
		}
	}

	/**
	 * Close the given Connection.
	 * @param con the Connection to close
	 */
	protected void closeConnection(Connection con) {
		if (logger.isDebugEnabled()) {
			logger.debug("Closing shared JMS Connection: " + con);
		}
		try {
			try (con) {
				if (this.startedCount > 0) {
					con.stop();
				}
			}
		}
		catch (jakarta.jms.IllegalStateException ex) {
			logger.debug("Ignoring Connection state exception - assuming already closed: " + ex);
		}
		catch (Throwable ex) {
			logger.warn("Could not close shared JMS Connection", ex);
		}
	}

	/**
	 * Wrap the given Connection with a proxy that delegates every method call to it
	 * but suppresses close calls. This is useful for allowing application code to
	 * handle a special framework Connection just like an ordinary Connection from a
	 * JMS ConnectionFactory.
	 * @param target the original Connection to wrap
	 * @return the wrapped Connection
	 */
	protected Connection getSharedConnectionProxy(Connection target) {
		List<Class<?>> classes = new ArrayList<>(3);
		classes.add(Connection.class);
		if (target instanceof QueueConnection) {
			classes.add(QueueConnection.class);
		}
		if (target instanceof TopicConnection) {
			classes.add(TopicConnection.class);
		}
		return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
				ClassUtils.toClassArray(classes), new SharedConnectionInvocationHandler());
	}


	/**
	 * Invocation handler for a cached JMS Connection proxy.
	 */
	private class SharedConnectionInvocationHandler implements InvocationHandler {

		private @Nullable ExceptionListener localExceptionListener;

		private boolean locallyStarted = false;

		@Override
		public @Nullable Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			switch (method.getName()) {
				case "equals" -> {
					Object other = args[0];
					if (proxy == other) {
						return true;
					}
					if (other == null || !Proxy.isProxyClass(other.getClass())) {
						return false;
					}
					InvocationHandler otherHandler = Proxy.getInvocationHandler(other);
					return (otherHandler instanceof SharedConnectionInvocationHandler sharedHandler &&
							factory() == sharedHandler.factory());
				}
				case "hashCode" -> {
					// Use hashCode of containing SingleConnectionFactory.
					return System.identityHashCode(factory());
				}
				case "toString" -> {
					return "Shared JMS Connection: " + getConnection();
				}
				case "setClientID" -> {
					// Handle setClientID method: throw exception if not compatible.
					String currentClientId = getConnection().getClientID();
					if (currentClientId != null && currentClientId.equals(args[0])) {
						return null;
					}
					else {
						throw new jakarta.jms.IllegalStateException(
								"setClientID call not supported on proxy for shared Connection. " +
								"Set the 'clientId' property on the SingleConnectionFactory instead.");
					}
				}
				case "setExceptionListener" -> {
					// Handle setExceptionListener method: add to the chain.
					connectionLock.lock();
					try {
						if (aggregatedExceptionListener != null) {
							ExceptionListener listener = (ExceptionListener) args[0];
							if (listener != this.localExceptionListener) {
								if (this.localExceptionListener != null) {
									aggregatedExceptionListener.delegates.remove(this.localExceptionListener);
								}
								if (listener != null) {
									aggregatedExceptionListener.delegates.add(listener);
								}
								this.localExceptionListener = listener;
							}
							return null;
						}
						else {
							throw new jakarta.jms.IllegalStateException(
									"setExceptionListener call not supported on proxy for shared Connection. " +
									"Set the 'exceptionListener' property on the SingleConnectionFactory instead. " +
									"Alternatively, activate SingleConnectionFactory's 'reconnectOnException' feature, " +
									"which will allow for registering further ExceptionListeners to the recovery chain.");
						}
					}
					finally {
						connectionLock.unlock();
					}
				}
				case "getExceptionListener" -> {
					connectionLock.lock();
					try {
						if (this.localExceptionListener != null) {
							return this.localExceptionListener;
						}
						else {
							return getExceptionListener();
						}
					}
					finally {
						connectionLock.unlock();
					}
				}
				case "start" -> {
					localStart();
					return null;
				}
				case "stop" -> {
					localStop();
					return null;
				}
				case "close" -> {
					localStop();
					connectionLock.lock();
					try {
						if (this.localExceptionListener != null) {
							if (aggregatedExceptionListener != null) {
								aggregatedExceptionListener.delegates.remove(this.localExceptionListener);
							}
							this.localExceptionListener = null;
						}
					}
					finally {
						connectionLock.unlock();
					}
					return null;
				}
				case "createSession", "createQueueSession", "createTopicSession" -> {
					// Default: JMS 2.0 createSession() method
					Integer mode = Session.AUTO_ACKNOWLEDGE;
					if (!ObjectUtils.isEmpty(args)) {
						if (args.length == 1) {
							// JMS 2.0 createSession(int) method
							mode = (Integer) args[0];
						}
						else if (args.length == 2) {
							// JMS 1.1 createSession(boolean, int) method
							boolean transacted = (Boolean) args[0];
							Integer ackMode = (Integer) args[1];
							mode = (transacted ? Session.SESSION_TRANSACTED : ackMode);
						}
					}
					Session session = getSession(getConnection(), mode);
					if (session != null) {
						if (!method.getReturnType().isInstance(session)) {
							String msg = "JMS Session does not implement specific domain: " + session;
							try {
								session.close();
							}
							catch (Throwable ex) {
								logger.trace("Failed to close newly obtained JMS Session", ex);
							}
							throw new jakarta.jms.IllegalStateException(msg);
						}
						return session;
					}
				}
			}
			try {
				return method.invoke(getConnection(), args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}

		private void localStart() throws JMSException {
			connectionLock.lock();
			try {
				if (!this.locallyStarted) {
					this.locallyStarted = true;
					if (startedCount == 0 && connection != null) {
						connection.start();
					}
					startedCount++;
				}
			}
			finally {
				connectionLock.unlock();
			}
		}

		private void localStop() throws JMSException {
			connectionLock.lock();
			try {
				if (this.locallyStarted) {
					this.locallyStarted = false;
					if (startedCount == 1 && connection != null) {
						connection.stop();
					}
					if (startedCount > 0) {
						startedCount--;
					}
				}
			}
			finally {
				connectionLock.unlock();
			}
		}

		private SingleConnectionFactory factory() {
			return SingleConnectionFactory.this;
		}
	}


	/**
	 * Internal aggregated ExceptionListener for handling the internal
	 * recovery listener in combination with user-specified listeners.
	 */
	private class AggregatedExceptionListener implements ExceptionListener {

		final Set<ExceptionListener> delegates = new LinkedHashSet<>(2);

		@Override
		public void onException(JMSException ex) {
			// Iterate over temporary copy in order to avoid ConcurrentModificationException,
			// since listener invocations may in turn trigger registration of listeners...
			Set<ExceptionListener> copy;
			connectionLock.lock();
			try {
				copy = new LinkedHashSet<>(this.delegates);
			}
			finally {
				connectionLock.unlock();
			}
			for (ExceptionListener listener : copy) {
				listener.onException(ex);
			}
		}
	}

}
