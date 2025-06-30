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
import java.util.List;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.QueueConnection;
import jakarta.jms.QueueConnectionFactory;
import jakarta.jms.QueueSession;
import jakarta.jms.Session;
import jakarta.jms.TopicConnection;
import jakarta.jms.TopicConnectionFactory;
import jakarta.jms.TopicSession;
import jakarta.jms.TransactionInProgressException;
import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Proxy for a target JMS {@link jakarta.jms.ConnectionFactory}, adding awareness of
 * Spring-managed transactions. Similar to a transactional JNDI ConnectionFactory
 * as provided by a Jakarta EE application server.
 *
 * <p>Messaging code which should remain unaware of Spring's JMS support can work with
 * this proxy to seamlessly participate in Spring-managed transactions. Note that the
 * transaction manager, for example {@link JmsTransactionManager}, still needs to work
 * with the underlying ConnectionFactory, <i>not</i> with this proxy.
 *
 * <p><b>Make sure that TransactionAwareConnectionFactoryProxy is the outermost
 * ConnectionFactory of a chain of ConnectionFactory proxies/adapters.</b>
 * TransactionAwareConnectionFactoryProxy can delegate either directly to the
 * target factory or to some intermediary adapter like
 * {@link UserCredentialsConnectionFactoryAdapter}.
 *
 * <p>Delegates to {@link ConnectionFactoryUtils} for automatically participating
 * in thread-bound transactions, for example managed by {@link JmsTransactionManager}.
 * {@code createSession} calls and {@code close} calls on returned Sessions
 * will behave properly within a transaction, that is, always work on the transactional
 * Session. If not within a transaction, normal ConnectionFactory behavior applies.
 *
 * <p>Note that transactional JMS Sessions will be registered on a per-Connection
 * basis. To share the same JMS Session across a transaction, make sure that you
 * operate on the same JMS Connection handle - either through reusing the handle
 * or through configuring a {@link SingleConnectionFactory} underneath.
 *
 * <p>Returned transactional Session proxies will implement the {@link SessionProxy}
 * interface to allow for access to the underlying target Session. This is only
 * intended for accessing vendor-specific Session API or for testing purposes
 * (for example, to perform manual transaction control). For typical application purposes,
 * simply use the standard JMS Session interface.
 *
 * <p>As of Spring Framework 5, this class delegates JMS 2.0 {@code JMSContext}
 * calls and therefore requires the JMS 2.0 API to be present at runtime.
 * It may nevertheless run against a JMS 1.1 driver (bound to the JMS 2.0 API)
 * as long as no actual JMS 2.0 calls are triggered by the application's setup.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see UserCredentialsConnectionFactoryAdapter
 * @see SingleConnectionFactory
 */
