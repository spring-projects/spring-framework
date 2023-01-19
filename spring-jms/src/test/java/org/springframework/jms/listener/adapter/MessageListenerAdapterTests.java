/*
 * Copyright 2002-2023 the original author or authors.
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

import jakarta.jms.BytesMessage;
import jakarta.jms.InvalidDestinationException;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageProducer;
import jakarta.jms.ObjectMessage;
import jakarta.jms.Queue;
import jakarta.jms.QueueSender;
import jakarta.jms.QueueSession;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.SimpleMessageConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Chris Beams
 */
class MessageListenerAdapterTests {

	private static final String TEXT = "I fancy a good cuppa right now";

	private static final Integer NUMBER = 1;

	private static final SerializableObject OBJECT = new SerializableObject();

	private static final String CORRELATION_ID = "100";

	private static final String RESPONSE_TEXT = "... wi' some full fat creamy milk. Top banana.";


	@Test
	void testWithMessageContentsDelegateForTextMessage() throws Exception {
		TextMessage textMessage = mock();
		// TextMessage contents must be unwrapped...
		given(textMessage.getText()).willReturn(TEXT);

		MessageContentsDelegate delegate = mock();

		MessageListenerAdapter adapter = new MessageListenerAdapter(delegate);
		adapter.onMessage(textMessage);

		verify(delegate).handleMessage(TEXT);
	}

	@Test
	void testWithMessageContentsDelegateForBytesMessage() throws Exception {
		BytesMessage bytesMessage = mock();
		// BytesMessage contents must be unwrapped...
		given(bytesMessage.getBodyLength()).willReturn(Long.valueOf(TEXT.getBytes().length));
		given(bytesMessage.readBytes(any(byte[].class))).willAnswer((Answer<Integer>) invocation -> {
			byte[] bytes = (byte[]) invocation.getArguments()[0];
			ByteArrayInputStream inputStream = new ByteArrayInputStream(TEXT.getBytes());
			return inputStream.read(bytes);
		});

		MessageContentsDelegate delegate = mock();

		MessageListenerAdapter adapter = new MessageListenerAdapter(delegate);
		adapter.onMessage(bytesMessage);

		verify(delegate).handleMessage(TEXT.getBytes());
	}

	@Test
	void testWithMessageContentsDelegateForObjectMessage() throws Exception {
		ObjectMessage objectMessage = mock();
		given(objectMessage.getObject()).willReturn(NUMBER);

		MessageContentsDelegate delegate = mock();

		MessageListenerAdapter adapter = new MessageListenerAdapter(delegate);
		adapter.onMessage(objectMessage);

		verify(delegate).handleMessage(NUMBER);
	}

	@Test
	void testWithMessageContentsDelegateForObjectMessageWithPlainObject() throws Exception {
		ObjectMessage objectMessage = mock();
		given(objectMessage.getObject()).willReturn(OBJECT);

		MessageContentsDelegate delegate = mock();

		MessageListenerAdapter adapter = new MessageListenerAdapter(delegate);
		adapter.onMessage(objectMessage);

		verify(delegate).handleMessage(OBJECT);
	}

	@Test
	void testWithMessageDelegate() throws Exception {
		TextMessage textMessage = mock();

		MessageDelegate delegate = mock();

		MessageListenerAdapter adapter = new MessageListenerAdapter(delegate);
		// we DON'T want the default SimpleMessageConversion happening...
		adapter.setMessageConverter(null);
		adapter.onMessage(textMessage);

		verify(delegate).handleMessage(textMessage);
	}

	@Test
	void testWhenTheAdapterItselfIsTheDelegate() throws Exception {
		TextMessage textMessage = mock();
		// TextMessage contents must be unwrapped...
		given(textMessage.getText()).willReturn(TEXT);

		StubMessageListenerAdapter adapter = new StubMessageListenerAdapter();
		adapter.onMessage(textMessage);
		assertThat(adapter.wasCalled()).isTrue();
	}

	@Test
	void testRainyDayWithNoApplicableHandlingMethods() throws Exception {
		TextMessage textMessage = mock();
		// TextMessage contents must be unwrapped...
		given(textMessage.getText()).willReturn(TEXT);

		StubMessageListenerAdapter adapter = new StubMessageListenerAdapter();
		adapter.setDefaultListenerMethod("walnutsRock");
		adapter.onMessage(textMessage);
		assertThat(adapter.wasCalled()).isFalse();
	}

