/*
 * Copyright 2002-2024 the original author or authors.
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.core.testfixture.security.TestPrincipal;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.user.SimpSubscription;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.CloseStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture for
 * {@link DefaultSimpUserRegistry}
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
class DefaultSimpUserRegistryTests {

	@Test
	void addOneSessionId() {
		TestPrincipal user = new TestPrincipal("joe");
		Message<byte[]> message = createMessage(SimpMessageType.CONNECT_ACK, "123");
		SessionConnectedEvent event = new SessionConnectedEvent(this, message, user);

		DefaultSimpUserRegistry registry = new DefaultSimpUserRegistry();
		registry.onApplicationEvent(event);

		SimpUser simpUser = registry.getUser("joe");
		assertThat(simpUser).isNotNull();

		assertThat(registry.getUserCount()).isEqualTo(1);
		assertThat(simpUser.getSessions()).hasSize(1);
		assertThat(simpUser.getSession("123")).isNotNull();
	}

	@Test
	void addMultipleSessionIds() {
		DefaultSimpUserRegistry registry = new DefaultSimpUserRegistry();

		TestPrincipal user = new TestPrincipal("joe");
		Message<byte[]> message = createMessage(SimpMessageType.CONNECT_ACK, "123");
		SessionConnectedEvent event = new SessionConnectedEvent(this, message, user);
		registry.onApplicationEvent(event);

		message = createMessage(SimpMessageType.CONNECT_ACK, "456");
		event = new SessionConnectedEvent(this, message, user);
		registry.onApplicationEvent(event);

		message = createMessage(SimpMessageType.CONNECT_ACK, "789");
		event = new SessionConnectedEvent(this, message, user);
		registry.onApplicationEvent(event);

		SimpUser simpUser = registry.getUser("joe");
		assertThat(simpUser).isNotNull();

		assertThat(registry.getUserCount()).isEqualTo(1);
		assertThat(simpUser.getSessions()).hasSize(3);
		assertThat(simpUser.getSession("123")).isNotNull();
		assertThat(simpUser.getSession("456")).isNotNull();
		assertThat(simpUser.getSession("789")).isNotNull();
	}

	@Test
	void removeSessionIds() {
		DefaultSimpUserRegistry registry = new DefaultSimpUserRegistry();

		TestPrincipal user = new TestPrincipal("joe");
		Message<byte[]> message = createMessage(SimpMessageType.CONNECT_ACK, "123");
		SessionConnectedEvent connectedEvent = new SessionConnectedEvent(this, message, user);
		registry.onApplicationEvent(connectedEvent);

		message = createMessage(SimpMessageType.CONNECT_ACK, "456");
		connectedEvent = new SessionConnectedEvent(this, message, user);
		registry.onApplicationEvent(connectedEvent);

		message = createMessage(SimpMessageType.CONNECT_ACK, "789");
		connectedEvent = new SessionConnectedEvent(this, message, user);
		registry.onApplicationEvent(connectedEvent);

		SimpUser simpUser = registry.getUser("joe");
		assertThat(simpUser).isNotNull();
		assertThat(simpUser.getSessions()).hasSize(3);

		CloseStatus status = CloseStatus.GOING_AWAY;
		message = createMessage(SimpMessageType.DISCONNECT, "456");
		SessionDisconnectEvent disconnectEvent = new SessionDisconnectEvent(this, message, "456", status, user);
		registry.onApplicationEvent(disconnectEvent);

		message = createMessage(SimpMessageType.DISCONNECT, "789");
		disconnectEvent = new SessionDisconnectEvent(this, message, "789", status, user);
		registry.onApplicationEvent(disconnectEvent);

		assertThat(simpUser.getSessions()).hasSize(1);
		assertThat(simpUser.getSession("123")).isNotNull();
	}

	@Test
	void findSubscriptions() {
		DefaultSimpUserRegistry registry = new DefaultSimpUserRegistry();

		TestPrincipal user = new TestPrincipal("joe");
		Message<byte[]> message = createMessage(SimpMessageType.CONNECT_ACK, "123");
		SessionConnectedEvent event = new SessionConnectedEvent(this, message, user);
		registry.onApplicationEvent(event);

		message = createMessage(SimpMessageType.SUBSCRIBE, "123", "sub1", "/match");
		SessionSubscribeEvent subscribeEvent = new SessionSubscribeEvent(this, message, user);
		registry.onApplicationEvent(subscribeEvent);

		message = createMessage(SimpMessageType.SUBSCRIBE, "123", "sub2", "/match");
		subscribeEvent = new SessionSubscribeEvent(this, message, user);
		registry.onApplicationEvent(subscribeEvent);

		message = createMessage(SimpMessageType.SUBSCRIBE, "123", "sub3", "/not-a-match");
		subscribeEvent = new SessionSubscribeEvent(this, message, user);
		registry.onApplicationEvent(subscribeEvent);

		Set<SimpSubscription> matches = registry.findSubscriptions(subscription -> subscription.getDestination().equals("/match"));

		assertThat(matches).hasSize(2);

		Iterator<SimpSubscription> iterator = matches.iterator();
		Set<String> sessionIds = new HashSet<>(2);
		sessionIds.add(iterator.next().getId());
		sessionIds.add(iterator.next().getId());
		assertThat(sessionIds).isEqualTo(new HashSet<>(Arrays.asList("sub1", "sub2")));
	}

	@Test
	void nullSessionId() {
		DefaultSimpUserRegistry registry = new DefaultSimpUserRegistry();

		TestPrincipal user = new TestPrincipal("joe");
		Message<byte[]> message = createMessage(SimpMessageType.CONNECT_ACK, "123");
		SessionConnectedEvent event = new SessionConnectedEvent(this, message, user);
		registry.onApplicationEvent(event);

		SimpUser simpUser = registry.getUser("joe");
		assertThat(simpUser.getSession(null)).isNull();
	}


	private Message<byte[]> createMessage(SimpMessageType type, String sessionId) {
		return createMessage(type, sessionId, null, null);
	}

	private Message<byte[]> createMessage(SimpMessageType type, String sessionId, String subscriptionId,
			String destination) {

		SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(type);
		accessor.setSessionId(sessionId);
		if (destination != null) {
			accessor.setDestination(destination);
		}
		if (subscriptionId != null) {
			accessor.setSubscriptionId(subscriptionId);
		}
		return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
	}

}
