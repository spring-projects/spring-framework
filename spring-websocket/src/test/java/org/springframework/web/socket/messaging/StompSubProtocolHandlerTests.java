/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.socket.messaging;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpAttributes;
import org.springframework.messaging.simp.SimpAttributesContextHolder;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.TestPrincipal;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompEncoder;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.user.DestinationUserNameProvider;
import org.springframework.messaging.support.ChannelInterceptorAdapter;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.messaging.support.ImmutableMessageChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.handler.TestWebSocketSession;
import org.springframework.web.socket.sockjs.transport.SockJsSession;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

/**
 * Test fixture for {@link StompSubProtocolHandler} tests.
 *
 * @author Rossen Stoyanchev
 */
public class StompSubProtocolHandlerTests {

	private static final byte[] EMPTY_PAYLOAD = new byte[0];

	private StompSubProtocolHandler protocolHandler;

	private TestWebSocketSession session;

	private MessageChannel channel;

	@SuppressWarnings("rawtypes")
	private ArgumentCaptor<Message> messageCaptor;


	@Before
	public void setup() {
		this.protocolHandler = new StompSubProtocolHandler();
		this.channel = Mockito.mock(MessageChannel.class);
		this.messageCaptor = ArgumentCaptor.forClass(Message.class);

		when(this.channel.send(any())).thenReturn(true);

		this.session = new TestWebSocketSession();
		this.session.setId("s1");
		this.session.setPrincipal(new TestPrincipal("joe"));
	}

	@Test
	public void handleMessageToClientWithConnectedFrame() {

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.CONNECTED);
		Message<byte[]> message = MessageBuilder.createMessage(EMPTY_PAYLOAD, headers.getMessageHeaders());
		this.protocolHandler.handleMessageToClient(this.session, message);

