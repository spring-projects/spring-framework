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

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.Session;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;

/**
 * A subclass of {@link JmsTransactionManager} for the JMS 1.0.2 specification,
 * not relying on JMS 1.1 methods like JmsTransactionManager itself.
 * This class can be used for JMS 1.0.2 providers, offering the same API as
 * JmsTransactionManager does for JMS 1.1 providers.
 *
 * <p>You need to set the {@link #setPubSubDomain "pubSubDomain" property},
 * since this class will always explicitly differentiate between a
 * {@link javax.jms.QueueConnection} and a {@link javax.jms.TopicConnection}.
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see #setConnectionFactory
 * @see #setPubSubDomain
 * @deprecated as of Spring 3.0, in favor of the JMS 1.1 based {@link JmsTransactionManager}
 */
@Deprecated
@SuppressWarnings("serial")
public class JmsTransactionManager102 extends JmsTransactionManager {

	private boolean pubSubDomain = false;


	/**
	 * Create a new JmsTransactionManager102 for bean-style usage.
	 * <p>Note: The ConnectionFactory has to be set before using the instance.
	 * This constructor can be used to prepare a JmsTemplate via a BeanFactory,
	 * typically setting the ConnectionFactory via setConnectionFactory.
	 * @see #setConnectionFactory
	 */
	public JmsTransactionManager102() {
		super();
	}

	/**
	 * Create a new JmsTransactionManager102, given a ConnectionFactory.
	 * @param connectionFactory the ConnectionFactory to manage transactions for
	 * @param pubSubDomain whether the Publish/Subscribe domain (Topics) or
	 * Point-to-Point domain (Queues) should be used
	 * @see #setPubSubDomain
	 */
	public JmsTransactionManager102(ConnectionFactory connectionFactory, boolean pubSubDomain) {
		setConnectionFactory(connectionFactory);
		this.pubSubDomain = pubSubDomain;
		afterPropertiesSet();
	}


	/**
	 * Configure the transaction manager with knowledge of the JMS domain used.
	 * This tells the JMS 1.0.2 provider which class hierarchy to use for creating
	 * Connections and Sessions.
	 * <p>Default is Point-to-Point (Queues).
	 * @param pubSubDomain {@code true} for Publish/Subscribe domain (Topics),
	 * {@code false} for Point-to-Point domain (Queues)
	 */
	public void setPubSubDomain(boolean pubSubDomain) {
		this.pubSubDomain = pubSubDomain;
	}

	/**
	 * Return whether the Publish/Subscribe domain (Topics) is used.
	 * Otherwise, the Point-to-Point domain (Queues) is used.
	 */
	public boolean isPubSubDomain() {
		return this.pubSubDomain;
	}


	/**
	 * In addition to checking if the connection factory is set, make sure
	 * that the supplied connection factory is of the appropriate type for
	 * the specified destination type: QueueConnectionFactory for queues,
	 * and TopicConnectionFactory for topics.
	 */
	public void afterPropertiesSet() {
		super.afterPropertiesSet();

		// Make sure that the ConnectionFactory passed is consistent.
		// Some provider implementations of the ConnectionFactory interface
		// implement both domain interfaces under the cover, so just check if
		// the selected domain is consistent with the type of connection factory.
		if (isPubSubDomain()) {
			if (!(getConnectionFactory() instanceof TopicConnectionFactory)) {
				throw new IllegalArgumentException(
						"Specified a Spring JMS 1.0.2 transaction manager for topics " +
						"but did not supply an instance of TopicConnectionFactory");
			}
		}
		else {
			if (!(getConnectionFactory() instanceof QueueConnectionFactory)) {
				throw new IllegalArgumentException(
						"Specified a Spring JMS 1.0.2 transaction manager for queues " +
						"but did not supply an instance of QueueConnectionFactory");
			}
		}
	}


	/**
	 * This implementation overrides the superclass method to use JMS 1.0.2 API.
	 */
	protected Connection createConnection() throws JMSException {
		if (isPubSubDomain()) {
			return ((TopicConnectionFactory) getConnectionFactory()).createTopicConnection();
		}
		else {
			return ((QueueConnectionFactory) getConnectionFactory()).createQueueConnection();
		}
	}

	/**
	 * This implementation overrides the superclass method to use JMS 1.0.2 API.
	 */
	protected Session createSession(Connection con) throws JMSException {
		if (isPubSubDomain()) {
			return ((TopicConnection) con).createTopicSession(true, Session.AUTO_ACKNOWLEDGE);
		}
		else {
			return ((QueueConnection) con).createQueueSession(true, Session.AUTO_ACKNOWLEDGE);
		}
	}

}
