/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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

import javax.naming.Context;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.DeliveryMode;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TemporaryQueue;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link JmsTemplate} using JMS 1.1.
 *
 * @author Andre Biryukov
 * @author Mark Pollack
 * @author Stephane Nicoll
 */
class JmsTemplateTests {

	private Context jndiContext = mock();

	private ConnectionFactory connectionFactory = mock();

	protected Connection connection = mock();

	private Session session = mock();

	private Queue queue = mock();

	private QosSettings qosSettings = new QosSettings(DeliveryMode.PERSISTENT, 9, 10000);


	/**
	 * Create the mock objects for testing.
	 */
	@BeforeEach
	void setupMocks() throws Exception {
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
	void testExceptionStackTrace() {
		JMSException jmsEx = new JMSException("could not connect");
		Exception innerEx = new Exception("host not found");
		jmsEx.setLinkedException(innerEx);
		JmsException springJmsEx = JmsUtils.convertJmsAccessException(jmsEx);
		StringWriter sw = new StringWriter();
		PrintWriter out = new PrintWriter(sw);
		springJmsEx.printStackTrace(out);
		String trace = sw.toString();
		assertThat(trace.indexOf("host not found")).as("inner jms exception not found").isGreaterThan(0);
	}

	@Test
	void testProducerCallback() throws Exception {
		JmsTemplate template = createTemplate();
		template.setConnectionFactory(this.connectionFactory);

		MessageProducer messageProducer = mock();
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
	void testProducerCallbackWithIdAndTimestampDisabled() throws Exception {
		JmsTemplate template = createTemplate();
		template.setConnectionFactory(this.connectionFactory);
		template.setMessageIdEnabled(false);
		template.setMessageTimestampEnabled(false);

		MessageProducer messageProducer = mock();
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
	void testSessionCallback() throws Exception {
		JmsTemplate template = createTemplate();
		template.setConnectionFactory(this.connectionFactory);

		template.execute((SessionCallback<Void>) session -> {
			session.getTransacted();
			return null;
		});

		verify(this.session).close();
		verify(this.connection).close();
	}

	@Test
	void testSessionCallbackWithinSynchronizedTransaction() throws Exception {
		SingleConnectionFactory scf = new SingleConnectionFactory(this.connectionFactory);
		JmsTemplate template = createTemplate();
		template.setConnectionFactory(scf);

		TransactionSynchronizationManager.initSynchronization();
		try {
			template.execute((SessionCallback<Void>) session -> {
				session.getTransacted();
				return null;
			});
			template.execute((SessionCallback<Void>) session -> {
				session.getTransacted();
				return null;
			});

			assertThat(ConnectionFactoryUtils.getTransactionalSession(scf, null, false)).isSameAs(this.session);
			assertThat(ConnectionFactoryUtils.getTransactionalSession(scf, scf.createConnection(), false)).isSameAs(this.session);

			TransactionAwareConnectionFactoryProxy tacf = new TransactionAwareConnectionFactoryProxy(scf);
			Connection tac = tacf.createConnection();
			Session tas = tac.createSession(false, Session.AUTO_ACKNOWLEDGE);
			tas.getTransacted();
			tas.close();
			tac.close();

			List<TransactionSynchronization> synchs = TransactionSynchronizationManager.getSynchronizations();
			assertThat(synchs).hasSize(1);
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
		assertThat(TransactionSynchronizationManager.getResourceMap()).isEmpty();

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
	 * {@code send(Destination d, MessageCreator messageCreator)}
	 */
	@Test
	void testSendDestination() throws Exception {
		doTestSendDestination(true, false, true, false);
	}

	/**
	 * Test sending to a destination using the method
	 * {@code send(String d, MessageCreator messageCreator)}
	 */
	@Test
	void testSendDestinationName() throws Exception {
		doTestSendDestination(false, false, true, false);
	}

	/**
	 * Test sending to a destination using the method
	 * {@code send(Destination d, MessageCreator messageCreator)} using QOS parameters.
	 */
	@Test
	void testSendDestinationWithQOS() throws Exception {
		doTestSendDestination(true, false, false, true);
	}

	/**
	 * Test sending to a destination using the method
	 * {@code send(String d, MessageCreator messageCreator)} using QOS parameters.
	 */
	@Test
	void testSendDestinationNameWithQOS() throws Exception {
		doTestSendDestination(false, false, false, true);
	}

	/**
	 * Test sending to the default destination.
	 */
	@Test
	void testSendDefaultDestination() throws Exception {
		doTestSendDestination(true, true, true, true);
	}

	/**
	 * Test sending to the default destination name.
	 */
	@Test
	void testSendDefaultDestinationName() throws Exception {
		doTestSendDestination(false, true, true, true);
	}

	/**
	 * Test sending to the default destination using explicit QOS parameters.
	 */
	@Test
	void testSendDefaultDestinationWithQOS() throws Exception {
		doTestSendDestination(true, true, false, false);
	}

	/**
	 * Test sending to the default destination name using explicit QOS parameters.
	 */
	@Test
	void testSendDefaultDestinationNameWithQOS() throws Exception {
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

		MessageProducer messageProducer = mock();
		TextMessage textMessage = mock();

		given(this.session.createProducer(this.queue)).willReturn(messageProducer);
		given(this.session.createTextMessage("just testing")).willReturn(textMessage);

		if (!ignoreQOS) {
			template.setQosSettings(this.qosSettings);
		}

		if (useDefaultDestination) {
			template.send(session -> session.createTextMessage("just testing"));
		}
		else {
			if (explicitDestination) {
				template.send(this.queue, (MessageCreator) session -> session.createTextMessage("just testing"));
			}
			else {
				template.send(destinationName, (MessageCreator) session -> session.createTextMessage("just testing"));
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
	void testConverter() throws Exception {
		JmsTemplate template = createTemplate();
		template.setConnectionFactory(this.connectionFactory);
		template.setMessageConverter(new SimpleMessageConverter());
		String s = "Hello world";

		MessageProducer messageProducer = mock();
		TextMessage textMessage = mock();

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
	void testReceiveDefaultDestination() throws Exception {
		doTestReceive(true, true, false, false, false, false, JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
	}

	@Test
	void testReceiveDefaultDestinationName() throws Exception {
		doTestReceive(false, true, false, false, false, false, JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
	}

	@Test
	void testReceiveDestination() throws Exception {
		doTestReceive(true, false, false, false, false, true, JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
	}

	@Test
	void testReceiveDestinationWithClientAcknowledge() throws Exception {
		doTestReceive(true, false, false, true, false, false, 1000);
	}

	@Test
	void testReceiveDestinationName() throws Exception {
		doTestReceive(false, false, false, false, false, true, 1000);
	}

	@Test
	void testReceiveDefaultDestinationWithSelector() throws Exception {
		doTestReceive(true, true, false, false, true, true, 1000);
	}

	@Test
	void testReceiveDefaultDestinationNameWithSelector() throws Exception {
		doTestReceive(false, true, false, false, true, true, JmsTemplate.RECEIVE_TIMEOUT_NO_WAIT);
	}

	@Test
	void testReceiveDestinationWithSelector() throws Exception {
		doTestReceive(true, false, false, false, true, false, 1000);
	}

	@Test
	void testReceiveDestinationWithClientAcknowledgeWithSelector() throws Exception {
		doTestReceive(true, false, false, true, true, true, JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
	}

	@Test
	void testReceiveDestinationNameWithSelector() throws Exception {
		doTestReceive(false, false, false, false, true, false, JmsTemplate.RECEIVE_TIMEOUT_NO_WAIT);
	}

	@Test
	void testReceiveAndConvertDefaultDestination() throws Exception {
		doTestReceive(true, true, true, false, false, false, 1000);
	}

	@Test
	void testReceiveAndConvertDefaultDestinationName() throws Exception {
		doTestReceive(false, true, true, false, false, false, 1000);
	}

	@Test
	void testReceiveAndConvertDestinationName() throws Exception {
		doTestReceive(false, false, true, false, false, true, JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
	}

	@Test
	void testReceiveAndConvertDestination() throws Exception {
		doTestReceive(true, false, true, false, false, true, 1000);
	}

	@Test
	void testReceiveAndConvertDefaultDestinationWithSelector() throws Exception {
		doTestReceive(true, true, true, false, true, true, JmsTemplate.RECEIVE_TIMEOUT_NO_WAIT);
	}

	@Test
	void testReceiveAndConvertDestinationNameWithSelector() throws Exception {
		doTestReceive(false, false, true, false, true, true, JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
	}

	@Test
	void testReceiveAndConvertDestinationWithSelector() throws Exception {
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

		MessageConsumer messageConsumer = mock();

		String selectorString = "selector";
		given(this.session.createConsumer(this.queue,
				messageSelector ? selectorString : null)).willReturn(messageConsumer);

		if (!useTransactedTemplate() && !useTransactedSession()) {
			given(this.session.getAcknowledgeMode()).willReturn(
					clientAcknowledge ? Session.CLIENT_ACKNOWLEDGE
							: Session.AUTO_ACKNOWLEDGE);
		}

		TextMessage textMessage = mock();

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
			assertThat(textFromMessage).as("Message text should be equal").isEqualTo("Hello World!");
		}
		else {
			assertThat(textMessage).as("Messages should refer to the same object").isEqualTo(message);
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
	void testSendAndReceiveDefaultDestination() throws Exception {
		doTestSendAndReceive(true, true, 1000L);
	}

	@Test
	void testSendAndReceiveDefaultDestinationName() throws Exception {
		doTestSendAndReceive(false, true, 1000L);
	}

	@Test
	void testSendAndReceiveDestination() throws Exception {
		doTestSendAndReceive(true, false, 1000L);
	}

	@Test
	void testSendAndReceiveDestinationName() throws Exception {
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
		TemporaryQueue replyDestination = mock();
		MessageProducer messageProducer = mock();
		given(localSession.createProducer(this.queue)).willReturn(messageProducer);
		given(localSession.createTemporaryQueue()).willReturn(replyDestination);

		MessageConsumer messageConsumer = mock();
		given(localSession.createConsumer(replyDestination)).willReturn(messageConsumer);


		TextMessage request = mock();
		MessageCreator messageCreator = mock();
		given(messageCreator.createMessage(localSession)).willReturn(request);

		TextMessage reply = mock();
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
		assertThat(message).as("Reply message not received").isSameAs(reply);
		verify(this.connection).start();
		verify(this.connection).close();
		verify(localSession).close();
		verify(messageConsumer).close();
		verify(messageProducer).close();
	}

	@Test
	void testIllegalStateException() throws Exception {
		doTestJmsException(new jakarta.jms.IllegalStateException(""), org.springframework.jms.IllegalStateException.class);
	}

	@Test
	void testInvalidClientIDException() throws Exception {
		doTestJmsException(new jakarta.jms.InvalidClientIDException(""), InvalidClientIDException.class);
	}

	@Test
	void testInvalidDestinationException() throws Exception {
		doTestJmsException(new jakarta.jms.InvalidDestinationException(""), InvalidDestinationException.class);
	}

	@Test
	void testInvalidSelectorException() throws Exception {
		doTestJmsException(new jakarta.jms.InvalidSelectorException(""), InvalidSelectorException.class);
	}

	@Test
	void testJmsSecurityException() throws Exception {
		doTestJmsException(new jakarta.jms.JMSSecurityException(""), JmsSecurityException.class);
	}

	@Test
	void testMessageEOFException() throws Exception {
		doTestJmsException(new jakarta.jms.MessageEOFException(""), MessageEOFException.class);
	}

	@Test
	void testMessageFormatException() throws Exception {
		doTestJmsException(new jakarta.jms.MessageFormatException(""), MessageFormatException.class);
	}

	@Test
	void testMessageNotReadableException() throws Exception {
		doTestJmsException(new jakarta.jms.MessageNotReadableException(""), MessageNotReadableException.class);
	}

	@Test
	void testMessageNotWriteableException() throws Exception {
		doTestJmsException(new jakarta.jms.MessageNotWriteableException(""), MessageNotWriteableException.class);
	}

	@Test
	void testResourceAllocationException() throws Exception {
		doTestJmsException(new jakarta.jms.ResourceAllocationException(""), ResourceAllocationException.class);
	}

	@Test
	void testTransactionInProgressException() throws Exception {
		doTestJmsException(new jakarta.jms.TransactionInProgressException(""), TransactionInProgressException.class);
	}

	@Test
	void testTransactionRolledBackException() throws Exception {
		doTestJmsException(new jakarta.jms.TransactionRolledBackException(""), TransactionRolledBackException.class);
	}

	@Test
	void testUncategorizedJmsException() throws Exception {
		doTestJmsException(new jakarta.jms.JMSException(""), UncategorizedJmsException.class);
	}

	protected void doTestJmsException(JMSException original, Class<? extends JmsException> thrownExceptionClass) throws Exception {
		JmsTemplate template = createTemplate();
		template.setConnectionFactory(this.connectionFactory);
		template.setMessageConverter(new SimpleMessageConverter());
		String s = "Hello world";

		MessageProducer messageProducer = mock();
		TextMessage textMessage = mock();

		reset(this.session);
		given(this.session.createProducer(this.queue)).willReturn(messageProducer);
		given(this.session.createTextMessage("Hello world")).willReturn(textMessage);

		willThrow(original).given(messageProducer).send(textMessage);

		assertThatExceptionOfType(thrownExceptionClass).isThrownBy(() ->
				template.convertAndSend(this.queue, s))
			.withCause(original);

		verify(messageProducer).close();
		verify(this.session).close();
		verify(this.connection).close();
	}

}