		assertEquals(1, this.session.getSentMessages().size());
		WebSocketMessage<?> textMessage = this.session.getSentMessages().get(0);
		assertEquals("CONNECTED\n" + "user-name:joe\n" + "\n" + "\u0000", textMessage.getPayload());
	}

	@Test
	public void handleMessageToClientWithDestinationUserNameProvider() {

		this.session.setPrincipal(new UniqueUser("joe"));

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.CONNECTED);
		Message<byte[]> message = MessageBuilder.createMessage(EMPTY_PAYLOAD, headers.getMessageHeaders());
		this.protocolHandler.handleMessageToClient(this.session, message);

		assertEquals(1, this.session.getSentMessages().size());
		WebSocketMessage<?> textMessage = this.session.getSentMessages().get(0);
		assertEquals("CONNECTED\n" + "user-name:joe\n" + "\n" + "\u0000", textMessage.getPayload());
	}

	@Test
	public void handleMessageToClientWithSimpConnectAck() {

		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
		accessor.setHeartbeat(10000, 10000);
		accessor.setAcceptVersion("1.0,1.1");
		Message<?> connectMessage = MessageBuilder.createMessage(EMPTY_PAYLOAD, accessor.getMessageHeaders());

		SimpMessageHeaderAccessor ackAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.CONNECT_ACK);
		ackAccessor.setHeader(SimpMessageHeaderAccessor.CONNECT_MESSAGE_HEADER, connectMessage);
		ackAccessor.setHeader(SimpMessageHeaderAccessor.HEART_BEAT_HEADER, new long[] {15000, 15000});
		Message<byte[]> ackMessage = MessageBuilder.createMessage(EMPTY_PAYLOAD, ackAccessor.getMessageHeaders());
		this.protocolHandler.handleMessageToClient(this.session, ackMessage);

		assertEquals(1, this.session.getSentMessages().size());
		TextMessage actual = (TextMessage) this.session.getSentMessages().get(0);
		assertEquals("CONNECTED\n" + "version:1.1\n" + "heart-beat:15000,15000\n" +
				"user-name:joe\n" + "\n" + "\u0000", actual.getPayload());
	}

	@Test
	public void handleMessageToClientWithSimpConnectAckDefaultHeartBeat() {

		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
		accessor.setHeartbeat(10000, 10000);
		accessor.setAcceptVersion("1.0,1.1");
		Message<?> connectMessage = MessageBuilder.createMessage(EMPTY_PAYLOAD, accessor.getMessageHeaders());

		SimpMessageHeaderAccessor ackAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.CONNECT_ACK);
		ackAccessor.setHeader(SimpMessageHeaderAccessor.CONNECT_MESSAGE_HEADER, connectMessage);
		Message<byte[]> ackMessage = MessageBuilder.createMessage(EMPTY_PAYLOAD, ackAccessor.getMessageHeaders());
		this.protocolHandler.handleMessageToClient(this.session, ackMessage);

		assertEquals(1, this.session.getSentMessages().size());
		TextMessage actual = (TextMessage) this.session.getSentMessages().get(0);
		assertEquals("CONNECTED\n" + "version:1.1\n" + "heart-beat:0,0\n" +
				"user-name:joe\n" + "\n" + "\u0000", actual.getPayload());
	}

	@Test
	public void handleMessageToClientWithSimpDisconnectAck() {

		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
		Message<?> connectMessage = MessageBuilder.createMessage(EMPTY_PAYLOAD, accessor.getMessageHeaders());

		SimpMessageHeaderAccessor ackAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.DISCONNECT_ACK);
		ackAccessor.setHeader(SimpMessageHeaderAccessor.DISCONNECT_MESSAGE_HEADER, connectMessage);
		Message<byte[]> ackMessage = MessageBuilder.createMessage(EMPTY_PAYLOAD, ackAccessor.getMessageHeaders());
		this.protocolHandler.handleMessageToClient(this.session, ackMessage);

		assertEquals(1, this.session.getSentMessages().size());
		TextMessage actual = (TextMessage) this.session.getSentMessages().get(0);
		assertEquals("ERROR\n" + "message:Session closed.\n" + "content-length:0\n" +
				"\n\u0000", actual.getPayload());
	}

	@Test
	public void handleMessageToClientWithSimpDisconnectAckAndReceipt() {

		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
		accessor.setReceipt("message-123");
		Message<?> connectMessage = MessageBuilder.createMessage(EMPTY_PAYLOAD, accessor.getMessageHeaders());

		SimpMessageHeaderAccessor ackAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.DISCONNECT_ACK);
		ackAccessor.setHeader(SimpMessageHeaderAccessor.DISCONNECT_MESSAGE_HEADER, connectMessage);
		Message<byte[]> ackMessage = MessageBuilder.createMessage(EMPTY_PAYLOAD, ackAccessor.getMessageHeaders());
		this.protocolHandler.handleMessageToClient(this.session, ackMessage);

		assertEquals(1, this.session.getSentMessages().size());
		TextMessage actual = (TextMessage) this.session.getSentMessages().get(0);
		assertEquals("RECEIPT\n" + "receipt-id:message-123\n" + "\n\u0000", actual.getPayload());
	}

	@Test
	public void handleMessageToClientWithSimpHeartbeat() {

		SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.HEARTBEAT);
		accessor.setSessionId("s1");
		accessor.setUser(new TestPrincipal("joe"));
		Message<byte[]> ackMessage = MessageBuilder.createMessage(EMPTY_PAYLOAD, accessor.getMessageHeaders());
		this.protocolHandler.handleMessageToClient(this.session, ackMessage);

		assertEquals(1, this.session.getSentMessages().size());
		TextMessage actual = (TextMessage) this.session.getSentMessages().get(0);
		assertEquals("\n", actual.getPayload());
	}

	@Test
	public void handleMessageToClientWithHeartbeatSuppressingSockJsHeartbeat() throws IOException {

		SockJsSession sockJsSession = Mockito.mock(SockJsSession.class);
		when(sockJsSession.getId()).thenReturn("s1");
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECTED);
		accessor.setHeartbeat(0, 10);
		Message<byte[]> message = MessageBuilder.createMessage(EMPTY_PAYLOAD, accessor.getMessageHeaders());
		this.protocolHandler.handleMessageToClient(sockJsSession, message);

		verify(sockJsSession).getId();
		verify(sockJsSession).getPrincipal();
		verify(sockJsSession).disableHeartbeat();
		verify(sockJsSession).sendMessage(any(WebSocketMessage.class));
		verifyNoMoreInteractions(sockJsSession);

		sockJsSession = Mockito.mock(SockJsSession.class);
		when(sockJsSession.getId()).thenReturn("s1");
		accessor = StompHeaderAccessor.create(StompCommand.CONNECTED);
		accessor.setHeartbeat(0, 0);
		message = MessageBuilder.createMessage(EMPTY_PAYLOAD, accessor.getMessageHeaders());
		this.protocolHandler.handleMessageToClient(sockJsSession, message);

		verify(sockJsSession).getId();
		verify(sockJsSession).getPrincipal();
		verify(sockJsSession).sendMessage(any(WebSocketMessage.class));
		verifyNoMoreInteractions(sockJsSession);
	}

	@Test
	public void handleMessageToClientWithUserDestination() {

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.MESSAGE);
		headers.setMessageId("mess0");
		headers.setSubscriptionId("sub0");
		headers.setDestination("/queue/foo-user123");
		headers.setNativeHeader(StompHeaderAccessor.ORIGINAL_DESTINATION, "/user/queue/foo");
		Message<byte[]> message = MessageBuilder.createMessage(EMPTY_PAYLOAD, headers.getMessageHeaders());
		this.protocolHandler.handleMessageToClient(this.session, message);

		assertEquals(1, this.session.getSentMessages().size());
		WebSocketMessage<?> textMessage = this.session.getSentMessages().get(0);
		assertTrue(((String) textMessage.getPayload()).contains("destination:/user/queue/foo\n"));
		assertFalse(((String) textMessage.getPayload()).contains(SimpMessageHeaderAccessor.ORIGINAL_DESTINATION));
	}

	// SPR-12475

	@Test
	public void handleMessageToClientWithBinaryWebSocketMessage() {

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.MESSAGE);
		headers.setMessageId("mess0");
		headers.setSubscriptionId("sub0");
		headers.setContentType(MimeTypeUtils.APPLICATION_OCTET_STREAM);
		headers.setDestination("/queue/foo");

		// Non-empty payload

		byte[] payload = new byte[1];
		Message<byte[]> message = MessageBuilder.createMessage(payload, headers.getMessageHeaders());
		this.protocolHandler.handleMessageToClient(this.session, message);

		assertEquals(1, this.session.getSentMessages().size());
		WebSocketMessage<?> webSocketMessage = this.session.getSentMessages().get(0);
		assertTrue(webSocketMessage instanceof BinaryMessage);

		// Empty payload

		payload = EMPTY_PAYLOAD;
		message = MessageBuilder.createMessage(payload, headers.getMessageHeaders());
		this.protocolHandler.handleMessageToClient(this.session, message);

		assertEquals(2, this.session.getSentMessages().size());
		webSocketMessage = this.session.getSentMessages().get(1);
		assertTrue(webSocketMessage instanceof TextMessage);
	}

	@Test
	public void handleMessageFromClient() {

		TextMessage textMessage = StompTextMessageBuilder.create(StompCommand.CONNECT).headers(
				"login:guest", "passcode:guest", "accept-version:1.1,1.0", "heart-beat:10000,10000").build();

		this.protocolHandler.afterSessionStarted(this.session, this.channel);
		this.protocolHandler.handleMessageFromClient(this.session, textMessage, this.channel);

		verify(this.channel).send(this.messageCaptor.capture());
		Message<?> actual = this.messageCaptor.getValue();
		assertNotNull(actual);

		assertEquals("s1", SimpMessageHeaderAccessor.getSessionId(actual.getHeaders()));
		assertNotNull(SimpMessageHeaderAccessor.getSessionAttributes(actual.getHeaders()));
		assertNotNull(SimpMessageHeaderAccessor.getUser(actual.getHeaders()));
		assertEquals("joe", SimpMessageHeaderAccessor.getUser(actual.getHeaders()).getName());
		assertNotNull(SimpMessageHeaderAccessor.getHeartbeat(actual.getHeaders()));
		assertArrayEquals(new long[] {10000, 10000}, SimpMessageHeaderAccessor.getHeartbeat(actual.getHeaders()));

		StompHeaderAccessor stompAccessor = StompHeaderAccessor.wrap(actual);
		assertEquals(StompCommand.CONNECT, stompAccessor.getCommand());
		assertEquals("guest", stompAccessor.getLogin());
		assertEquals("guest", stompAccessor.getPasscode());
		assertArrayEquals(new long[] {10000, 10000}, stompAccessor.getHeartbeat());
		assertEquals(new HashSet<>(Arrays.asList("1.1","1.0")), stompAccessor.getAcceptVersion());
		assertEquals(0, this.session.getSentMessages().size());
	}

	@Test
	public void handleMessageFromClientWithImmutableMessageInterceptor() {
		AtomicReference<Boolean> mutable = new AtomicReference<>();
		ExecutorSubscribableChannel channel = new ExecutorSubscribableChannel();
		channel.addInterceptor(new ChannelInterceptorAdapter() {
			@Override
			public Message<?> preSend(Message<?> message, MessageChannel channel) {
				mutable.set(MessageHeaderAccessor.getAccessor(message, MessageHeaderAccessor.class).isMutable());
				return message;
			}
		});
		channel.addInterceptor(new ImmutableMessageChannelInterceptor());

		StompSubProtocolHandler handler = new StompSubProtocolHandler();
		handler.afterSessionStarted(this.session, channel);

		TextMessage message = StompTextMessageBuilder.create(StompCommand.CONNECT).build();
		handler.handleMessageFromClient(this.session, message, channel);
		assertNotNull(mutable.get());
		assertTrue(mutable.get());
	}

	@Test
	public void handleMessageFromClientWithoutImmutableMessageInterceptor() {
		AtomicReference<Boolean> mutable = new AtomicReference<>();
		ExecutorSubscribableChannel channel = new ExecutorSubscribableChannel();
		channel.addInterceptor(new ChannelInterceptorAdapter() {
			@Override
			public Message<?> preSend(Message<?> message, MessageChannel channel) {
				mutable.set(MessageHeaderAccessor.getAccessor(message, MessageHeaderAccessor.class).isMutable());
				return message;
			}
		});

		StompSubProtocolHandler handler = new StompSubProtocolHandler();
		handler.afterSessionStarted(this.session, channel);

		TextMessage message = StompTextMessageBuilder.create(StompCommand.CONNECT).build();
		handler.handleMessageFromClient(this.session, message, channel);
		assertNotNull(mutable.get());
		assertFalse(mutable.get());
	}

	@Test // SPR-14690
	public void handleMessageFromClientWithTokenAuthentication() {
		ExecutorSubscribableChannel channel = new ExecutorSubscribableChannel();
		channel.addInterceptor(new AuthenticationInterceptor("__pete__@gmail.com"));
		channel.addInterceptor(new ImmutableMessageChannelInterceptor());

		TestMessageHandler messageHandler = new TestMessageHandler();
		channel.subscribe(messageHandler);

		StompSubProtocolHandler handler = new StompSubProtocolHandler();
		handler.afterSessionStarted(this.session, channel);

		TextMessage wsMessage = StompTextMessageBuilder.create(StompCommand.CONNECT).build();
		handler.handleMessageFromClient(this.session, wsMessage, channel);

		assertEquals(1, messageHandler.getMessages().size());
		Message<?> message = messageHandler.getMessages().get(0);
		Principal user = SimpMessageHeaderAccessor.getUser(message.getHeaders());
		assertNotNull(user);
		assertEquals("__pete__@gmail.com", user.getName());
	}

	@Test
	public void handleMessageFromClientWithInvalidStompCommand() {

		TextMessage textMessage = new TextMessage("FOO\n\n\0");

		this.protocolHandler.afterSessionStarted(this.session, this.channel);
		this.protocolHandler.handleMessageFromClient(this.session, textMessage, this.channel);

		verifyZeroInteractions(this.channel);
		assertEquals(1, this.session.getSentMessages().size());
		TextMessage actual = (TextMessage) this.session.getSentMessages().get(0);
		assertTrue(actual.getPayload().startsWith("ERROR"));
	}

	@Test
	public void eventPublication() {

		TestPublisher publisher = new TestPublisher();

		this.protocolHandler.setApplicationEventPublisher(publisher);
		this.protocolHandler.afterSessionStarted(this.session, this.channel);

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.CONNECT);
		Message<byte[]> message = MessageBuilder.createMessage(EMPTY_PAYLOAD, headers.getMessageHeaders());
		TextMessage textMessage = new TextMessage(new StompEncoder().encode(message));
		this.protocolHandler.handleMessageFromClient(this.session, textMessage, this.channel);

		headers = StompHeaderAccessor.create(StompCommand.CONNECTED);
		message = MessageBuilder.createMessage(EMPTY_PAYLOAD, headers.getMessageHeaders());
		this.protocolHandler.handleMessageToClient(this.session, message);

		headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
		message = MessageBuilder.createMessage(EMPTY_PAYLOAD, headers.getMessageHeaders());
		textMessage = new TextMessage(new StompEncoder().encode(message));
		this.protocolHandler.handleMessageFromClient(this.session, textMessage, this.channel);

		headers = StompHeaderAccessor.create(StompCommand.UNSUBSCRIBE);
		message = MessageBuilder.createMessage(EMPTY_PAYLOAD, headers.getMessageHeaders());
		textMessage = new TextMessage(new StompEncoder().encode(message));
		this.protocolHandler.handleMessageFromClient(this.session, textMessage, this.channel);

		this.protocolHandler.afterSessionEnded(this.session, CloseStatus.BAD_DATA, this.channel);

		assertEquals("Unexpected events " + publisher.events, 5, publisher.events.size());
		assertEquals(SessionConnectEvent.class, publisher.events.get(0).getClass());
		assertEquals(SessionConnectedEvent.class, publisher.events.get(1).getClass());
		assertEquals(SessionSubscribeEvent.class, publisher.events.get(2).getClass());
		assertEquals(SessionUnsubscribeEvent.class, publisher.events.get(3).getClass());
		assertEquals(SessionDisconnectEvent.class, publisher.events.get(4).getClass());
	}

	@Test
	public void eventPublicationWithExceptions() {

		ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);

		this.protocolHandler.setApplicationEventPublisher(publisher);
		this.protocolHandler.afterSessionStarted(this.session, this.channel);

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.CONNECT);
		Message<byte[]> message = MessageBuilder.createMessage(EMPTY_PAYLOAD, headers.getMessageHeaders());
		TextMessage textMessage = new TextMessage(new StompEncoder().encode(message));
		this.protocolHandler.handleMessageFromClient(this.session, textMessage, this.channel);

		verify(this.channel).send(this.messageCaptor.capture());
		Message<?> actual = this.messageCaptor.getValue();
		assertNotNull(actual);
		assertEquals(StompCommand.CONNECT, StompHeaderAccessor.wrap(actual).getCommand());
		reset(this.channel);

		headers = StompHeaderAccessor.create(StompCommand.CONNECTED);
		message = MessageBuilder.createMessage(EMPTY_PAYLOAD, headers.getMessageHeaders());
		this.protocolHandler.handleMessageToClient(this.session, message);

		assertEquals(1, this.session.getSentMessages().size());
		textMessage = (TextMessage) this.session.getSentMessages().get(0);
		assertEquals("CONNECTED\n" + "user-name:joe\n" + "\n" + "\u0000", textMessage.getPayload());

		this.protocolHandler.afterSessionEnded(this.session, CloseStatus.BAD_DATA, this.channel);

		verify(this.channel).send(this.messageCaptor.capture());
		actual = this.messageCaptor.getValue();
		assertNotNull(actual);
		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(actual);
		assertEquals(StompCommand.DISCONNECT, accessor.getCommand());
		assertEquals("s1", accessor.getSessionId());
		assertEquals("joe", accessor.getUser().getName());
	}

	@Test
	public void webSocketScope() {

		Runnable runnable = Mockito.mock(Runnable.class);
		SimpAttributes simpAttributes = new SimpAttributes(this.session.getId(), this.session.getAttributes());
		simpAttributes.setAttribute("name", "value");
		simpAttributes.registerDestructionCallback("name", runnable);

		MessageChannel testChannel = new MessageChannel() {
			@Override
			public boolean send(Message<?> message) {
				SimpAttributes simpAttributes = SimpAttributesContextHolder.currentAttributes();
				assertThat(simpAttributes.getAttribute("name"), is("value"));
				return true;
			}
			@Override
			public boolean send(Message<?> message, long timeout) {
				return false;
			}
		};

		this.protocolHandler.afterSessionStarted(this.session, this.channel);

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.CONNECT);
		Message<byte[]> message = MessageBuilder.createMessage(EMPTY_PAYLOAD, headers.getMessageHeaders());
		TextMessage textMessage = new TextMessage(new StompEncoder().encode(message));

		this.protocolHandler.handleMessageFromClient(this.session, textMessage, testChannel);
		assertEquals(Collections.<WebSocketMessage<?>>emptyList(), session.getSentMessages());

		this.protocolHandler.afterSessionEnded(this.session, CloseStatus.BAD_DATA, testChannel);
		assertEquals(Collections.<WebSocketMessage<?>>emptyList(), this.session.getSentMessages());
		verify(runnable, times(1)).run();
	}


	private static class UniqueUser extends TestPrincipal implements DestinationUserNameProvider {

		private UniqueUser(String name) {
			super(name);
		}

		@Override
		public String getDestinationUserName() {
			return "Me myself and I";
		}
	}

	private static class TestPublisher implements ApplicationEventPublisher {

		private final List<ApplicationEvent> events = new ArrayList<>();

		@Override
		public void publishEvent(ApplicationEvent event) {
			events.add(event);
		}

		@Override
		public void publishEvent(Object event) {
			publishEvent(new PayloadApplicationEvent<>(this, event));
		}
	}

	private static class TestMessageHandler implements MessageHandler {

		private final List<Message> messages = new ArrayList<>();

		public List<Message> getMessages() {
			return this.messages;
		}

		@Override
		public void handleMessage(Message<?> message) throws MessagingException {
			this.messages.add(message);
		}
	}

	private static class AuthenticationInterceptor extends ChannelInterceptorAdapter {

		private final String name;


		public AuthenticationInterceptor(String name) {
			this.name = name;
		}

@Override
public Message<?> preSend(Message<?> message, MessageChannel channel) {
	TestPrincipal user = new TestPrincipal(name);
	MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class).setUser(user);
	return message;
}
	}
}
