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

package org.springframework.jms.listener;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;

import org.springframework.jms.connection.JmsResourceHolder;

/**
 * A subclass of {@link DefaultMessageListenerContainer} for the JMS 1.0.2 specification,
 * not relying on JMS 1.1 methods like SimpleMessageListenerContainer itself.
 *
 * <p>This class can be used for JMS 1.0.2 providers, offering the same facility as
 * DefaultMessageListenerContainer does for JMS 1.1 providers.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @deprecated as of Spring 3.0, in favor of the JMS 1.1 based {@link DefaultMessageListenerContainer}
 */
@Deprecated
public class DefaultMessageListenerContainer102 extends DefaultMessageListenerContainer {

	/**
	 * This implementation overrides the superclass method to accept either
	 * a QueueConnection or a TopicConnection, depending on the domain.
	 */
	protected Connection getConnection(JmsResourceHolder holder) {
		return holder.getConnection(isPubSubDomain() ? TopicConnection.class : QueueConnection.class);
	}

	/**
	 * This implementation overrides the superclass method to accept either
	 * a QueueSession or a TopicSession, depending on the domain.
	 */
	protected Session getSession(JmsResourceHolder holder) {
		return holder.getSession(isPubSubDomain() ? TopicSession.class : QueueSession.class);
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
			return ((TopicConnection) con).createTopicSession(isSessionTransacted(), getSessionAcknowledgeMode());
		}
		else {
			return ((QueueConnection) con).createQueueSession(isSessionTransacted(), getSessionAcknowledgeMode());
		}
	}

	/**
	 * This implementation overrides the superclass method to use JMS 1.0.2 API.
	 */
	protected MessageConsumer createConsumer(Session session, Destination destination) throws JMSException {
		if (isPubSubDomain()) {
			if (isSubscriptionDurable()) {
				return ((TopicSession) session).createDurableSubscriber(
						(Topic) destination, getDurableSubscriptionName(), getMessageSelector(), isPubSubNoLocal());
			}
			else {
				return ((TopicSession) session).createSubscriber(
						(Topic) destination, getMessageSelector(), isPubSubNoLocal());
			}
		}
		else {
			return ((QueueSession) session).createReceiver((Queue) destination, getMessageSelector());
		}
	}

	/**
	 * This implementation overrides the superclass method to avoid using
	 * JMS 1.1's Session {@code getAcknowledgeMode()} method.
	 * The best we can do here is to check the setting on the listener container.
	 */
	protected boolean isClientAcknowledge(Session session) throws JMSException {
		return (getSessionAcknowledgeMode() == Session.CLIENT_ACKNOWLEDGE);
	}

}
