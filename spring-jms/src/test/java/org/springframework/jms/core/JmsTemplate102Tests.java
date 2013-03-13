/*
 * Copyright 2002-2013 the original author or authors.
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

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.naming.Context;
import javax.naming.NamingException;

import org.junit.Before;
import org.junit.Test;
import org.springframework.jms.InvalidClientIDException;
import org.springframework.jms.InvalidDestinationException;
import org.springframework.jms.InvalidSelectorException;
import org.springframework.jms.JmsException;
import org.springframework.jms.JmsSecurityException;
import org.springframework.jms.MessageEOFException;
import org.springframework.jms.MessageFormatException;
import org.springframework.jms.MessageNotReadableException;
import org.springframework.jms.MessageNotWriteableException;
import org.springframework.jms.ResourceAllocationException;
import org.springframework.jms.TransactionInProgressException;
import org.springframework.jms.TransactionRolledBackException;
import org.springframework.jms.UncategorizedJmsException;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.jms.support.destination.JndiDestinationResolver;
import org.springframework.jndi.JndiTemplate;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for the JmsTemplate implemented using JMS 1.0.2.
 *
 * @author Andre Biryukov
 * @author Mark Pollack
 */
public class JmsTemplate102Tests {

	private Context jndiContext;
	private QueueConnectionFactory queueConnectionFactory;
	private QueueConnection queueConnection;
	private QueueSession queueSession;
	private Queue queue;
	private TopicConnectionFactory topicConnectionFactory;
	private TopicConnection topicConnection;
	private TopicSession topicSession;
	private Topic topic;

	private int deliveryMode = DeliveryMode.PERSISTENT;
	private int priority = 9;
	private int timeToLive = 10000;


	@Before
	public void setUpMocks() throws Exception {
		jndiContext = mock(Context.class);
		createMockForQueues();
		createMockForTopics();
	}

	private void createMockForTopics() throws JMSException, NamingException {
		topicConnectionFactory = mock(TopicConnectionFactory.class);
		topicConnection = mock(TopicConnection.class);
		topic = mock(Topic.class);
		topicSession = mock(TopicSession.class);

		given(topicConnectionFactory.createTopicConnection()).willReturn(topicConnection);
		given(topicConnection.createTopicSession(useTransactedTemplate(),
				Session.AUTO_ACKNOWLEDGE)).willReturn(topicSession);
		given(topicSession.getTransacted()).willReturn(useTransactedSession());
		given(jndiContext.lookup("testTopic")).willReturn(topic);
	}

	private void createMockForQueues() throws JMSException, NamingException {
		queueConnectionFactory = mock(QueueConnectionFactory.class);
		queueConnection = mock(QueueConnection.class);
		queue = mock(Queue.class);
		queueSession = mock(QueueSession.class);

		given(queueConnectionFactory.createQueueConnection()).willReturn(queueConnection);
		given(queueConnection.createQueueSession(useTransactedTemplate(),
				Session.AUTO_ACKNOWLEDGE)).willReturn(queueSession);
		given(queueSession.getTransacted()).willReturn(useTransactedSession());
		given(jndiContext.lookup("testQueue")).willReturn(queue);
	}

	private JmsTemplate102 createTemplate() {
		JmsTemplate102 template = new JmsTemplate102();
		JndiDestinationResolver destMan = new JndiDestinationResolver();
		destMan.setJndiTemplate(new JndiTemplate() {
			@Override
			protected Context createInitialContext() {
				return jndiContext;
			}
		});
		template.setDestinationResolver(destMan);
		template.setSessionTransacted(useTransactedTemplate());
		return template;
	}

	protected boolean useTransactedSession() {
		return false;
	}

	protected boolean useTransactedTemplate() {
		return false;
	}


	@Test
	public void testTopicSessionCallback() throws Exception {
		JmsTemplate102 template = createTemplate();
		template.setPubSubDomain(true);
		template.setConnectionFactory(topicConnectionFactory);
		template.afterPropertiesSet();

		template.execute(new SessionCallback() {
			@Override
			public Object doInJms(Session session) throws JMSException {
				session.getTransacted();
				return null;
			}
		});

		verify(topicSession).close();
		verify(topicConnection).close();
	}

