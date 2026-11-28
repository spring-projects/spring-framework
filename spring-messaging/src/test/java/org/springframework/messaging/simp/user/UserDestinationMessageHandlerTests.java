/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.simp.user;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import org.springframework.core.testfixture.security.TestPrincipal;
import org.springframework.messaging.Message;
import org.springframework.messaging.StubMessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.broker.OrderedMessageChannelDecorator;
import org.springframework.messaging.simp.stomp.StompBrokerRelayMessageHandler;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.messaging.support.ImmutableMessageChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.messaging.simp.SimpMessageHeaderAccessor.ORIGINAL_DESTINATION;

/**
 * Tests for {@link UserDestinationMessageHandler}.
 */
class UserDestinationMessageHandlerTests {

	private static final String SESSION_ID = "123";

	private final SimpUserRegistry registry = mock();

	private final SubscribableChannel brokerChannel = mock();

	private final UserDestinationMessageHandler handler = new UserDestinationMessageHandler(
			new StubMessageChannel(), this.brokerChannel, new DefaultUserDestinationResolver(this.registry));


	@Test
	@SuppressWarnings("rawtypes")
	void handleSubscribe() {
		given(this.brokerChannel.send(Mockito.any(Message.class))).willReturn(true);
		this.handler.handleMessage(createWith(SimpMessageType.SUBSCRIBE, "joe", SESSION_ID, "/user/queue/foo"));

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		Mockito.verify(this.brokerChannel).send(captor.capture());

		Message message = captor.getValue();
		assertThat(SimpMessageHeaderAccessor.getDestination(message.getHeaders())).isEqualTo("/queue/foo-user123");
	}

	@Test
	@SuppressWarnings("rawtypes")
	void handleUnsubscribe() {
		given(this.brokerChannel.send(Mockito.any(Message.class))).willReturn(true);
		this.handler.handleMessage(createWith(SimpMessageType.UNSUBSCRIBE, "joe", "123", "/user/queue/foo"));

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		Mockito.verify(this.brokerChannel).send(captor.capture());

		Message message = captor.getValue();
		assertThat(SimpMessageHeaderAccessor.getDestination(message.getHeaders())).isEqualTo("/queue/foo-user123");
	}

	@Test
	@SuppressWarnings("rawtypes")
	void handleMessage() {
		TestSimpUser simpUser = new TestSimpUser("joe");
		simpUser.addSessions(new TestSimpSession("123"));
		given(this.registry.getUser("joe")).willReturn(simpUser);
		given(this.brokerChannel.send(Mockito.any(Message.class))).willReturn(true);
		this.handler.handleMessage(createWith(SimpMessageType.MESSAGE, "joe", "123", "/user/joe/queue/foo"));

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		Mockito.verify(this.brokerChannel).send(captor.capture());

		SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(captor.getValue());
		assertThat(accessor.getDestination()).isEqualTo("/queue/foo-user123");
		assertThat(accessor.getFirstNativeHeader(ORIGINAL_DESTINATION)).isEqualTo("/user/queue/foo");
	}

	@Test
	void handleMessageWithPreserveReceiveOrderToMultipleSessions() {
		TestSimpUser simpUser = new TestSimpUser("joe");
		simpUser.addSessions(new TestSimpSession("123"), new TestSimpSession("456"));
		when(this.registry.getUser("joe")).thenReturn(simpUser);
		ExecutorSubscribableChannel inboundChannel = new ExecutorSubscribableChannel();
		OrderedMessageChannelDecorator.configureInterceptor(inboundChannel, true);
		ExecutorSubscribableChannel brokerChannel = new ExecutorSubscribableChannel();
		brokerChannel.addInterceptor(new ImmutableMessageChannelInterceptor());
		List<String> destinations = new ArrayList<>();
		brokerChannel.subscribe(message -> {
			String destination = SimpMessageHeaderAccessor.getDestination(message.getHeaders());
			if (destination != null && !destination.startsWith("/user/")) {
				destinations.add(destination);
			}
		});
		brokerChannel.subscribe(new UserDestinationMessageHandler(inboundChannel, brokerChannel,
				new DefaultUserDestinationResolver(this.registry)));
		SimpMessagingTemplate messagingTemplate = new SimpMessagingTemplate(brokerChannel);

		messagingTemplate.convertAndSendToUser("joe", "/queue/foo", "data");

		assertThat(destinations).containsExactlyInAnyOrder("/queue/foo-user123", "/queue/foo-user456");
	}

	@Test
	@SuppressWarnings("rawtypes")
	void handleMessageWithoutSessionIds() {
		UserDestinationResolver resolver = mock();
		Message message = createWith(SimpMessageType.MESSAGE, "joe", null, "/user/joe/queue/foo");
		UserDestinationResult result = new UserDestinationResult("/queue/foo-user123", Set.of("/queue/foo-user123"), "/user/queue/foo", "joe");
		given(resolver.resolveDestination(message)).willReturn(result);

		given(this.brokerChannel.send(Mockito.any(Message.class))).willReturn(true);
		UserDestinationMessageHandler handler = new UserDestinationMessageHandler(new StubMessageChannel(), this.brokerChannel, resolver);
		handler.handleMessage(message);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		Mockito.verify(this.brokerChannel).send(captor.capture());

		SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(captor.getValue());
		assertThat(accessor.getDestination()).isEqualTo("/queue/foo-user123");
		assertThat(accessor.getFirstNativeHeader(ORIGINAL_DESTINATION)).isEqualTo("/user/queue/foo");
	}

