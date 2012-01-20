/*
 * Copyright 2002-2007 the original author or authors.
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
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * {@link javax.jms.ConnectionFactory} implementation that delegates all calls
 * to a given target {@link javax.jms.ConnectionFactory}, adapting specific
 * <code>create(Queue/Topic)Connection</code> calls to the target ConnectionFactory
 * if necessary (e.g. when running JMS 1.0.2 API based code against a generic
 * JMS 1.1 ConnectionFactory, such as ActiveMQ's PooledConnectionFactory).
 *
 * <p>This class allows for being subclassed, with subclasses overriding only
 * those methods (such as {@link #createConnection()}) that should not simply
 * delegate to the target ConnectionFactory.
 *
 * <p>Can also be defined as-is, wrapping a specific target ConnectionFactory,
 * using the "shouldStopConnections" flag to indicate whether Connections
 * obtained from the target factory are supposed to be stopped before closed.
 * The latter may be necessary for some connection pools that simply return
 * released connections to the pool, not stopping them while they sit in the pool.
 *
 * @author Juergen Hoeller
 * @since 2.0.2
 * @see #createConnection()
 * @see #setShouldStopConnections
 * @see ConnectionFactoryUtils#releaseConnection
 */
public class DelegatingConnectionFactory
		implements SmartConnectionFactory, QueueConnectionFactory, TopicConnectionFactory, InitializingBean {

	private ConnectionFactory targetConnectionFactory;

	private boolean shouldStopConnections = false;


	/**
	 * Set the target ConnectionFactory that this ConnectionFactory should delegate to.
	 */
	public void setTargetConnectionFactory(ConnectionFactory targetConnectionFactory) {
		Assert.notNull(targetConnectionFactory, "'targetConnectionFactory' must not be null");
		this.targetConnectionFactory = targetConnectionFactory;
	}

	/**
	 * Return the target ConnectionFactory that this ConnectionFactory delegates to.
	 */
	public ConnectionFactory getTargetConnectionFactory() {
		return this.targetConnectionFactory;
	}

	/**
	 * Indicate whether Connections obtained from the target factory are supposed
	 * to be stopped before closed ("true") or simply closed ("false").
	 * The latter may be necessary for some connection pools that simply return
	 * released connections to the pool, not stopping them while they sit in the pool.
	 * <p>Default is "false", simply closing Connections.
	 * @see ConnectionFactoryUtils#releaseConnection
	 */
	public void setShouldStopConnections(boolean shouldStopConnections) {
		this.shouldStopConnections = shouldStopConnections;
	}

	public void afterPropertiesSet() {
		if (getTargetConnectionFactory() == null) {
			throw new IllegalArgumentException("'targetConnectionFactory' is required");
		}
	}


	public Connection createConnection() throws JMSException {
		return getTargetConnectionFactory().createConnection();
	}

	public Connection createConnection(String username, String password) throws JMSException {
		return getTargetConnectionFactory().createConnection(username, password);
	}

	public QueueConnection createQueueConnection() throws JMSException {
		ConnectionFactory cf = getTargetConnectionFactory();
		if (cf instanceof QueueConnectionFactory) {
			return ((QueueConnectionFactory) cf).createQueueConnection();
		}
		else {
			Connection con = cf.createConnection();
			if (!(con instanceof QueueConnection)) {
				throw new javax.jms.IllegalStateException("'targetConnectionFactory' is not a QueueConnectionFactory");
			}
			return (QueueConnection) con;
		}
	}

	public QueueConnection createQueueConnection(String username, String password) throws JMSException {
		ConnectionFactory cf = getTargetConnectionFactory();
		if (cf instanceof QueueConnectionFactory) {
			return ((QueueConnectionFactory) cf).createQueueConnection(username, password);
		}
		else {
			Connection con = cf.createConnection(username, password);
			if (!(con instanceof QueueConnection)) {
				throw new javax.jms.IllegalStateException("'targetConnectionFactory' is not a QueueConnectionFactory");
			}
			return (QueueConnection) con;
		}
	}

	public TopicConnection createTopicConnection() throws JMSException {
		ConnectionFactory cf = getTargetConnectionFactory();
		if (cf instanceof TopicConnectionFactory) {
			return ((TopicConnectionFactory) cf).createTopicConnection();
		}
		else {
			Connection con = cf.createConnection();
			if (!(con instanceof TopicConnection)) {
				throw new javax.jms.IllegalStateException("'targetConnectionFactory' is not a TopicConnectionFactory");
			}
			return (TopicConnection) con;
		}
	}

	public TopicConnection createTopicConnection(String username, String password) throws JMSException {
		ConnectionFactory cf = getTargetConnectionFactory();
		if (cf instanceof TopicConnectionFactory) {
			return ((TopicConnectionFactory) cf).createTopicConnection(username, password);
		}
		else {
			Connection con = cf.createConnection(username, password);
			if (!(con instanceof TopicConnection)) {
				throw new javax.jms.IllegalStateException("'targetConnectionFactory' is not a TopicConnectionFactory");
			}
			return (TopicConnection) con;
		}
	}

	public boolean shouldStop(Connection con) {
		return this.shouldStopConnections;
	}

}