	/**
	 * Test the execute(ProducerCallback) using a topic.
	 */
	@Test
	public void testTopicProducerCallback() throws Exception {
		JmsTemplate102 template = createTemplate();
		template.setPubSubDomain(true);
		template.setConnectionFactory(topicConnectionFactory);
		template.afterPropertiesSet();

		TopicPublisher topicPublisher = mock(TopicPublisher.class);

		given(topicSession.createPublisher(null)).willReturn(topicPublisher);
		given(topicPublisher.getPriority()).willReturn(4);

		template.execute(new ProducerCallback() {
			@Override
			public Object doInJms(Session session, MessageProducer producer) throws JMSException {
				session.getTransacted();
				producer.getPriority();
				return null;
			}
		});

		verify(topicPublisher).close();
		verify(topicSession).close();
		verify(topicConnection).close();
	}

	/**
	 * Test the execute(ProducerCallback) using a topic.
	 */
	@Test
	public void testTopicProducerCallbackWithIdAndTimestampDisabled() throws Exception {
		JmsTemplate102 template = createTemplate();
		template.setPubSubDomain(true);
		template.setConnectionFactory(topicConnectionFactory);
		template.setMessageIdEnabled(false);
		template.setMessageTimestampEnabled(false);
		template.afterPropertiesSet();

		TopicPublisher topicPublisher = mock(TopicPublisher.class);

		given(topicSession.createPublisher(null)).willReturn(topicPublisher);
		given(topicPublisher.getPriority()).willReturn(4);

		template.execute(new ProducerCallback() {
			@Override
			public Object doInJms(Session session, MessageProducer producer) throws JMSException {
				boolean b = session.getTransacted();
				int i = producer.getPriority();
				return null;
			}
		});

		verify(topicPublisher).setDisableMessageID(true);
		verify(topicPublisher).setDisableMessageTimestamp(true);
		verify(topicPublisher).close();
		verify(topicSession).close();
		verify(topicConnection).close();
	}

	/**
	 * Test the method execute(SessionCallback action) with using the
	 * point to point domain as specified by the value of isPubSubDomain = false.
	 */
	@Test
	public void testQueueSessionCallback() throws Exception {
		JmsTemplate102 template = createTemplate();
		// Point-to-Point (queues) are the default domain
		template.setConnectionFactory(queueConnectionFactory);
		template.afterPropertiesSet();

		template.execute(new SessionCallback() {
			@Override
			public Object doInJms(Session session) throws JMSException {
				boolean b = session.getTransacted();
				return null;
			}
		});

		verify(queueSession).close();
		verify(queueConnection).close();
	}

	/**
	 * Test the method execute(ProducerCallback) with a Queue.
	 */
	@Test
	public void testQueueProducerCallback() throws Exception {
		JmsTemplate102 template = createTemplate();
		// Point-to-Point (queues) are the default domain.
		template.setConnectionFactory(queueConnectionFactory);
		template.afterPropertiesSet();

		QueueSender queueSender = mock(QueueSender.class);

		given(queueSession.createSender(null)).willReturn(queueSender);
		given(queueSender.getPriority()).willReturn(4);

		template.execute(new ProducerCallback() {
			@Override
			public Object doInJms(Session session, MessageProducer producer)
				throws JMSException {
				boolean b = session.getTransacted();
				int i = producer.getPriority();
				return null;
			}
		});

		verify(queueSender).close();
		verify(queueSession).close();
		verify(queueConnection).close();
	}

	@Test
	public void testQueueProducerCallbackWithIdAndTimestampDisabled() throws Exception {
		JmsTemplate102 template = createTemplate();
		// Point-to-Point (queues) are the default domain.
		template.setConnectionFactory(queueConnectionFactory);
		template.setMessageIdEnabled(false);
		template.setMessageTimestampEnabled(false);
		template.afterPropertiesSet();

		QueueSender queueSender = mock(QueueSender.class);

		given(queueSession.createSender(null)).willReturn(queueSender);
		given(queueSender.getPriority()).willReturn(4);

		template.execute(new ProducerCallback() {
			@Override
			public Object doInJms(Session session, MessageProducer producer) throws JMSException {
				boolean b = session.getTransacted();
				int i = producer.getPriority();
				return null;
			}
		});

		verify(queueSender).setDisableMessageID(true);
		verify(queueSender).setDisableMessageTimestamp(true);
		verify(queueSender).close();
		verify(queueSession).close();
		verify(queueConnection).close();
	}

