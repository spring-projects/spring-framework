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

package org.springframework.jms.listener.adapter;

import static org.junit.Assert.*;

import java.io.Serializable;

import javax.jms.BytesMessage;
import javax.jms.IllegalStateException;
import javax.jms.InvalidDestinationException;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.easymock.MockControl;
import org.junit.Test;

import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.SimpleMessageConverter;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class MessageListenerAdapterTests {

	private static final String TEXT = "I fancy a good cuppa right now";

	private static final Integer NUMBER = new Integer(1);

	private static final SerializableObject OBJECT = new SerializableObject();

	private static final String CORRELATION_ID = "100";

	private static final String RESPONSE_TEXT = "... wi' some full fat creamy milk. Top banana.";


	@Test
	public void testWithMessageContentsDelegateForTextMessage() throws Exception {
		MockControl mockTextMessage = MockControl.createControl(TextMessage.class);
		TextMessage textMessage = (TextMessage) mockTextMessage.getMock();
		// TextMessage contents must be unwrapped...
		textMessage.getText();
		mockTextMessage.setReturnValue(TEXT);
		mockTextMessage.replay();

		MockControl mockDelegate = MockControl.createControl(MessageContentsDelegate.class);
		MessageContentsDelegate delegate = (MessageContentsDelegate) mockDelegate.getMock();
		delegate.handleMessage(TEXT);
		mockDelegate.setVoidCallable();
		mockDelegate.replay();

		MessageListenerAdapter adapter = new MessageListenerAdapter(delegate);
		adapter.onMessage(textMessage);

		mockDelegate.verify();
		mockTextMessage.verify();
	}

	@Test
	public void testWithMessageContentsDelegateForBytesMessage() throws Exception {
		MockControl mockBytesMessage = MockControl.createControl(BytesMessage.class);
		BytesMessage bytesMessage = (BytesMessage) mockBytesMessage.getMock();
		// BytesMessage contents must be unwrapped...
		bytesMessage.getBodyLength();
		mockBytesMessage.setReturnValue(TEXT.getBytes().length);
		bytesMessage.readBytes(null);
		mockBytesMessage.setMatcher(MockControl.ALWAYS_MATCHER);
		mockBytesMessage.setReturnValue(TEXT.getBytes().length);
		mockBytesMessage.replay();

		MockControl mockDelegate = MockControl.createControl(MessageContentsDelegate.class);
		MessageContentsDelegate delegate = (MessageContentsDelegate) mockDelegate.getMock();
		delegate.handleMessage(TEXT.getBytes());
		mockDelegate.setMatcher(MockControl.ALWAYS_MATCHER);
		mockDelegate.setVoidCallable();
		mockDelegate.replay();

		MessageListenerAdapter adapter = new MessageListenerAdapter(delegate);
		adapter.onMessage(bytesMessage);

		mockDelegate.verify();
		mockBytesMessage.verify();
	}

	@Test
	public void testWithMessageContentsDelegateForObjectMessage() throws Exception {
		MockControl mockObjectMessage = MockControl.createControl(ObjectMessage.class);
		ObjectMessage objectMessage = (ObjectMessage) mockObjectMessage.getMock();
		objectMessage.getObject();
		mockObjectMessage.setReturnValue(NUMBER);
		mockObjectMessage.replay();

		MockControl mockDelegate = MockControl.createControl(MessageContentsDelegate.class);
		MessageContentsDelegate delegate = (MessageContentsDelegate) mockDelegate.getMock();
		delegate.handleMessage(NUMBER);
		mockDelegate.setVoidCallable();
		mockDelegate.replay();

		MessageListenerAdapter adapter = new MessageListenerAdapter(delegate);
		adapter.onMessage(objectMessage);

		mockDelegate.verify();
		mockObjectMessage.verify();
	}

	@Test
	public void testWithMessageContentsDelegateForObjectMessageWithPlainObject() throws Exception {
		MockControl mockObjectMessage = MockControl.createControl(ObjectMessage.class);
		ObjectMessage objectMessage = (ObjectMessage) mockObjectMessage.getMock();
		objectMessage.getObject();
		mockObjectMessage.setReturnValue(OBJECT);
		mockObjectMessage.replay();

		MockControl mockDelegate = MockControl.createControl(MessageContentsDelegate.class);
		MessageContentsDelegate delegate = (MessageContentsDelegate) mockDelegate.getMock();
		delegate.handleMessage(OBJECT);
		mockDelegate.setVoidCallable();
		mockDelegate.replay();

		MessageListenerAdapter adapter = new MessageListenerAdapter(delegate);
		adapter.onMessage(objectMessage);

		mockDelegate.verify();
		mockObjectMessage.verify();
	}

	@Test
	public void testWithMessageDelegate() throws Exception {
		MockControl mockTextMessage = MockControl.createControl(TextMessage.class);
		TextMessage textMessage = (TextMessage) mockTextMessage.getMock();
		mockTextMessage.replay();

		MockControl mockDelegate = MockControl.createControl(MessageDelegate.class);
		MessageDelegate delegate = (MessageDelegate) mockDelegate.getMock();
		delegate.handleMessage(textMessage);
		mockDelegate.setVoidCallable();
		mockDelegate.replay();

		MessageListenerAdapter adapter = new MessageListenerAdapter(delegate);
		// we DON'T want the default SimpleMessageConversion happening...
		adapter.setMessageConverter(null);
		adapter.onMessage(textMessage);

		mockDelegate.verify();
		mockTextMessage.verify();
	}

	@Test
	public void testWhenTheAdapterItselfIsTheDelegate() throws Exception {
		MockControl mockTextMessage = MockControl.createControl(TextMessage.class);
		TextMessage textMessage = (TextMessage) mockTextMessage.getMock();
		// TextMessage contents must be unwrapped...
		textMessage.getText();
		mockTextMessage.setReturnValue(TEXT);
		mockTextMessage.replay();

		StubMessageListenerAdapter adapter = new StubMessageListenerAdapter();
		adapter.onMessage(textMessage);
		assertTrue(adapter.wasCalled());

		mockTextMessage.verify();
	}

	@Test
	public void testRainyDayWithNoApplicableHandlingMethods() throws Exception {
		MockControl mockTextMessage = MockControl.createControl(TextMessage.class);
		TextMessage textMessage = (TextMessage) mockTextMessage.getMock();
		// TextMessage contents must be unwrapped...
		textMessage.getText();
		mockTextMessage.setReturnValue(TEXT);
		mockTextMessage.replay();

		StubMessageListenerAdapter adapter = new StubMessageListenerAdapter();
		adapter.setDefaultListenerMethod("walnutsRock");
		adapter.onMessage(textMessage);
		assertFalse(adapter.wasCalled());

		mockTextMessage.verify();
	}

	@Test
	public void testThatAnExceptionThrownFromTheHandlingMethodIsSimplySwallowedByDefault() throws Exception {
		final IllegalArgumentException exception = new IllegalArgumentException();

		MockControl mockTextMessage = MockControl.createControl(TextMessage.class);
		TextMessage textMessage = (TextMessage) mockTextMessage.getMock();
		mockTextMessage.replay();

		MockControl mockDelegate = MockControl.createControl(MessageDelegate.class);
		MessageDelegate delegate = (MessageDelegate) mockDelegate.getMock();
		delegate.handleMessage(textMessage);
		mockDelegate.setThrowable(exception);
		mockDelegate.replay();

		MessageListenerAdapter adapter = new MessageListenerAdapter(delegate) {
			@Override
			protected void handleListenerException(Throwable ex) {
				assertNotNull("The Throwable passed to the handleListenerException(..) method must never be null.", ex);
				assertTrue("The Throwable passed to the handleListenerException(..) method must be of type [ListenerExecutionFailedException].",
						ex instanceof ListenerExecutionFailedException);
				ListenerExecutionFailedException lefx = (ListenerExecutionFailedException) ex;
				Throwable cause = lefx.getCause();
				assertNotNull("The cause of a ListenerExecutionFailedException must be preserved.", cause);
				assertSame(exception, cause);
			}
		};
		// we DON'T want the default SimpleMessageConversion happening...
		adapter.setMessageConverter(null);
		adapter.onMessage(textMessage);

		mockDelegate.verify();
		mockTextMessage.verify();
	}

	@Test
	public void testThatTheDefaultMessageConverterisIndeedTheSimpleMessageConverter() throws Exception {
		MessageListenerAdapter adapter = new MessageListenerAdapter();
		assertNotNull("The default [MessageConverter] must never be null.", adapter.getMessageConverter());
		assertTrue("The default [MessageConverter] must be of the type [SimpleMessageConverter]",
				adapter.getMessageConverter() instanceof SimpleMessageConverter);
	}

	@Test
	public void testThatWhenNoDelegateIsSuppliedTheDelegateIsAssumedToBeTheMessageListenerAdapterItself() throws Exception {
		MessageListenerAdapter adapter = new MessageListenerAdapter();
		assertSame(adapter, adapter.getDelegate());
	}

	@Test
	public void testThatTheDefaultMessageHandlingMethodNameIsTheConstantDefault() throws Exception {
		MessageListenerAdapter adapter = new MessageListenerAdapter();
		assertEquals(MessageListenerAdapter.ORIGINAL_DEFAULT_LISTENER_METHOD, adapter.getDefaultListenerMethod());
	}

	@Test
	public void testWithResponsiveMessageDelegate_DoesNotSendReturnTextMessageIfNoSessionSupplied() throws Exception {
		MockControl mockTextMessage = MockControl.createControl(TextMessage.class);
		TextMessage textMessage = (TextMessage) mockTextMessage.getMock();
		mockTextMessage.replay();

		MockControl mockDelegate = MockControl.createControl(ResponsiveMessageDelegate.class);
		ResponsiveMessageDelegate delegate = (ResponsiveMessageDelegate) mockDelegate.getMock();
		delegate.handleMessage(textMessage);
		mockDelegate.setReturnValue(TEXT);
		mockDelegate.replay();

		MessageListenerAdapter adapter = new MessageListenerAdapter(delegate);
		// we DON'T want the default SimpleMessageConversion happening...
		adapter.setMessageConverter(null);
		adapter.onMessage(textMessage);

		mockDelegate.verify();
		mockTextMessage.verify();
	}

	@Test
	public void testWithResponsiveMessageDelegateWithDefaultDestination_SendsReturnTextMessageWhenSessionSupplied() throws Exception {
		MockControl mockDestination = MockControl.createControl(Queue.class);
		Queue destination = (Queue) mockDestination.getMock();
		mockDestination.replay();

		MockControl mockSentTextMessage = MockControl.createControl(TextMessage.class);
		TextMessage sentTextMessage = (TextMessage) mockSentTextMessage.getMock();
		// correlation ID is queried when response is being created...
		sentTextMessage.getJMSCorrelationID();
		mockSentTextMessage.setReturnValue(CORRELATION_ID);
		// Reply-To is queried when response is being created...
		sentTextMessage.getJMSReplyTo();
		mockSentTextMessage.setReturnValue(null); // we want to fall back to the default...
		mockSentTextMessage.replay();

		MockControl mockResponseTextMessage = MockControl.createControl(TextMessage.class);
		TextMessage responseTextMessage = (TextMessage) mockResponseTextMessage.getMock();
		responseTextMessage.setJMSCorrelationID(CORRELATION_ID);
		mockResponseTextMessage.setVoidCallable();
		mockResponseTextMessage.replay();

		MockControl mockQueueSender = MockControl.createControl(QueueSender.class);
		QueueSender queueSender = (QueueSender) mockQueueSender.getMock();
		queueSender.send(responseTextMessage);
		mockQueueSender.setVoidCallable();
		queueSender.close();
		mockQueueSender.setVoidCallable();
		mockQueueSender.replay();

		MockControl mockSession = MockControl.createControl(Session.class);
		Session session = (Session) mockSession.getMock();
		session.createTextMessage(RESPONSE_TEXT);
		mockSession.setReturnValue(responseTextMessage);
		session.createProducer(destination);
		mockSession.setReturnValue(queueSender);
		mockSession.replay();

		MockControl mockDelegate = MockControl.createControl(ResponsiveMessageDelegate.class);
		ResponsiveMessageDelegate delegate = (ResponsiveMessageDelegate) mockDelegate.getMock();
		delegate.handleMessage(sentTextMessage);
		mockDelegate.setReturnValue(RESPONSE_TEXT);
		mockDelegate.replay();

		MessageListenerAdapter adapter = new MessageListenerAdapter(delegate) {
			@Override
			protected Object extractMessage(Message message) {
				return message;
			}
		};
		adapter.setDefaultResponseDestination(destination);
		adapter.onMessage(sentTextMessage, session);

		mockDelegate.verify();
		mockSentTextMessage.verify();
		mockResponseTextMessage.verify();
		mockSession.verify();
		mockDestination.verify();
		mockQueueSender.verify();
	}

	@Test
	public void testWithResponsiveMessageDelegateNoDefaultDestination_SendsReturnTextMessageWhenSessionSupplied() throws Exception {
		MockControl mockDestination = MockControl.createControl(Queue.class);
		Queue destination = (Queue) mockDestination.getMock();
		mockDestination.replay();

		MockControl mockSentTextMessage = MockControl.createControl(TextMessage.class);
		TextMessage sentTextMessage = (TextMessage) mockSentTextMessage.getMock();
		// correlation ID is queried when response is being created...
		sentTextMessage.getJMSCorrelationID();
		mockSentTextMessage.setReturnValue(null);
		sentTextMessage.getJMSMessageID();
		mockSentTextMessage.setReturnValue(CORRELATION_ID);
		// Reply-To is queried when response is being created...
		sentTextMessage.getJMSReplyTo();
		mockSentTextMessage.setReturnValue(destination);
		mockSentTextMessage.replay();

		MockControl mockResponseTextMessage = MockControl.createControl(TextMessage.class);
		TextMessage responseTextMessage = (TextMessage) mockResponseTextMessage.getMock();
		responseTextMessage.setJMSCorrelationID(CORRELATION_ID);
		mockResponseTextMessage.setVoidCallable();
		mockResponseTextMessage.replay();

		MockControl mockMessageProducer = MockControl.createControl(MessageProducer.class);
		MessageProducer messageProducer = (MessageProducer) mockMessageProducer.getMock();
		messageProducer.send(responseTextMessage);
		mockMessageProducer.setVoidCallable();
		messageProducer.close();
		mockMessageProducer.setVoidCallable();
		mockMessageProducer.replay();

		MockControl mockSession = MockControl.createControl(Session.class);
		Session session = (Session) mockSession.getMock();
		session.createTextMessage(RESPONSE_TEXT);
		mockSession.setReturnValue(responseTextMessage);
		session.createProducer(destination);
		mockSession.setReturnValue(messageProducer);
		mockSession.replay();

		MockControl mockDelegate = MockControl.createControl(ResponsiveMessageDelegate.class);
		ResponsiveMessageDelegate delegate = (ResponsiveMessageDelegate) mockDelegate.getMock();
		delegate.handleMessage(sentTextMessage);
		mockDelegate.setReturnValue(RESPONSE_TEXT);
		mockDelegate.replay();

		MessageListenerAdapter adapter = new MessageListenerAdapter(delegate) {
			@Override
			protected Object extractMessage(Message message) {
				return message;
			}
		};
		adapter.onMessage(sentTextMessage, session);

		mockDelegate.verify();
		mockSentTextMessage.verify();
		mockResponseTextMessage.verify();
		mockSession.verify();
		mockDestination.verify();
		mockMessageProducer.verify();
	}

	@Test
	public void testWithResponsiveMessageDelegateNoDefaultDestinationAndNoReplyToDestination_SendsReturnTextMessageWhenSessionSupplied() throws Exception {
		MockControl mockSentTextMessage = MockControl.createControl(TextMessage.class);
		final TextMessage sentTextMessage = (TextMessage) mockSentTextMessage.getMock();
		// correlation ID is queried when response is being created...
		sentTextMessage.getJMSCorrelationID();
		mockSentTextMessage.setReturnValue(CORRELATION_ID);
		// Reply-To is queried when response is being created...
		sentTextMessage.getJMSReplyTo();
		mockSentTextMessage.setReturnValue(null);
		mockSentTextMessage.replay();

		MockControl mockResponseTextMessage = MockControl.createControl(TextMessage.class);
		TextMessage responseTextMessage = (TextMessage) mockResponseTextMessage.getMock();
		responseTextMessage.setJMSCorrelationID(CORRELATION_ID);
		mockResponseTextMessage.setVoidCallable();
		mockResponseTextMessage.replay();

		MockControl mockSession = MockControl.createControl(QueueSession.class);
		final QueueSession session = (QueueSession) mockSession.getMock();
		session.createTextMessage(RESPONSE_TEXT);
		mockSession.setReturnValue(responseTextMessage);
		mockSession.replay();

		MockControl mockDelegate = MockControl.createControl(ResponsiveMessageDelegate.class);
		ResponsiveMessageDelegate delegate = (ResponsiveMessageDelegate) mockDelegate.getMock();
		delegate.handleMessage(sentTextMessage);
		mockDelegate.setReturnValue(RESPONSE_TEXT);
		mockDelegate.replay();

		final MessageListenerAdapter adapter = new MessageListenerAdapter(delegate) {
			@Override
			protected Object extractMessage(Message message) {
				return message;
			}
		};
		try {
			adapter.onMessage(sentTextMessage, session);
			fail("expected InvalidDestinationException");
		} catch(InvalidDestinationException ex) { /* expected */ }

		mockDelegate.verify();
		mockSentTextMessage.verify();
		mockResponseTextMessage.verify();
		mockSession.verify();
	}

	@Test
	public void testWithResponsiveMessageDelegateNoDefaultDestination_SendsReturnTextMessageWhenSessionSupplied_AndSendingThrowsJMSException() throws Exception {
		MockControl mockDestination = MockControl.createControl(Queue.class);
		Queue destination = (Queue) mockDestination.getMock();
		mockDestination.replay();

		MockControl mockSentTextMessage = MockControl.createControl(TextMessage.class);
		final TextMessage sentTextMessage = (TextMessage) mockSentTextMessage.getMock();
		// correlation ID is queried when response is being created...
		sentTextMessage.getJMSCorrelationID();
		mockSentTextMessage.setReturnValue(CORRELATION_ID);
		// Reply-To is queried when response is being created...
		sentTextMessage.getJMSReplyTo();
		mockSentTextMessage.setReturnValue(destination);
		mockSentTextMessage.replay();

		MockControl mockResponseTextMessage = MockControl.createControl(TextMessage.class);
		TextMessage responseTextMessage = (TextMessage) mockResponseTextMessage.getMock();
		responseTextMessage.setJMSCorrelationID(CORRELATION_ID);
		mockResponseTextMessage.setVoidCallable();
		mockResponseTextMessage.replay();

		MockControl mockMessageProducer = MockControl.createControl(MessageProducer.class);
		MessageProducer messageProducer = (MessageProducer) mockMessageProducer.getMock();
		messageProducer.send(responseTextMessage);
		mockMessageProducer.setThrowable(new JMSException("Dow!"));
		// ensure that regardless of a JMSException the producer is closed...
		messageProducer.close();
		mockMessageProducer.setVoidCallable();
		mockMessageProducer.replay();

		MockControl mockSession = MockControl.createControl(QueueSession.class);
		final QueueSession session = (QueueSession) mockSession.getMock();
		session.createTextMessage(RESPONSE_TEXT);
		mockSession.setReturnValue(responseTextMessage);
		session.createProducer(destination);
		mockSession.setReturnValue(messageProducer);
		mockSession.replay();

		MockControl mockDelegate = MockControl.createControl(ResponsiveMessageDelegate.class);
		ResponsiveMessageDelegate delegate = (ResponsiveMessageDelegate) mockDelegate.getMock();
		delegate.handleMessage(sentTextMessage);
		mockDelegate.setReturnValue(RESPONSE_TEXT);
		mockDelegate.replay();

		final MessageListenerAdapter adapter = new MessageListenerAdapter(delegate) {
			@Override
			protected Object extractMessage(Message message) {
				return message;
			}
		};
		try {
			adapter.onMessage(sentTextMessage, session);
			fail("expected JMSException");
		} catch(JMSException ex) { /* expected */ }

		mockDelegate.verify();
		mockSentTextMessage.verify();
		mockResponseTextMessage.verify();
		mockSession.verify();
		mockDestination.verify();
		mockMessageProducer.verify();
	}

	@Test
	public void testWithResponsiveMessageDelegateDoesNotSendReturnTextMessageWhenSessionSupplied_AndListenerMethodThrowsException() throws Exception {
		MockControl mockMessage = MockControl.createControl(TextMessage.class);
		final TextMessage message = (TextMessage) mockMessage.getMock();
		mockMessage.replay();

		MockControl mockSession = MockControl.createControl(QueueSession.class);
		final QueueSession session = (QueueSession) mockSession.getMock();
		mockSession.replay();

		MockControl mockDelegate = MockControl.createControl(ResponsiveMessageDelegate.class);
		ResponsiveMessageDelegate delegate = (ResponsiveMessageDelegate) mockDelegate.getMock();
		delegate.handleMessage(message);
		mockDelegate.setThrowable(new IllegalArgumentException("Dow!"));
		mockDelegate.replay();

		final MessageListenerAdapter adapter = new MessageListenerAdapter(delegate) {
			@Override
			protected Object extractMessage(Message message) {
				return message;
			}
		};
		try {
			adapter.onMessage(message, session);
			fail("expected ListenerExecutionFailedException");
		} catch(ListenerExecutionFailedException ex) { /* expected */ }

		mockDelegate.verify();
		mockMessage.verify();
		mockSession.verify();
	}

	@Test
	public void testFailsIfNoDefaultListenerMethodNameIsSupplied() throws Exception {
		MockControl mockMessage = MockControl.createControl(TextMessage.class);
		final TextMessage message = (TextMessage) mockMessage.getMock();
		message.getText();
		mockMessage.setReturnValue(TEXT);

		mockMessage.replay();

		final MessageListenerAdapter adapter = new MessageListenerAdapter() {
			@Override
			protected void handleListenerException(Throwable ex) {
				assertTrue(ex instanceof IllegalStateException);
			}
		};
		adapter.setDefaultListenerMethod(null);
		adapter.onMessage(message);

		mockMessage.verify();
	}

	@Test
	public void testFailsWhenOverriddenGetListenerMethodNameReturnsNull() throws Exception {
		MockControl mockMessage = MockControl.createControl(TextMessage.class);
		final TextMessage message = (TextMessage) mockMessage.getMock();
		message.getText();
		mockMessage.setReturnValue(TEXT);

		mockMessage.replay();

		final MessageListenerAdapter adapter = new MessageListenerAdapter() {
			@Override
			protected void handleListenerException(Throwable ex) {
				assertTrue(ex instanceof javax.jms.IllegalStateException);
			}
			@Override
			protected String getListenerMethodName(Message originalMessage, Object extractedMessage) {
				return null;
			}
		};
		adapter.setDefaultListenerMethod(null);
		adapter.onMessage(message);

		mockMessage.verify();
	}

	@Test
	public void testWithResponsiveMessageDelegateWhenReturnTypeIsNotAJMSMessageAndNoMessageConverterIsSupplied() throws Exception {
		MockControl mockSentTextMessage = MockControl.createControl(TextMessage.class);
		final TextMessage sentTextMessage = (TextMessage) mockSentTextMessage.getMock();
		mockSentTextMessage.replay();

		MockControl mockSession = MockControl.createControl(Session.class);
		final Session session = (Session) mockSession.getMock();
		mockSession.replay();

		MockControl mockDelegate = MockControl.createControl(ResponsiveMessageDelegate.class);
		ResponsiveMessageDelegate delegate = (ResponsiveMessageDelegate) mockDelegate.getMock();
		delegate.handleMessage(sentTextMessage);
		mockDelegate.setReturnValue(RESPONSE_TEXT);
		mockDelegate.replay();

		final MessageListenerAdapter adapter = new MessageListenerAdapter(delegate) {
			@Override
			protected Object extractMessage(Message message) {
				return message;
			}
		};
		adapter.setMessageConverter(null);
		try {
			adapter.onMessage(sentTextMessage, session);
			fail("expected MessageConversionException");
		} catch(MessageConversionException ex) { /* expected */ }

		mockDelegate.verify();
		mockSentTextMessage.verify();
		mockSession.verify();
	}

	@Test
	public void testWithResponsiveMessageDelegateWhenReturnTypeIsAJMSMessageAndNoMessageConverterIsSupplied() throws Exception {
		MockControl mockDestination = MockControl.createControl(Queue.class);
		Queue destination = (Queue) mockDestination.getMock();
		mockDestination.replay();

		MockControl mockSentTextMessage = MockControl.createControl(TextMessage.class);
		final TextMessage sentTextMessage = (TextMessage) mockSentTextMessage.getMock();
		// correlation ID is queried when response is being created...
		sentTextMessage.getJMSCorrelationID();
		mockSentTextMessage.setReturnValue(CORRELATION_ID);
		// Reply-To is queried when response is being created...
		sentTextMessage.getJMSReplyTo();
		mockSentTextMessage.setReturnValue(destination);
		mockSentTextMessage.replay();

		MockControl mockResponseMessage = MockControl.createControl(TextMessage.class);
		TextMessage responseMessage = (TextMessage) mockResponseMessage.getMock();
		responseMessage.setJMSCorrelationID(CORRELATION_ID);
		mockResponseMessage.setVoidCallable();
		mockResponseMessage.replay();

		MockControl mockQueueSender = MockControl.createControl(QueueSender.class);
		QueueSender queueSender = (QueueSender) mockQueueSender.getMock();
		queueSender.send(responseMessage);
		mockQueueSender.setVoidCallable();
		queueSender.close();
		mockQueueSender.setVoidCallable();
		mockQueueSender.replay();

		MockControl mockSession = MockControl.createControl(Session.class);
		Session session = (Session) mockSession.getMock();
		session.createProducer(destination);
		mockSession.setReturnValue(queueSender);
		mockSession.replay();

		MockControl mockDelegate = MockControl.createControl(ResponsiveJmsTextMessageReturningMessageDelegate.class);
		ResponsiveJmsTextMessageReturningMessageDelegate delegate = (ResponsiveJmsTextMessageReturningMessageDelegate) mockDelegate.getMock();
		delegate.handleMessage(sentTextMessage);
		mockDelegate.setReturnValue(responseMessage);
		mockDelegate.replay();

		final MessageListenerAdapter adapter = new MessageListenerAdapter(delegate) {
			@Override
			protected Object extractMessage(Message message) {
				return message;
			}
		};
		adapter.setMessageConverter(null);
		adapter.onMessage(sentTextMessage, session);

		mockDestination.verify();
		mockDelegate.verify();
		mockSentTextMessage.verify();
		mockSession.verify();
		mockQueueSender.verify();
		mockResponseMessage.verify();
	}


	@SuppressWarnings("serial")
	private static class SerializableObject implements Serializable {
	}

}