	@Test
	void testThatAnExceptionThrownFromTheHandlingMethodIsSimplySwallowedByDefault() throws Exception {
		final IllegalArgumentException exception = new IllegalArgumentException();

		TextMessage textMessage = mock();
		MessageDelegate delegate = mock();
		willThrow(exception).given(delegate).handleMessage(textMessage);

		MessageListenerAdapter adapter = new MessageListenerAdapter(delegate) {
			@Override
			protected void handleListenerException(Throwable ex) {
				assertThat(ex).as("The Throwable passed to the handleListenerException(..) method must never be null.").isNotNull();
				boolean condition = ex instanceof ListenerExecutionFailedException;
				assertThat(condition).as("The Throwable passed to the handleListenerException(..) method must be of type [ListenerExecutionFailedException].").isTrue();
				ListenerExecutionFailedException lefx = (ListenerExecutionFailedException) ex;
				Throwable cause = lefx.getCause();
				assertThat(cause).as("The cause of a ListenerExecutionFailedException must be preserved.").isNotNull();
				assertThat(cause).isSameAs(exception);
			}
		};
		// we DON'T want the default SimpleMessageConversion happening...
		adapter.setMessageConverter(null);
		adapter.onMessage(textMessage);
	}

	@Test
	void testThatTheDefaultMessageConverterisIndeedTheSimpleMessageConverter() throws Exception {
		MessageListenerAdapter adapter = new MessageListenerAdapter();
		assertThat(adapter.getMessageConverter()).as("The default [MessageConverter] must never be null.").isNotNull();
		boolean condition = adapter.getMessageConverter() instanceof SimpleMessageConverter;
		assertThat(condition).as("The default [MessageConverter] must be of the type [SimpleMessageConverter]").isTrue();
	}

	@Test
	void testThatWhenNoDelegateIsSuppliedTheDelegateIsAssumedToBeTheMessageListenerAdapterItself() throws Exception {
		MessageListenerAdapter adapter = new MessageListenerAdapter();
		assertThat(adapter.getDelegate()).isSameAs(adapter);
	}

	@Test
	void testThatTheDefaultMessageHandlingMethodNameIsTheConstantDefault() throws Exception {
		MessageListenerAdapter adapter = new MessageListenerAdapter();
		assertThat(adapter.getDefaultListenerMethod()).isEqualTo(MessageListenerAdapter.ORIGINAL_DEFAULT_LISTENER_METHOD);
	}

	@Test
	void testWithResponsiveMessageDelegate_DoesNotSendReturnTextMessageIfNoSessionSupplied() throws Exception {
		TextMessage textMessage = mock();
		ResponsiveMessageDelegate delegate = mock();
		given(delegate.handleMessage(textMessage)).willReturn(TEXT);

		MessageListenerAdapter adapter = new MessageListenerAdapter(delegate);
		// we DON'T want the default SimpleMessageConversion happening...
		adapter.setMessageConverter(null);
		adapter.onMessage(textMessage);
	}