	/**
	 * Test the setting of the JmsTemplate properties.
	 */
	@Test
	public void testBeanProperties() throws Exception {
		JmsTemplate102 template = createTemplate();
		template.setConnectionFactory(queueConnectionFactory);

		assertTrue("connection factory ok", template.getConnectionFactory() == queueConnectionFactory);

		JmsTemplate102 s102 = createTemplate();
		try {
			s102.afterPropertiesSet();
			fail("IllegalArgumentException not thrown. ConnectionFactory should be set");
		}
		catch (IllegalArgumentException ex) {
			// expected
		}

		// The default is for the JmsTemplate102 to send to queues.
		// Test to make sure exeception is thrown and has reasonable message.
		s102 = createTemplate();
		s102.setConnectionFactory(topicConnectionFactory);
		try {
			s102.afterPropertiesSet();
			fail("IllegalArgumentException not thrown. Mismatch of Destination and ConnectionFactory types.");
		}
		catch (IllegalArgumentException ex) {
			// expected
		}

		s102 = createTemplate();
		s102.setConnectionFactory(queueConnectionFactory);
		s102.setPubSubDomain(true);
		try {
			s102.afterPropertiesSet();
			fail("IllegalArgumentException not thrown. Mismatch of Destination and ConnectionFactory types.");
		}
		catch (IllegalArgumentException ex) {
			// expected
		}
	}

	/**
	 * Test the method send(String destination, MessgaeCreator c) using
	 * a queue and default QOS values.
	 */
	@Test
	public void testSendStringQueue() throws Exception {
		sendQueue(true, false, false, true);
	}

	/**
	 * Test the method send(String destination, MessageCreator c) when
	 * explicit QOS parameters are enabled, using a queue.
	 */
	@Test
	public void testSendStringQueueWithQOS() throws Exception {
		sendQueue(false, false, false, false);
	}

	/**
	 * Test the method send(MessageCreator c) using default QOS values.
	 */
	@Test
	public void testSendDefaultDestinationQueue() throws Exception {
		sendQueue(true, false, true, true);
	}

	/**
	 * Test the method send(MessageCreator c) using explicit QOS values.
	 */
	@Test
	public void testSendDefaultDestinationQueueWithQOS() throws Exception {
		sendQueue(false, false, true, false);
	}

	/**
	 * Test the method send(String destination, MessageCreator c) using
	 * a topic and default QOS values.
	 */
	@Test
	public void testSendStringTopic() throws Exception {
		sendTopic(true, false);
	}

	/**
	 * Test the method send(String destination, MessageCreator c) using explicit
	 * QOS values.
	 */
	@Test
	public void testSendStringTopicWithQOS() throws Exception {
		sendTopic(false, false);
	}

	/**
	 * Test the method send(Destination queue, MessgaeCreator c) using
	 * a queue and default QOS values.
	 */
	@Test
	public void testSendQueue() throws Exception {
		sendQueue(true, false, false, true);
	}

	/**
	 * Test the method send(Destination queue, MessageCreator c) sing explicit
	 * QOS values.
	 */
	@Test
	public void testSendQueueWithQOS() throws Exception {
		sendQueue(false, false, false, false);
	}

	/**
	 * Test the method send(Destination queue, MessgaeCreator c) using
	 * a topic and default QOS values.
	 */
	@Test
	public void testSendTopic() throws Exception {
		sendTopic(true, false);
	}

	/**
	 * Test the method send(Destination queue, MessageCreator c) using explicity
	 * QOS values.
	 */
	@Test
	public void testSendTopicWithQOS() throws Exception {
		sendQueue(false, false, false, true);
	}