	@Test
	@SuppressWarnings("rawtypes")
	void handleMessageWithoutActiveSession() {
		this.handler.setBroadcastDestination("/topic/unresolved");
		given(this.brokerChannel.send(Mockito.any(Message.class))).willReturn(true);
		this.handler.handleMessage(createWith(SimpMessageType.MESSAGE, "joe", "123", "/user/joe/queue/foo"));

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		Mockito.verify(this.brokerChannel).send(captor.capture());

		Message message = captor.getValue();
		SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(message);
		assertThat(accessor.getDestination()).isEqualTo("/topic/unresolved");
		assertThat(accessor.getFirstNativeHeader(ORIGINAL_DESTINATION)).isEqualTo("/user/joe/queue/foo");

		// Should ignore our own broadcast to brokerChannel

		this.handler.handleMessage(message);
		Mockito.verifyNoMoreInteractions(this.brokerChannel);
	}

	@Test
	@SuppressWarnings("rawtypes")
	void handleMessageFromBrokerWithActiveSession() {
		TestSimpUser simpUser = new TestSimpUser("joe");
		simpUser.addSessions(new TestSimpSession("123"));
		given(this.registry.getUser("joe")).willReturn(simpUser);

		this.handler.setBroadcastDestination("/topic/unresolved");
		given(this.brokerChannel.send(Mockito.any(Message.class))).willReturn(true);

		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.MESSAGE);
		accessor.setSessionId(StompBrokerRelayMessageHandler.SYSTEM_SESSION_ID);
		accessor.setDestination("/topic/unresolved");
		accessor.setNativeHeader(ORIGINAL_DESTINATION, "/user/joe/queue/foo");
		accessor.setNativeHeader("customHeader", "customHeaderValue");
		accessor.setLeaveMutable(true);
		byte[] payload = "payload".getBytes(StandardCharsets.UTF_8);
		this.handler.handleMessage(MessageBuilder.createMessage(payload, accessor.getMessageHeaders()));

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		Mockito.verify(this.brokerChannel).send(captor.capture());
		assertThat(captor.getValue()).isNotNull();
		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(captor.getValue());
		assertThat(headers.getDestination()).isEqualTo("/queue/foo-user123");
		assertThat(headers.getFirstNativeHeader(ORIGINAL_DESTINATION)).isEqualTo("/user/queue/foo");
		assertThat(headers.getFirstNativeHeader("customHeader")).isEqualTo("customHeaderValue");
		assertThat((byte[]) captor.getValue().getPayload()).isEqualTo(payload);
	}

	@Test
	void handleMessageFromBrokerWithoutActiveSession() {
		this.handler.setBroadcastDestination("/topic/unresolved");
		given(this.brokerChannel.send(Mockito.any(Message.class))).willReturn(true);

		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.MESSAGE);
		accessor.setSessionId(StompBrokerRelayMessageHandler.SYSTEM_SESSION_ID);
		accessor.setDestination("/topic/unresolved");
		accessor.setNativeHeader(ORIGINAL_DESTINATION, "/user/joe/queue/foo");
		accessor.setLeaveMutable(true);
		byte[] payload = "payload".getBytes(StandardCharsets.UTF_8);
		this.handler.handleMessage(MessageBuilder.createMessage(payload, accessor.getMessageHeaders()));

		// No re-broadcast
		verifyNoMoreInteractions(this.brokerChannel);
	}

	@Test
	void ignoreMessage() {

		// no destination
		this.handler.handleMessage(createWith(SimpMessageType.MESSAGE, "joe", "123", null));
		verifyNoInteractions(this.brokerChannel);

		// not a user destination
		this.handler.handleMessage(createWith(SimpMessageType.MESSAGE, "joe", "123", "/queue/foo"));
		verifyNoInteractions(this.brokerChannel);

		// subscribe + not a user destination
		this.handler.handleMessage(createWith(SimpMessageType.SUBSCRIBE, "joe", "123", "/queue/foo"));
		verifyNoInteractions(this.brokerChannel);

		// no match on message type
		this.handler.handleMessage(createWith(SimpMessageType.CONNECT, "joe", "123", "user/joe/queue/foo"));
		verifyNoInteractions(this.brokerChannel);
	}


	private Message<?> createWith(
			SimpMessageType type, @Nullable String user, @Nullable String sessionId, @Nullable String destination) {

		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create(type);
		if (destination != null) {
			headers.setDestination(destination);
		}
		if (user != null) {
			headers.setUser(new TestPrincipal(user));
		}
		if (sessionId != null) {
			headers.setSessionId(sessionId);
		}
		return MessageBuilder.withPayload(new byte[0]).setHeaders(headers).build();
	}

}
