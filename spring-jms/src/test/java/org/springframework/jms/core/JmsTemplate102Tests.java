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

import junit.framework.TestCase;
import org.easymock.MockControl;

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

/**
 * Unit tests for the JmsTemplate implemented using JMS 1.0.2.
 *
 * @author Andre Biryukov
 * @author Mark Pollack
 */
public class JmsTemplate102Tests extends TestCase {

	private Context mockJndiContext;
	private MockControl mockJndiControl;

	private MockControl queueConnectionFactoryControl;
	private QueueConnectionFactory mockQueueConnectionFactory;

	private MockControl queueConnectionControl;
	private QueueConnection mockQueueConnection;

	private MockControl queueSessionControl;
	private QueueSession mockQueueSession;

	private MockControl queueControl;
	private Queue mockQueue;

	private MockControl topicConnectionFactoryControl;
	private TopicConnectionFactory mockTopicConnectionFactory;

	private MockControl topicConnectionControl;
	private TopicConnection mockTopicConnection;

	private MockControl topicSessionControl;
	private TopicSession mockTopicSession;

	private MockControl topicControl;
	private Topic mockTopic;

	private int deliveryMode = DeliveryMode.PERSISTENT;
	private int priority = 9;
	private int timeToLive = 10000;


	/**
	 * Create the mock objects for testing.
	 */
	protected void setUp() throws Exception {
		mockJndiControl = MockControl.createControl(Context.class);
		mockJndiContext = (Context) this.mockJndiControl.getMock();

		createMockForQueues();
		createMockForTopics();

		mockJndiContext.close();
		mockJndiControl.replay();
	}

	private void createMockForTopics() throws JMSException, NamingException {
		topicConnectionFactoryControl = MockControl.createControl(TopicConnectionFactory.class);
		mockTopicConnectionFactory = (TopicConnectionFactory) topicConnectionFactoryControl.getMock();

		topicConnectionControl = MockControl.createControl(TopicConnection.class);
		mockTopicConnection = (TopicConnection) topicConnectionControl.getMock();

		topicControl = MockControl.createControl(Topic.class);
		mockTopic = (Topic) topicControl.getMock();

		topicSessionControl = MockControl.createControl(TopicSession.class);
		mockTopicSession = (TopicSession) topicSessionControl.getMock();

		mockTopicConnectionFactory.createTopicConnection();
		topicConnectionFactoryControl.setReturnValue(mockTopicConnection);
		topicConnectionFactoryControl.replay();

		mockTopicConnection.createTopicSession(useTransactedTemplate(), Session.AUTO_ACKNOWLEDGE);
		topicConnectionControl.setReturnValue(mockTopicSession);
		mockTopicSession.getTransacted();
		topicSessionControl.setReturnValue(useTransactedSession());

		mockJndiContext.lookup("testTopic");
		mockJndiControl.setReturnValue(mockTopic);
	}

	private void createMockForQueues() throws JMSException, NamingException {
		queueConnectionFactoryControl = MockControl.createControl(QueueConnectionFactory.class);
		mockQueueConnectionFactory = (QueueConnectionFactory) queueConnectionFactoryControl.getMock();

		queueConnectionControl = MockControl.createControl(QueueConnection.class);
		mockQueueConnection = (QueueConnection) queueConnectionControl.getMock();

		queueControl = MockControl.createControl(Queue.class);
		mockQueue = (Queue) queueControl.getMock();

		queueSessionControl = MockControl.createControl(QueueSession.class);
		mockQueueSession = (QueueSession) queueSessionControl.getMock();

		mockQueueConnectionFactory.createQueueConnection();
		queueConnectionFactoryControl.setReturnValue(mockQueueConnection);
		queueConnectionFactoryControl.replay();

		mockQueueConnection.createQueueSession(useTransactedTemplate(), Session.AUTO_ACKNOWLEDGE);
		queueConnectionControl.setReturnValue(mockQueueSession);
		mockQueueSession.getTransacted();
		queueSessionControl.setReturnValue(useTransactedSession());

		mockJndiContext.lookup("testQueue");
		mockJndiControl.setReturnValue(mockQueue);
	}

