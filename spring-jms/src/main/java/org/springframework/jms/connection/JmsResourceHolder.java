/*
 * Copyright 2002-2012 the original author or authors.
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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.TransactionInProgressException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.transaction.support.ResourceHolderSupport;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * JMS resource holder, wrapping a JMS Connection and a JMS Session.
 * JmsTransactionManager binds instances of this class to the thread,
 * for a given JMS ConnectionFactory.
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

	private ConnectionFactory connectionFactory;

	private boolean frozen = false;

	private final List<Connection> connections = new LinkedList<Connection>();

	private final List<Session> sessions = new LinkedList<Session>();

	private final Map<Connection, List<Session>> sessionsPerConnection =
			new HashMap<Connection, List<Session>>();


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
	 * resource holder is associated with (may be <code>null</code>)
	 */
	public JmsResourceHolder(ConnectionFactory connectionFactory) {
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
	 * resource holder is associated with (may be <code>null</code>)
	 * @param connection the JMS Connection
	 * @param session the JMS Session
	 */
	public JmsResourceHolder(ConnectionFactory connectionFactory, Connection connection, Session session) {
		this.connectionFactory = connectionFactory;
		addConnection(connection);
		addSession(session, connection);
		this.frozen = true;
	}


	public final boolean isFrozen() {
		return this.frozen;
	}

	public final void addConnection(Connection connection) {
		Assert.isTrue(!this.frozen, "Cannot add Connection because JmsResourceHolder is frozen");
		Assert.notNull(connection, "Connection must not be null");
		if (!this.connections.contains(connection)) {
			this.connections.add(connection);
		}
	}

	public final void addSession(Session session) {
		addSession(session, null);
	}

	public final void addSession(Session session, Connection connection) {
		Assert.isTrue(!this.frozen, "Cannot add Session because JmsResourceHolder is frozen");
		Assert.notNull(session, "Session must not be null");
		if (!this.sessions.contains(session)) {
			this.sessions.add(session);
			if (connection != null) {
				List<Session> sessions = this.sessionsPerConnection.get(connection);
				if (sessions == null) {
					sessions = new LinkedList<Session>();
					this.sessionsPerConnection.put(connection, sessions);
				}
				sessions.add(session);
			}
		}
	}

	public boolean containsSession(Session session) {
		return this.sessions.contains(session);
	}


	public Connection getConnection() {
		return (!this.connections.isEmpty() ? this.connections.get(0) : null);
	}

	public Connection getConnection(Class<? extends Connection> connectionType) {
		return CollectionUtils.findValueOfType(this.connections, connectionType);
	}

	public Session getSession() {
		return (!this.sessions.isEmpty() ? this.sessions.get(0) : null);
	}

	public Session getSession(Class<? extends Session> sessionType) {
		return getSession(sessionType, null);
	}

	public Session getSession(Class<? extends Session> sessionType, Connection connection) {
		List<Session> sessions = this.sessions;
		if (connection != null) {
			sessions = this.sessionsPerConnection.get(connection);
		}
		return CollectionUtils.findValueOfType(sessions, sessionType);
	}


	public void commitAll() throws JMSException {
		for (Session session : this.sessions) {
			try {
				session.commit();
			}
			catch (TransactionInProgressException ex) {
				// Ignore -> can only happen in case of a JTA transaction.
			}
			// Let IllegalStateException through: It might point out an unexpectedly closed session.
		}
	}

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