	@Test
	void testWithResponsiveMessageDelegateWithDefaultDestination_SendsReturnTextMessageWhenSessionSupplied() throws Exception {
		Queue destination = mock();
		TextMessage sentTextMessage = mock();
		// correlation ID is queried when response is being created...
		given(sentTextMessage.getJMSCorrelationID()).willReturn(
				CORRELATION_ID);
		// Reply-To is queried when response is being created...
		given(sentTextMessage.getJMSReplyTo()).willReturn(null); // we want to fall back to the default...

		TextMessage responseTextMessage = mock();

		QueueSender queueSender = mock();
		Session session = mock();
		given(session.createTextMessage(RESPONSE_TEXT)).willReturn(responseTextMessage);
		given(session.createProducer(destination)).willReturn(queueSender);

		ResponsiveMessageDelegate delegate = mock();
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
	void testWithResponsiveMessageDelegateNoDefaultDestination_SendsReturnTextMessageWhenSessionSupplied() throws Exception {
		Queue destination = mock();
		TextMessage sentTextMessage = mock();
		// correlation ID is queried when response is being created...
		given(sentTextMessage.getJMSCorrelationID()).willReturn(null);
		given(sentTextMessage.getJMSMessageID()).willReturn(CORRELATION_ID);
		// Reply-To is queried when response is being created...
		given(sentTextMessage.getJMSReplyTo()).willReturn(destination);

		TextMessage responseTextMessage = mock();
		MessageProducer messageProducer = mock();
		Session session = mock();
		given(session.createTextMessage(RESPONSE_TEXT)).willReturn(responseTextMessage);
		given(session.createProducer(destination)).willReturn(messageProducer);

		ResponsiveMessageDelegate delegate = mock();
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
	void testWithResponsiveMessageDelegateNoDefaultDestinationAndNoReplyToDestination_SendsReturnTextMessageWhenSessionSupplied() throws Exception {
		final TextMessage sentTextMessage = mock();
		// correlation ID is queried when response is being created...
		given(sentTextMessage.getJMSCorrelationID()).willReturn(CORRELATION_ID);
		// Reply-To is queried when response is being created...
		given(sentTextMessage.getJMSReplyTo()).willReturn(null);

		TextMessage responseTextMessage = mock();
		final QueueSession session = mock();
		given(session.createTextMessage(RESPONSE_TEXT)).willReturn(responseTextMessage);

		ResponsiveMessageDelegate delegate = mock();
		given(delegate.handleMessage(sentTextMessage)).willReturn(RESPONSE_TEXT);

		final MessageListenerAdapter adapter = new MessageListenerAdapter(delegate) {
			@Override
			protected Object extractMessage(Message message) {
				return message;
			}
		};
		assertThatExceptionOfType(ReplyFailureException.class).isThrownBy(() ->
				adapter.onMessage(sentTextMessage, session))
			.withCauseExactlyInstanceOf(InvalidDestinationException.class);

		verify(responseTextMessage).setJMSCorrelationID(CORRELATION_ID);
		verify(delegate).handleMessage(sentTextMessage);
	}

	@Test
	void testWithResponsiveMessageDelegateNoDefaultDestination_SendsReturnTextMessageWhenSessionSupplied_AndSendingThrowsJMSException() throws Exception {
		Queue destination = mock();

		final TextMessage sentTextMessage = mock();
		// correlation ID is queried when response is being created...
		given(sentTextMessage.getJMSCorrelationID()).willReturn(CORRELATION_ID);
		// Reply-To is queried when response is being created...
		given(sentTextMessage.getJMSReplyTo()).willReturn(destination);

		TextMessage responseTextMessage = mock();
		MessageProducer messageProducer = mock();
		willThrow(new JMSException("Doe!")).given(messageProducer).send(responseTextMessage);

		final QueueSession session = mock();
		given(session.createTextMessage(RESPONSE_TEXT)).willReturn(responseTextMessage);
		given(session.createProducer(destination)).willReturn(messageProducer);

		ResponsiveMessageDelegate delegate = mock();
		given(delegate.handleMessage(sentTextMessage)).willReturn(RESPONSE_TEXT);

		final MessageListenerAdapter adapter = new MessageListenerAdapter(delegate) {
			@Override
			protected Object extractMessage(Message message) {
				return message;
			}
		};
		assertThatExceptionOfType(ReplyFailureException.class).isThrownBy(() ->
				adapter.onMessage(sentTextMessage, session))
			.withCauseExactlyInstanceOf(JMSException.class);

		verify(responseTextMessage).setJMSCorrelationID(CORRELATION_ID);
		verify(messageProducer).close();
		verify(delegate).handleMessage(sentTextMessage);
	}

	@Test
	void testWithResponsiveMessageDelegateDoesNotSendReturnTextMessageWhenSessionSupplied_AndListenerMethodThrowsException() throws Exception {
		final TextMessage message = mock();
		final QueueSession session = mock();

		ResponsiveMessageDelegate delegate = mock();
		willThrow(new IllegalArgumentException("Doe!")).given(delegate).handleMessage(message);

		final MessageListenerAdapter adapter = new MessageListenerAdapter(delegate) {
			@Override
			protected Object extractMessage(Message message) {
				return message;
			}
		};
		assertThatExceptionOfType(ListenerExecutionFailedException.class).isThrownBy(() ->
				adapter.onMessage(message, session));
	}

	@Test
	void testWithResponsiveMessageDelegateWhenReturnTypeIsNotAJMSMessageAndNoMessageConverterIsSupplied() throws Exception {
		final TextMessage sentTextMessage = mock();
		final Session session = mock();
		ResponsiveMessageDelegate delegate = mock();
		given(delegate.handleMessage(sentTextMessage)).willReturn(RESPONSE_TEXT);

		final MessageListenerAdapter adapter = new MessageListenerAdapter(delegate) {
			@Override
			protected Object extractMessage(Message message) {
				return message;
			}
		};
		adapter.setMessageConverter(null);
		assertThatExceptionOfType(ReplyFailureException.class).isThrownBy(() ->
				adapter.onMessage(sentTextMessage, session))
			.withCauseExactlyInstanceOf(MessageConversionException.class);
	}

	@Test
	void testWithResponsiveMessageDelegateWhenReturnTypeIsAJMSMessageAndNoMessageConverterIsSupplied() throws Exception {
		Queue destination = mock();
		final TextMessage sentTextMessage = mock();
		// correlation ID is queried when response is being created...
		given(sentTextMessage.getJMSCorrelationID()).willReturn(CORRELATION_ID);
		// Reply-To is queried when response is being created...
		given(sentTextMessage.getJMSReplyTo()).willReturn(destination);

		TextMessage responseMessage = mock();
		QueueSender queueSender = mock();

		Session session = mock();
		given(session.createProducer(destination)).willReturn(queueSender);

		ResponsiveJmsTextMessageReturningMessageDelegate delegate = mock();
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
