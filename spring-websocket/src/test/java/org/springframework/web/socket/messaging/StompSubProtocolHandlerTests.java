/*
 * Copyright 2002-2019 the original author or authors.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.core.testfixture.security.TestPrincipal;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpAttributes;
import org.springframework.messaging.simp.SimpAttributesContextHolder;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompEncoder;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.user.DestinationUserNameProvider;
import org.springframework.messaging.support.ChannelInterceptor;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Test fixture for {@link StompSubProtocolHandler} tests.
 * @author Rossen Stoyanchev
 */
public class StompSubProtocolHandlerTests {

	private static final byte[] EMPTY_PAYLOAD = new byte[0];

	private StompSubProtocolHandler protocolHandler;

	private TestWebSocketSession session;

	private MessageChannel channel;

	@SuppressWarnings("rawtypes")
	private ArgumentCaptor<Message> messageCaptor;


	@BeforeEach
	public void setup() {
		this.protocolHandler = new StompSubProtocolHandler();
		this.channel = Mockito.mock(MessageChannel.class);
		this.messageCaptor = ArgumentCaptor.forClass(Message.class);

		given(this.channel.send(any())).willReturn(true);

		this.session = new TestWebSocketSession();
		this.session.setId("s1");
		this.session.setPrincipal(new TestPrincipal("joe"));
	}

	@Test
	public void handleMessageToClientWithConnectedFrame() {

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.CONNECTED);
		Message<byte[]> message = MessageBuilder.createMessage(EMPTY_PAYLOAD, headers.getMessageHeaders());
		this.protocolHandler.handleMessageToClient(this.session, message);

