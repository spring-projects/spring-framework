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

package org.springframework.jms.core;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
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
import org.springframework.jms.connection.ConnectionFactoryUtils;
import org.springframework.jms.connection.SingleConnectionFactory;
import org.springframework.jms.connection.TransactionAwareConnectionFactoryProxy;
import org.springframework.jms.support.JmsUtils;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.jms.support.destination.JndiDestinationResolver;
import org.springframework.jndi.JndiTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Unit tests for the JmsTemplate implemented using JMS 1.1.
 *
 * @author Andre Biryukov
 * @author Mark Pollack
 */
public class JmsTemplateTests extends TestCase {

	private Context mockJndiContext;
	private MockControl mockJndiControl;

	private MockControl connectionFactoryControl;
	private ConnectionFactory mockConnectionFactory;

	private MockControl connectionControl;
	private Connection mockConnection;

	private MockControl sessionControl;
	private Session mockSession;

	private MockControl queueControl;
	private Destination mockQueue;

	private int deliveryMode = DeliveryMode.PERSISTENT;
	private int priority = 9;
	private int timeToLive = 10000;


	/**
	 * Create the mock objects for testing.
	 */
	@Override
	protected void setUp() throws Exception {
		mockJndiControl = MockControl.createControl(Context.class);
		mockJndiContext = (Context) this.mockJndiControl.getMock();

		createMockforDestination();

		mockJndiContext.close();
		mockJndiControl.replay();
	}

	private void createMockforDestination() throws JMSException, NamingException {
		connectionFactoryControl = MockControl.createControl(ConnectionFactory.class);
		mockConnectionFactory = (ConnectionFactory) connectionFactoryControl.getMock();

		connectionControl = MockControl.createControl(Connection.class);
		mockConnection = (Connection) connectionControl.getMock();

		sessionControl = MockControl.createControl(Session.class);
		mockSession = (Session) sessionControl.getMock();

		queueControl = MockControl.createControl(Queue.class);
		mockQueue = (Queue) queueControl.getMock();

		mockConnectionFactory.createConnection();
		connectionFactoryControl.setReturnValue(mockConnection);
		connectionFactoryControl.replay();

		mockConnection.createSession(useTransactedTemplate(), Session.AUTO_ACKNOWLEDGE);
		connectionControl.setReturnValue(mockSession);
		mockSession.getTransacted();
		sessionControl.setReturnValue(useTransactedSession());

		mockJndiContext.lookup("testDestination");
		mockJndiControl.setReturnValue(mockQueue);
	}

