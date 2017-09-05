/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.messaging.simp.user;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.security.Principal;

import org.junit.Before;
import org.junit.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.TestPrincipal;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

/**
 * Unit tests for
 * {@link org.springframework.messaging.simp.user.DefaultUserDestinationResolver}.
 *
 * @author Rossen Stoyanchev
 */
public class DefaultUserDestinationResolverTests {

	private DefaultUserDestinationResolver resolver;

	private SimpUserRegistry registry;


	@Before
	public void setup() {
		TestSimpUser simpUser = new TestSimpUser("joe");
		simpUser.addSessions(new TestSimpSession("123"));

		this.registry = mock(SimpUserRegistry.class);
		when(this.registry.getUser("joe")).thenReturn(simpUser);

		this.resolver = new DefaultUserDestinationResolver(this.registry);
	}

	@Test
	public void handleSubscribe() {
		TestPrincipal user = new TestPrincipal("joe");
		String sourceDestination = "/user/queue/foo";

		Message<?> message = createMessage(SimpMessageType.SUBSCRIBE, user, "123", sourceDestination);
		UserDestinationResult actual = this.resolver.resolveDestination(message);

		assertEquals(sourceDestination, actual.getSourceDestination());
		assertEquals(1, actual.getTargetDestinations().size());
		assertEquals("/queue/foo-user123", actual.getTargetDestinations().iterator().next());
		assertEquals(sourceDestination, actual.getSubscribeDestination());
		assertEquals(user.getName(), actual.getUser());
	}

	@Test // SPR-14044
	public void handleSubscribeForDestinationWithoutLeadingSlash() {
		AntPathMatcher pathMatcher = new AntPathMatcher();
		pathMatcher.setPathSeparator(".");
		this.resolver.setPathMatcher(pathMatcher);

		TestPrincipal user = new TestPrincipal("joe");
		String destination = "/user/jms.queue.call";
		Message<?> message = createMessage(SimpMessageType.SUBSCRIBE, user, "123", destination);
		UserDestinationResult actual = this.resolver.resolveDestination(message);

		assertEquals(1, actual.getTargetDestinations().size());
		assertEquals("jms.queue.call-user123", actual.getTargetDestinations().iterator().next());
		assertEquals(destination, actual.getSubscribeDestination());
	}

	@Test // SPR-11325
	public void handleSubscribeOneUserMultipleSessions() {

		TestSimpUser simpUser = new TestSimpUser("joe");
		simpUser.addSessions(new TestSimpSession("123"), new TestSimpSession("456"));
		when(this.registry.getUser("joe")).thenReturn(simpUser);

		TestPrincipal user = new TestPrincipal("joe");
		Message<?> message = createMessage(SimpMessageType.SUBSCRIBE, user, "456", "/user/queue/foo");
		UserDestinationResult actual = this.resolver.resolveDestination(message);

		assertEquals(1, actual.getTargetDestinations().size());
		assertEquals("/queue/foo-user456", actual.getTargetDestinations().iterator().next());
	}

	@Test
	public void handleSubscribeNoUser() {
		String sourceDestination = "/user/queue/foo";
		Message<?> message = createMessage(SimpMessageType.SUBSCRIBE, null, "123", sourceDestination);
		UserDestinationResult actual = this.resolver.resolveDestination(message);

		assertEquals(sourceDestination, actual.getSourceDestination());
		assertEquals(1, actual.getTargetDestinations().size());
		assertEquals("/queue/foo-user" + "123", actual.getTargetDestinations().iterator().next());
		assertEquals(sourceDestination, actual.getSubscribeDestination());
		assertNull(actual.getUser());
	}

	@Test
	public void handleUnsubscribe() {
		TestPrincipal user = new TestPrincipal("joe");
		Message<?> message = createMessage(SimpMessageType.UNSUBSCRIBE, user, "123", "/user/queue/foo");
		UserDestinationResult actual = this.resolver.resolveDestination(message);

		assertEquals(1, actual.getTargetDestinations().size());
		assertEquals("/queue/foo-user123", actual.getTargetDestinations().iterator().next());
	}

	@Test
	public void handleMessage() {
		TestPrincipal user = new TestPrincipal("joe");
		String sourceDestination = "/user/joe/queue/foo";
		Message<?> message = createMessage(SimpMessageType.MESSAGE, user, "123", sourceDestination);
		UserDestinationResult actual = this.resolver.resolveDestination(message);

		assertEquals(sourceDestination, actual.getSourceDestination());
		assertEquals(1, actual.getTargetDestinations().size());
		assertEquals("/queue/foo-user123", actual.getTargetDestinations().iterator().next());
		assertEquals("/user/queue/foo", actual.getSubscribeDestination());
		assertEquals(user.getName(), actual.getUser());
	}

