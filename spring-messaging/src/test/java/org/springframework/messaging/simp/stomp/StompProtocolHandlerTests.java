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

package org.springframework.messaging.simp.stomp;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.support.TestPrincipal;
import org.springframework.web.socket.support.TestWebSocketSession;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Test fixture for {@link StompProtocolHandler} tests.
 *
 * @author Rossen Stoyanchev
 */
public class StompProtocolHandlerTests {

	private StompProtocolHandler stompHandler;

	private TestWebSocketSession session;

	private MessageChannel channel;

	private ArgumentCaptor<Message> messageCaptor;


	@Before
	public void setup() {
		this.stompHandler = new StompProtocolHandler();
		this.channel = Mockito.mock(MessageChannel.class);
		this.messageCaptor = ArgumentCaptor.forClass(Message.class);

		this.session = new TestWebSocketSession();
		this.session.setId("s1");
		this.session.setPrincipal(new TestPrincipal("joe"));
	}

	@Test
	public void connectedResponseIsSentWhenHandlingConnect() {
		this.stompHandler.setHandleConnect(true);

		TextMessage textMessage = StompTextMessageBuilder.create(StompCommand.CONNECT).headers(
				"login:guest", "passcode:guest", "accept-version:1.1,1.0", "heart-beat:10000,10000").build();

		this.stompHandler.handleMessageFromClient(this.session, textMessage, this.channel);

		verifyNoMoreInteractions(this.channel);

		// Check CONNECTED reply

		assertEquals(1, this.session.getSentMessages().size());
		textMessage = (TextMessage) this.session.getSentMessages().get(0);
		Message<?> message = new StompDecoder().decode(ByteBuffer.wrap(textMessage.getPayload().getBytes()));
		StompHeaderAccessor replyHeaders = StompHeaderAccessor.wrap(message);

		assertEquals(StompCommand.CONNECTED, replyHeaders.getCommand());
		assertEquals("1.1", replyHeaders.getVersion());
		assertArrayEquals(new long[] {0, 0}, replyHeaders.getHeartbeat());
		assertEquals("joe", replyHeaders.getNativeHeader("user-name").get(0));
		assertEquals("s1", replyHeaders.getNativeHeader("queue-suffix").get(0));
	}

	@Test
	public void connectIsForwardedWhenNotHandlingConnect() {
		this.stompHandler.setHandleConnect(false);

		TextMessage textMessage = StompTextMessageBuilder.create(StompCommand.CONNECT).headers(
				"login:guest", "passcode:guest", "accept-version:1.1,1.0", "heart-beat:10000,10000").build();

		this.stompHandler.handleMessageFromClient(this.session, textMessage, this.channel);

		verify(this.channel).send(this.messageCaptor.capture());
		Message<?> actual = this.messageCaptor.getValue();
		assertNotNull(actual);

		StompHeaderAccessor headers = StompHeaderAccessor.wrap(actual);
		assertEquals(StompCommand.CONNECT, headers.getCommand());
		assertEquals("s1", headers.getSessionId());
		assertEquals("joe", headers.getUser().getName());
		assertEquals("guest", headers.getLogin());
		assertEquals("PROTECTED", headers.getPasscode());
		assertArrayEquals(new long[] {10000, 10000}, headers.getHeartbeat());
		assertEquals(new HashSet<>(Arrays.asList("1.1","1.0")), headers.getAcceptVersion());

		assertEquals(0, this.session.getSentMessages().size());
	}

}
