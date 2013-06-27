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

package org.springframework.jms.listener.adapter;

import java.io.ByteArrayInputStream;

import javax.jms.BytesMessage;
import javax.jms.InvalidDestinationException;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.jms.support.converter.SimpleMessageConverter102;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for the {@link MessageListenerAdapter102} class.
 *
 * @author Rick Evans
 * @author Chris Beams
 */
@Deprecated
public final class MessageListenerAdapter102Tests {

	private static final String TEXT = "The Runaways";
	private static final String CORRELATION_ID = "100";
	private static final String RESPONSE_TEXT = "Old Lace";


	@Test
	public void testWithMessageContentsDelegateForBytesMessage() throws Exception {

		BytesMessage bytesMessage = mock(BytesMessage.class);
		// BytesMessage contents must be unwrapped...
		given(bytesMessage.readBytes(any(byte[].class))).willAnswer(new Answer<Integer>() {
			@Override
			public Integer answer(InvocationOnMock invocation) throws Throwable {
				byte[] bytes = (byte[]) invocation.getArguments()[0];
				ByteArrayInputStream inputStream = new ByteArrayInputStream(TEXT.getBytes());
				return inputStream.read(bytes);
			}
		});
		MessageContentsDelegate delegate = mock(MessageContentsDelegate.class);

		MessageListenerAdapter102 adapter = new MessageListenerAdapter102(delegate);
		adapter.onMessage(bytesMessage);

		verify(delegate).handleMessage(TEXT.getBytes());
	}

	@Test
	public void testWithMessageDelegate() throws Exception {

		TextMessage textMessage = mock(TextMessage.class);
		MessageDelegate delegate = mock(MessageDelegate.class);

		MessageListenerAdapter102 adapter = new MessageListenerAdapter102(delegate);
		// we DON'T want the default SimpleMessageConversion happening...
		adapter.setMessageConverter(null);
		adapter.onMessage(textMessage);

		verify(delegate).handleMessage(textMessage);
	}

	@Test
	public void testThatTheDefaultMessageConverterisIndeedTheSimpleMessageConverter102() throws Exception {
		MessageListenerAdapter102 adapter = new MessageListenerAdapter102();
		assertNotNull("The default [MessageConverter] must never be null.", adapter.getMessageConverter());
		assertTrue("The default [MessageConverter] must be of the type [SimpleMessageConverter102]; if you've just changed it, then change this test to reflect your change.", adapter.getMessageConverter() instanceof SimpleMessageConverter102);
	}

	@Test
	public void testWithResponsiveMessageDelegate_DoesNotSendReturnTextMessageIfNoSessionSupplied() throws Exception {

		TextMessage textMessage = mock(TextMessage.class);

		ResponsiveMessageDelegate delegate = mock(ResponsiveMessageDelegate.class);
		given(delegate.handleMessage(textMessage)).willReturn(TEXT);

		MessageListenerAdapter102 adapter = new MessageListenerAdapter102(delegate);
		// we DON'T want the default SimpleMessageConversion happening...
		adapter.setMessageConverter(null);
		adapter.onMessage(textMessage);

		verify(delegate).handleMessage(textMessage);
	}

