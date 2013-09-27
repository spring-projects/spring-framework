/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.simp.handler;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.MessageBuilder;

import static org.junit.Assert.*;

import static org.mockito.Mockito.*;


/**
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class SimpleBrokerMessageHandlerTests {

	private SimpleBrokerMessageHandler messageHandler;

	@Mock
	private MessageChannel clientChannel;

	@Captor
	ArgumentCaptor<Message<?>> messageCaptor;


	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		this.messageHandler = new SimpleBrokerMessageHandler(this.clientChannel, Collections.<String>emptyList());
	}


	@Test
	public void subcribePublish() {

		this.messageHandler.start();

		this.messageHandler.handleMessage(createSubscriptionMessage("sess1", "sub1", "/foo"));
		this.messageHandler.handleMessage(createSubscriptionMessage("sess1", "sub2", "/foo"));
		this.messageHandler.handleMessage(createSubscriptionMessage("sess1", "sub3", "/bar"));

		this.messageHandler.handleMessage(createSubscriptionMessage("sess2", "sub1", "/foo"));
		this.messageHandler.handleMessage(createSubscriptionMessage("sess2", "sub2", "/foo"));
		this.messageHandler.handleMessage(createSubscriptionMessage("sess2", "sub3", "/bar"));

		this.messageHandler.handleMessage(createMessage("/foo", "message1"));
		this.messageHandler.handleMessage(createMessage("/bar", "message2"));

		verify(this.clientChannel, times(6)).send(this.messageCaptor.capture());
		assertCapturedMessage("sess1", "sub1", "/foo");
		assertCapturedMessage("sess1", "sub2", "/foo");
		assertCapturedMessage("sess2", "sub1", "/foo");
		assertCapturedMessage("sess2", "sub2", "/foo");
		assertCapturedMessage("sess1", "sub3", "/bar");
		assertCapturedMessage("sess2", "sub3", "/bar");
	}

	@Test
	public void subcribeDisconnectPublish() {

		String sess1 = "sess1";
		String sess2 = "sess2";

		this.messageHandler.start();

		this.messageHandler.handleMessage(createSubscriptionMessage(sess1, "sub1", "/foo"));
		this.messageHandler.handleMessage(createSubscriptionMessage(sess1, "sub2", "/foo"));
		this.messageHandler.handleMessage(createSubscriptionMessage(sess1, "sub3", "/bar"));

		this.messageHandler.handleMessage(createSubscriptionMessage(sess2, "sub1", "/foo"));
		this.messageHandler.handleMessage(createSubscriptionMessage(sess2, "sub2", "/foo"));
		this.messageHandler.handleMessage(createSubscriptionMessage(sess2, "sub3", "/bar"));

		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create(SimpMessageType.DISCONNECT);
		headers.setSessionId(sess1);
		Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).copyHeaders(headers.toMap()).build();
		this.messageHandler.handleMessage(message);

		this.messageHandler.handleMessage(createMessage("/foo", "message1"));
		this.messageHandler.handleMessage(createMessage("/bar", "message2"));

		verify(this.clientChannel, times(3)).send(this.messageCaptor.capture());
		assertCapturedMessage(sess2, "sub1", "/foo");
		assertCapturedMessage(sess2, "sub2", "/foo");
		assertCapturedMessage(sess2, "sub3", "/bar");
	}

	@Test
	public void connect() {

		String sess1 = "sess1";

		this.messageHandler.start();

		Message<String> connectMessage = createConnectMessage(sess1);
		this.messageHandler.handleMessage(connectMessage);

		verify(this.clientChannel, times(1)).send(this.messageCaptor.capture());
		Message<?> connectAckMessage = this.messageCaptor.getValue();

		SimpMessageHeaderAccessor connectAckHeaders = SimpMessageHeaderAccessor.wrap(connectAckMessage);
		assertEquals(connectMessage, connectAckHeaders.getHeader(SimpMessageHeaderAccessor.CONNECT_MESSAGE_HEADER));
		assertEquals(sess1, connectAckHeaders.getSessionId());
	}


	protected Message<String> createSubscriptionMessage(String sessionId, String subcriptionId, String destination) {

		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create(SimpMessageType.SUBSCRIBE);
		headers.setSubscriptionId(subcriptionId);
		headers.setDestination(destination);
		headers.setSessionId(sessionId);

		return MessageBuilder.withPayload("").copyHeaders(headers.toMap()).build();
	}

	protected Message<String> createConnectMessage(String sessionId) {
		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create(SimpMessageType.CONNECT);
		headers.setSessionId(sessionId);

		return MessageBuilder.withPayloadAndHeaders("", headers).build();
	}

	protected Message<String> createMessage(String destination, String payload) {

		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
		headers.setDestination(destination);

		return MessageBuilder.withPayload(payload).copyHeaders(headers.toMap()).build();
	}

	protected boolean assertCapturedMessage(String sessionId, String subcriptionId, String destination) {
		for (Message<?> message : this.messageCaptor.getAllValues()) {
			SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(message);
			if (sessionId.equals(headers.getSessionId())) {
				if (subcriptionId.equals(headers.getSubscriptionId())) {
					if (destination.equals(headers.getDestination())) {
						return true;
					}
				}
			}
		}
		return false;
	}

}
