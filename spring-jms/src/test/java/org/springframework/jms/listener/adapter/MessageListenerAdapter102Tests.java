/*
 * Copyright 2002-2006 the original author or authors.
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

import org.easymock.MockControl;
import org.junit.Test;
import org.springframework.jms.support.converter.SimpleMessageConverter102;

/**
 * Unit tests for the {@link MessageListenerAdapter102} class.
 *
 * @author Rick Evans
 * @author Chris Beams
 */
public final class MessageListenerAdapter102Tests {

	private static final String TEXT = "The Runaways";
	private static final String CORRELATION_ID = "100";
	private static final String RESPONSE_TEXT = "Old Lace";


	@Test
	public void testWithMessageContentsDelegateForBytesMessage() throws Exception {

		MockControl mockBytesMessage = MockControl.createControl(BytesMessage.class);
		BytesMessage bytesMessage = (BytesMessage) mockBytesMessage.getMock();
		// BytesMessage contents must be unwrapped...
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

		MessageListenerAdapter102 adapter = new MessageListenerAdapter102(delegate);
		adapter.onMessage(bytesMessage);

		mockDelegate.verify();
		mockBytesMessage.verify();
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

		MessageListenerAdapter102 adapter = new MessageListenerAdapter102(delegate);
		// we DON'T want the default SimpleMessageConversion happening...
		adapter.setMessageConverter(null);
		adapter.onMessage(textMessage);

		mockDelegate.verify();
		mockTextMessage.verify();
	}

	@Test
	public void testThatTheDefaultMessageConverterisIndeedTheSimpleMessageConverter102() throws Exception {
		MessageListenerAdapter102 adapter = new MessageListenerAdapter102();
		assertNotNull("The default [MessageConverter] must never be null.", adapter.getMessageConverter());
		assertTrue("The default [MessageConverter] must be of the type [SimpleMessageConverter102]; if you've just changed it, then change this test to reflect your change.", adapter.getMessageConverter() instanceof SimpleMessageConverter102);
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

		MessageListenerAdapter102 adapter = new MessageListenerAdapter102(delegate);
		// we DON'T want the default SimpleMessageConversion happening...
		adapter.setMessageConverter(null);
		adapter.onMessage(textMessage);

		mockDelegate.verify();
		mockTextMessage.verify();
	}