	@Test
	public void testWithResponsiveMessageDelegateWithDefaultDestination_SendsReturnTextMessageWhenSessionSuppliedForQueue() throws Exception {

		Queue destination = mock(Queue.class);

		TextMessage sentTextMessage = mock(TextMessage.class);
		// correlation ID is queried when response is being created...
		given(sentTextMessage.getJMSCorrelationID()).willReturn(CORRELATION_ID);
		// Reply-To is queried when response is being created...
		given(sentTextMessage.getJMSReplyTo()).willReturn(null);

		TextMessage responseTextMessage = mock(TextMessage.class);
		QueueSender queueSender = mock(QueueSender.class);
		QueueSession session = mock(QueueSession.class);
		given(session.createTextMessage(RESPONSE_TEXT)).willReturn(responseTextMessage);
		given(session.createSender(destination)).willReturn(queueSender);

		ResponsiveMessageDelegate delegate = mock(ResponsiveMessageDelegate.class);
		given(delegate.handleMessage(sentTextMessage)).willReturn(RESPONSE_TEXT);

		MessageListenerAdapter102 adapter = new MessageListenerAdapter102(delegate) {
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
	public void testWithResponsiveMessageDelegateWithDefaultDestination_SendsReturnTextMessageWhenSessionSuppliedForTopic() throws Exception {

		Topic destination = mock(Topic.class);
		TextMessage sentTextMessage = mock(TextMessage.class);
		// correlation ID is queried when response is being created...
		given(sentTextMessage.getJMSCorrelationID()).willReturn(CORRELATION_ID);
		// Reply-To is queried when response is being created...
		given(sentTextMessage.getJMSReplyTo()).willReturn(null); // we want to fall back to the default...

		TextMessage responseTextMessage = mock(TextMessage.class);
		TopicPublisher topicPublisher = mock(TopicPublisher.class);
		TopicSession session = mock(TopicSession.class);
		given(session.createTextMessage(RESPONSE_TEXT)).willReturn(responseTextMessage);
		given(session.createPublisher(destination)).willReturn(topicPublisher);

		ResponsiveMessageDelegate delegate = mock(ResponsiveMessageDelegate.class);
		given(delegate.handleMessage(sentTextMessage)).willReturn(RESPONSE_TEXT);

		MessageListenerAdapter102 adapter = new MessageListenerAdapter102(delegate) {
			@Override
			protected Object extractMessage(Message message) {
				return message;
			}
		};
		adapter.setDefaultResponseDestination(destination);
		adapter.onMessage(sentTextMessage, session);

		verify(responseTextMessage).setJMSCorrelationID(CORRELATION_ID);
		verify(topicPublisher).publish(responseTextMessage);
		verify(topicPublisher).close();
		verify(delegate).handleMessage(sentTextMessage);
	}

	@Test
	public void testWithResponsiveMessageDelegateNoDefaultDestination_SendsReturnTextMessageWhenSessionSupplied() throws Exception {

		Queue destination = mock(Queue.class);
		TextMessage sentTextMessage = mock(TextMessage.class);
		// correlation ID is queried when response is being created...
		given(sentTextMessage.getJMSCorrelationID()).willReturn(CORRELATION_ID);
		// Reply-To is queried when response is being created...
		given(sentTextMessage.getJMSReplyTo()).willReturn(destination);

		TextMessage responseTextMessage = mock(TextMessage.class);
		QueueSender queueSender = mock(QueueSender.class);
		QueueSession session = mock(QueueSession.class);
		given(session.createTextMessage(RESPONSE_TEXT)).willReturn(responseTextMessage);
		given(session.createSender(destination)).willReturn(queueSender);

		ResponsiveMessageDelegate delegate = mock(ResponsiveMessageDelegate.class);
		given(delegate.handleMessage(sentTextMessage)).willReturn(RESPONSE_TEXT);

		MessageListenerAdapter102 adapter = new MessageListenerAdapter102(delegate) {
			@Override
			protected Object extractMessage(Message message) {
				return message;
			}
		};
		adapter.onMessage(sentTextMessage, session);


		verify(responseTextMessage).setJMSCorrelationID(CORRELATION_ID);
		verify(queueSender).send(responseTextMessage);
		verify(queueSender).close();
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

		final MessageListenerAdapter102 adapter = new MessageListenerAdapter102(delegate) {
			@Override
			protected Object extractMessage(Message message) {
				return message;
			}
		};
		try {
			adapter.onMessage(sentTextMessage, session);
			fail("expected InvalidDestinationException");
		} catch (InvalidDestinationException ex) { /* expected */ }

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
		QueueSender queueSender = mock(QueueSender.class);
		willThrow(new JMSException("Doe!")).given(queueSender).send(responseTextMessage);
		// ensure that regardless of a JMSException the producer is closed...

		final QueueSession session = mock(QueueSession.class);
		given(session.createTextMessage(RESPONSE_TEXT)).willReturn(responseTextMessage);
		given(session.createSender(destination)).willReturn(queueSender);

		ResponsiveMessageDelegate delegate = mock(ResponsiveMessageDelegate.class);
		given(delegate.handleMessage(sentTextMessage)).willReturn(RESPONSE_TEXT);

		final MessageListenerAdapter102 adapter = new MessageListenerAdapter102(delegate) {
			@Override
			protected Object extractMessage(Message message) {
				return message;
			}
		};
		try {
			adapter.onMessage(sentTextMessage, session);
			fail("expected JMSException");
		} catch (JMSException ex) { /* expected */ }


		verify(responseTextMessage).setJMSCorrelationID(CORRELATION_ID);
		verify(queueSender).close();
		verify(delegate).handleMessage(sentTextMessage);
	}

	@Test
	public void testWithResponsiveMessageDelegateDoesNotSendReturnTextMessageWhenSessionSupplied_AndListenerMethodThrowsException() throws Exception {

		final TextMessage sentTextMessage = mock(TextMessage.class);
		final QueueSession session = mock(QueueSession.class);

		ResponsiveMessageDelegate delegate = mock(ResponsiveMessageDelegate.class);
		willThrow(new IllegalArgumentException("Doe!")).given(delegate).handleMessage(sentTextMessage);

		final MessageListenerAdapter102 adapter = new MessageListenerAdapter102(delegate) {
			@Override
			protected Object extractMessage(Message message) {
				return message;
			}
		};
		try {
			adapter.onMessage(sentTextMessage, session);
			fail("expected ListenerExecutionFailedException");
		} catch (ListenerExecutionFailedException ex) { /* expected */ }
	}

}
