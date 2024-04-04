/*
 * Copyright 2002-2020 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Session;
import jakarta.jms.TransactionInProgressException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.transaction.support.ResourceHolderSupport;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Resource holder wrapping a JMS {@link Connection} and a JMS {@link Session}.
 * {@link JmsTransactionManager} binds instances of this class to the thread,
 * for a given JMS {@link ConnectionFactory}.
 *
 * <p>Note: This is an SPI class, not intended to be used by applications.
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see JmsTransactionManager
 * @see org.springframework.jms.core.JmsTemplate
 */
public class JmsResourceHolder extends ResourceHolderSupport {

	private static final Log logger = LogFactory.getLog(JmsResourceHolder.class);

	@Nullable
	private ConnectionFactory connectionFactory;

	private boolean frozen = false;

	private final Deque<Connection> connections = new ArrayDeque<>();

	private final Deque<Session> sessions = new ArrayDeque<>();

	private final Map<Connection, Deque<Session>> sessionsPerConnection = new HashMap<>();


	/**
	 * Create a new JmsResourceHolder that is open for resources to be added.
	 * @see #addConnection
	 * @see #addSession
	 */
	public JmsResourceHolder() {
	}

	/**
	 * Create a new JmsResourceHolder that is open for resources to be added.
	 * @param connectionFactory the JMS ConnectionFactory that this
	 * resource holder is associated with (may be {@code null})
	 */
	public JmsResourceHolder(@Nullable ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	/**
	 * Create a new JmsResourceHolder for the given JMS Session.
	 * @param session the JMS Session
	 */
	public JmsResourceHolder(Session session) {
		addSession(session);
		this.frozen = true;
	}

	/**
	 * Create a new JmsResourceHolder for the given JMS resources.
	 * @param connection the JMS Connection
	 * @param session the JMS Session
	 */
	public JmsResourceHolder(Connection connection, Session session) {
		addConnection(connection);
		addSession(session, connection);
		this.frozen = true;
	}

	/**
	 * Create a new JmsResourceHolder for the given JMS resources.
	 * @param connectionFactory the JMS ConnectionFactory that this
	 * resource holder is associated with (may be {@code null})
	 * @param connection the JMS Connection
	 * @param session the JMS Session
	 */
	public JmsResourceHolder(@Nullable ConnectionFactory connectionFactory, Connection connection, Session session) {
		this.connectionFactory = connectionFactory;
		addConnection(connection);
		addSession(session, connection);
		this.frozen = true;
	}


	/**
	 * Return whether this resource holder is frozen, i.e. does not
	 * allow for adding further Connections and Sessions to it.
	 * @see #addConnection
	 * @see #addSession
	 */
	public final boolean isFrozen() {
		return this.frozen;
	}

	/**
	 * Add the given Connection to this resource holder.
	 */
	public final void addConnection(Connection connection) {
		Assert.isTrue(!this.frozen, "Cannot add Connection because JmsResourceHolder is frozen");
		Assert.notNull(connection, "Connection must not be null");
		if (!this.connections.contains(connection)) {
			this.connections.add(connection);
		}
	}

	/**
	 * Add the given Session to this resource holder.
	 */
	public final void addSession(Session session) {
		addSession(session, null);
	}

	/**
	 * Add the given Session to this resource holder,
	 * registered for a specific Connection.
	 */
	public final void addSession(Session session, @Nullable Connection connection) {
		Assert.isTrue(!this.frozen, "Cannot add Session because JmsResourceHolder is frozen");
		Assert.notNull(session, "Session must not be null");
		if (!this.sessions.contains(session)) {
			this.sessions.add(session);
			if (connection != null) {
				Deque<Session> sessions =
						this.sessionsPerConnection.computeIfAbsent(connection, k -> new ArrayDeque<>());
				sessions.add(session);
			}
		}
	}

	/**
	 * Determine whether the given Session is registered
	 * with this resource holder.
	 */
	public boolean containsSession(Session session) {
		return this.sessions.contains(session);
	}


	/**
	 * Return this resource holder's default Connection,
	 * or {@code null} if none.
	 */
	@Nullable
	public Connection getConnection() {
		return this.connections.peek();
	}

	/**
	 * Return this resource holder's Connection of the given type,
	 * or {@code null} if none.
	 */
	@Nullable
	public <C extends Connection> C getConnection(Class<C> connectionType) {
		return CollectionUtils.findValueOfType(this.connections, connectionType);
	}

	/**
	 * Return an existing original Session, if any.
	 * <p>In contrast to {@link #getSession()}, this must not lazily initialize
	 * a new Session, not even in {@link JmsResourceHolder} subclasses.
	 */
	@Nullable
	Session getOriginalSession() {
		return this.sessions.peek();
	}

	/**
	 * Return this resource holder's default Session,
	 * or {@code null} if none.
	 */
	@Nullable
	public Session getSession() {
		return this.sessions.peek();
	}

	/**
	 * Return this resource holder's Session of the given type,
	 * or {@code null} if none.
	 */
	@Nullable
	public <S extends Session> S getSession(Class<S> sessionType) {
		return getSession(sessionType, null);
	}

	/**
	 * Return this resource holder's Session of the given type
	 * for the given connection, or {@code null} if none.
	 */
	@Nullable
	@SuppressWarnings("NullAway")
	public <S extends Session> S getSession(Class<S> sessionType, @Nullable Connection connection) {
		Deque<Session> sessions =
				(connection != null ? this.sessionsPerConnection.get(connection) : this.sessions);
		return CollectionUtils.findValueOfType(sessions, sessionType);
	}


	/**
	 * Commit all of this resource holder's Sessions.
	 * @throws JMSException if thrown from a Session commit attempt
	 * @see Session#commit()
	 */
	public void commitAll() throws JMSException {
		for (Session session : this.sessions) {
			try {
				session.commit();
			}
			catch (TransactionInProgressException ex) {
				// Ignore -> can only happen in case of a JTA transaction.
			}
			catch (jakarta.jms.IllegalStateException ex) {
				if (this.connectionFactory != null) {
					try {
						Method getDataSourceMethod = this.connectionFactory.getClass().getMethod("getDataSource");
						Object ds = ReflectionUtils.invokeMethod(getDataSourceMethod, this.connectionFactory);
						while (ds != null) {
							if (TransactionSynchronizationManager.hasResource(ds)) {
								// IllegalStateException from sharing the underlying JDBC Connection
								// which typically gets committed first, e.g. with Oracle AQ --> ignore
								return;
							}
							try {
								// Check for decorated DataSource a la Spring's DelegatingDataSource
								Method getTargetDataSourceMethod = ds.getClass().getMethod("getTargetDataSource");
								ds = ReflectionUtils.invokeMethod(getTargetDataSourceMethod, ds);
							}
							catch (NoSuchMethodException nsme) {
								ds = null;
							}
						}
					}
					catch (Throwable ex2) {
						if (logger.isDebugEnabled()) {
							logger.debug("No working getDataSource method found on ConnectionFactory: " + ex2);
						}
						// No working getDataSource method - cannot perform DataSource transaction check
					}
				}
				throw ex;
			}
		}
	}

	/**
	 * Close all of this resource holder's Sessions and clear its state.
	 * @see Session#close()
	 */
	public void closeAll() {
		for (Session session : this.sessions) {
			try {
				session.close();
			}
			catch (Throwable ex) {
				logger.debug("Could not close synchronized JMS Session after transaction", ex);
			}
		}
		for (Connection con : this.connections) {
			ConnectionFactoryUtils.releaseConnection(con, this.connectionFactory, true);
		}
		this.connections.clear();
		this.sessions.clear();
		this.sessionsPerConnection.clear();
	}

}
