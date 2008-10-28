/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.jms.listener.serversession;

import javax.jms.Connection;
import javax.jms.ConnectionConsumer;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.ServerSessionPool;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;

/**
 * A subclass of {@link ServerSessionMessageListenerContainer} for the JMS 1.0.2 specification,
 * not relying on JMS 1.1 methods like ServerSessionMessageListenerContainer itself.
 *
 * <p>This class can be used for JMS 1.0.2 providers, offering the same facility as
 * ServerSessionMessageListenerContainer does for JMS 1.1 providers.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @deprecated as of Spring 2.5, in favor of DefaultMessageListenerContainer
 * and JmsMessageEndpointManager. To be removed in Spring 3.0.
 */
public class ServerSessionMessageListenerContainer102 extends ServerSessionMessageListenerContainer {

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
	protected ConnectionConsumer createConsumer(Connection con, Destination destination, ServerSessionPool pool)
			throws JMSException {

		if (isPubSubDomain()) {
			if (isSubscriptionDurable()) {
				return ((TopicConnection) con).createDurableConnectionConsumer(
						(Topic) destination, getDurableSubscriptionName(), getMessageSelector(), pool, getMaxMessagesPerTask());
			}
			else {
				return ((TopicConnection) con).createConnectionConsumer(
						(Topic) destination, getMessageSelector(), pool, getMaxMessagesPerTask());
			}
		}
		else {
			return ((QueueConnection) con).createConnectionConsumer(
					(Queue) destination, getMessageSelector(), pool, getMaxMessagesPerTask());
		}
	}

	/**
	 * This implementation overrides the superclass method to use JMS 1.0.2 API.
	 */
	protected Session createSession(Connection con) throws JMSException {
		if (isPubSubDomain()) {
			return ((TopicConnection) con).createTopicSession(isSessionTransacted(), getSessionAcknowledgeMode());
		}
		else {
			return ((QueueConnection) con).createQueueSession(isSessionTransacted(), getSessionAcknowledgeMode());
		}
	}

	/**
	 * This implementation overrides the superclass method to avoid using
	 * JMS 1.1's Session <code>getAcknowledgeMode()</code> method.
	 * The best we can do here is to check the setting on the listener container.
	 */
	protected boolean isClientAcknowledge(Session session) throws JMSException {
		return (getSessionAcknowledgeMode() == Session.CLIENT_ACKNOWLEDGE);
	}

}
