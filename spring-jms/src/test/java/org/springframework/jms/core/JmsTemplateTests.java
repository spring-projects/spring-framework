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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

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
public class JmsTemplateTests {

	private Context jndiContext;
	private ConnectionFactory connectionFactory;
	private Connection connection;
	private Session session;
	private Destination queue;

	private int deliveryMode = DeliveryMode.PERSISTENT;
	private int priority = 9;
	private int timeToLive = 10000;


	/**
	 * Create the mock objects for testing.
	 */
	@Before
	public void setupMocks() throws Exception {
		jndiContext = mock(Context.class);
		connectionFactory = mock(ConnectionFactory.class);
		connection = mock(Connection.class);
		session = mock(Session.class);
		queue = mock(Queue.class);

		given(connectionFactory.createConnection()).willReturn(connection);
		given(connection.createSession(useTransactedTemplate(),
				Session.AUTO_ACKNOWLEDGE)).willReturn(session);
		given(session.getTransacted()).willReturn(useTransactedSession());
		given(jndiContext.lookup("testDestination")).willReturn(queue);
	}

	private JmsTemplate createTemplate() {
		JmsTemplate template = new JmsTemplate();
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

	@Test
	public void testProducerCallback() throws Exception {
		JmsTemplate template = createTemplate();
		template.setConnectionFactory(connectionFactory);

		MessageProducer messageProducer = mock(MessageProducer.class);
		given(session.createProducer(null)).willReturn(messageProducer);
		given(messageProducer.getPriority()).willReturn(4);

		template.execute(new ProducerCallback() {
			@Override
			public Object doInJms(Session session, MessageProducer producer) throws JMSException {
				session.getTransacted();
				producer.getPriority();
				return null;
			}
		});

		verify(messageProducer).close();
		verify(session).close();
		verify(connection).close();
	}

	@Test
	public void testProducerCallbackWithIdAndTimestampDisabled() throws Exception {
		JmsTemplate template = createTemplate();
		template.setConnectionFactory(connectionFactory);
		template.setMessageIdEnabled(false);
		template.setMessageTimestampEnabled(false);

		MessageProducer messageProducer = mock(MessageProducer.class);
		given(session.createProducer(null)).willReturn(messageProducer);
		given(messageProducer.getPriority()).willReturn(4);

		template.execute(new ProducerCallback() {
			@Override
			public Object doInJms(Session session, MessageProducer producer) throws JMSException {
				session.getTransacted();
				producer.getPriority();
				return null;
			}
		});

		verify(messageProducer).setDisableMessageID(true);
		verify(messageProducer).setDisableMessageTimestamp(true);
		verify(messageProducer).close();
		verify(session).close();
		verify(connection).close();
	}

	/**
	 * Test the method execute(SessionCallback action).
	 */
	@Test
	public void testSessionCallback() throws Exception {
		JmsTemplate template = createTemplate();
		template.setConnectionFactory(connectionFactory);

		template.execute(new SessionCallback() {
			@Override
			public Object doInJms(Session session) throws JMSException {
				session.getTransacted();
				return null;
			}
		});

		verify(session).close();
		verify(connection).close();
	}

	@Test
	public void testSessionCallbackWithinSynchronizedTransaction() throws Exception {
		SingleConnectionFactory scf = new SingleConnectionFactory(connectionFactory);
		JmsTemplate template = createTemplate();
		template.setConnectionFactory(scf);

		TransactionSynchronizationManager.initSynchronization();
		try {
			template.execute(new SessionCallback() {
				@Override
				public Object doInJms(Session session) throws JMSException {
					session.getTransacted();
					return null;
				}
			});
			template.execute(new SessionCallback() {
				@Override
				public Object doInJms(Session session) throws JMSException {
					session.getTransacted();
					return null;
				}
			});

			assertSame(session, ConnectionFactoryUtils.getTransactionalSession(scf, null, false));
			assertSame(session, ConnectionFactoryUtils.getTransactionalSession(scf, scf.createConnection(), false));

			TransactionAwareConnectionFactoryProxy tacf = new TransactionAwareConnectionFactoryProxy(scf);
			Connection tac = tacf.createConnection();
			Session tas = tac.createSession(false, Session.AUTO_ACKNOWLEDGE);
			tas.getTransacted();
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

		verify(connection).start();
		if (useTransactedTemplate()) {
			verify(session).commit();
		}
		verify(session).close();
		verify(connection).stop();
		verify(connection).close();
	}

	/**
	 * Test sending to a destination using the method
	 * send(Destination d, MessageCreator messageCreator)
	 */
	@Test
	public void testSendDestination() throws Exception {
		doTestSendDestination(true, false, true, false);
	}

	/**
	 * Test seding to a destination using the method
	 * send(String d, MessageCreator messageCreator)
	 */
	@Test
	public void testSendDestinationName() throws Exception {
		doTestSendDestination(false, false, true, false);
	}

	/**
	 * Test sending to a destination using the method
	 * send(Destination d, MessageCreator messageCreator) using QOS parameters.
	 */
	@Test
	public void testSendDestinationWithQOS() throws Exception {
		doTestSendDestination(true, false, false, true);
	}

	/**
	 * Test sending to a destination using the method
	 * send(String d, MessageCreator messageCreator) using QOS parameters.
	 */
	@Test
	public void testSendDestinationNameWithQOS() throws Exception {
		doTestSendDestination(false, false, false, true);
	}

	/**
	 * Test sending to the default destination.
	 */
	@Test
	public void testSendDefaultDestination() throws Exception {
		doTestSendDestination(true, true, true, true);
	}

	/**
	 * Test sending to the default destination name.
	 */
	@Test
	public void testSendDefaultDestinationName() throws Exception {
		doTestSendDestination(false, true, true, true);
	}

	/**
	 * Test sending to the default destination using explicit QOS parameters.
	 */
	@Test
	public void testSendDefaultDestinationWithQOS() throws Exception {
		doTestSendDestination(true, true, false, false);
	}

	/**
	 * Test sending to the default destination name using explicit QOS parameters.
	 */
	@Test
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
		template.setConnectionFactory(connectionFactory);

		String destinationName = "testDestination";

		if (useDefaultDestination) {
			if (explicitDestination) {
				template.setDefaultDestination(queue);
			}
			else {
				template.setDefaultDestinationName(destinationName);
			}
		}
		if (disableIdAndTimestamp) {
			template.setMessageIdEnabled(false);
			template.setMessageTimestampEnabled(false);
		}

		MessageProducer messageProducer = mock(MessageProducer.class);
		TextMessage textMessage = mock(TextMessage.class);

		given(session.createProducer(queue)).willReturn(messageProducer);
		given(session.createTextMessage("just testing")).willReturn(textMessage);

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
			if (explicitDestination) {
				template.send(queue, new MessageCreator() {
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

		if (useTransactedTemplate()) {
			verify(session).commit();
		}

		if (disableIdAndTimestamp) {
			verify(messageProducer).setDisableMessageID(true);
			verify(messageProducer).setDisableMessageTimestamp(true);
		}

		if (ignoreQOS) {
			verify(messageProducer).send(textMessage);
		}
		else {
			verify(messageProducer).send(textMessage, deliveryMode, priority, timeToLive);
		}
		verify(messageProducer).close();
		verify(session).close();
		verify(connection).close();
	}

	@Test
	public void testConverter() throws Exception {
		JmsTemplate template = createTemplate();
		template.setConnectionFactory(connectionFactory);
		template.setMessageConverter(new SimpleMessageConverter());
		String s = "Hello world";

		MessageProducer messageProducer = mock(MessageProducer.class);
		TextMessage textMessage = mock(TextMessage.class);

		given(session.createProducer(queue)).willReturn(messageProducer);
		given(session.createTextMessage("Hello world")).willReturn(textMessage);

		template.convertAndSend(queue, s);

		verify(messageProducer).send(textMessage);
		verify(messageProducer).close();
		if (useTransactedTemplate()) {
			verify(session).commit();
		}
		verify(session).close();
		verify(connection).close();
	}

	@Test
	public void testReceiveDefaultDestination() throws Exception {
		doTestReceive(true, true, false, false, false, false, JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
	}

	@Test
	public void testReceiveDefaultDestinationName() throws Exception {
		doTestReceive(false, true, false, false, false, false, JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
	}

	@Test
	public void testReceiveDestination() throws Exception {
		doTestReceive(true, false, false, false, false, true, JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
	}

	@Test
	public void testReceiveDestinationWithClientAcknowledge() throws Exception {
		doTestReceive(true, false, false, true, false, false, 1000);
	}

	@Test
	public void testReceiveDestinationName() throws Exception {
		doTestReceive(false, false, false, false, false, true, 1000);
	}

	@Test
	public void testReceiveDefaultDestinationWithSelector() throws Exception {
		doTestReceive(true, true, false, false, true, true, 1000);
	}

	@Test
	public void testReceiveDefaultDestinationNameWithSelector() throws Exception {
		doTestReceive(false, true, false, false, true, true, JmsTemplate.RECEIVE_TIMEOUT_NO_WAIT);
	}

	@Test
	public void testReceiveDestinationWithSelector() throws Exception {
		doTestReceive(true, false, false, false, true, false, 1000);
	}

	@Test
	public void testReceiveDestinationWithClientAcknowledgeWithSelector() throws Exception {
		doTestReceive(true, false, false, true, true, true, JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
	}

	@Test
	public void testReceiveDestinationNameWithSelector() throws Exception {
		doTestReceive(false, false, false, false, true, false, JmsTemplate.RECEIVE_TIMEOUT_NO_WAIT);
	}

	@Test
	public void testReceiveAndConvertDefaultDestination() throws Exception {
		doTestReceive(true, true, true, false, false, false, 1000);
	}

	@Test
	public void testReceiveAndConvertDefaultDestinationName() throws Exception {
		doTestReceive(false, true, true, false, false, false, 1000);
	}

	@Test
	public void testReceiveAndConvertDestinationName() throws Exception {
		doTestReceive(false, false, true, false, false, true, JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
	}

	@Test
	public void testReceiveAndConvertDestination() throws Exception {
		doTestReceive(true, false, true, false, false, true, 1000);
	}

	@Test
	public void testReceiveAndConvertDefaultDestinationWithSelector() throws Exception {
		doTestReceive(true, true, true, false, true, true, JmsTemplate.RECEIVE_TIMEOUT_NO_WAIT);
	}

	@Test
	public void testReceiveAndConvertDestinationNameWithSelector() throws Exception {
		doTestReceive(false, false, true, false, true, true, JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
	}

	@Test
	public void testReceiveAndConvertDestinationWithSelector() throws Exception {
		doTestReceive(true, false, true, false, true, false, 1000);
	}

	private void doTestReceive(
			boolean explicitDestination, boolean useDefaultDestination, boolean testConverter,
			boolean clientAcknowledge, boolean messageSelector, boolean noLocal, long timeout)
			throws Exception {

		JmsTemplate template = createTemplate();
		template.setConnectionFactory(connectionFactory);

		String destinationName = "testDestination";

		if (useDefaultDestination) {
			if (explicitDestination) {
				template.setDefaultDestination(queue);
			}
			else {
				template.setDefaultDestinationName(destinationName);
			}
		}
		if (noLocal) {
			template.setPubSubNoLocal(true);
		}
		template.setReceiveTimeout(timeout);

		MessageConsumer messageConsumer = mock(MessageConsumer.class);

		String selectorString = "selector";
		given(session.createConsumer(queue,
				messageSelector ? selectorString : null)).willReturn(messageConsumer);

		if (!useTransactedTemplate() && !useTransactedSession()) {
			given(session.getAcknowledgeMode()).willReturn(
					clientAcknowledge ? Session.CLIENT_ACKNOWLEDGE
							: Session.AUTO_ACKNOWLEDGE);
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
						(messageSelector ? template.receiveSelectedAndConvert(queue, selectorString) :
						template.receiveAndConvert(queue));
			}
			else {
				message = (messageSelector ? template.receiveSelected(queue, selectorString) :
						template.receive(queue));
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

		if (testConverter) {
			assertEquals("Message text should be equal", "Hello World!", textFromMessage);
		}
		else {
			assertEquals("Messages should refer to the same object", message, textMessage);
		}

		verify(connection).start();
		verify(connection).close();
		if (useTransactedTemplate()) {
			verify(session).commit();
		}
		verify(session).close();
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
		template.setConnectionFactory(connectionFactory);
		template.setMessageConverter(new SimpleMessageConverter());
		String s = "Hello world";

		MessageProducer messageProducer = mock(MessageProducer.class);
		TextMessage textMessage = mock(TextMessage.class);

		reset(session);
		given(session.createProducer(queue)).willReturn(messageProducer);
		given(session.createTextMessage("Hello world")).willReturn(textMessage);

		willThrow(original).given(messageProducer).send(textMessage);

		try {
			template.convertAndSend(queue, s);
			fail("Should have thrown JmsException");
		}
		catch (JmsException wrappedEx) {
			// expected
			assertEquals(thrownExceptionClass, wrappedEx.getClass());
			assertEquals(original, wrappedEx.getCause());
		}

		verify(messageProducer).close();
		verify(session).close();
		verify(connection).close();
	}

}
