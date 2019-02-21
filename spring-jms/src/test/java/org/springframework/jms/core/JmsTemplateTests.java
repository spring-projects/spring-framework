/*
 * Copyright 2002-2018 the original author or authors.
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
import javax.jms.TemporaryQueue;
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
import org.springframework.jms.support.QosSettings;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.jms.support.destination.JndiDestinationResolver;
import org.springframework.jndi.JndiTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for the JmsTemplate implemented using JMS 1.1.
 *
 * @author Andre Biryukov
 * @author Mark Pollack
 * @author Stephane Nicoll
 */
public class JmsTemplateTests {

	private Context jndiContext;

	private ConnectionFactory connectionFactory;

	protected Connection connection;

	private Session session;

	private Destination queue;

	private QosSettings qosSettings = new QosSettings(DeliveryMode.PERSISTENT, 9, 10000);


	/**
	 * Create the mock objects for testing.
	 */
	@Before
	public void setupMocks() throws Exception {
		this.jndiContext = mock(Context.class);
		this.connectionFactory = mock(ConnectionFactory.class);
		this.connection = mock(Connection.class);
		this.session = mock(Session.class);
		this.queue = mock(Queue.class);

		given(this.connectionFactory.createConnection()).willReturn(this.connection);
		given(this.connection.createSession(useTransactedTemplate(), Session.AUTO_ACKNOWLEDGE)).willReturn(this.session);
		given(this.session.getTransacted()).willReturn(useTransactedSession());
		given(this.jndiContext.lookup("testDestination")).willReturn(this.queue);
	}