	/**
	 * Common method for testing a send method that uses the MessageCreator
	 * callback but with different QOS options.
	 */
	private void sendQueue(
			boolean ignoreQOS, boolean explicitQueue, boolean useDefaultDestination, boolean disableIdAndTimestamp)
			throws Exception {

		JmsTemplate102 template = createTemplate();
		template.setConnectionFactory(queueConnectionFactory);
		template.afterPropertiesSet();

		if (useDefaultDestination) {
			template.setDefaultDestination(queue);
		}
		if (disableIdAndTimestamp) {
			template.setMessageIdEnabled(false);
			template.setMessageTimestampEnabled(false);
		}

		QueueSender queueSender = mock(QueueSender.class);
		TextMessage message = mock(TextMessage.class);


		given(queueSession.createSender(this.queue)).willReturn(queueSender);
		given(queueSession.createTextMessage("just testing")).willReturn(message);

		if (!ignoreQOS) {
			template.setExplicitQosEnabled(true);
			template.setDeliveryMode(deliveryMode);
			template.setPriority(priority);
			template.setTimeToLive(timeToLive);
		}

		if (useDefaultDestination) {
			template.send(new MessageCreator() {
				@Override
				public Message createMessage(Session session) throws JMSException {
					return session.createTextMessage("just testing");
				}
			});
		}
		else {
			if (explicitQueue) {
				template.send(queue, new MessageCreator() {
					@Override
					public Message createMessage(Session session) throws JMSException {
						return session.createTextMessage("just testing");
					}
				});
			}
			else {
				template.send("testQueue", new MessageCreator() {
					@Override
					public Message createMessage(Session session)
						throws JMSException {
						return session.createTextMessage("just testing");
					}
				});
			}
		}

		if (disableIdAndTimestamp) {
			verify(queueSender).setDisableMessageID(true);
			verify(queueSender).setDisableMessageTimestamp(true);
		}

		if (useTransactedTemplate()) {
			verify(queueSession).commit();
		}

		if (ignoreQOS) {
			verify(queueSender).send(message);
		}
		else {
			verify(queueSender).send(message, deliveryMode, priority, timeToLive);
		}

		verify(queueSender).close();
		verify(queueSession).close();
		verify(queueConnection).close();
	}

	private void sendTopic(boolean ignoreQOS, boolean explicitTopic) throws Exception {
		JmsTemplate102 template = createTemplate();
		template.setPubSubDomain(true);
		template.setConnectionFactory(topicConnectionFactory);
		template.afterPropertiesSet();

		TopicPublisher topicPublisher = mock(TopicPublisher.class);
		TextMessage message = mock(TextMessage.class);

		given(topicSession.createPublisher(this.topic)).willReturn(topicPublisher);
		given(topicSession.createTextMessage("just testing")).willReturn(message);

		if (ignoreQOS) {
			topicPublisher.publish(message);
		}
		else {
			template.setExplicitQosEnabled(true);
			template.setDeliveryMode(deliveryMode);
			template.setPriority(priority);
			template.setTimeToLive(timeToLive);
			topicPublisher.publish(message, deliveryMode, priority, timeToLive);
		}

		template.setPubSubDomain(true);

		if (explicitTopic) {
			template.send(topic, new MessageCreator() {
				@Override
				public Message createMessage(Session session)
					throws JMSException {
					return session.createTextMessage("just testing");
				}
			});
		}
		else {
			template.send("testTopic", new MessageCreator() {
				@Override
				public Message createMessage(Session session)
					throws JMSException {
					return session.createTextMessage("just testing");
				}
			});
		}

		if (useTransactedTemplate()) {
			verify(topicSession).commit();
		}
		verify(topicPublisher).close();
		verify(topicSession).close();
		verify(topicConnection).close();
		verify(jndiContext).close();
	}

	@Test
	public void testConverter() throws Exception {
		JmsTemplate102 template = createTemplate();
		template.setConnectionFactory(queueConnectionFactory);
		template.setMessageConverter(new SimpleMessageConverter());
		String s = "Hello world";

		QueueSender queueSender = mock(QueueSender.class);
		TextMessage message = mock(TextMessage.class);

		given(queueSession.createSender(this.queue)).willReturn(queueSender);
		given(queueSession.createTextMessage("Hello world")).willReturn(message);

		template.convertAndSend(queue, s);

		if (useTransactedTemplate()) {
			verify(queueSession).commit();
		}
		verify(queueSender).send(message);
		verify(queueSender).close();
		verify(queueSession).close();
		verify(queueConnection).close();
	}