	private JmsTemplate102 createTemplate() {
		JmsTemplate102 template = new JmsTemplate102();
		JndiDestinationResolver destMan = new JndiDestinationResolver();
		destMan.setJndiTemplate(new JndiTemplate() {
			protected Context createInitialContext() {
				return mockJndiContext;
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


	public void testTopicSessionCallback() throws Exception {
		JmsTemplate102 template = createTemplate();
		template.setPubSubDomain(true);
		template.setConnectionFactory(mockTopicConnectionFactory);
		template.afterPropertiesSet();

		mockTopicSession.close();
		topicSessionControl.setVoidCallable(1);

		mockTopicConnection.close();
		topicConnectionControl.setVoidCallable(1);

		topicSessionControl.replay();
		topicConnectionControl.replay();

		template.execute(new SessionCallback() {
			public Object doInJms(Session session) throws JMSException {
				boolean b = session.getTransacted();
				return null;
			}
		});

		topicConnectionFactoryControl.verify();
		topicConnectionControl.verify();
		topicSessionControl.verify();
	}

	/**
	 * Test the execute(ProducerCallback) using a topic.
	 */
	public void testTopicProducerCallback() throws Exception {
		JmsTemplate102 template = createTemplate();
		template.setPubSubDomain(true);
		template.setConnectionFactory(mockTopicConnectionFactory);
		template.afterPropertiesSet();

		MockControl topicPublisherControl = MockControl.createControl(TopicPublisher.class);
		TopicPublisher mockTopicPublisher = (TopicPublisher) topicPublisherControl.getMock();

		mockTopicSession.createPublisher(null);
		topicSessionControl.setReturnValue(mockTopicPublisher);

		mockTopicPublisher.getPriority();
		topicPublisherControl.setReturnValue(4);

		mockTopicPublisher.close();
		topicPublisherControl.setVoidCallable(1);
		mockTopicSession.close();
		topicSessionControl.setVoidCallable(1);
		mockTopicConnection.close();
		topicConnectionControl.setVoidCallable(1);

		topicPublisherControl.replay();
		topicSessionControl.replay();
		topicConnectionControl.replay();

		template.execute(new ProducerCallback() {
			public Object doInJms(Session session, MessageProducer producer) throws JMSException {
				boolean b = session.getTransacted();
				int i = producer.getPriority();
				return null;
			}
		});

		topicConnectionFactoryControl.verify();
		topicConnectionControl.verify();
		topicSessionControl.verify();
	}

	/**
	 * Test the execute(ProducerCallback) using a topic.
	 */
	public void testTopicProducerCallbackWithIdAndTimestampDisabled() throws Exception {
		JmsTemplate102 template = createTemplate();
		template.setPubSubDomain(true);
		template.setConnectionFactory(mockTopicConnectionFactory);
		template.setMessageIdEnabled(false);
		template.setMessageTimestampEnabled(false);
		template.afterPropertiesSet();

		MockControl topicPublisherControl = MockControl.createControl(TopicPublisher.class);
		TopicPublisher mockTopicPublisher = (TopicPublisher) topicPublisherControl.getMock();

		mockTopicSession.createPublisher(null);
		topicSessionControl.setReturnValue(mockTopicPublisher);

		mockTopicPublisher.setDisableMessageID(true);
		topicPublisherControl.setVoidCallable(1);
		mockTopicPublisher.setDisableMessageTimestamp(true);
		topicPublisherControl.setVoidCallable(1);
		mockTopicPublisher.getPriority();
		topicPublisherControl.setReturnValue(4);

		mockTopicPublisher.close();
		topicPublisherControl.setVoidCallable(1);
		mockTopicSession.close();
		topicSessionControl.setVoidCallable(1);
		mockTopicConnection.close();
		topicConnectionControl.setVoidCallable(1);

		topicPublisherControl.replay();
		topicSessionControl.replay();
		topicConnectionControl.replay();

		template.execute(new ProducerCallback() {
			public Object doInJms(Session session, MessageProducer producer) throws JMSException {
				boolean b = session.getTransacted();
				int i = producer.getPriority();
				return null;
			}
		});

		topicConnectionFactoryControl.verify();
		topicConnectionControl.verify();
		topicSessionControl.verify();
	}

	/**
	 * Test the method execute(SessionCallback action) with using the
	 * point to point domain as specified by the value of isPubSubDomain = false.
	 */
	public void testQueueSessionCallback() throws Exception {
		JmsTemplate102 template = createTemplate();
		// Point-to-Point (queues) are the default domain
		template.setConnectionFactory(mockQueueConnectionFactory);
		template.afterPropertiesSet();

		mockQueueSession.close();
		queueSessionControl.setVoidCallable(1);

		mockQueueConnection.close();
		queueConnectionControl.setVoidCallable(1);

		queueSessionControl.replay();
		queueConnectionControl.replay();

		template.execute(new SessionCallback() {
			public Object doInJms(Session session) throws JMSException {
				boolean b = session.getTransacted();
				return null;
			}
		});

		queueConnectionFactoryControl.verify();
		queueConnectionControl.verify();
		queueSessionControl.verify();
	}

	/**
	 * Test the method execute(ProducerCallback) with a Queue.
	 */
	public void testQueueProducerCallback() throws Exception {
		JmsTemplate102 template = createTemplate();
		// Point-to-Point (queues) are the default domain.
		template.setConnectionFactory(mockQueueConnectionFactory);
		template.afterPropertiesSet();

		MockControl queueSenderControl = MockControl.createControl(QueueSender.class);
		QueueSender mockQueueSender = (QueueSender) queueSenderControl.getMock();

		mockQueueSession.createSender(null);
		queueSessionControl.setReturnValue(mockQueueSender);

		mockQueueSender.getPriority();
		queueSenderControl.setReturnValue(4);

		mockQueueSender.close();
		queueSenderControl.setVoidCallable(1);
		mockQueueSession.close();
		queueSessionControl.setVoidCallable(1);
		mockQueueConnection.close();
		queueConnectionControl.setVoidCallable(1);

		queueSenderControl.replay();
		queueSessionControl.replay();
		queueConnectionControl.replay();

		template.execute(new ProducerCallback() {
			public Object doInJms(Session session, MessageProducer producer)
				throws JMSException {
				boolean b = session.getTransacted();
				int i = producer.getPriority();
				return null;
			}
		});

		queueConnectionFactoryControl.verify();
		queueConnectionControl.verify();
		queueSessionControl.verify();
	}

	public void testQueueProducerCallbackWithIdAndTimestampDisabled() throws Exception {
		JmsTemplate102 template = createTemplate();
		// Point-to-Point (queues) are the default domain.
		template.setConnectionFactory(mockQueueConnectionFactory);
		template.setMessageIdEnabled(false);
		template.setMessageTimestampEnabled(false);
		template.afterPropertiesSet();

		MockControl queueSenderControl = MockControl.createControl(QueueSender.class);
		QueueSender mockQueueSender = (QueueSender) queueSenderControl.getMock();

		mockQueueSession.createSender(null);
		queueSessionControl.setReturnValue(mockQueueSender);

		mockQueueSender.setDisableMessageID(true);
		queueSenderControl.setVoidCallable(1);
		mockQueueSender.setDisableMessageTimestamp(true);
		queueSenderControl.setVoidCallable(1);
		mockQueueSender.getPriority();
		queueSenderControl.setReturnValue(4);

		mockQueueSender.close();
		queueSenderControl.setVoidCallable(1);
		mockQueueSession.close();
		queueSessionControl.setVoidCallable(1);
		mockQueueConnection.close();
		queueConnectionControl.setVoidCallable(1);

		queueSenderControl.replay();
		queueSessionControl.replay();
		queueConnectionControl.replay();

		template.execute(new ProducerCallback() {
			public Object doInJms(Session session, MessageProducer producer) throws JMSException {
				boolean b = session.getTransacted();
				int i = producer.getPriority();
				return null;
			}
		});

		queueConnectionFactoryControl.verify();
		queueConnectionControl.verify();
		queueSessionControl.verify();
	}

	/**
	 * Test the setting of the JmsTemplate properties.
	 */
	public void testBeanProperties() throws Exception {
		JmsTemplate102 template = createTemplate();
		template.setConnectionFactory(mockQueueConnectionFactory);

		assertTrue("connection factory ok", template.getConnectionFactory() == mockQueueConnectionFactory);

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
		s102.setConnectionFactory(mockTopicConnectionFactory);
		try {
			s102.afterPropertiesSet();
			fail("IllegalArgumentException not thrown. Mismatch of Destination and ConnectionFactory types.");
		}
		catch (IllegalArgumentException ex) {
			// expected
		}

		s102 = createTemplate();
		s102.setConnectionFactory(mockQueueConnectionFactory);
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
	public void testSendStringQueue() throws Exception {
		sendQueue(true, false, false, true);
	}

	/**
	 * Test the method send(String destination, MessageCreator c) when
	 * explicit QOS parameters are enabled, using a queue.
	 */
	public void testSendStringQueueWithQOS() throws Exception {
		sendQueue(false, false, false, false);
	}

	/**
	 * Test the method send(MessageCreator c) using default QOS values.
	 */
	public void testSendDefaultDestinationQueue() throws Exception {
		sendQueue(true, false, true, true);
	}

	/**
	 * Test the method send(MessageCreator c) using explicit QOS values.
	 */
	public void testSendDefaultDestinationQueueWithQOS() throws Exception {
		sendQueue(false, false, true, false);
	}

	/**
	 * Test the method send(String destination, MessageCreator c) using
	 * a topic and default QOS values.
	 */
	public void testSendStringTopic() throws Exception {
		sendTopic(true, false);
	}

	/**
	 * Test the method send(String destination, MessageCreator c) using explicit
	 * QOS values.
	 */
	public void testSendStringTopicWithQOS() throws Exception {
		sendTopic(false, false);
	}

	/**
	 * Test the method send(Destination queue, MessgaeCreator c) using
	 * a queue and default QOS values.
	 */
	public void testSendQueue() throws Exception {
		sendQueue(true, false, false, true);
	}

	/**
	 * Test the method send(Destination queue, MessageCreator c) sing explicit
	 * QOS values.
	 */
	public void testSendQueueWithQOS() throws Exception {
		sendQueue(false, false, false, false);
	}

	/**
	 * Test the method send(Destination queue, MessgaeCreator c) using
	 * a topic and default QOS values.
	 */
	public void testSendTopic() throws Exception {
		sendTopic(true, false);
	}

	/**
	 * Test the method send(Destination queue, MessageCreator c) using explicity
	 * QOS values.
	 */
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
		template.setConnectionFactory(mockQueueConnectionFactory);
		template.afterPropertiesSet();

		if (useDefaultDestination) {
			template.setDefaultDestination(mockQueue);
		}
		if (disableIdAndTimestamp) {
			template.setMessageIdEnabled(false);
			template.setMessageTimestampEnabled(false);
		}

		MockControl queueSenderControl = MockControl.createControl(QueueSender.class);
		QueueSender mockQueueSender = (QueueSender) queueSenderControl.getMock();

		MockControl messageControl = MockControl.createControl(TextMessage.class);
		TextMessage mockMessage = (TextMessage) messageControl.getMock();

		if (disableIdAndTimestamp) {
			mockQueueSender.setDisableMessageID(true);
			queueSenderControl.setVoidCallable(1);
			mockQueueSender.setDisableMessageTimestamp(true);
			queueSenderControl.setVoidCallable(1);
		}

		mockQueueSession.createSender(this.mockQueue);
		queueSessionControl.setReturnValue(mockQueueSender);
		mockQueueSession.createTextMessage("just testing");
		queueSessionControl.setReturnValue(mockMessage);

		if (useTransactedTemplate()) {
			mockQueueSession.commit();
			queueSessionControl.setVoidCallable(1);
		}

		if (ignoreQOS) {
			mockQueueSender.send(mockMessage);
		}
		else {
			template.setExplicitQosEnabled(true);
			template.setDeliveryMode(deliveryMode);
			template.setPriority(priority);
			template.setTimeToLive(timeToLive);
			mockQueueSender.send(mockMessage, deliveryMode, priority, timeToLive);
		}
		queueSenderControl.setVoidCallable(1);

		mockQueueSender.close();
		queueSenderControl.setVoidCallable(1);
		mockQueueSession.close();
		queueSessionControl.setVoidCallable(1);
		mockQueueConnection.close();
		queueConnectionControl.setVoidCallable(1);

		queueSenderControl.replay();
		queueSessionControl.replay();
		queueConnectionControl.replay();

		if (useDefaultDestination) {
			template.send(new MessageCreator() {
				public Message createMessage(Session session) throws JMSException {
					return session.createTextMessage("just testing");
				}
			});
		}
		else {
			if (explicitQueue) {
				template.send(mockQueue, new MessageCreator() {
					public Message createMessage(Session session) throws JMSException {
						return session.createTextMessage("just testing");
					}
				});
			}
			else {
				template.send("testQueue", new MessageCreator() {
					public Message createMessage(Session session)
						throws JMSException {
						return session.createTextMessage("just testing");
					}
				});
			}
		}

		queueConnectionFactoryControl.verify();
		queueConnectionControl.verify();
		queueSessionControl.verify();
		queueSenderControl.verify();
	}

	private void sendTopic(boolean ignoreQOS, boolean explicitTopic) throws Exception {
		JmsTemplate102 template = createTemplate();
		template.setPubSubDomain(true);
		template.setConnectionFactory(mockTopicConnectionFactory);
		template.afterPropertiesSet();

		MockControl topicPublisherControl = MockControl.createControl(TopicPublisher.class);
		TopicPublisher mockTopicPublisher = (TopicPublisher) topicPublisherControl.getMock();

		MockControl messageControl = MockControl.createControl(TextMessage.class);
		TextMessage mockMessage = (TextMessage) messageControl.getMock();

		mockTopicSession.createPublisher(this.mockTopic);
		topicSessionControl.setReturnValue(mockTopicPublisher);
		mockTopicSession.createTextMessage("just testing");
		topicSessionControl.setReturnValue(mockMessage);

		if (useTransactedTemplate()) {
			mockTopicSession.commit();
			topicSessionControl.setVoidCallable(1);
		}

		mockTopicPublisher.close();
		topicPublisherControl.setVoidCallable(1);
		mockTopicSession.close();
		topicSessionControl.setVoidCallable(1);
		mockTopicConnection.close();
		topicConnectionControl.setVoidCallable(1);


		topicSessionControl.replay();
		topicConnectionControl.replay();

		if (ignoreQOS) {
			mockTopicPublisher.publish(mockMessage);
		}
		else {
			template.setExplicitQosEnabled(true);
			template.setDeliveryMode(deliveryMode);
			template.setPriority(priority);
			template.setTimeToLive(timeToLive);
			mockTopicPublisher.publish(mockMessage, deliveryMode, priority, timeToLive);
		}
		topicPublisherControl.replay();

		template.setPubSubDomain(true);

		if (explicitTopic) {
			template.send(mockTopic, new MessageCreator() {
				public Message createMessage(Session session)
					throws JMSException {
					return session.createTextMessage("just testing");
				}
			});
		}
		else {
			template.send("testTopic", new MessageCreator() {
				public Message createMessage(Session session)
					throws JMSException {
					return session.createTextMessage("just testing");
				}
			});
		}

		topicConnectionFactoryControl.verify();
		topicConnectionControl.verify();
		topicSessionControl.verify();
		topicPublisherControl.verify();
	}

	public void testConverter() throws Exception {
		JmsTemplate102 template = createTemplate();
		template.setConnectionFactory(mockQueueConnectionFactory);
		template.setMessageConverter(new SimpleMessageConverter());
		String s = "Hello world";

		MockControl queueSenderControl = MockControl.createControl(QueueSender.class);
		QueueSender mockQueueSender = (QueueSender) queueSenderControl.getMock();
		MockControl messageControl = MockControl.createControl(TextMessage.class);
		TextMessage mockMessage = (TextMessage) messageControl.getMock();

		mockQueueSession.createSender(this.mockQueue);
		queueSessionControl.setReturnValue(mockQueueSender);
		mockQueueSession.createTextMessage("Hello world");
		queueSessionControl.setReturnValue(mockMessage);

		if (useTransactedTemplate()) {
			mockQueueSession.commit();
			queueSessionControl.setVoidCallable(1);
		}

		mockQueueSender.send(mockMessage);
		queueSenderControl.setVoidCallable(1);

		mockQueueSender.close();
		queueSenderControl.setVoidCallable(1);
		mockQueueSession.close();
		queueSessionControl.setVoidCallable(1);
		mockQueueConnection.close();
		queueConnectionControl.setVoidCallable(1);

		queueSenderControl.replay();
		queueSessionControl.replay();
		queueConnectionControl.replay();

		template.convertAndSend(mockQueue, s);

		queueConnectionFactoryControl.verify();
		queueConnectionControl.verify();
		queueSessionControl.verify();
		queueSenderControl.verify();
	}

	public void testQueueReceiveDefaultDestination() throws Exception {
		doTestReceive(false, false, true, false, false, false, false, JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
	}

	public void testQueueReceiveDestination() throws Exception {
		doTestReceive(false, true, false, false, false, false, true, JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
	}

	public void testQueueReceiveDestinationWithClientAcknowledge() throws Exception {
		doTestReceive(false, true, false, false, true, false, false, 1000);
	}

	public void testQueueReceiveStringDestination() throws Exception {
		doTestReceive(false, false, false, false, false, false, true, JmsTemplate.RECEIVE_TIMEOUT_NO_WAIT);
	}

	public void testQueueReceiveDefaultDestinationWithSelector() throws Exception {
		doTestReceive(false, false, true, false, false, true, true, 1000);
	}

	public void testQueueReceiveDestinationWithSelector() throws Exception {
		doTestReceive(false, true, false, false, false, true, false, JmsTemplate.RECEIVE_TIMEOUT_NO_WAIT);
	}

	public void testQueueReceiveDestinationWithClientAcknowledgeWithSelector() throws Exception {
		doTestReceive(false, true, false, false, true, true, true, JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
	}

	public void testQueueReceiveStringDestinationWithSelector() throws Exception {
		doTestReceive(false, false, false, false, false, true, false, JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
	}

	public void testQueueReceiveAndConvertDefaultDestination() throws Exception {
		doTestReceive(false, false, true, true, false, false, false, 1000);
	}

	public void testQueueReceiveAndConvertStringDestination() throws Exception {
		doTestReceive(false, false, false, true, false, false, true, JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
	}

	public void testQueueReceiveAndConvertDestination() throws Exception {
		doTestReceive(false, true, false, true, false, false, true, 1000);
	}

	public void testQueueReceiveAndConvertDefaultDestinationWithSelector() throws Exception {
		doTestReceive(false, false, true, true, false, true, true, JmsTemplate.RECEIVE_TIMEOUT_NO_WAIT);
	}

	public void testQueueReceiveAndConvertStringDestinationWithSelector() throws Exception {
		doTestReceive(false, false, false, true, false, true, true, JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
	}

	public void testQueueReceiveAndConvertDestinationWithSelector() throws Exception {
		doTestReceive(false, true, false, true, false, true, false, 1000);
	}

	public void testTopicReceiveDefaultDestination() throws Exception {
		doTestReceive(true, false, true, false, false, false, false, JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
	}

	public void testTopicReceiveDestination() throws Exception {
		doTestReceive(true, true, false, false, false, false, true, JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
	}

	public void testTopicReceiveDestinationWithClientAcknowledge() throws Exception {
		doTestReceive(true, true, false, false, true, false, false, 1000);
	}

	public void testTopicReceiveStringDestination() throws Exception {
		doTestReceive(true, false, false, false, false, false, true, JmsTemplate.RECEIVE_TIMEOUT_NO_WAIT);
	}

	public void testTopicReceiveDefaultDestinationWithSelector() throws Exception {
		doTestReceive(true, false, true, false, false, true, true, 1000);
	}

	public void testTopicReceiveDestinationWithSelector() throws Exception {
		doTestReceive(true, true, false, false, false, true, false, 1000);
	}

	public void testTopicReceiveDestinationWithClientAcknowledgeWithSelector() throws Exception {
		doTestReceive(true, true, false, false, true, true, true, JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
	}

	public void testTopicReceiveStringDestinationWithSelector() throws Exception {
		doTestReceive(true, false, false, false, false, true, false, JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
	}

	public void testTopicReceiveAndConvertDefaultDestination() throws Exception {
		doTestReceive(true, false, true, true, false, false, false, 1000);
	}

	public void testTopicReceiveAndConvertStringDestination() throws Exception {
		doTestReceive(true, false, false, true, false, false, true, JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
	}

	public void testTopicReceiveAndConvertDestination() throws Exception {
		doTestReceive(true, true, false, true, false, false, true, 1000);
	}

	public void testTopicReceiveAndConvertDefaultDestinationWithSelector() throws Exception {
		doTestReceive(true, false, true, true, false, true, true, JmsTemplate.RECEIVE_TIMEOUT_NO_WAIT);
	}

	public void testTopicReceiveAndConvertStringDestinationWithSelector() throws Exception {
		doTestReceive(true, false, false, true, false, true, true, JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
	}

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
		if (pubSub) {
			template.setConnectionFactory(mockTopicConnectionFactory);
		}
		else {
			template.setConnectionFactory(mockQueueConnectionFactory);
		}

		// Override the default settings for client ack used in the test setup.
		// Can't use Session.getAcknowledgeMode()
		if (pubSub) {
			topicConnectionControl.reset();
			if (clientAcknowledge) {
				template.setSessionAcknowledgeMode(Session.CLIENT_ACKNOWLEDGE);
				mockTopicConnection.createTopicSession(useTransactedTemplate(), Session.CLIENT_ACKNOWLEDGE);
			}
			else {
				template.setSessionAcknowledgeMode(Session.AUTO_ACKNOWLEDGE);
				mockTopicConnection.createTopicSession(useTransactedTemplate(), Session.AUTO_ACKNOWLEDGE);
			}
			topicConnectionControl.setReturnValue(mockTopicSession);
		}
		else {
			queueConnectionControl.reset();
			if (clientAcknowledge) {
				template.setSessionAcknowledgeMode(Session.CLIENT_ACKNOWLEDGE);
				mockQueueConnection.createQueueSession(useTransactedTemplate(), Session.CLIENT_ACKNOWLEDGE);
			}
			else {
				template.setSessionAcknowledgeMode(Session.AUTO_ACKNOWLEDGE);
				mockQueueConnection.createQueueSession(useTransactedTemplate(), Session.AUTO_ACKNOWLEDGE);
			}
			queueConnectionControl.setReturnValue(mockQueueSession);
		}

		Destination dest = pubSub ? (Destination) mockTopic : (Destination) mockQueue;

		if (useDefaultDestination) {
			template.setDefaultDestination(dest);
		}
		if (noLocal) {
			template.setPubSubNoLocal(true);
		}
		template.setReceiveTimeout(timeout);

		if (pubSub) {
			mockTopicConnection.start();
			topicConnectionControl.setVoidCallable(1);
			mockTopicConnection.close();
			topicConnectionControl.setVoidCallable(1);
		}
		else {
			mockQueueConnection.start();
			queueConnectionControl.setVoidCallable(1);
			mockQueueConnection.close();
			queueConnectionControl.setVoidCallable(1);
		}

		String selectorString = "selector";
		MockControl messageConsumerControl = null;
		MessageConsumer mockMessageConsumer = null;

		if (pubSub) {
			messageConsumerControl = MockControl.createControl(TopicSubscriber.class);
			TopicSubscriber mockTopicSubscriber = (TopicSubscriber) messageConsumerControl.getMock();
			mockMessageConsumer = mockTopicSubscriber;
			mockTopicSession.createSubscriber(mockTopic, messageSelector ? selectorString : null, noLocal);
			topicSessionControl.setReturnValue(mockTopicSubscriber);
		}
		else {
			messageConsumerControl = MockControl.createControl(QueueReceiver.class);
			QueueReceiver mockQueueReceiver = (QueueReceiver) messageConsumerControl.getMock();
			mockMessageConsumer = mockQueueReceiver;
			mockQueueSession.createReceiver(mockQueue, messageSelector ? selectorString : null);
			queueSessionControl.setReturnValue(mockQueueReceiver);
		}

		if (useTransactedTemplate()) {
			if (pubSub) {
				mockTopicSession.commit();
				topicSessionControl.setVoidCallable(1);
			}
			else {
				mockQueueSession.commit();
				queueSessionControl.setVoidCallable(1);
			}
		}

		if (pubSub) {
			mockTopicSession.close();
			topicSessionControl.setVoidCallable(1);
		}
		else {
			mockQueueSession.close();
			queueSessionControl.setVoidCallable(1);
		}

		MockControl messageControl = MockControl.createControl(TextMessage.class);
		TextMessage mockMessage = (TextMessage) messageControl.getMock();

		if (testConverter) {
			mockMessage.getText();
			messageControl.setReturnValue("Hello World!");
		}
		if (!useTransactedSession() && clientAcknowledge) {
			mockMessage.acknowledge();
			messageControl.setVoidCallable(1);
		}

		if (pubSub) {
			topicSessionControl.replay();
			topicConnectionControl.replay();
		}
		else {
			queueSessionControl.replay();
			queueConnectionControl.replay();
		}
		messageControl.replay();

		if (timeout == JmsTemplate.RECEIVE_TIMEOUT_NO_WAIT) {
			mockMessageConsumer.receiveNoWait();
		}
		else if (timeout == JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT) {
			mockMessageConsumer.receive();
		}
		else {
			mockMessageConsumer.receive(timeout);
		}
		messageConsumerControl.setReturnValue(mockMessage);
		mockMessageConsumer.close();
		messageConsumerControl.setVoidCallable(1);

		messageConsumerControl.replay();

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

		if (pubSub) {
			topicConnectionFactoryControl.verify();
			topicConnectionControl.verify();
			topicSessionControl.verify();
		}
		else {
			queueConnectionFactoryControl.verify();
			queueConnectionControl.verify();
			queueSessionControl.verify();
		}
		messageConsumerControl.verify();
		messageControl.verify();

		if (testConverter) {
			assertEquals("Message text should be equal", "Hello World!", textFromMessage);
		}
		else {
			assertEquals("Messages should refer to the same object", message, mockMessage);
		}
	}

	public void testIllegalStateException() throws Exception {
		doTestJmsException(new javax.jms.IllegalStateException(""), org.springframework.jms.IllegalStateException.class);
	}

	public void testInvalidClientIDException() throws Exception {
		doTestJmsException(new javax.jms.InvalidClientIDException(""), InvalidClientIDException.class);
	}

	public void testInvalidDestinationException() throws Exception {
		doTestJmsException(new javax.jms.InvalidDestinationException(""), InvalidDestinationException.class);
	}

	public void testInvalidSelectorException() throws Exception {
		doTestJmsException(new javax.jms.InvalidSelectorException(""), InvalidSelectorException.class);
	}

	public void testJmsSecurityException() throws Exception {
		doTestJmsException(new javax.jms.JMSSecurityException(""), JmsSecurityException.class);
	}

	public void testMessageEOFException() throws Exception {
		doTestJmsException(new javax.jms.MessageEOFException(""), MessageEOFException.class);
	}

	public void testMessageFormatException() throws Exception {
		doTestJmsException(new javax.jms.MessageFormatException(""), MessageFormatException.class);
	}

	public void testMessageNotReadableException() throws Exception {
		doTestJmsException(new javax.jms.MessageNotReadableException(""), MessageNotReadableException.class);
	}

	public void testMessageNotWriteableException() throws Exception {
		doTestJmsException(new javax.jms.MessageNotWriteableException(""), MessageNotWriteableException.class);
	}

	public void testResourceAllocationException() throws Exception {
		doTestJmsException(new javax.jms.ResourceAllocationException(""), ResourceAllocationException.class);
	}

	public void testTransactionInProgressException() throws Exception {
		doTestJmsException(new javax.jms.TransactionInProgressException(""), TransactionInProgressException.class);
	}

	public void testTransactionRolledBackException() throws Exception {
		doTestJmsException(new javax.jms.TransactionRolledBackException(""), TransactionRolledBackException.class);
	}

	public void testUncategorizedJmsException() throws Exception {
		doTestJmsException(new javax.jms.JMSException(""), UncategorizedJmsException.class);
	}

	protected void doTestJmsException(JMSException original, Class thrownExceptionClass) throws Exception {
		JmsTemplate template = createTemplate();
		template.setConnectionFactory(mockQueueConnectionFactory);
		template.setMessageConverter(new SimpleMessageConverter());
		String s = "Hello world";

		MockControl queueSenderControl = MockControl.createControl(QueueSender.class);
		QueueSender mockQueueSender = (QueueSender) queueSenderControl.getMock();
		MockControl messageControl = MockControl.createControl(TextMessage.class);
		TextMessage mockMessage = (TextMessage) messageControl.getMock();

		queueSessionControl.reset();
		mockQueueSession.createSender(mockQueue);
		queueSessionControl.setReturnValue(mockQueueSender);
		mockQueueSession.createTextMessage("Hello world");
		queueSessionControl.setReturnValue(mockMessage);

		mockQueueSender.send(mockMessage);
		queueSenderControl.setThrowable(original, 1);
		mockQueueSender.close();
		queueSenderControl.setVoidCallable(1);

		mockQueueSession.close();
		queueSessionControl.setVoidCallable(1);
		mockQueueConnection.close();
		queueConnectionControl.setVoidCallable(1);

		queueSenderControl.replay();
		queueSessionControl.replay();
		queueConnectionControl.replay();

		try {
			template.convertAndSend(mockQueue, s);
			fail("Should have thrown JmsException");
		}
		catch (JmsException wrappedEx) {
			// expected
			assertEquals(thrownExceptionClass, wrappedEx.getClass());
			assertEquals(original, wrappedEx.getCause());
		}

		queueSenderControl.verify();
		queueSessionControl.verify();
		queueConnectionControl.verify();
		queueConnectionFactoryControl.verify();
	}

}