	@Test
	public void testWithResponsiveMessageDelegateWithDefaultDestination_SendsReturnTextMessageWhenSessionSuppliedForQueue() throws Exception {

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

		MockControl mockSession = MockControl.createControl(QueueSession.class);
		QueueSession session = (QueueSession) mockSession.getMock();
		session.createTextMessage(RESPONSE_TEXT);
		mockSession.setReturnValue(responseTextMessage);
		session.createSender(destination);
		mockSession.setReturnValue(queueSender);
		mockSession.replay();

		MockControl mockDelegate = MockControl.createControl(ResponsiveMessageDelegate.class);
		ResponsiveMessageDelegate delegate = (ResponsiveMessageDelegate) mockDelegate.getMock();
		delegate.handleMessage(sentTextMessage);
		mockDelegate.setReturnValue(RESPONSE_TEXT);
		mockDelegate.replay();

		MessageListenerAdapter102 adapter = new MessageListenerAdapter102(delegate) {
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
	public void testWithResponsiveMessageDelegateWithDefaultDestination_SendsReturnTextMessageWhenSessionSuppliedForTopic() throws Exception {

		MockControl mockDestination = MockControl.createControl(Topic.class);
		Topic destination = (Topic) mockDestination.getMock();
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

		MockControl mockTopicPublisher = MockControl.createControl(TopicPublisher.class);
		TopicPublisher topicPublisher = (TopicPublisher) mockTopicPublisher.getMock();
		topicPublisher.publish(responseTextMessage);
		mockTopicPublisher.setVoidCallable();
		topicPublisher.close();
		mockTopicPublisher.setVoidCallable();
		mockTopicPublisher.replay();

		MockControl mockSession = MockControl.createControl(TopicSession.class);
		TopicSession session = (TopicSession) mockSession.getMock();
		session.createTextMessage(RESPONSE_TEXT);
		mockSession.setReturnValue(responseTextMessage);
		session.createPublisher(destination);
		mockSession.setReturnValue(topicPublisher);
		mockSession.replay();

		MockControl mockDelegate = MockControl.createControl(ResponsiveMessageDelegate.class);
		ResponsiveMessageDelegate delegate = (ResponsiveMessageDelegate) mockDelegate.getMock();
		delegate.handleMessage(sentTextMessage);
		mockDelegate.setReturnValue(RESPONSE_TEXT);
		mockDelegate.replay();

		MessageListenerAdapter102 adapter = new MessageListenerAdapter102(delegate) {
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
		mockTopicPublisher.verify();
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

		MockControl mockQueueSender = MockControl.createControl(QueueSender.class);
		QueueSender queueSender = (QueueSender) mockQueueSender.getMock();
		queueSender.send(responseTextMessage);
		mockQueueSender.setVoidCallable();
		queueSender.close();
		mockQueueSender.setVoidCallable();
		mockQueueSender.replay();

		MockControl mockSession = MockControl.createControl(QueueSession.class);
		QueueSession session = (QueueSession) mockSession.getMock();
		session.createTextMessage(RESPONSE_TEXT);
		mockSession.setReturnValue(responseTextMessage);
		session.createSender(destination);
		mockSession.setReturnValue(queueSender);
		mockSession.replay();

		MockControl mockDelegate = MockControl.createControl(ResponsiveMessageDelegate.class);
		ResponsiveMessageDelegate delegate = (ResponsiveMessageDelegate) mockDelegate.getMock();
		delegate.handleMessage(sentTextMessage);
		mockDelegate.setReturnValue(RESPONSE_TEXT);
		mockDelegate.replay();

		MessageListenerAdapter102 adapter = new MessageListenerAdapter102(delegate) {
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
		mockQueueSender.verify();
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

		MockControl mockQueueSender = MockControl.createControl(QueueSender.class);
		QueueSender queueSender = (QueueSender) mockQueueSender.getMock();
		queueSender.send(responseTextMessage);
		mockQueueSender.setThrowable(new JMSException("Dow!"));
		// ensure that regardless of a JMSException the producer is closed...
		queueSender.close();
		mockQueueSender.setVoidCallable();
		mockQueueSender.replay();

		MockControl mockSession = MockControl.createControl(QueueSession.class);
		final QueueSession session = (QueueSession) mockSession.getMock();
		session.createTextMessage(RESPONSE_TEXT);
		mockSession.setReturnValue(responseTextMessage);
		session.createSender(destination);
		mockSession.setReturnValue(queueSender);
		mockSession.replay();

		MockControl mockDelegate = MockControl.createControl(ResponsiveMessageDelegate.class);
		ResponsiveMessageDelegate delegate = (ResponsiveMessageDelegate) mockDelegate.getMock();
		delegate.handleMessage(sentTextMessage);
		mockDelegate.setReturnValue(RESPONSE_TEXT);
		mockDelegate.replay();

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

		mockDelegate.verify();
		mockSentTextMessage.verify();
		mockResponseTextMessage.verify();
		mockSession.verify();
		mockDestination.verify();
		mockQueueSender.verify();
	}

	@Test
	public void testWithResponsiveMessageDelegateDoesNotSendReturnTextMessageWhenSessionSupplied_AndListenerMethodThrowsException() throws Exception {

		MockControl mockSentTextMessage = MockControl.createControl(TextMessage.class);
		final TextMessage sentTextMessage = (TextMessage) mockSentTextMessage.getMock();
		mockSentTextMessage.replay();

		MockControl mockSession = MockControl.createControl(QueueSession.class);
		final QueueSession session = (QueueSession) mockSession.getMock();
		mockSession.replay();

		MockControl mockDelegate = MockControl.createControl(ResponsiveMessageDelegate.class);
		ResponsiveMessageDelegate delegate = (ResponsiveMessageDelegate) mockDelegate.getMock();
		delegate.handleMessage(sentTextMessage);
		mockDelegate.setThrowable(new IllegalArgumentException("Dow!"));
		mockDelegate.replay();

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

		mockDelegate.verify();
		mockSentTextMessage.verify();
		mockSession.verify();
	}

}
