/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.jms.listener.adapter;

import java.io.ByteArrayInputStream;
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

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.SimpleMessageConverter;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

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
		TextMessage textMessage = mock(TextMessage.class);
		// TextMessage contents must be unwrapped...
		given(textMessage.getText()).willReturn(TEXT);

		MessageContentsDelegate delegate = mock(MessageContentsDelegate.class);

		MessageListenerAdapter adapter = new MessageListenerAdapter(delegate);
		adapter.onMessage(textMessage);

		verify(delegate).handleMessage(TEXT);
	}

	@Test
	public void testWithMessageContentsDelegateForBytesMessage() throws Exception {
		BytesMessage bytesMessage = mock(BytesMessage.class);
		// BytesMessage contents must be unwrapped...
		given(bytesMessage.getBodyLength()).willReturn(new Long(TEXT.getBytes().length));
		given(bytesMessage.readBytes(any(byte[].class))).willAnswer(new Answer<Integer>() {
			@Override
			public Integer answer(InvocationOnMock invocation) throws Throwable {
				byte[] bytes = (byte[]) invocation.getArguments()[0];
				ByteArrayInputStream inputStream = new ByteArrayInputStream(TEXT.getBytes());
				return inputStream.read(bytes);
			}
		});

		MessageContentsDelegate delegate = mock(MessageContentsDelegate.class);

		MessageListenerAdapter adapter = new MessageListenerAdapter(delegate);
		adapter.onMessage(bytesMessage);

		verify(delegate).handleMessage(TEXT.getBytes());
	}

	@Test
	public void testWithMessageContentsDelegateForObjectMessage() throws Exception {
		ObjectMessage objectMessage = mock(ObjectMessage.class);
		given(objectMessage.getObject()).willReturn(NUMBER);

		MessageContentsDelegate delegate = mock(MessageContentsDelegate.class);

		MessageListenerAdapter adapter = new MessageListenerAdapter(delegate);
		adapter.onMessage(objectMessage);

		verify(delegate).handleMessage(NUMBER);
	}

	@Test
	public void testWithMessageContentsDelegateForObjectMessageWithPlainObject() throws Exception {
		ObjectMessage objectMessage = mock(ObjectMessage.class);
		given(objectMessage.getObject()).willReturn(OBJECT);

		MessageContentsDelegate delegate = mock(MessageContentsDelegate.class);

		MessageListenerAdapter adapter = new MessageListenerAdapter(delegate);
		adapter.onMessage(objectMessage);

		verify(delegate).handleMessage(OBJECT);
	}

	@Test
	public void testWithMessageDelegate() throws Exception {
		TextMessage textMessage = mock(TextMessage.class);

		MessageDelegate delegate = mock(MessageDelegate.class);

		MessageListenerAdapter adapter = new MessageListenerAdapter(delegate);
		// we DON'T want the default SimpleMessageConversion happening...
		adapter.setMessageConverter(null);
		adapter.onMessage(textMessage);

		verify(delegate).handleMessage(textMessage);
	}

	@Test
	public void testWhenTheAdapterItselfIsTheDelegate() throws Exception {
		TextMessage textMessage = mock(TextMessage.class);
		// TextMessage contents must be unwrapped...
		given(textMessage.getText()).willReturn(TEXT);

		StubMessageListenerAdapter adapter = new StubMessageListenerAdapter();
		adapter.onMessage(textMessage);
		assertTrue(adapter.wasCalled());
	}

	@Test
	public void testRainyDayWithNoApplicableHandlingMethods() throws Exception {
		TextMessage textMessage = mock(TextMessage.class);
		// TextMessage contents must be unwrapped...
		given(textMessage.getText()).willReturn(TEXT);

		StubMessageListenerAdapter adapter = new StubMessageListenerAdapter();
		adapter.setDefaultListenerMethod("walnutsRock");
		adapter.onMessage(textMessage);
		assertFalse(adapter.wasCalled());
	}

	@Test
	public void testThatAnExceptionThrownFromTheHandlingMethodIsSimplySwallowedByDefault() throws Exception {
		final IllegalArgumentException exception = new IllegalArgumentException();

		TextMessage textMessage = mock(TextMessage.class);
		MessageDelegate delegate = mock(MessageDelegate.class);
		willThrow(exception).given(delegate).handleMessage(textMessage);

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
		TextMessage textMessage = mock(TextMessage.class);
		ResponsiveMessageDelegate delegate = mock(ResponsiveMessageDelegate.class);
		given(delegate.handleMessage(textMessage)).willReturn(TEXT);

		MessageListenerAdapter adapter = new MessageListenerAdapter(delegate);
		// we DON'T want the default SimpleMessageConversion happening...
		adapter.setMessageConverter(null);
		adapter.onMessage(textMessage);
	}

	@Test
	public void testWithResponsiveMessageDelegateWithDefaultDestination_SendsReturnTextMessageWhenSessionSupplied() throws Exception {
		Queue destination = mock(Queue.class);
		TextMessage sentTextMessage = mock(TextMessage.class);
		// correlation ID is queried when response is being created...
		given(sentTextMessage.getJMSCorrelationID()).willReturn(
				CORRELATION_ID);
		// Reply-To is queried when response is being created...
		given(sentTextMessage.getJMSReplyTo()).willReturn(null); // we want to fall back to the default...

		TextMessage responseTextMessage = mock(TextMessage.class);

		QueueSender queueSender = mock(QueueSender.class);
		Session session = mock(Session.class);
		given(session.createTextMessage(RESPONSE_TEXT)).willReturn(responseTextMessage);
		given(session.createProducer(destination)).willReturn(queueSender);

		ResponsiveMessageDelegate delegate = mock(ResponsiveMessageDelegate.class);
		given(delegate.handleMessage(sentTextMessage)).willReturn(RESPONSE_TEXT);

		MessageListenerAdapter adapter = new MessageListenerAdapter(delegate) {
			@Override
			protected Object extractMessage(Message message) {
				return message;
			}
		};
		adapter.setDefaultResponseDestination(destination);
		adapter.onMessage(sentTextMessage, session);

		verify(responseTextMessage).setJMSCorrelationID(CORRELATION_ID);
		verify(queueSender).send(responseTextMessage);
		verify(queueSender).close();
		verify(delegate).handleMessage(sentTextMessage);
	}

	@Test
	public void testWithResponsiveMessageDelegateNoDefaultDestination_SendsReturnTextMessageWhenSessionSupplied() throws Exception {
		Queue destination = mock(Queue.class);
		TextMessage sentTextMessage = mock(TextMessage.class);
		// correlation ID is queried when response is being created...
		given(sentTextMessage.getJMSCorrelationID()).willReturn(null);
		given(sentTextMessage.getJMSMessageID()).willReturn(CORRELATION_ID);
		// Reply-To is queried when response is being created...
		given(sentTextMessage.getJMSReplyTo()).willReturn(destination);

		TextMessage responseTextMessage = mock(TextMessage.class);
		MessageProducer messageProducer = mock(MessageProducer.class);
		Session session = mock(Session.class);
		given(session.createTextMessage(RESPONSE_TEXT)).willReturn(responseTextMessage);
		given(session.createProducer(destination)).willReturn(messageProducer);

		ResponsiveMessageDelegate delegate = mock(ResponsiveMessageDelegate.class);
		given(delegate.handleMessage(sentTextMessage)).willReturn(RESPONSE_TEXT);

		MessageListenerAdapter adapter = new MessageListenerAdapter(delegate) {
			@Override
			protected Object extractMessage(Message message) {
				return message;
			}
		};
		adapter.onMessage(sentTextMessage, session);

		verify(responseTextMessage).setJMSCorrelationID(CORRELATION_ID);
		verify(messageProducer).send(responseTextMessage);
		verify(messageProducer).close();
		verify(delegate).handleMessage(sentTextMessage);
	}

	@Test
	public void testWithResponsiveMessageDelegateNoDefaultDestinationAndNoReplyToDestination_SendsReturnTextMessageWhenSessionSupplied() throws Exception {
		final TextMessage sentTextMessage = mock(TextMessage.class);
		// correlation ID is queried when response is being created...
		given(sentTextMessage.getJMSCorrelationID()).willReturn(CORRELATION_ID);
		// Reply-To is queried when response is being created...
		given(sentTextMessage.getJMSReplyTo()).willReturn(null);

		TextMessage responseTextMessage = mock(TextMessage.class);
		final QueueSession session = mock(QueueSession.class);
		given(session.createTextMessage(RESPONSE_TEXT)).willReturn(responseTextMessage);

		ResponsiveMessageDelegate delegate = mock(ResponsiveMessageDelegate.class);
		given(delegate.handleMessage(sentTextMessage)).willReturn(RESPONSE_TEXT);

		final MessageListenerAdapter adapter = new MessageListenerAdapter(delegate) {
			@Override
			protected Object extractMessage(Message message) {
				return message;
			}
		};
		try {
			adapter.onMessage(sentTextMessage, session);
			fail("expected CouldNotSendReplyException with InvalidDestinationException");
		}
		catch (ReplyFailureException ex) {
			assertEquals(InvalidDestinationException.class, ex.getCause().getClass());
		}

		verify(responseTextMessage).setJMSCorrelationID(CORRELATION_ID);
		verify(delegate).handleMessage(sentTextMessage);
	}

	@Test
	public void testWithResponsiveMessageDelegateNoDefaultDestination_SendsReturnTextMessageWhenSessionSupplied_AndSendingThrowsJMSException() throws Exception {
		Queue destination = mock(Queue.class);

		final TextMessage sentTextMessage = mock(TextMessage.class);
		// correlation ID is queried when response is being created...
		given(sentTextMessage.getJMSCorrelationID()).willReturn(CORRELATION_ID);
		// Reply-To is queried when response is being created...
		given(sentTextMessage.getJMSReplyTo()).willReturn(destination);

		TextMessage responseTextMessage = mock(TextMessage.class);
		MessageProducer messageProducer = mock(MessageProducer.class);
		willThrow(new JMSException("Doe!")).given(messageProducer).send(responseTextMessage);

		final QueueSession session = mock(QueueSession.class);
		given(session.createTextMessage(RESPONSE_TEXT)).willReturn(responseTextMessage);
		given(session.createProducer(destination)).willReturn(messageProducer);

		ResponsiveMessageDelegate delegate = mock(ResponsiveMessageDelegate.class);
		given(delegate.handleMessage(sentTextMessage)).willReturn(RESPONSE_TEXT);

		final MessageListenerAdapter adapter = new MessageListenerAdapter(delegate) {
			@Override
			protected Object extractMessage(Message message) {
				return message;
			}
		};
		try {
			adapter.onMessage(sentTextMessage, session);
			fail("expected CouldNotSendReplyException with JMSException");
		}
		catch (ReplyFailureException ex) {
			assertEquals(JMSException.class, ex.getCause().getClass());
		}

		verify(responseTextMessage).setJMSCorrelationID(CORRELATION_ID);
		verify(messageProducer).close();
		verify(delegate).handleMessage(sentTextMessage);
	}

	@Test
	public void testWithResponsiveMessageDelegateDoesNotSendReturnTextMessageWhenSessionSupplied_AndListenerMethodThrowsException() throws Exception {
		final TextMessage message = mock(TextMessage.class);
		final QueueSession session = mock(QueueSession.class);

		ResponsiveMessageDelegate delegate = mock(ResponsiveMessageDelegate.class);
		willThrow(new IllegalArgumentException("Doe!")).given(delegate).handleMessage(message);

		final MessageListenerAdapter adapter = new MessageListenerAdapter(delegate) {
			@Override
			protected Object extractMessage(Message message) {
				return message;
			}
		};
		try {
			adapter.onMessage(message, session);
			fail("expected ListenerExecutionFailedException");
		}
		catch (ListenerExecutionFailedException ex) { /* expected */ }
	}

	@Test
	public void testFailsIfNoDefaultListenerMethodNameIsSupplied() throws Exception {
		final TextMessage message = mock(TextMessage.class);
		given(message.getText()).willReturn(TEXT);

		final MessageListenerAdapter adapter = new MessageListenerAdapter() {
			@Override
			protected void handleListenerException(Throwable ex) {
				assertTrue(ex instanceof IllegalStateException);
			}
		};
		adapter.setDefaultListenerMethod(null);
		adapter.onMessage(message);
	}

	@Test
	public void testFailsWhenOverriddenGetListenerMethodNameReturnsNull() throws Exception {
		final TextMessage message = mock(TextMessage.class);
		given(message.getText()).willReturn(TEXT);

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
	}

	@Test
	public void testWithResponsiveMessageDelegateWhenReturnTypeIsNotAJMSMessageAndNoMessageConverterIsSupplied() throws Exception {
		final TextMessage sentTextMessage = mock(TextMessage.class);
		final Session session = mock(Session.class);
		ResponsiveMessageDelegate delegate = mock(ResponsiveMessageDelegate.class);
		given(delegate.handleMessage(sentTextMessage)).willReturn(RESPONSE_TEXT);

		final MessageListenerAdapter adapter = new MessageListenerAdapter(delegate) {
			@Override
			protected Object extractMessage(Message message) {
				return message;
			}
		};
		adapter.setMessageConverter(null);
		try {
			adapter.onMessage(sentTextMessage, session);
			fail("expected CouldNotSendReplyException with MessageConversionException");
		}
		catch (ReplyFailureException ex) {
			assertEquals(MessageConversionException.class, ex.getCause().getClass());
		}
	}

	@Test
	public void testWithResponsiveMessageDelegateWhenReturnTypeIsAJMSMessageAndNoMessageConverterIsSupplied() throws Exception {
		Queue destination = mock(Queue.class);
		final TextMessage sentTextMessage = mock(TextMessage.class);
		// correlation ID is queried when response is being created...
		given(sentTextMessage.getJMSCorrelationID()).willReturn(CORRELATION_ID);
		// Reply-To is queried when response is being created...
		given(sentTextMessage.getJMSReplyTo()).willReturn(destination);

		TextMessage responseMessage = mock(TextMessage.class);
		QueueSender queueSender = mock(QueueSender.class);

		Session session = mock(Session.class);
		given(session.createProducer(destination)).willReturn(queueSender);

		ResponsiveJmsTextMessageReturningMessageDelegate delegate = mock(ResponsiveJmsTextMessageReturningMessageDelegate.class);
		given(delegate.handleMessage(sentTextMessage)).willReturn(responseMessage);

		final MessageListenerAdapter adapter = new MessageListenerAdapter(delegate) {
			@Override
			protected Object extractMessage(Message message) {
				return message;
			}
		};
		adapter.setMessageConverter(null);
		adapter.onMessage(sentTextMessage, session);

		verify(responseMessage).setJMSCorrelationID(CORRELATION_ID);
		verify(queueSender).send(responseMessage);
		verify(queueSender).close();
	}


	@SuppressWarnings("serial")
	private static class SerializableObject implements Serializable {
	}

}