	private JmsTemplate createTemplate() {
		JmsTemplate template = new JmsTemplate();
		JndiDestinationResolver destMan = new JndiDestinationResolver();
		destMan.setJndiTemplate(new JndiTemplate() {
			@Override
			protected Context createInitialContext() {
				return JmsTemplateTests.this.jndiContext;
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

	protected Session getLocalSession() {
		return this.session;
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
		template.setConnectionFactory(this.connectionFactory);

		MessageProducer messageProducer = mock(MessageProducer.class);
		given(this.session.createProducer(null)).willReturn(messageProducer);
		given(messageProducer.getPriority()).willReturn(4);

		template.execute((ProducerCallback<Void>) (session1, producer) -> {
			session1.getTransacted();
			producer.getPriority();
			return null;
		});

		verify(messageProducer).close();
		verify(this.session).close();
		verify(this.connection).close();
	}

	@Test
	public void testProducerCallbackWithIdAndTimestampDisabled() throws Exception {
		JmsTemplate template = createTemplate();
		template.setConnectionFactory(this.connectionFactory);
		template.setMessageIdEnabled(false);
		template.setMessageTimestampEnabled(false);

		MessageProducer messageProducer = mock(MessageProducer.class);
		given(this.session.createProducer(null)).willReturn(messageProducer);
		given(messageProducer.getPriority()).willReturn(4);

		template.execute((ProducerCallback<Void>) (session1, producer) -> {
			session1.getTransacted();
			producer.getPriority();
			return null;
		});

		verify(messageProducer).setDisableMessageID(true);
		verify(messageProducer).setDisableMessageTimestamp(true);
		verify(messageProducer).close();
		verify(this.session).close();
		verify(this.connection).close();
	}

	/**
	 * Test the method execute(SessionCallback action).
	 */
	@Test
	public void testSessionCallback() throws Exception {
		JmsTemplate template = createTemplate();
		template.setConnectionFactory(this.connectionFactory);

		template.execute(new SessionCallback<Void>() {
			@Override
			public Void doInJms(Session session) throws JMSException {
				session.getTransacted();
				return null;
			}
		});

		verify(this.session).close();
		verify(this.connection).close();
	}

	@Test
	public void testSessionCallbackWithinSynchronizedTransaction() throws Exception {
		SingleConnectionFactory scf = new SingleConnectionFactory(this.connectionFactory);
		JmsTemplate template = createTemplate();
		template.setConnectionFactory(scf);

		TransactionSynchronizationManager.initSynchronization();
		try {
			template.execute(new SessionCallback<Void>() {
				@Override
				public Void doInJms(Session session) throws JMSException {
					session.getTransacted();
					return null;
				}
			});
			template.execute(new SessionCallback<Void>() {
				@Override
				public Void doInJms(Session session) throws JMSException {
					session.getTransacted();
					return null;
				}
			});

			assertSame(this.session, ConnectionFactoryUtils.getTransactionalSession(scf, null, false));
			assertSame(this.session, ConnectionFactoryUtils.getTransactionalSession(scf, scf.createConnection(), false));

			TransactionAwareConnectionFactoryProxy tacf = new TransactionAwareConnectionFactoryProxy(scf);
			Connection tac = tacf.createConnection();
			Session tas = tac.createSession(false, Session.AUTO_ACKNOWLEDGE);
			tas.getTransacted();
			tas.close();
			tac.close();

			List<TransactionSynchronization> synchs = TransactionSynchronizationManager.getSynchronizations();
			assertEquals(1, synchs.size());
			TransactionSynchronization synch = synchs.get(0);
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

		verify(this.connection).start();
		if (useTransactedTemplate()) {
			verify(this.session).commit();
		}
		verify(this.session).close();
		verify(this.connection).stop();
		verify(this.connection).close();
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
	 * Test sending to a destination using the method
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
		template.setConnectionFactory(this.connectionFactory);

		String destinationName = "testDestination";

		if (useDefaultDestination) {
			if (explicitDestination) {
				template.setDefaultDestination(this.queue);
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

		given(this.session.createProducer(this.queue)).willReturn(messageProducer);
		given(this.session.createTextMessage("just testing")).willReturn(textMessage);

		if (!ignoreQOS) {
			template.setQosSettings(this.qosSettings);
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
				template.send(this.queue, new MessageCreator() {
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
			verify(this.session).commit();
		}

		if (disableIdAndTimestamp) {
			verify(messageProducer).setDisableMessageID(true);
			verify(messageProducer).setDisableMessageTimestamp(true);
		}

		if (ignoreQOS) {
			verify(messageProducer).send(textMessage);
		}
		else {
			verify(messageProducer).send(textMessage, this.qosSettings.getDeliveryMode(),
					this.qosSettings.getPriority(), this.qosSettings.getTimeToLive());
		}
		verify(messageProducer).close();
		verify(this.session).close();
		verify(this.connection).close();
	}

	@Test
	public void testConverter() throws Exception {
		JmsTemplate template = createTemplate();
		template.setConnectionFactory(this.connectionFactory);
		template.setMessageConverter(new SimpleMessageConverter());
		String s = "Hello world";

		MessageProducer messageProducer = mock(MessageProducer.class);
		TextMessage textMessage = mock(TextMessage.class);

		given(this.session.createProducer(this.queue)).willReturn(messageProducer);
		given(this.session.createTextMessage("Hello world")).willReturn(textMessage);

		template.convertAndSend(this.queue, s);

		verify(messageProducer).send(textMessage);
		verify(messageProducer).close();
		if (useTransactedTemplate()) {
			verify(this.session).commit();
		}
		verify(this.session).close();
		verify(this.connection).close();
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
		template.setConnectionFactory(this.connectionFactory);

		String destinationName = "testDestination";

		if (useDefaultDestination) {
			if (explicitDestination) {
				template.setDefaultDestination(this.queue);
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
		given(this.session.createConsumer(this.queue,
				messageSelector ? selectorString : null)).willReturn(messageConsumer);

		if (!useTransactedTemplate() && !useTransactedSession()) {
			given(this.session.getAcknowledgeMode()).willReturn(
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
						(messageSelector ? template.receiveSelectedAndConvert(this.queue, selectorString) :
						template.receiveAndConvert(this.queue));
			}
			else {
				message = (messageSelector ? template.receiveSelected(this.queue, selectorString) :
						template.receive(this.queue));
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

		verify(this.connection).start();
		verify(this.connection).close();
		if (useTransactedTemplate()) {
			verify(this.session).commit();
		}
		verify(this.session).close();
		if (!useTransactedSession() && clientAcknowledge) {
			verify(textMessage).acknowledge();
		}
		verify(messageConsumer).close();
	}

	@Test
	public void testSendAndReceiveDefaultDestination() throws Exception {
		doTestSendAndReceive(true, true, 1000L);
	}

	@Test
	public void testSendAndReceiveDefaultDestinationName() throws Exception {
		doTestSendAndReceive(false, true, 1000L);
	}

	@Test
	public void testSendAndReceiveDestination() throws Exception {
		doTestSendAndReceive(true, false, 1000L);
	}

	@Test
	public void testSendAndReceiveDestinationName() throws Exception {
		doTestSendAndReceive(false, false, 1000L);
	}

	private void doTestSendAndReceive(boolean explicitDestination, boolean useDefaultDestination, long timeout)
			throws Exception {

		JmsTemplate template = createTemplate();
		template.setConnectionFactory(this.connectionFactory);

		String destinationName = "testDestination";
		if (useDefaultDestination) {
			if (explicitDestination) {
				template.setDefaultDestination(this.queue);
			}
			else {
				template.setDefaultDestinationName(destinationName);
			}
		}
		template.setReceiveTimeout(timeout);

		Session localSession = getLocalSession();
		TemporaryQueue replyDestination = mock(TemporaryQueue.class);
		MessageProducer messageProducer = mock(MessageProducer.class);
		given(localSession.createProducer(this.queue)).willReturn(messageProducer);
		given(localSession.createTemporaryQueue()).willReturn(replyDestination);

		MessageConsumer messageConsumer = mock(MessageConsumer.class);
		given(localSession.createConsumer(replyDestination)).willReturn(messageConsumer);


		TextMessage request = mock(TextMessage.class);
		MessageCreator messageCreator = mock(MessageCreator.class);
		given(messageCreator.createMessage(localSession)).willReturn(request);

		TextMessage reply = mock(TextMessage.class);
		if (timeout == JmsTemplate.RECEIVE_TIMEOUT_NO_WAIT) {
			given(messageConsumer.receiveNoWait()).willReturn(reply);
		}
		else if (timeout == JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT) {
			given(messageConsumer.receive()).willReturn(reply);
		}
		else {
			given(messageConsumer.receive(timeout)).willReturn(reply);
		}

		Message message = null;
		if (useDefaultDestination) {
			message = template.sendAndReceive(messageCreator);
		}
		else if (explicitDestination) {
			message = template.sendAndReceive(this.queue, messageCreator);
		}
		else {
			message = template.sendAndReceive(destinationName, messageCreator);
		}

		// replyTO set on the request
		verify(request).setJMSReplyTo(replyDestination);
		assertSame("Reply message not received", reply, message);
		verify(this.connection).start();
		verify(this.connection).close();
		verify(localSession).close();
		verify(messageConsumer).close();
		verify(messageProducer).close();
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

	protected void doTestJmsException(JMSException original, Class<? extends JmsException> thrownExceptionClass) throws Exception {
		JmsTemplate template = createTemplate();
		template.setConnectionFactory(this.connectionFactory);
		template.setMessageConverter(new SimpleMessageConverter());
		String s = "Hello world";

		MessageProducer messageProducer = mock(MessageProducer.class);
		TextMessage textMessage = mock(TextMessage.class);

		reset(this.session);
		given(this.session.createProducer(this.queue)).willReturn(messageProducer);
		given(this.session.createTextMessage("Hello world")).willReturn(textMessage);

		willThrow(original).given(messageProducer).send(textMessage);

		try {
			template.convertAndSend(this.queue, s);
			fail("Should have thrown JmsException");
		}
		catch (JmsException wrappedEx) {
			// expected
			assertEquals(thrownExceptionClass, wrappedEx.getClass());
			assertEquals(original, wrappedEx.getCause());
		}

		verify(messageProducer).close();
		verify(this.session).close();
		verify(this.connection).close();
	}

}
