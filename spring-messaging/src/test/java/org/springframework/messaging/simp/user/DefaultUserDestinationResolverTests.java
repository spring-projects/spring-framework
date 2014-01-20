/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.messaging.simp.user;

import org.junit.Before;
import org.junit.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.TestPrincipal;
import org.springframework.messaging.support.MessageBuilder;

import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link org.springframework.messaging.simp.user.DefaultUserDestinationResolver}.
 */
public class DefaultUserDestinationResolverTests {

	public static final String SESSION_ID = "123";

	private DefaultUserDestinationResolver resolver;

	private UserSessionRegistry registry;


	@Before
	public void setup() {
		this.registry = new DefaultUserSessionRegistry();
		this.registry.registerSessionId("joe", SESSION_ID);

		this.resolver = new DefaultUserDestinationResolver(this.registry);
	}


	@Test
	public void handleSubscribe() {
		Message<?> message = createMessage(SimpMessageType.SUBSCRIBE, "joe", SESSION_ID, "/user/queue/foo");
		Set<String> actual = this.resolver.resolveDestination(message);

		assertEquals(1, actual.size());
		assertEquals("/queue/foo-user123", actual.iterator().next());
	}

	// SPR-11325

	@Test
	public void handleSubscribeOneUserMultipleSessions() {

		this.registry.registerSessionId("joe", "456");
		this.registry.registerSessionId("joe", "789");

		Message<?> message = createMessage(SimpMessageType.SUBSCRIBE, "joe", SESSION_ID, "/user/queue/foo");
		Set<String> actual = this.resolver.resolveDestination(message);

		assertEquals(1, actual.size());
		assertEquals("/queue/foo-user123", actual.iterator().next());
	}

	@Test
	public void handleUnsubscribe() {
		Message<?> message = createMessage(SimpMessageType.UNSUBSCRIBE, "joe", SESSION_ID, "/user/queue/foo");
		Set<String> actual = this.resolver.resolveDestination(message);

		assertEquals(1, actual.size());
		assertEquals("/queue/foo-user123", actual.iterator().next());
	}

	@Test
	public void handleMessage() {
		Message<?> message = createMessage(SimpMessageType.MESSAGE, "joe", SESSION_ID, "/user/joe/queue/foo");
		Set<String> actual = this.resolver.resolveDestination(message);

		assertEquals(1, actual.size());
		assertEquals("/queue/foo-user123", actual.iterator().next());
	}


	@Test
	public void ignoreMessage() {

		// no destination
		Message<?> message = createMessage(SimpMessageType.MESSAGE, "joe", SESSION_ID, null);
		Set<String> actual = this.resolver.resolveDestination(message);
		assertEquals(0, actual.size());

		// not a user destination
		message = createMessage(SimpMessageType.MESSAGE, "joe", SESSION_ID, "/queue/foo");
		actual = this.resolver.resolveDestination(message);
		assertEquals(0, actual.size());

		// subscribe + no user
		message = createMessage(SimpMessageType.SUBSCRIBE, null, SESSION_ID, "/user/queue/foo");
		actual = this.resolver.resolveDestination(message);
		assertEquals(0, actual.size());

		// subscribe + not a user destination
		message = createMessage(SimpMessageType.SUBSCRIBE, "joe", SESSION_ID, "/queue/foo");
		actual = this.resolver.resolveDestination(message);
		assertEquals(0, actual.size());

		// no match on message type
		message = createMessage(SimpMessageType.CONNECT, "joe", SESSION_ID, "user/joe/queue/foo");
		actual = this.resolver.resolveDestination(message);
		assertEquals(0, actual.size());
	}


	private Message<?> createMessage(SimpMessageType messageType, String user, String sessionId, String destination) {
		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create(messageType);
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