	@Test
	public void testQueueReceiveDefaultDestination() throws Exception {
		doTestReceive(false, false, true, false, false, false, false, JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
	}

	@Test
	public void testQueueReceiveDestination() throws Exception {
		doTestReceive(false, true, false, false, false, false, true, JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
	}

	@Test
	public void testQueueReceiveDestinationWithClientAcknowledge() throws Exception {
		doTestReceive(false, true, false, false, true, false, false, 1000);
	}

	@Test
	public void testQueueReceiveStringDestination() throws Exception {
		doTestReceive(false, false, false, false, false, false, true, JmsTemplate.RECEIVE_TIMEOUT_NO_WAIT);
	}

	@Test
	public void testQueueReceiveDefaultDestinationWithSelector() throws Exception {
		doTestReceive(false, false, true, false, false, true, true, 1000);
	}

	@Test
	public void testQueueReceiveDestinationWithSelector() throws Exception {
		doTestReceive(false, true, false, false, false, true, false, JmsTemplate.RECEIVE_TIMEOUT_NO_WAIT);
	}

	@Test
	public void testQueueReceiveDestinationWithClientAcknowledgeWithSelector() throws Exception {
		doTestReceive(false, true, false, false, true, true, true, JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
	}

	@Test
	public void testQueueReceiveStringDestinationWithSelector() throws Exception {
		doTestReceive(false, false, false, false, false, true, false, JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
	}

	@Test
	public void testQueueReceiveAndConvertDefaultDestination() throws Exception {
		doTestReceive(false, false, true, true, false, false, false, 1000);
	}

	@Test
	public void testQueueReceiveAndConvertStringDestination() throws Exception {
		doTestReceive(false, false, false, true, false, false, true, JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
	}

	@Test
	public void testQueueReceiveAndConvertDestination() throws Exception {
		doTestReceive(false, true, false, true, false, false, true, 1000);
	}

	@Test
	public void testQueueReceiveAndConvertDefaultDestinationWithSelector() throws Exception {
		doTestReceive(false, false, true, true, false, true, true, JmsTemplate.RECEIVE_TIMEOUT_NO_WAIT);
	}

	@Test
	public void testQueueReceiveAndConvertStringDestinationWithSelector() throws Exception {
		doTestReceive(false, false, false, true, false, true, true, JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
	}

	@Test
	public void testQueueReceiveAndConvertDestinationWithSelector() throws Exception {
		doTestReceive(false, true, false, true, false, true, false, 1000);
	}

	@Test
	public void testTopicReceiveDefaultDestination() throws Exception {
		doTestReceive(true, false, true, false, false, false, false, JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
	}

	@Test
	public void testTopicReceiveDestination() throws Exception {
		doTestReceive(true, true, false, false, false, false, true, JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
	}

	@Test
	public void testTopicReceiveDestinationWithClientAcknowledge() throws Exception {
		doTestReceive(true, true, false, false, true, false, false, 1000);
	}

	@Test
	public void testTopicReceiveStringDestination() throws Exception {
		doTestReceive(true, false, false, false, false, false, true, JmsTemplate.RECEIVE_TIMEOUT_NO_WAIT);
	}

	@Test
	public void testTopicReceiveDefaultDestinationWithSelector() throws Exception {
		doTestReceive(true, false, true, false, false, true, true, 1000);
	}

	@Test
	public void testTopicReceiveDestinationWithSelector() throws Exception {
		doTestReceive(true, true, false, false, false, true, false, 1000);
	}

	@Test
	public void testTopicReceiveDestinationWithClientAcknowledgeWithSelector() throws Exception {
		doTestReceive(true, true, false, false, true, true, true, JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
	}

	@Test
	public void testTopicReceiveStringDestinationWithSelector() throws Exception {
		doTestReceive(true, false, false, false, false, true, false, JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
	}

	@Test
	public void testTopicReceiveAndConvertDefaultDestination() throws Exception {
		doTestReceive(true, false, true, true, false, false, false, 1000);
	}

	@Test
	public void testTopicReceiveAndConvertStringDestination() throws Exception {
		doTestReceive(true, false, false, true, false, false, true, JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
	}

	@Test
	public void testTopicReceiveAndConvertDestination() throws Exception {
		doTestReceive(true, true, false, true, false, false, true, 1000);
	}

	@Test
	public void testTopicReceiveAndConvertDefaultDestinationWithSelector() throws Exception {
		doTestReceive(true, false, true, true, false, true, true, JmsTemplate.RECEIVE_TIMEOUT_NO_WAIT);
	}

	@Test
	public void testTopicReceiveAndConvertStringDestinationWithSelector() throws Exception {
		doTestReceive(true, false, false, true, false, true, true, JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
	}

	@Test
	public void testTopicReceiveAndConvertDestinationWithSelector() throws Exception {
		doTestReceive(true, true, false, true, false, true, false, 1000);
	}

	private void doTestReceive(
			boolean pubSub,
			boolean explicitDestination, boolean useDefaultDestination, boolean testConverter,
			boolean clientAcknowledge, boolean messageSelector, boolean noLocal, long timeout)
			throws Exception {

		JmsTemplate102 template = createTemplate();
		template.setPubSubDomain(pubSub);
		template.setConnectionFactory(pubSub ? topicConnectionFactory : queueConnectionFactory);

		// Override the default settings for client ack used in the test setup.
		// Can't use Session.getAcknowledgeMode()
		if (pubSub) {
			reset(topicConnection);
			if (clientAcknowledge) {
				template.setSessionAcknowledgeMode(Session.CLIENT_ACKNOWLEDGE);
				given(topicConnection.createTopicSession(
						useTransactedTemplate(), Session.CLIENT_ACKNOWLEDGE)).willReturn(topicSession);
			}
			else {
				template.setSessionAcknowledgeMode(Session.AUTO_ACKNOWLEDGE);
				given(topicConnection.createTopicSession(
						useTransactedTemplate(), Session.AUTO_ACKNOWLEDGE)).willReturn(topicSession);
			}
		}
		else {
			reset(queueConnection);
			if (clientAcknowledge) {
				template.setSessionAcknowledgeMode(Session.CLIENT_ACKNOWLEDGE);
				given(queueConnection.createQueueSession(
						useTransactedTemplate(), Session.CLIENT_ACKNOWLEDGE)).willReturn(queueSession);
			}
			else {
				template.setSessionAcknowledgeMode(Session.AUTO_ACKNOWLEDGE);
				given(queueConnection.createQueueSession(
						useTransactedTemplate(), Session.AUTO_ACKNOWLEDGE)).willReturn(queueSession);
			}
		}

		Destination dest = pubSub ? (Destination) topic : (Destination) queue;

		if (useDefaultDestination) {
			template.setDefaultDestination(dest);
		}
		if (noLocal) {
			template.setPubSubNoLocal(true);
		}
		template.setReceiveTimeout(timeout);


		String selectorString = "selector";
		MessageConsumer messageConsumer = null;

		if (pubSub) {
			TopicSubscriber topicSubscriber = mock(TopicSubscriber.class);
			messageConsumer = topicSubscriber;
			given(topicSession.createSubscriber(topic,
					messageSelector ? selectorString : null, noLocal)).willReturn(topicSubscriber);
		}
		else {
			QueueReceiver queueReceiver = mock(QueueReceiver.class);
			messageConsumer = queueReceiver;
			given(queueSession.createReceiver(queue,
					messageSelector ? selectorString : null)).willReturn(queueReceiver);
		}

		TextMessage textMessage = mock(TextMessage.class);

		if (testConverter) {
			given(textMessage.getText()).willReturn("Hello World!");
		}

		if (timeout == JmsTemplate.RECEIVE_TIMEOUT_NO_WAIT) {
			given(messageConsumer.receiveNoWait()).willReturn(textMessage);
		}
		else if (timeout == JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT) {
			given(messageConsumer.receive()).willReturn(textMessage);
		}
		else {
			given(messageConsumer.receive(timeout)).willReturn(textMessage);
		}

		Message message = null;
		String textFromMessage = null;

		if (useDefaultDestination) {
			if (testConverter) {
				textFromMessage = (String)
						(messageSelector ? template.receiveSelectedAndConvert(selectorString) :
						template.receiveAndConvert());
			}
			else {
				message = (messageSelector ? template.receiveSelected(selectorString) : template.receive());
			}
		}
		else if (explicitDestination) {
			if (testConverter) {
				textFromMessage = (String)
						(messageSelector ? template.receiveSelectedAndConvert(dest, selectorString) :
						template.receiveAndConvert(dest));
			}
			else {
				message = (messageSelector ? template.receiveSelected(dest, selectorString) :
						template.receive(dest));
			}
		}
		else {
			String destinationName = (pubSub ? "testTopic" : "testQueue");
			if (testConverter) {
				textFromMessage = (String)
						(messageSelector ? template.receiveSelectedAndConvert(destinationName, selectorString) :
						template.receiveAndConvert(destinationName));
			}
			else {
				message = (messageSelector ? template.receiveSelected(destinationName, selectorString) :
						template.receive(destinationName));
			}
		}

		if (testConverter) {
			assertEquals("Message text should be equal", "Hello World!", textFromMessage);
		}
		else {
			assertEquals("Messages should refer to the same object", message, textMessage);
		}

		if (pubSub) {
			verify(topicConnection).start();
			verify(topicConnection).close();
			verify(topicSession).close();
		}
		else {
			verify(queueConnection).start();
			verify(queueConnection).close();
			verify(queueSession).close();
		}


		if (useTransactedTemplate()) {
			if (pubSub) {
				verify(topicSession).commit();
			}
			else {
				verify(queueSession).commit();
			}
		}

		if (!useTransactedSession() && clientAcknowledge) {
			verify(textMessage).acknowledge();
		}

		verify(messageConsumer).close();
	}

	@Test
	public void testIllegalStateException() throws Exception {
		doTestJmsException(new javax.jms.IllegalStateException(""), org.springframework.jms.IllegalStateException.class);
	}

	@Test
	public void testInvalidClientIDException() throws Exception {
		doTestJmsException(new javax.jms.InvalidClientIDException(""), InvalidClientIDException.class);
	}

	@Test
	public void testInvalidDestinationException() throws Exception {
		doTestJmsException(new javax.jms.InvalidDestinationException(""), InvalidDestinationException.class);
	}

	@Test
	public void testInvalidSelectorException() throws Exception {
		doTestJmsException(new javax.jms.InvalidSelectorException(""), InvalidSelectorException.class);
	}

	@Test
	public void testJmsSecurityException() throws Exception {
		doTestJmsException(new javax.jms.JMSSecurityException(""), JmsSecurityException.class);
	}

	@Test
	public void testMessageEOFException() throws Exception {
		doTestJmsException(new javax.jms.MessageEOFException(""), MessageEOFException.class);
	}

	@Test
	public void testMessageFormatException() throws Exception {
		doTestJmsException(new javax.jms.MessageFormatException(""), MessageFormatException.class);
	}

	@Test
	public void testMessageNotReadableException() throws Exception {
		doTestJmsException(new javax.jms.MessageNotReadableException(""), MessageNotReadableException.class);
	}

	@Test
	public void testMessageNotWriteableException() throws Exception {
		doTestJmsException(new javax.jms.MessageNotWriteableException(""), MessageNotWriteableException.class);
	}

	@Test
	public void testResourceAllocationException() throws Exception {
		doTestJmsException(new javax.jms.ResourceAllocationException(""), ResourceAllocationException.class);
	}

	@Test
	public void testTransactionInProgressException() throws Exception {
		doTestJmsException(new javax.jms.TransactionInProgressException(""), TransactionInProgressException.class);
	}

	@Test
	public void testTransactionRolledBackException() throws Exception {
		doTestJmsException(new javax.jms.TransactionRolledBackException(""), TransactionRolledBackException.class);
	}

	@Test
	public void testUncategorizedJmsException() throws Exception {
		doTestJmsException(new javax.jms.JMSException(""), UncategorizedJmsException.class);
	}

	protected void doTestJmsException(JMSException original, Class thrownExceptionClass) throws Exception {
		JmsTemplate template = createTemplate();
		template.setConnectionFactory(queueConnectionFactory);
		template.setMessageConverter(new SimpleMessageConverter());
		String s = "Hello world";

		QueueSender queueSender = mock(QueueSender.class);
		TextMessage textMessage = mock(TextMessage.class);

		reset(queueSession);
		given(queueSession.createSender(queue)).willReturn(queueSender);
		given(queueSession.createTextMessage("Hello world")).willReturn(textMessage);

		willThrow(original).given(queueSender).send(textMessage);

		try {
			template.convertAndSend(queue, s);
			fail("Should have thrown JmsException");
		}
		catch (JmsException wrappedEx) {
			// expected
			assertEquals(thrownExceptionClass, wrappedEx.getClass());
			assertEquals(original, wrappedEx.getCause());
		}

		verify(queueSender).close();
		verify(queueSession).close();
		verify(queueConnection).close();
	}

}
