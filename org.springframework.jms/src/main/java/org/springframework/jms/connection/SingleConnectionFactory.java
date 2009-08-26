/*
 * Copyright 2002-2009 the original author or authors.
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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.Session;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * A JMS ConnectionFactory adapter that returns the same Connection
 * from all {@link #createConnection()} calls, and ignores calls to
 * {@link javax.jms.Connection#close()}. According to the JMS Connection
 * model, this is perfectly thread-safe (in contrast to e.g. JDBC). The
 * shared Connection can be automatically recovered in case of an Exception.
 *
 * <p>You can either pass in a specific JMS Connection directly or let this
 * factory lazily create a Connection via a given target ConnectionFactory.
 * This factory generally works with JMS 1.1 as well as the JMS 1.0.2 API.
 *
 * <p>Note that when using the JMS 1.0.2 API, this ConnectionFactory will switch
 * into queue/topic mode according to the JMS API methods used at runtime:
 * <code>createQueueConnection</code> and <code>createTopicConnection</code> will
 * lead to queue/topic mode, respectively; generic <code>createConnection</code>
 * calls will lead to a JMS 1.1 connection which is able to serve both modes.
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
public class SingleConnectionFactory
		implements ConnectionFactory, QueueConnectionFactory, TopicConnectionFactory, ExceptionListener,
		InitializingBean, DisposableBean {

	protected final Log logger = LogFactory.getLog(getClass());

	private ConnectionFactory targetConnectionFactory;

	private String clientId;

	private ExceptionListener exceptionListener;

	private boolean reconnectOnException = false;

	/** Wrapped Connection */
	private Connection target;

	/** Proxy Connection */
	private Connection connection;

	/** A hint whether to create a queue or topic connection */
	private Boolean pubSubMode;

	/** Whether the shared Connection has been started */
	private boolean started = false;

	/** Synchronization monitor for the shared Connection */
	private final Object connectionMonitor = new Object();


	/**
	 * Create a new SingleConnectionFactory for bean-style usage.
	 * @see #setTargetConnectionFactory
	 */
	public SingleConnectionFactory() {
	}

	/**
	 * Create a new SingleConnectionFactory that always returns the given Connection.
	 * @param target the single Connection
	 */
	public SingleConnectionFactory(Connection target) {
		Assert.notNull(target, "Target Connection must not be null");
		this.target = target;
		this.connection = getSharedConnectionProxy(target);
	}

	/**
	 * Create a new SingleConnectionFactory that always returns a single
	 * Connection that it will lazily create via the given target
	 * ConnectionFactory.
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
	public void setTargetConnectionFactory(ConnectionFactory targetConnectionFactory) {
		this.targetConnectionFactory = targetConnectionFactory;
	}

	/**
	 * Return the target ConnectionFactory which will be used to lazily
	 * create a single Connection, if any.
	 */
	public ConnectionFactory getTargetConnectionFactory() {
		return this.targetConnectionFactory;
	}

	/**
	 * Specify a JMS client ID for the single Connection created and exposed
	 * by this ConnectionFactory.
	 * <p>Note that client IDs need to be unique among all active Connections
	 * of the underlying JMS provider. Furthermore, a client ID can only be
	 * assigned if the original ConnectionFactory hasn't already assigned one.
	 * @see javax.jms.Connection#setClientID
	 * @see #setTargetConnectionFactory
	 */
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	/**
	 * Return a JMS client ID for the single Connection created and exposed
	 * by this ConnectionFactory, if any.
	 */
	protected String getClientId() {
		return this.clientId;
	}

	/**
	 * Specify an JMS ExceptionListener implementation that should be
	 * registered with with the single Connection created by this factory.
	 * @see #setReconnectOnException
	 */
	public void setExceptionListener(ExceptionListener exceptionListener) {
		this.exceptionListener = exceptionListener;
	}

	/**
	 * Return the JMS ExceptionListener implementation that should be registered
	 * with with the single Connection created by this factory, if any.
	 */
	protected ExceptionListener getExceptionListener() {
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
	public void afterPropertiesSet() {
		if (this.connection == null && getTargetConnectionFactory() == null) {
			throw new IllegalArgumentException("Connection or 'targetConnectionFactory' is required");
		}
	}


	public Connection createConnection() throws JMSException {
		synchronized (this.connectionMonitor) {
			if (this.connection == null) {
				initConnection();
			}
			return this.connection;
		}
	}

	public Connection createConnection(String username, String password) throws JMSException {
		throw new javax.jms.IllegalStateException(
				"SingleConnectionFactory does not support custom username and password");
	}

	public QueueConnection createQueueConnection() throws JMSException {
		Connection con;
		synchronized (this.connectionMonitor) {
			this.pubSubMode = Boolean.FALSE;
			con = createConnection();
		}
		if (!(con instanceof QueueConnection)) {
			throw new javax.jms.IllegalStateException(
					"This SingleConnectionFactory does not hold a QueueConnection but rather: " + con);
		}
		return ((QueueConnection) con);
	}

	public QueueConnection createQueueConnection(String username, String password) throws JMSException {
		throw new javax.jms.IllegalStateException(
				"SingleConnectionFactory does not support custom username and password");
	}

	public TopicConnection createTopicConnection() throws JMSException {
		Connection con;
		synchronized (this.connectionMonitor) {
			this.pubSubMode = Boolean.TRUE;
			con = createConnection();
		}
		if (!(con instanceof TopicConnection)) {
			throw new javax.jms.IllegalStateException(
					"This SingleConnectionFactory does not hold a TopicConnection but rather: " + con);
		}
		return ((TopicConnection) con);
	}

	public TopicConnection createTopicConnection(String username, String password) throws JMSException {
		throw new javax.jms.IllegalStateException(
				"SingleConnectionFactory does not support custom username and password");
	}


	/**
	 * Initialize the underlying shared Connection.
	 * <p>Closes and reinitializes the Connection if an underlying
	 * Connection is present already.
	 * @throws javax.jms.JMSException if thrown by JMS API methods
	 */
	public void initConnection() throws JMSException {
		if (getTargetConnectionFactory() == null) {
			throw new IllegalStateException(
					"'targetConnectionFactory' is required for lazily initializing a Connection");
		}
		synchronized (this.connectionMonitor) {
			if (this.target != null) {
				closeConnection(this.target);
			}
			this.target = doCreateConnection();
			prepareConnection(this.target);
			if (logger.isInfoEnabled()) {
				logger.info("Established shared JMS Connection: " + this.target);
			}
			this.connection = getSharedConnectionProxy(this.target);
		}
	}

	/**
	 * Exception listener callback that renews the underlying single Connection.
	 */
	public void onException(JMSException ex) {
		logger.warn("Encountered a JMSException - resetting the underlying JMS Connection", ex);
		resetConnection();
	}

	/**
	 * Close the underlying shared connection.
	 * The provider of this ConnectionFactory needs to care for proper shutdown.
	 * <p>As this bean implements DisposableBean, a bean factory will
	 * automatically invoke this on destruction of its cached singletons.
	 */
	public void destroy() {
		resetConnection();
	}

	/**
	 * Reset the underlying shared Connection, to be reinitialized on next access.
	 */
	public void resetConnection() {
		synchronized (this.connectionMonitor) {
			if (this.target != null) {
				closeConnection(this.target);
			}
			this.target = null;
			this.connection = null;
		}
	}

	/**
	 * Create a JMS Connection via this template's ConnectionFactory.
	 * @return the new JMS Connection
	 * @throws javax.jms.JMSException if thrown by JMS API methods
	 */
	protected Connection doCreateConnection() throws JMSException {
		ConnectionFactory cf = getTargetConnectionFactory();
		if (Boolean.FALSE.equals(this.pubSubMode) && cf instanceof QueueConnectionFactory) {
			return ((QueueConnectionFactory) cf).createQueueConnection();
		}
		else if (Boolean.TRUE.equals(this.pubSubMode) && cf instanceof TopicConnectionFactory) {
			return ((TopicConnectionFactory) cf).createTopicConnection();
		}
		else {
			return getTargetConnectionFactory().createConnection();
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
		if (getExceptionListener() != null || isReconnectOnException()) {
			ExceptionListener listenerToUse = getExceptionListener();
			if (isReconnectOnException()) {
				listenerToUse = new InternalChainedExceptionListener(this, listenerToUse);
			}
			con.setExceptionListener(listenerToUse);
		}
	}

	/**
	 * Template method for obtaining a (potentially cached) Session.
	 * <p>The default implementation always returns <code>null</code>.
	 * Subclasses may override this for exposing specific Session handles,
	 * possibly delegating to {@link #createSession} for the creation of raw
	 * Session objects that will then get wrapped and returned from here.
	 * @param con the JMS Connection to operate on
	 * @param mode the Session acknowledgement mode
	 * (<code>Session.TRANSACTED</code> or one of the common modes)
	 * @return the Session to use, or <code>null</code> to indicate
	 * creation of a raw standard Session
	 * @throws JMSException if thrown by the JMS API
	 */
	protected Session getSession(Connection con, Integer mode) throws JMSException {
		return null;
	}

	/**
	 * Create a default Session for this ConnectionFactory,
	 * adaptign to JMS 1.0.2 style queue/topic mode if necessary.
	 * @param con the JMS Connection to operate on
	 * @param mode the Session acknowledgement mode
	 * (<code>Session.TRANSACTED</code> or one of the common modes)
	 * @return the newly created Session
	 * @throws JMSException if thrown by the JMS API
	 */
	protected Session createSession(Connection con, Integer mode) throws JMSException {
		// Determine JMS API arguments...
		boolean transacted = (mode == Session.SESSION_TRANSACTED);
		int ackMode = (transacted ? Session.AUTO_ACKNOWLEDGE : mode);
		// Now actually call the appropriate JMS factory method...
		if (Boolean.FALSE.equals(this.pubSubMode) && con instanceof QueueConnection) {
			return ((QueueConnection) con).createQueueSession(transacted, ackMode);
		}
		else if (Boolean.TRUE.equals(this.pubSubMode) && con instanceof TopicConnection) {
			return ((TopicConnection) con).createTopicSession(transacted, ackMode);
		}
		else {
			return con.createSession(transacted, ackMode);
		}
	}

	/**
	 * Close the given Connection.
	 * @param con the Connection to close
	 */
	protected void closeConnection(Connection con) {
		if (logger.isDebugEnabled()) {
			logger.debug("Closing shared JMS Connection: " + this.target);
		}
		try {
			try {
				if (this.started) {
					this.started = false;
					con.stop();
				}
			}
			finally {
				con.close();
			}
		}
		catch (javax.jms.IllegalStateException ex) {
			logger.debug("Ignoring Connection state exception - assuming already closed: " + ex);
		}
		catch (Throwable ex) {
			logger.debug("Could not close shared JMS Connection", ex);
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
		List<Class> classes = new ArrayList<Class>(3);
		classes.add(Connection.class);
		if (target instanceof QueueConnection) {
			classes.add(QueueConnection.class);
		}
		if (target instanceof TopicConnection) {
			classes.add(TopicConnection.class);
		}
		return (Connection) Proxy.newProxyInstance(
				Connection.class.getClassLoader(),
				classes.toArray(new Class[classes.size()]),
				new SharedConnectionInvocationHandler(target));
	}


	/**
	 * Invocation handler for a cached JMS Connection proxy.
	 */
	private class SharedConnectionInvocationHandler implements InvocationHandler {

		private final Connection target;

		public SharedConnectionInvocationHandler(Connection target) {
			this.target = target;
		}

		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (method.getName().equals("equals")) {
				// Only consider equal when proxies are identical.
				return (proxy == args[0]);
			}
			else if (method.getName().equals("hashCode")) {
				// Use hashCode of Connection proxy.
				return System.identityHashCode(proxy);
			}
			else if (method.getName().equals("toString")) {
				return "Shared JMS Connection: " + this.target;
			}
			else if (method.getName().equals("setClientID")) {
				// Handle setClientID method: throw exception if not compatible.
				String currentClientId = this.target.getClientID();
				if (currentClientId != null && currentClientId.equals(args[0])) {
					return null;
				}
				else {
					throw new javax.jms.IllegalStateException(
							"setClientID call not supported on proxy for shared Connection. " +
							"Set the 'clientId' property on the SingleConnectionFactory instead.");
				}
			}
			else if (method.getName().equals("setExceptionListener")) {
				// Handle setExceptionListener method: add to the chain.
				ExceptionListener currentExceptionListener = this.target.getExceptionListener();
				if (currentExceptionListener instanceof InternalChainedExceptionListener && args[0] != null) {
					((InternalChainedExceptionListener) currentExceptionListener).addDelegate((ExceptionListener) args[0]);
					return null;
				}
				else {
					throw new javax.jms.IllegalStateException(
							"setExceptionListener call not supported on proxy for shared Connection. " +
							"Set the 'exceptionListener' property on the SingleConnectionFactory instead. " +
							"Alternatively, activate SingleConnectionFactory's 'reconnectOnException' feature, " +
							"which will allow for registering further ExceptionListeners to the recovery chain.");
				}
			}
			else if (method.getName().equals("start")) {
				// Handle start method: track started state.
				synchronized (connectionMonitor) {
					if (!started) {
						this.target.start();
						started = true;
					}
				}
				return null;
			}
			else if (method.getName().equals("stop")) {
				// Handle stop method: don't pass the call on.
				return null;
			}
			else if (method.getName().equals("close")) {
				// Handle close method: don't pass the call on.
				return null;
			}
			else if (method.getName().equals("createSession") || method.getName().equals("createQueueSession") ||
					method.getName().equals("createTopicSession")) {
				boolean transacted = (Boolean) args[0];
				Integer ackMode = (Integer) args[1];
				Integer mode = (transacted ? Session.SESSION_TRANSACTED : ackMode);
				Session session = getSession(this.target, mode);
				if (session != null) {
					if (!method.getReturnType().isInstance(session)) {
						String msg = "JMS Session does not implement specific domain: " + session;
						try {
							session.close();
						}
						catch (Throwable ex) {
							logger.trace("Failed to close newly obtained JMS Session", ex);
						}
						throw new javax.jms.IllegalStateException(msg);
					}
					return session;
				}
			}
			try {
				Object retVal = method.invoke(this.target, args);
				if (method.getName().equals("getExceptionListener") && retVal instanceof InternalChainedExceptionListener) {
					// Handle getExceptionListener method: hide internal chain.
					InternalChainedExceptionListener listener = (InternalChainedExceptionListener) retVal;
					return listener.getUserListener();
				}
				else {
					return retVal;
				}
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}
	}


	/**
	 * Internal chained ExceptionListener for handling the internal recovery listener
	 * in combination with a user-specified listener.
	 */
	private static class InternalChainedExceptionListener extends ChainedExceptionListener {

		private ExceptionListener userListener;

		public InternalChainedExceptionListener(ExceptionListener internalListener, ExceptionListener userListener) {
			addDelegate(internalListener);
			if (userListener != null) {
				addDelegate(userListener);
				this.userListener = userListener;
			}
		}

		public ExceptionListener getUserListener() {
			return this.userListener;
		}
	}

}