	private JmsTemplate createTemplate() {
		JmsTemplate template = new JmsTemplate();
		JndiDestinationResolver destMan = new JndiDestinationResolver();
		destMan.setJndiTemplate(new JndiTemplate() {
			@Override
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


	public void testExceptionStackTrace() {
		JMSException jmsEx = new JMSException("could not connect");
		Exception innerEx = new Exception("host not found");
		jmsEx.setLinkedException(innerEx);
		JmsException springJmsEx = JmsUtils.convertJmsAccessException(jmsEx);
		StringWriter sw = new StringWriter();
		PrintWriter out = new PrintWriter(sw);
		springJmsEx.printStackTrace(out);
		String trace = sw.toString();
		assertTrue("inner jms exception not found", trace.indexOf("host not found") > 0);
	}

	public void testProducerCallback() throws Exception {
		JmsTemplate template = createTemplate();
		template.setConnectionFactory(mockConnectionFactory);

		MockControl messageProducerControl = MockControl.createControl(MessageProducer.class);
		MessageProducer mockMessageProducer = (MessageProducer) messageProducerControl.getMock();

		mockSession.createProducer(null);
		sessionControl.setReturnValue(mockMessageProducer);

		mockMessageProducer.getPriority();
		messageProducerControl.setReturnValue(4);

		mockMessageProducer.close();
		messageProducerControl.setVoidCallable(1);
		mockSession.close();
		sessionControl.setVoidCallable(1);
		mockConnection.close();
		connectionControl.setVoidCallable(1);

		messageProducerControl.replay();
		sessionControl.replay();
		connectionControl.replay();

		template.execute(new ProducerCallback() {
			@Override
			public Object doInJms(Session session, MessageProducer producer) throws JMSException {
				boolean b = session.getTransacted();
				int i = producer.getPriority();
				return null;
			}
		});

		connectionFactoryControl.verify();
		connectionControl.verify();
		sessionControl.verify();
	}

	public void testProducerCallbackWithIdAndTimestampDisabled() throws Exception {
		JmsTemplate template = createTemplate();
		template.setConnectionFactory(mockConnectionFactory);
		template.setMessageIdEnabled(false);
		template.setMessageTimestampEnabled(false);

		MockControl messageProducerControl = MockControl.createControl(MessageProducer.class);
		MessageProducer mockMessageProducer = (MessageProducer) messageProducerControl.getMock();

		mockSession.createProducer(null);
		sessionControl.setReturnValue(mockMessageProducer);

		mockMessageProducer.setDisableMessageID(true);
		messageProducerControl.setVoidCallable(1);
		mockMessageProducer.setDisableMessageTimestamp(true);
		messageProducerControl.setVoidCallable(1);
		mockMessageProducer.getPriority();
		messageProducerControl.setReturnValue(4);

		mockMessageProducer.close();
		messageProducerControl.setVoidCallable(1);
		mockSession.close();
		sessionControl.setVoidCallable(1);
		mockConnection.close();
		connectionControl.setVoidCallable(1);

		messageProducerControl.replay();
		sessionControl.replay();
		connectionControl.replay();

		template.execute(new ProducerCallback() {
			@Override
			public Object doInJms(Session session, MessageProducer producer) throws JMSException {
				boolean b = session.getTransacted();
				int i = producer.getPriority();
				return null;
			}
		});

		connectionFactoryControl.verify();
		connectionControl.verify();
		sessionControl.verify();
	}

	/**
	 * Test the method execute(SessionCallback action).
	 */
	public void testSessionCallback() throws Exception {
		JmsTemplate template = createTemplate();
		template.setConnectionFactory(mockConnectionFactory);

		mockSession.close();
		sessionControl.setVoidCallable(1);

		mockConnection.close();
		connectionControl.setVoidCallable(1);

		sessionControl.replay();
		connectionControl.replay();

		template.execute(new SessionCallback() {
			@Override
			public Object doInJms(Session session) throws JMSException {
				boolean b = session.getTransacted();
				return null;
			}
		});

		connectionFactoryControl.verify();
		connectionControl.verify();
		sessionControl.verify();
	}

	public void testSessionCallbackWithinSynchronizedTransaction() throws Exception {
		SingleConnectionFactory scf = new SingleConnectionFactory(mockConnectionFactory);
		JmsTemplate template = createTemplate();
		template.setConnectionFactory(scf);

		mockConnection.start();
		connectionControl.setVoidCallable(1);
		// We're gonna call getTransacted 3 times, i.e. 2 more times.
		mockSession.getTransacted();
		sessionControl.setReturnValue(useTransactedSession(), 2);
		if (useTransactedTemplate()) {
			mockSession.commit();
			sessionControl.setVoidCallable(1);
		}
		mockSession.close();
		sessionControl.setVoidCallable(1);
		mockConnection.stop();
		connectionControl.setVoidCallable(1);
		mockConnection.close();
		connectionControl.setVoidCallable(1);

		sessionControl.replay();
		connectionControl.replay();

		TransactionSynchronizationManager.initSynchronization();
		try {
			template.execute(new SessionCallback() {
				@Override
				public Object doInJms(Session session) throws JMSException {
					boolean b = session.getTransacted();
					return null;
				}
			});
			template.execute(new SessionCallback() {
				@Override
				public Object doInJms(Session session) throws JMSException {
					boolean b = session.getTransacted();
					return null;
				}
			});

			assertSame(mockSession, ConnectionFactoryUtils.getTransactionalSession(scf, null, false));
			assertSame(mockSession, ConnectionFactoryUtils.getTransactionalSession(scf, scf.createConnection(), false));

			TransactionAwareConnectionFactoryProxy tacf = new TransactionAwareConnectionFactoryProxy(scf);
			Connection tac = tacf.createConnection();
			Session tas = tac.createSession(false, Session.AUTO_ACKNOWLEDGE);
			boolean b = tas.getTransacted();
			tas.close();
			tac.close();

			List synchs = TransactionSynchronizationManager.getSynchronizations();
			assertEquals(1, synchs.size());
			TransactionSynchronization synch = (TransactionSynchronization) synchs.get(0);
			synch.beforeCommit(false);
			synch.beforeCompletion();
			synch.afterCommit();
			synch.afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);
		}
		finally {
			TransactionSynchronizationManager.clearSynchronization();
			scf.destroy();
		}
		assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());

		connectionFactoryControl.verify();
		connectionControl.verify();
		sessionControl.verify();
	}

	/**
	 * Test sending to a destination using the method
	 * send(Destination d, MessageCreator messageCreator)
	 */
	public void testSendDestination() throws Exception {
		doTestSendDestination(true, false, true, false);
	}

	/**
	 * Test seding to a destination using the method
	 * send(String d, MessageCreator messageCreator)
	 */
	public void testSendDestinationName() throws Exception {
		doTestSendDestination(false, false, true, false);
	}

	/**
	 * Test sending to a destination using the method
	 * send(Destination d, MessageCreator messageCreator) using QOS parameters.
	 */
	public void testSendDestinationWithQOS() throws Exception {
		doTestSendDestination(true, false, false, true);
	}

	/**
	 * Test sending to a destination using the method
	 * send(String d, MessageCreator messageCreator) using QOS parameters.
	 */
	public void testSendDestinationNameWithQOS() throws Exception {
		doTestSendDestination(false, false, false, true);
	}

	/**
	 * Test sending to the default destination.
	 */
	public void testSendDefaultDestination() throws Exception {
		doTestSendDestination(true, true, true, true);
	}

	/**
	 * Test sending to the default destination name.
	 */
	public void testSendDefaultDestinationName() throws Exception {
		doTestSendDestination(false, true, true, true);
	}

	/**
	 * Test sending to the default destination using explicit QOS parameters.
	 */
	public void testSendDefaultDestinationWithQOS() throws Exception {
		doTestSendDestination(true, true, false, false);
	}

	/**
	 * Test sending to the default destination name using explicit QOS parameters.
	 */
	public void testSendDefaultDestinationNameWithQOS() throws Exception {
		doTestSendDestination(false, true, false, false);
	}

	/**
	 * Common method for testing a send method that uses the MessageCreator
	 * callback but with different QOS options.
	 * @param ignoreQOS test using default QOS options.
	 */
	private void doTestSendDestination(
			boolean explicitDestination, boolean useDefaultDestination,
			boolean ignoreQOS, boolean disableIdAndTimestamp) throws Exception {

		JmsTemplate template = createTemplate();
		template.setConnectionFactory(mockConnectionFactory);

		String destinationName = "testDestination";

		if (useDefaultDestination) {
			if (explicitDestination) {
				template.setDefaultDestination(mockQueue);
			}
			else {
				template.setDefaultDestinationName(destinationName);
			}
		}
		if (disableIdAndTimestamp) {
			template.setMessageIdEnabled(false);
			template.setMessageTimestampEnabled(false);
		}

		MockControl messageProducerControl = MockControl.createControl(MessageProducer.class);
		MessageProducer mockMessageProducer = (MessageProducer) messageProducerControl.getMock();

		MockControl messageControl = MockControl.createControl(TextMessage.class);
		TextMessage mockMessage = (TextMessage) messageControl.getMock();

		mockSession.createProducer(mockQueue);
		sessionControl.setReturnValue(mockMessageProducer);
		mockSession.createTextMessage("just testing");
		sessionControl.setReturnValue(mockMessage);

		if (useTransactedTemplate()) {
			mockSession.commit();
			sessionControl.setVoidCallable(1);
		}

		if (disableIdAndTimestamp) {
			mockMessageProducer.setDisableMessageID(true);
			messageProducerControl.setVoidCallable(1);
			mockMessageProducer.setDisableMessageTimestamp(true);
			messageProducerControl.setVoidCallable(1);
		}

		if (ignoreQOS) {
			mockMessageProducer.send(mockMessage);
		}
		else {
			template.setExplicitQosEnabled(true);
			template.setDeliveryMode(deliveryMode);
			template.setPriority(priority);
			template.setTimeToLive(timeToLive);
			mockMessageProducer.send(mockMessage, deliveryMode, priority, timeToLive);
		}
		messageProducerControl.setVoidCallable(1);

		mockMessageProducer.close();
		messageProducerControl.setVoidCallable(1);
		mockSession.close();
		sessionControl.setVoidCallable(1);
		mockConnection.close();
		connectionControl.setVoidCallable(1);

		messageProducerControl.replay();
		sessionControl.replay();
		connectionControl.replay();

		if (useDefaultDestination) {
			template.send(new MessageCreator() {
				@Override
				public Message createMessage(Session session) throws JMSException {
					return session.createTextMessage("just testing");
				}
			});
		}
		else {
			if (explicitDestination) {
				template.send(mockQueue, new MessageCreator() {
					@Override
					public Message createMessage(Session session) throws JMSException {
						return session.createTextMessage("just testing");
					}
				});
			}
			else {
				template.send(destinationName, new MessageCreator() {
					@Override
					public Message createMessage(Session session) throws JMSException {
						return session.createTextMessage("just testing");
					}
				});
			}
		}

		connectionFactoryControl.verify();
		connectionControl.verify();
		sessionControl.verify();
		messageProducerControl.verify();
	}

	public void testConverter() throws Exception {
		JmsTemplate template = createTemplate();
		template.setConnectionFactory(mockConnectionFactory);
		template.setMessageConverter(new SimpleMessageConverter());
		String s = "Hello world";

		MockControl messageProducerControl = MockControl.createControl(MessageProducer.class);
		MessageProducer mockMessageProducer = (MessageProducer) messageProducerControl.getMock();
		MockControl messageControl = MockControl.createControl(TextMessage.class);
		TextMessage mockMessage = (TextMessage) messageControl.getMock();

		mockSession.createProducer(mockQueue);
		sessionControl.setReturnValue(mockMessageProducer);
		mockSession.createTextMessage("Hello world");
		sessionControl.setReturnValue(mockMessage);

		mockMessageProducer.send(mockMessage);
		messageProducerControl.setVoidCallable(1);
		mockMessageProducer.close();
		messageProducerControl.setVoidCallable(1);

		if (useTransactedTemplate()) {
			mockSession.commit();
			sessionControl.setVoidCallable(1);
		}

		mockSession.close();
		sessionControl.setVoidCallable(1);
		mockConnection.close();
		connectionControl.setVoidCallable(1);

		messageProducerControl.replay();
		sessionControl.replay();
		connectionControl.replay();

		template.convertAndSend(mockQueue, s);

		messageProducerControl.verify();
		sessionControl.verify();
		connectionControl.verify();
		connectionFactoryControl.verify();
	}

	public void testReceiveDefaultDestination() throws Exception {
		doTestReceive(true, true, false, false, false, false, JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
	}

	public void testReceiveDefaultDestinationName() throws Exception {
		doTestReceive(false, true, false, false, false, false, JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
	}

	public void testReceiveDestination() throws Exception {
		doTestReceive(true, false, false, false, false, true, JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
	}

	public void testReceiveDestinationWithClientAcknowledge() throws Exception {
		doTestReceive(true, false, false, true, false, false, 1000);
	}

	public void testReceiveDestinationName() throws Exception {
		doTestReceive(false, false, false, false, false, true, 1000);
	}

	public void testReceiveDefaultDestinationWithSelector() throws Exception {
		doTestReceive(true, true, false, false, true, true, 1000);
	}

	public void testReceiveDefaultDestinationNameWithSelector() throws Exception {
		doTestReceive(false, true, false, false, true, true, JmsTemplate.RECEIVE_TIMEOUT_NO_WAIT);
	}

	public void testReceiveDestinationWithSelector() throws Exception {
		doTestReceive(true, false, false, false, true, false, 1000);
	}

	public void testReceiveDestinationWithClientAcknowledgeWithSelector() throws Exception {
		doTestReceive(true, false, false, true, true, true, JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
	}

	public void testReceiveDestinationNameWithSelector() throws Exception {
		doTestReceive(false, false, false, false, true, false, JmsTemplate.RECEIVE_TIMEOUT_NO_WAIT);
	}

	public void testReceiveAndConvertDefaultDestination() throws Exception {
		doTestReceive(true, true, true, false, false, false, 1000);
	}

	public void testReceiveAndConvertDefaultDestinationName() throws Exception {
		doTestReceive(false, true, true, false, false, false, 1000);
	}

	public void testReceiveAndConvertDestinationName() throws Exception {
		doTestReceive(false, false, true, false, false, true, JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
	}

	public void testReceiveAndConvertDestination() throws Exception {
		doTestReceive(true, false, true, false, false, true, 1000);
	}

	public void testReceiveAndConvertDefaultDestinationWithSelector() throws Exception {
		doTestReceive(true, true, true, false, true, true, JmsTemplate.RECEIVE_TIMEOUT_NO_WAIT);
	}

	public void testReceiveAndConvertDestinationNameWithSelector() throws Exception {
		doTestReceive(false, false, true, false, true, true, JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
	}

	public void testReceiveAndConvertDestinationWithSelector() throws Exception {
		doTestReceive(true, false, true, false, true, false, 1000);
	}

	private void doTestReceive(
			boolean explicitDestination, boolean useDefaultDestination, boolean testConverter,
			boolean clientAcknowledge, boolean messageSelector, boolean noLocal, long timeout)
			throws Exception {

		JmsTemplate template = createTemplate();
		template.setConnectionFactory(mockConnectionFactory);

		String destinationName = "testDestination";

		if (useDefaultDestination) {
			if (explicitDestination) {
				template.setDefaultDestination(mockQueue);
			}
			else {
				template.setDefaultDestinationName(destinationName);
			}
		}
		if (noLocal) {
			template.setPubSubNoLocal(true);
		}
		template.setReceiveTimeout(timeout);

		mockConnection.start();
		connectionControl.setVoidCallable(1);
		mockConnection.close();
		connectionControl.setVoidCallable(1);

		MockControl messageConsumerControl = MockControl.createControl(MessageConsumer.class);
		MessageConsumer mockMessageConsumer = (MessageConsumer) messageConsumerControl.getMock();

		String selectorString = "selector";
		mockSession.createConsumer(mockQueue, messageSelector ? selectorString : null);
		sessionControl.setReturnValue(mockMessageConsumer);

		if (useTransactedTemplate()) {
			mockSession.commit();
			sessionControl.setVoidCallable(1);
		}
		else if (!useTransactedSession()) {
			mockSession.getAcknowledgeMode();
			if (clientAcknowledge) {
				sessionControl.setReturnValue(Session.CLIENT_ACKNOWLEDGE, 1);
			}
			else {
				sessionControl.setReturnValue(Session.AUTO_ACKNOWLEDGE, 1);
			}
		}

		mockSession.close();
		sessionControl.setVoidCallable(1);

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

		sessionControl.replay();
		connectionControl.replay();
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
						(messageSelector ? template.receiveSelectedAndConvert(mockQueue, selectorString) :
						template.receiveAndConvert(mockQueue));
			}
			else {
				message = (messageSelector ? template.receiveSelected(mockQueue, selectorString) :
						template.receive(mockQueue));
			}
		}
		else {
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

		connectionFactoryControl.verify();
		connectionControl.verify();
		sessionControl.verify();
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
		template.setConnectionFactory(mockConnectionFactory);
		template.setMessageConverter(new SimpleMessageConverter());
		String s = "Hello world";

		MockControl messageProducerControl = MockControl.createControl(MessageProducer.class);
		MessageProducer mockMessageProducer = (MessageProducer) messageProducerControl.getMock();
		MockControl messageControl = MockControl.createControl(TextMessage.class);
		TextMessage mockMessage = (TextMessage) messageControl.getMock();

		sessionControl.reset();
		mockSession.createProducer(mockQueue);
		sessionControl.setReturnValue(mockMessageProducer);
		mockSession.createTextMessage("Hello world");
		sessionControl.setReturnValue(mockMessage);

		mockMessageProducer.send(mockMessage);
		messageProducerControl.setThrowable(original, 1);
		mockMessageProducer.close();
		messageProducerControl.setVoidCallable(1);

		mockSession.close();
		sessionControl.setVoidCallable(1);
		mockConnection.close();
		connectionControl.setVoidCallable(1);

		messageProducerControl.replay();
		sessionControl.replay();
		connectionControl.replay();

		try {
			template.convertAndSend(mockQueue, s);
			fail("Should have thrown JmsException");
		}
		catch (JmsException wrappedEx) {
			// expected
			assertEquals(thrownExceptionClass, wrappedEx.getClass());
			assertEquals(original, wrappedEx.getCause());
		}

		messageProducerControl.verify();
		sessionControl.verify();
		connectionControl.verify();
		connectionFactoryControl.verify();
	}

}