public class TransactionAwareConnectionFactoryProxy
		implements ConnectionFactory, QueueConnectionFactory, TopicConnectionFactory {

	private @Nullable ConnectionFactory targetConnectionFactory;

	private boolean synchedLocalTransactionAllowed = false;


	/**
	 * Create a new TransactionAwareConnectionFactoryProxy.
	 */
	public TransactionAwareConnectionFactoryProxy() {
	}

	/**
	 * Create a new TransactionAwareConnectionFactoryProxy.
	 * @param targetConnectionFactory the target ConnectionFactory
	 */
	public TransactionAwareConnectionFactoryProxy(ConnectionFactory targetConnectionFactory) {
		setTargetConnectionFactory(targetConnectionFactory);
	}


	/**
	 * Set the target ConnectionFactory that this ConnectionFactory should delegate to.
	 */
	public final void setTargetConnectionFactory(ConnectionFactory targetConnectionFactory) {
		Assert.notNull(targetConnectionFactory, "'targetConnectionFactory' must not be null");
		this.targetConnectionFactory = targetConnectionFactory;
	}

	/**
	 * Return the target ConnectionFactory that this ConnectionFactory should delegate to.
	 */
	protected ConnectionFactory getTargetConnectionFactory() {
		ConnectionFactory target = this.targetConnectionFactory;
		Assert.state(target != null, "'targetConnectionFactory' is required");
		return target;
	}

	/**
	 * Set whether to allow for a local JMS transaction that is synchronized with a
	 * Spring-managed transaction (where the main transaction might be a JDBC-based
	 * one for a specific DataSource, for example), with the JMS transaction committing
	 * right after the main transaction. If not allowed, the given ConnectionFactory
	 * needs to handle transaction enlistment underneath the covers.
	 * <p>Default is "false": If not within a managed transaction that encompasses
	 * the underlying JMS ConnectionFactory, standard Sessions will be returned.
	 * Turn this flag on to allow participation in any Spring-managed transaction,
	 * with a local JMS transaction synchronized with the main transaction.
	 */
	public void setSynchedLocalTransactionAllowed(boolean synchedLocalTransactionAllowed) {
		this.synchedLocalTransactionAllowed = synchedLocalTransactionAllowed;
	}

	/**
	 * Return whether to allow for a local JMS transaction that is synchronized
	 * with a Spring-managed transaction.
	 */
	protected boolean isSynchedLocalTransactionAllowed() {
		return this.synchedLocalTransactionAllowed;
	}


	@Override
	public Connection createConnection() throws JMSException {
		Connection targetConnection = getTargetConnectionFactory().createConnection();
		return getTransactionAwareConnectionProxy(targetConnection);
	}

	@Override
	public Connection createConnection(String username, String password) throws JMSException {
		Connection targetConnection = getTargetConnectionFactory().createConnection(username, password);
		return getTransactionAwareConnectionProxy(targetConnection);
	}

	@Override
	public QueueConnection createQueueConnection() throws JMSException {
		ConnectionFactory target = getTargetConnectionFactory();
		if (!(target instanceof QueueConnectionFactory queueFactory)) {
			throw new jakarta.jms.IllegalStateException("'targetConnectionFactory' is no QueueConnectionFactory");
		}
		QueueConnection targetConnection = queueFactory.createQueueConnection();
		return (QueueConnection) getTransactionAwareConnectionProxy(targetConnection);
	}

	@Override
	public QueueConnection createQueueConnection(String username, String password) throws JMSException {
		ConnectionFactory target = getTargetConnectionFactory();
		if (!(target instanceof QueueConnectionFactory queueFactory)) {
			throw new jakarta.jms.IllegalStateException("'targetConnectionFactory' is no QueueConnectionFactory");
		}
		QueueConnection targetConnection = queueFactory.createQueueConnection(username, password);
		return (QueueConnection) getTransactionAwareConnectionProxy(targetConnection);
	}

	@Override
	public TopicConnection createTopicConnection() throws JMSException {
		ConnectionFactory target = getTargetConnectionFactory();
		if (!(target instanceof TopicConnectionFactory topicFactory)) {
			throw new jakarta.jms.IllegalStateException("'targetConnectionFactory' is no TopicConnectionFactory");
		}
		TopicConnection targetConnection = topicFactory.createTopicConnection();
		return (TopicConnection) getTransactionAwareConnectionProxy(targetConnection);
	}

	@Override
	public TopicConnection createTopicConnection(String username, String password) throws JMSException {
		ConnectionFactory target = getTargetConnectionFactory();
		if (!(target instanceof TopicConnectionFactory topicFactory)) {
			throw new jakarta.jms.IllegalStateException("'targetConnectionFactory' is no TopicConnectionFactory");
		}
		TopicConnection targetConnection = topicFactory.createTopicConnection(username, password);
		return (TopicConnection) getTransactionAwareConnectionProxy(targetConnection);
	}

	@Override
	public JMSContext createContext() {
		return getTargetConnectionFactory().createContext();
	}

	@Override
	public JMSContext createContext(String userName, String password) {
		return getTargetConnectionFactory().createContext(userName, password);
	}

	@Override
	public JMSContext createContext(String userName, String password, int sessionMode) {
		return getTargetConnectionFactory().createContext(userName, password, sessionMode);
	}

	@Override
	public JMSContext createContext(int sessionMode) {
		return getTargetConnectionFactory().createContext(sessionMode);
	}


	/**
	 * Wrap the given Connection with a proxy that delegates every method call to it
	 * but handles Session lookup in a transaction-aware fashion.
	 * @param target the original Connection to wrap
	 * @return the wrapped Connection
	 */
	protected Connection getTransactionAwareConnectionProxy(Connection target) {
		List<Class<?>> classes = new ArrayList<>(3);
		classes.add(Connection.class);
		if (target instanceof QueueConnection) {
			classes.add(QueueConnection.class);
		}
		if (target instanceof TopicConnection) {
			classes.add(TopicConnection.class);
		}
		return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
				ClassUtils.toClassArray(classes), new TransactionAwareConnectionInvocationHandler(target));
	}


	/**
	 * Invocation handler that exposes transactional Sessions for the underlying Connection.
	 */
	private class TransactionAwareConnectionInvocationHandler implements InvocationHandler {

		private final Connection target;

		public TransactionAwareConnectionInvocationHandler(Connection target) {
			this.target = target;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// Invocation on ConnectionProxy interface coming in...

			switch (method.getName()) {
				case "equals" -> {
					// Only consider equal when proxies are identical.
					return (proxy == args[0]);
				}
				case "hashCode" -> {
					// Use hashCode of Connection proxy.
					return System.identityHashCode(proxy);
				}
			}

			if (Session.class == method.getReturnType()) {
				Session session = ConnectionFactoryUtils.getTransactionalSession(
						getTargetConnectionFactory(), this.target, isSynchedLocalTransactionAllowed());
				if (session != null) {
					return getCloseSuppressingSessionProxy(session);
				}
			}
			else if (QueueSession.class == method.getReturnType()) {
				QueueSession session = ConnectionFactoryUtils.getTransactionalQueueSession(
						(QueueConnectionFactory) getTargetConnectionFactory(), (QueueConnection) this.target,
						isSynchedLocalTransactionAllowed());
				if (session != null) {
					return getCloseSuppressingSessionProxy(session);
				}
			}
			else if (TopicSession.class == method.getReturnType()) {
				TopicSession session = ConnectionFactoryUtils.getTransactionalTopicSession(
						(TopicConnectionFactory) getTargetConnectionFactory(), (TopicConnection) this.target,
						isSynchedLocalTransactionAllowed());
				if (session != null) {
					return getCloseSuppressingSessionProxy(session);
				}
			}

			// Invoke method on target Connection.
			try {
				return method.invoke(this.target, args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}

		private Session getCloseSuppressingSessionProxy(Session target) {
			List<Class<?>> classes = new ArrayList<>(3);
			classes.add(SessionProxy.class);
			if (target instanceof QueueSession) {
				classes.add(QueueSession.class);
			}
			if (target instanceof TopicSession) {
				classes.add(TopicSession.class);
			}
			return (Session) Proxy.newProxyInstance(SessionProxy.class.getClassLoader(),
					ClassUtils.toClassArray(classes), new CloseSuppressingSessionInvocationHandler(target));
		}
	}


	/**
	 * Invocation handler that suppresses close calls for a transactional JMS Session.
	 */
	private static class CloseSuppressingSessionInvocationHandler implements InvocationHandler {

		private final Session target;

		public CloseSuppressingSessionInvocationHandler(Session target) {
			this.target = target;
		}

		@Override
		public @Nullable Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// Invocation on SessionProxy interface coming in...

			return switch (method.getName()) {
				// Only consider equal when proxies are identical.
				case "equals" -> (proxy == args[0]);
				// Use hashCode of Connection proxy.
				case "hashCode" -> System.identityHashCode(proxy);
				case "commit" ->
					throw new TransactionInProgressException("Commit call not allowed within a managed transaction");
				case "rollback" ->
					throw new TransactionInProgressException("Rollback call not allowed within a managed transaction");
				// Handle close method: not to be closed within a transaction.
				case "close" -> null;
				// Handle getTargetSession method: return underlying Session.
				case "getTargetSession" -> this.target;
				default -> {
					try {
						// Invoke method on target Session.
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
