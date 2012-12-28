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

package org.springframework.jms.core;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;

import org.springframework.jms.connection.JmsResourceHolder;
import org.springframework.jms.support.converter.SimpleMessageConverter102;

/**
 * A subclass of {@link JmsTemplate} for the JMS 1.0.2 specification, not relying
 * on JMS 1.1 methods like JmsTemplate itself. This class can be used for JMS
 * 1.0.2 providers, offering the same API as JmsTemplate does for JMS 1.1 providers.
 *
 * <p>You must specify the domain (or style) of messaging to be either
 * Point-to-Point (Queues) or Publish/Subscribe (Topics), using the
 * {@link #setPubSubDomain "pubSubDomain" property}.
 * Point-to-Point (Queues) is the default domain.
 *
 * <p>The "pubSubDomain" property is an important setting due to the use of similar
 * but separate class hierarchies in the JMS 1.0.2 API. JMS 1.1 provides a new
 * domain-independent API that allows for easy mix-and-match use of Point-to-Point
 * and Publish/Subscribe styles.
 *
 * <p>This template uses a
 * {@link org.springframework.jms.support.destination.DynamicDestinationResolver}
 * and a {@link org.springframework.jms.support.converter.SimpleMessageConverter102}
 * as default strategies for resolving a destination name and converting a message,
 * respectively.
 *
 * @author Mark Pollack
 * @author Juergen Hoeller
 * @since 1.1
 * @see #setConnectionFactory
 * @see #setPubSubDomain
 * @see javax.jms.Queue
 * @see javax.jms.Topic
 * @see javax.jms.QueueSession
 * @see javax.jms.TopicSession
 * @see javax.jms.QueueSender
 * @see javax.jms.TopicPublisher
 * @see javax.jms.QueueReceiver
 * @see javax.jms.TopicSubscriber
 * @deprecated as of Spring 3.0, in favor of the JMS 1.1 based {@link JmsTemplate}
 */
@Deprecated
public class JmsTemplate102 extends JmsTemplate {

	/**
	 * Create a new JmsTemplate102 for bean-style usage.
	 * <p>Note: The ConnectionFactory has to be set before using the instance.
	 * This constructor can be used to prepare a JmsTemplate via a BeanFactory,
	 * typically setting the ConnectionFactory via setConnectionFactory.
	 * @see #setConnectionFactory
	 */
	public JmsTemplate102() {
		super();
	}

	/**
	 * Create a new JmsTemplate102, given a ConnectionFactory.
	 * @param connectionFactory the ConnectionFactory to obtain Connections from
	 * @param pubSubDomain whether the Publish/Subscribe domain (Topics) or
	 * Point-to-Point domain (Queues) should be used
	 * @see #setPubSubDomain
	 */
	public JmsTemplate102(ConnectionFactory connectionFactory, boolean pubSubDomain) {
		this();
		setConnectionFactory(connectionFactory);
		setPubSubDomain(pubSubDomain);
		afterPropertiesSet();
	}

	/**
	 * Initialize the default implementations for the template's strategies:
	 * DynamicDestinationResolver and SimpleMessageConverter102.
	 * @see #setDestinationResolver
	 * @see #setMessageConverter
	 * @see org.springframework.jms.support.destination.DynamicDestinationResolver
	 * @see org.springframework.jms.support.converter.SimpleMessageConverter102
	 */
	protected void initDefaultStrategies() {
		setMessageConverter(new SimpleMessageConverter102());
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
						"Specified a Spring JMS 1.0.2 template for topics " +
						"but did not supply an instance of TopicConnectionFactory");
			}
		}
		else {
			if (!(getConnectionFactory() instanceof QueueConnectionFactory)) {
				throw new IllegalArgumentException(
						"Specified a Spring JMS 1.0.2 template for queues " +
						"but did not supply an instance of QueueConnectionFactory");
			}
		}
	}


	/**
	 * This implementation overrides the superclass method to accept either
	 * a QueueConnection or a TopicConnection, depending on the domain.
	 */
	protected Connection getConnection(JmsResourceHolder holder) {
		return holder.getConnection(isPubSubDomain() ? (Class) TopicConnection.class : QueueConnection.class);
	}

	/**
	 * This implementation overrides the superclass method to accept either
	 * a QueueSession or a TopicSession, depending on the domain.
	 */
	protected Session getSession(JmsResourceHolder holder) {
		return holder.getSession(isPubSubDomain() ? (Class) TopicSession.class : QueueSession.class);
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
	protected MessageProducer doCreateProducer(Session session, Destination destination) throws JMSException {
		if (isPubSubDomain()) {
			return ((TopicSession) session).createPublisher((Topic) destination);
		}
		else {
			return ((QueueSession) session).createSender((Queue) destination);
		}
	}

	/**
	 * This implementation overrides the superclass method to use JMS 1.0.2 API.
	 */
	protected MessageConsumer createConsumer(Session session, Destination destination, String messageSelector)
			throws JMSException {

		if (isPubSubDomain()) {
			return ((TopicSession) session).createSubscriber((Topic) destination, messageSelector, isPubSubNoLocal());
		}
		else {
			return ((QueueSession) session).createReceiver((Queue) destination, messageSelector);
		}
	}

	protected QueueBrowser createBrowser(Session session, Queue queue, String messageSelector)
			throws JMSException {

		if (isPubSubDomain()) {
			throw new javax.jms.IllegalStateException("Cannot create QueueBrowser for a TopicSession");
		}
		else {
			return ((QueueSession) session).createBrowser(queue, messageSelector);
		}
	}

	/**
	 * This implementation overrides the superclass method to use JMS 1.0.2 API.
	 */
	protected void doSend(MessageProducer producer, Message message) throws JMSException {
		if (isPubSubDomain()) {
			if (isExplicitQosEnabled()) {
				((TopicPublisher) producer).publish(message, getDeliveryMode(), getPriority(), getTimeToLive());
			}
			else {
				((TopicPublisher) producer).publish(message);
			}
		}
		else {
			if (isExplicitQosEnabled()) {
				((QueueSender) producer).send(message, getDeliveryMode(), getPriority(), getTimeToLive());
			}
			else {
				((QueueSender) producer).send(message);
			}
		}
	}

	/**
	 * This implementation overrides the superclass method to avoid using
	 * JMS 1.1's Session {@code getAcknowledgeMode()} method.
	 * The best we can do here is to check the setting on the template.
	 * @see #getSessionAcknowledgeMode()
	 */
	protected boolean isClientAcknowledge(Session session) throws JMSException {
		return (getSessionAcknowledgeMode() == Session.CLIENT_ACKNOWLEDGE);
	}

}