		assertThat(this.session.getSentMessages().size()).isEqualTo(1);
		WebSocketMessage<?> textMessage = this.session.getSentMessages().get(0);
		assertThat(textMessage.getPayload()).isEqualTo(("CONNECTED\n" + "user-name:joe\n" + "\n" + "\u0000"));
	}

	@Test
	public void handleMessageToClientWithDestinationUserNameProvider() {

		this.session.setPrincipal(new UniqueUser("joe"));

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.CONNECTED);
		Message<byte[]> message = MessageBuilder.createMessage(EMPTY_PAYLOAD, headers.getMessageHeaders());
		this.protocolHandler.handleMessageToClient(this.session, message);

		assertThat(this.session.getSentMessages().size()).isEqualTo(1);
		WebSocketMessage<?> textMessage = this.session.getSentMessages().get(0);
		assertThat(textMessage.getPayload()).isEqualTo(("CONNECTED\n" + "user-name:joe\n" + "\n" + "\u0000"));
	}

	@Test
	public void handleMessageToClientWithSimpConnectAck() {

		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
		accessor.setHeartbeat(10000, 10000);
		accessor.setAcceptVersion("1.0,1.1,1.2");
		Message<?> connectMessage = MessageBuilder.createMessage(EMPTY_PAYLOAD, accessor.getMessageHeaders());

		SimpMessageHeaderAccessor ackAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.CONNECT_ACK);
		ackAccessor.setHeader(SimpMessageHeaderAccessor.CONNECT_MESSAGE_HEADER, connectMessage);
		ackAccessor.setHeader(SimpMessageHeaderAccessor.HEART_BEAT_HEADER, new long[] {15000, 15000});
		Message<byte[]> ackMessage = MessageBuilder.createMessage(EMPTY_PAYLOAD, ackAccessor.getMessageHeaders());
		this.protocolHandler.handleMessageToClient(this.session, ackMessage);

		assertThat(this.session.getSentMessages().size()).isEqualTo(1);
		TextMessage actual = (TextMessage) this.session.getSentMessages().get(0);
		assertThat(actual.getPayload()).isEqualTo(("CONNECTED\n" + "version:1.2\n" + "heart-beat:15000,15000\n" +
				"user-name:joe\n" + "\n" + "\u0000"));
	}

	@Test
	public void handleMessageToClientWithSimpConnectAckDefaultHeartBeat() {

		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
		accessor.setHeartbeat(10000, 10000);
		accessor.setAcceptVersion("1.0");
		Message<?> connectMessage = MessageBuilder.createMessage(EMPTY_PAYLOAD, accessor.getMessageHeaders());

		SimpMessageHeaderAccessor ackAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.CONNECT_ACK);
		ackAccessor.setHeader(SimpMessageHeaderAccessor.CONNECT_MESSAGE_HEADER, connectMessage);
		Message<byte[]> ackMessage = MessageBuilder.createMessage(EMPTY_PAYLOAD, ackAccessor.getMessageHeaders());
		this.protocolHandler.handleMessageToClient(this.session, ackMessage);

		assertThat(this.session.getSentMessages().size()).isEqualTo(1);
		TextMessage actual = (TextMessage) this.session.getSentMessages().get(0);
		assertThat(actual.getPayload()).isEqualTo(("CONNECTED\n" + "version:1.0\n" + "heart-beat:0,0\n" +
				"user-name:joe\n" + "\n" + "\u0000"));
	}

	@Test
	public void handleMessageToClientWithSimpDisconnectAck() {

		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
		Message<?> connectMessage = MessageBuilder.createMessage(EMPTY_PAYLOAD, accessor.getMessageHeaders());

		SimpMessageHeaderAccessor ackAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.DISCONNECT_ACK);
		ackAccessor.setHeader(SimpMessageHeaderAccessor.DISCONNECT_MESSAGE_HEADER, connectMessage);
		Message<byte[]> ackMessage = MessageBuilder.createMessage(EMPTY_PAYLOAD, ackAccessor.getMessageHeaders());
		this.protocolHandler.handleMessageToClient(this.session, ackMessage);

		assertThat(this.session.getSentMessages().size()).isEqualTo(1);
		TextMessage actual = (TextMessage) this.session.getSentMessages().get(0);
		assertThat(actual.getPayload()).isEqualTo(("ERROR\n" + "message:Session closed.\n" + "content-length:0\n" +
				"\n\u0000"));
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

		assertThat(this.session.getSentMessages().size()).isEqualTo(1);
		TextMessage actual = (TextMessage) this.session.getSentMessages().get(0);
		assertThat(actual.getPayload()).isEqualTo(("RECEIPT\n" + "receipt-id:message-123\n" + "\n\u0000"));
	}

	@Test
	public void handleMessageToClientWithSimpHeartbeat() {

		SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.HEARTBEAT);
		accessor.setSessionId("s1");
		accessor.setUser(new TestPrincipal("joe"));
		Message<byte[]> ackMessage = MessageBuilder.createMessage(EMPTY_PAYLOAD, accessor.getMessageHeaders());
		this.protocolHandler.handleMessageToClient(this.session, ackMessage);

		assertThat(this.session.getSentMessages().size()).isEqualTo(1);
		TextMessage actual = (TextMessage) this.session.getSentMessages().get(0);
		assertThat(actual.getPayload()).isEqualTo("\n");
	}

	@Test
	public void handleMessageToClientWithHeartbeatSuppressingSockJsHeartbeat() throws IOException {

		SockJsSession sockJsSession = Mockito.mock(SockJsSession.class);
		given(sockJsSession.getId()).willReturn("s1");
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
		given(sockJsSession.getId()).willReturn("s1");
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

		assertThat(this.session.getSentMessages().size()).isEqualTo(1);
		WebSocketMessage<?> textMessage = this.session.getSentMessages().get(0);
		assertThat(((String) textMessage.getPayload()).contains("destination:/user/queue/foo\n")).isTrue();
		assertThat(((String) textMessage.getPayload()).contains(SimpMessageHeaderAccessor.ORIGINAL_DESTINATION)).isFalse();
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

		assertThat(this.session.getSentMessages().size()).isEqualTo(1);
		WebSocketMessage<?> webSocketMessage = this.session.getSentMessages().get(0);
		assertThat(webSocketMessage instanceof BinaryMessage).isTrue();

		// Empty payload

		payload = EMPTY_PAYLOAD;
		message = MessageBuilder.createMessage(payload, headers.getMessageHeaders());
		this.protocolHandler.handleMessageToClient(this.session, message);

		assertThat(this.session.getSentMessages().size()).isEqualTo(2);
		webSocketMessage = this.session.getSentMessages().get(1);
		assertThat(webSocketMessage instanceof TextMessage).isTrue();
	}

	@Test
	public void handleMessageFromClient() {

		TextMessage textMessage = StompTextMessageBuilder.create(StompCommand.STOMP).headers(
				"login:guest", "passcode:guest", "accept-version:1.1,1.0", "heart-beat:10000,10000").build();

		this.protocolHandler.afterSessionStarted(this.session, this.channel);
		this.protocolHandler.handleMessageFromClient(this.session, textMessage, this.channel);

		verify(this.channel).send(this.messageCaptor.capture());
		Message<?> actual = this.messageCaptor.getValue();
		assertThat(actual).isNotNull();

		assertThat(SimpMessageHeaderAccessor.getSessionId(actual.getHeaders())).isEqualTo("s1");
		assertThat(SimpMessageHeaderAccessor.getSessionAttributes(actual.getHeaders())).isNotNull();
		assertThat(SimpMessageHeaderAccessor.getUser(actual.getHeaders())).isNotNull();
		assertThat(SimpMessageHeaderAccessor.getUser(actual.getHeaders()).getName()).isEqualTo("joe");
		assertThat(SimpMessageHeaderAccessor.getHeartbeat(actual.getHeaders())).isNotNull();
		assertThat(SimpMessageHeaderAccessor.getHeartbeat(actual.getHeaders())).isEqualTo(new long[] {10000, 10000});

		StompHeaderAccessor stompAccessor = StompHeaderAccessor.wrap(actual);
		assertThat(stompAccessor.getCommand()).isEqualTo(StompCommand.STOMP);
		assertThat(stompAccessor.getLogin()).isEqualTo("guest");
		assertThat(stompAccessor.getPasscode()).isEqualTo("guest");
		assertThat(stompAccessor.getHeartbeat()).isEqualTo(new long[] {10000, 10000});
		assertThat(stompAccessor.getAcceptVersion()).isEqualTo(new HashSet<>(Arrays.asList("1.1","1.0")));
		assertThat(this.session.getSentMessages().size()).isEqualTo(0);
	}

	@Test
	public void handleMessageFromClientWithImmutableMessageInterceptor() {
		AtomicReference<Boolean> mutable = new AtomicReference<>();
		ExecutorSubscribableChannel channel = new ExecutorSubscribableChannel();
		channel.addInterceptor(new ChannelInterceptor() {
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
		assertThat(mutable.get()).isNotNull();
		assertThat(mutable.get()).isTrue();
	}

	@Test
	public void handleMessageFromClientWithoutImmutableMessageInterceptor() {
		AtomicReference<Boolean> mutable = new AtomicReference<>();
		ExecutorSubscribableChannel channel = new ExecutorSubscribableChannel();
		channel.addInterceptor(new ChannelInterceptor() {
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
		assertThat(mutable.get()).isNotNull();
		assertThat(mutable.get()).isFalse();
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

		assertThat(messageHandler.getMessages().size()).isEqualTo(1);
		Message<?> message = messageHandler.getMessages().get(0);
		Principal user = SimpMessageHeaderAccessor.getUser(message.getHeaders());
		assertThat(user).isNotNull();
		assertThat(user.getName()).isEqualTo("__pete__@gmail.com");

		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECTED);
		message = MessageBuilder.createMessage(EMPTY_PAYLOAD, accessor.getMessageHeaders());
		handler.handleMessageToClient(this.session, message);

		assertThat(this.session.getSentMessages()).hasSize(1);
		WebSocketMessage<?> textMessage = this.session.getSentMessages().get(0);
		assertThat(textMessage.getPayload())
				.isEqualTo("CONNECTED\n" + "user-name:__pete__@gmail.com\n" + "\n" + "\u0000");
	}

	@Test
	public void handleMessageFromClientWithInvalidStompCommand() {

		TextMessage textMessage = new TextMessage("FOO\n\n\0");

		this.protocolHandler.afterSessionStarted(this.session, this.channel);
		this.protocolHandler.handleMessageFromClient(this.session, textMessage, this.channel);

		verifyNoInteractions(this.channel);
		assertThat(this.session.getSentMessages().size()).isEqualTo(1);
		TextMessage actual = (TextMessage) this.session.getSentMessages().get(0);
		assertThat(actual.getPayload().startsWith("ERROR")).isTrue();
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

		assertThat(publisher.events.size()).as("Unexpected events " + publisher.events).isEqualTo(5);
		assertThat(publisher.events.get(0).getClass()).isEqualTo(SessionConnectEvent.class);
		assertThat(publisher.events.get(1).getClass()).isEqualTo(SessionConnectedEvent.class);
		assertThat(publisher.events.get(2).getClass()).isEqualTo(SessionSubscribeEvent.class);
		assertThat(publisher.events.get(3).getClass()).isEqualTo(SessionUnsubscribeEvent.class);
		assertThat(publisher.events.get(4).getClass()).isEqualTo(SessionDisconnectEvent.class);
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
		assertThat(actual).isNotNull();
		assertThat(StompHeaderAccessor.wrap(actual).getCommand()).isEqualTo(StompCommand.CONNECT);
		reset(this.channel);

		headers = StompHeaderAccessor.create(StompCommand.CONNECTED);
		message = MessageBuilder.createMessage(EMPTY_PAYLOAD, headers.getMessageHeaders());
		this.protocolHandler.handleMessageToClient(this.session, message);

		assertThat(this.session.getSentMessages().size()).isEqualTo(1);
		textMessage = (TextMessage) this.session.getSentMessages().get(0);
		assertThat(textMessage.getPayload()).isEqualTo(("CONNECTED\n" + "user-name:joe\n" + "\n" + "\u0000"));

		this.protocolHandler.afterSessionEnded(this.session, CloseStatus.BAD_DATA, this.channel);

		verify(this.channel).send(this.messageCaptor.capture());
		actual = this.messageCaptor.getValue();
		assertThat(actual).isNotNull();
		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(actual);
		assertThat(accessor.getCommand()).isEqualTo(StompCommand.DISCONNECT);
		assertThat(accessor.getSessionId()).isEqualTo("s1");
		assertThat(accessor.getUser().getName()).isEqualTo("joe");
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
				assertThat(simpAttributes.getAttribute("name")).isEqualTo("value");
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
		assertThat(session.getSentMessages()).isEqualTo(Collections.<WebSocketMessage<?>>emptyList());

		this.protocolHandler.afterSessionEnded(this.session, CloseStatus.BAD_DATA, testChannel);
		assertThat(this.session.getSentMessages()).isEqualTo(Collections.<WebSocketMessage<?>>emptyList());
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

		private final List<Message<?>> messages = new ArrayList<>();

		public List<Message<?>> getMessages() {
			return this.messages;
		}

		@Override
		public void handleMessage(Message<?> message) throws MessagingException {
			this.messages.add(message);
		}
	}

	private static class AuthenticationInterceptor implements ChannelInterceptor {

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