	@Test // SPR-14044
	public void handleMessageForDestinationWithDotSeparator() {
		AntPathMatcher pathMatcher = new AntPathMatcher();
		pathMatcher.setPathSeparator(".");
		this.resolver.setPathMatcher(pathMatcher);

		TestPrincipal user = new TestPrincipal("joe");
		String destination = "/user/joe/jms.queue.call";
		Message<?> message = createMessage(SimpMessageType.MESSAGE, user, "123", destination);
		UserDestinationResult actual = this.resolver.resolveDestination(message);

		assertEquals(1, actual.getTargetDestinations().size());
		assertEquals("jms.queue.call-user123", actual.getTargetDestinations().iterator().next());
		assertEquals("/user/jms.queue.call", actual.getSubscribeDestination());
	}

	@Test // SPR-12444
	public void handleMessageToOtherUser() {

		TestSimpUser otherSimpUser = new TestSimpUser("anna");
		otherSimpUser.addSessions(new TestSimpSession("456"));
		when(this.registry.getUser("anna")).thenReturn(otherSimpUser);

		TestPrincipal user = new TestPrincipal("joe");
		TestPrincipal otherUser = new TestPrincipal("anna");
		String sourceDestination = "/user/anna/queue/foo";
		Message<?> message = createMessage(SimpMessageType.MESSAGE, user, "456", sourceDestination);

		UserDestinationResult actual = this.resolver.resolveDestination(message);

		assertEquals(sourceDestination, actual.getSourceDestination());
		assertEquals(1, actual.getTargetDestinations().size());
		assertEquals("/queue/foo-user456", actual.getTargetDestinations().iterator().next());
		assertEquals("/user/queue/foo", actual.getSubscribeDestination());
		assertEquals(otherUser.getName(), actual.getUser());
	}

	@Test
	public void handleMessageEncodedUserName() {
		String userName = "http://joe.openid.example.org/";

		TestSimpUser simpUser = new TestSimpUser(userName);
		simpUser.addSessions(new TestSimpSession("openid123"));
		when(this.registry.getUser(userName)).thenReturn(simpUser);

		String destination = "/user/" + StringUtils.replace(userName, "/", "%2F") + "/queue/foo";

		Message<?> message = createMessage(SimpMessageType.MESSAGE, new TestPrincipal("joe"), null, destination);
		UserDestinationResult actual = this.resolver.resolveDestination(message);

		assertEquals(1, actual.getTargetDestinations().size());
		assertEquals("/queue/foo-useropenid123", actual.getTargetDestinations().iterator().next());
	}

	@Test
	public void handleMessageWithNoUser() {
		String sourceDestination = "/user/" + "123" + "/queue/foo";
		Message<?> message = createMessage(SimpMessageType.MESSAGE, null, "123", sourceDestination);
		UserDestinationResult actual = this.resolver.resolveDestination(message);

		assertEquals(sourceDestination, actual.getSourceDestination());
		assertEquals(1, actual.getTargetDestinations().size());
		assertEquals("/queue/foo-user123", actual.getTargetDestinations().iterator().next());
		assertEquals("/user/queue/foo", actual.getSubscribeDestination());
		assertNull(actual.getUser());
	}

	@Test
	public void ignoreMessage() {

		// no destination
		TestPrincipal user = new TestPrincipal("joe");
		Message<?> message = createMessage(SimpMessageType.MESSAGE, user, "123", null);
		UserDestinationResult actual = this.resolver.resolveDestination(message);
		assertNull(actual);

		// not a user destination
		message = createMessage(SimpMessageType.MESSAGE, user, "123", "/queue/foo");
		actual = this.resolver.resolveDestination(message);
		assertNull(actual);

		// subscribe + not a user destination
		message = createMessage(SimpMessageType.SUBSCRIBE, user, "123", "/queue/foo");
		actual = this.resolver.resolveDestination(message);
		assertNull(actual);

		// no match on message type
		message = createMessage(SimpMessageType.CONNECT, user, "123", "user/joe/queue/foo");
		actual = this.resolver.resolveDestination(message);
		assertNull(actual);
	}

	private Message<?> createMessage(SimpMessageType type, Principal user, String sessionId, String destination) {
		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create(type);
		if (destination != null) {
			headers.setDestination(destination);
		}
		if (user != null) {
			headers.setUser(user);
		}
		if (sessionId != null) {
			headers.setSessionId(sessionId);
		}
		return MessageBuilder.withPayload(new byte[0]).setHeaders(headers).build();
	}

}
