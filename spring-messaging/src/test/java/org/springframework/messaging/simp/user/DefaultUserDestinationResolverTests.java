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

package org.springframework.messaging.simp.user;

import java.security.Principal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.testfixture.security.TestPrincipal;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for
 * {@link org.springframework.messaging.simp.user.DefaultUserDestinationResolver}.
 *
 * @author Rossen Stoyanchev
 */
public class DefaultUserDestinationResolverTests {

	private DefaultUserDestinationResolver resolver;

	private SimpUserRegistry registry;


	@BeforeEach
	public void setup() {
		TestSimpUser simpUser = new TestSimpUser("joe");
		simpUser.addSessions(new TestSimpSession("123"));

		this.registry = mock(SimpUserRegistry.class);
		given(this.registry.getUser("joe")).willReturn(simpUser);

		this.resolver = new DefaultUserDestinationResolver(this.registry);
	}

	@Test
	public void handleSubscribe() {
		TestPrincipal user = new TestPrincipal("joe");
		String sourceDestination = "/user/queue/foo";

		Message<?> message = createMessage(SimpMessageType.SUBSCRIBE, user, "123", sourceDestination);
		UserDestinationResult actual = this.resolver.resolveDestination(message);

		assertThat(actual.getSourceDestination()).isEqualTo(sourceDestination);
		assertThat(actual.getTargetDestinations().size()).isEqualTo(1);
		assertThat(actual.getTargetDestinations().iterator().next()).isEqualTo("/queue/foo-user123");
		assertThat(actual.getSubscribeDestination()).isEqualTo(sourceDestination);
		assertThat(actual.getUser()).isEqualTo(user.getName());
	}

	@Test // SPR-14044
	public void handleSubscribeForDestinationWithoutLeadingSlash() {
		this.resolver.setRemoveLeadingSlash(true);

		TestPrincipal user = new TestPrincipal("joe");
		String destination = "/user/jms.queue.call";
		Message<?> message = createMessage(SimpMessageType.SUBSCRIBE, user, "123", destination);
		UserDestinationResult actual = this.resolver.resolveDestination(message);

		assertThat(actual.getTargetDestinations().size()).isEqualTo(1);
		assertThat(actual.getTargetDestinations().iterator().next()).isEqualTo("jms.queue.call-user123");
		assertThat(actual.getSubscribeDestination()).isEqualTo(destination);
	}

	@Test // SPR-11325
	public void handleSubscribeOneUserMultipleSessions() {

		TestSimpUser simpUser = new TestSimpUser("joe");
		simpUser.addSessions(new TestSimpSession("123"), new TestSimpSession("456"));
		given(this.registry.getUser("joe")).willReturn(simpUser);

		TestPrincipal user = new TestPrincipal("joe");
		Message<?> message = createMessage(SimpMessageType.SUBSCRIBE, user, "456", "/user/queue/foo");
		UserDestinationResult actual = this.resolver.resolveDestination(message);

		assertThat(actual.getTargetDestinations().size()).isEqualTo(1);
		assertThat(actual.getTargetDestinations().iterator().next()).isEqualTo("/queue/foo-user456");
	}

	@Test
	public void handleSubscribeNoUser() {
		String sourceDestination = "/user/queue/foo";
		Message<?> message = createMessage(SimpMessageType.SUBSCRIBE, null, "123", sourceDestination);
		UserDestinationResult actual = this.resolver.resolveDestination(message);

		assertThat(actual.getSourceDestination()).isEqualTo(sourceDestination);
		assertThat(actual.getTargetDestinations().size()).isEqualTo(1);
		assertThat(actual.getTargetDestinations().iterator().next()).isEqualTo(("/queue/foo-user" + "123"));
		assertThat(actual.getSubscribeDestination()).isEqualTo(sourceDestination);
		assertThat(actual.getUser()).isNull();
	}

	@Test // gh-23836
	public void handleSubscribeInvalidUserName() {
		TestPrincipal user = new TestPrincipal("joe%2F");
		String sourceDestination = "/user/queue/foo";

		Message<?> message = createMessage(SimpMessageType.SUBSCRIBE, user, "123", sourceDestination);
		assertThatIllegalArgumentException().isThrownBy(() -> this.resolver.resolveDestination(message));
	}

	@Test
	public void handleUnsubscribe() {
		TestPrincipal user = new TestPrincipal("joe");
		Message<?> message = createMessage(SimpMessageType.UNSUBSCRIBE, user, "123", "/user/queue/foo");
		UserDestinationResult actual = this.resolver.resolveDestination(message);

		assertThat(actual.getTargetDestinations().size()).isEqualTo(1);
		assertThat(actual.getTargetDestinations().iterator().next()).isEqualTo("/queue/foo-user123");
	}

	@Test
	public void handleMessage() {
		TestPrincipal user = new TestPrincipal("joe");
		String sourceDestination = "/user/joe/queue/foo";
		Message<?> message = createMessage(SimpMessageType.MESSAGE, user, "123", sourceDestination);
		UserDestinationResult actual = this.resolver.resolveDestination(message);

		assertThat(actual.getSourceDestination()).isEqualTo(sourceDestination);
		assertThat(actual.getTargetDestinations().size()).isEqualTo(1);
		assertThat(actual.getTargetDestinations().iterator().next()).isEqualTo("/queue/foo-user123");
		assertThat(actual.getSubscribeDestination()).isEqualTo("/user/queue/foo");
		assertThat(actual.getUser()).isEqualTo(user.getName());
	}

	@Test // SPR-14044
	public void handleMessageForDestinationWithDotSeparator() {
		this.resolver.setRemoveLeadingSlash(true);

		TestPrincipal user = new TestPrincipal("joe");
		String destination = "/user/joe/jms.queue.call";
		Message<?> message = createMessage(SimpMessageType.MESSAGE, user, "123", destination);
		UserDestinationResult actual = this.resolver.resolveDestination(message);

		assertThat(actual.getTargetDestinations().size()).isEqualTo(1);
		assertThat(actual.getTargetDestinations().iterator().next()).isEqualTo("jms.queue.call-user123");
		assertThat(actual.getSubscribeDestination()).isEqualTo("/user/jms.queue.call");
	}

	@Test // SPR-12444
	public void handleMessageToOtherUser() {

		TestSimpUser otherSimpUser = new TestSimpUser("anna");
		otherSimpUser.addSessions(new TestSimpSession("456"));
		given(this.registry.getUser("anna")).willReturn(otherSimpUser);

		TestPrincipal user = new TestPrincipal("joe");
		TestPrincipal otherUser = new TestPrincipal("anna");
		String sourceDestination = "/user/anna/queue/foo";
		Message<?> message = createMessage(SimpMessageType.MESSAGE, user, "456", sourceDestination);

		UserDestinationResult actual = this.resolver.resolveDestination(message);

		assertThat(actual.getSourceDestination()).isEqualTo(sourceDestination);
		assertThat(actual.getTargetDestinations().size()).isEqualTo(1);
		assertThat(actual.getTargetDestinations().iterator().next()).isEqualTo("/queue/foo-user456");
		assertThat(actual.getSubscribeDestination()).isEqualTo("/user/queue/foo");
		assertThat(actual.getUser()).isEqualTo(otherUser.getName());
	}

	@Test
	public void handleMessageEncodedUserName() {
		String userName = "https://joe.openid.example.org/";

		TestSimpUser simpUser = new TestSimpUser(userName);
		simpUser.addSessions(new TestSimpSession("openid123"));
		given(this.registry.getUser(userName)).willReturn(simpUser);

		String destination = "/user/" + StringUtils.replace(userName, "/", "%2F") + "/queue/foo";

		Message<?> message = createMessage(SimpMessageType.MESSAGE, new TestPrincipal("joe"), null, destination);
		UserDestinationResult actual = this.resolver.resolveDestination(message);

		assertThat(actual.getTargetDestinations().size()).isEqualTo(1);
		assertThat(actual.getTargetDestinations().iterator().next()).isEqualTo("/queue/foo-useropenid123");
	}

	@Test
	public void handleMessageWithNoUser() {
		String sourceDestination = "/user/" + "123" + "/queue/foo";
		Message<?> message = createMessage(SimpMessageType.MESSAGE, null, "123", sourceDestination);
		UserDestinationResult actual = this.resolver.resolveDestination(message);

		assertThat(actual.getSourceDestination()).isEqualTo(sourceDestination);
		assertThat(actual.getTargetDestinations().size()).isEqualTo(1);
		assertThat(actual.getTargetDestinations().iterator().next()).isEqualTo("/queue/foo-user123");
		assertThat(actual.getSubscribeDestination()).isEqualTo("/user/queue/foo");
		assertThat(actual.getUser()).isNull();
	}

	@Test
	public void ignoreMessage() {

		// no destination
		TestPrincipal user = new TestPrincipal("joe");
		Message<?> message = createMessage(SimpMessageType.MESSAGE, user, "123", null);
		UserDestinationResult actual = this.resolver.resolveDestination(message);
		assertThat(actual).isNull();

		// not a user destination
		message = createMessage(SimpMessageType.MESSAGE, user, "123", "/queue/foo");
		actual = this.resolver.resolveDestination(message);
		assertThat(actual).isNull();

		// subscribe + not a user destination
		message = createMessage(SimpMessageType.SUBSCRIBE, user, "123", "/queue/foo");
		actual = this.resolver.resolveDestination(message);
		assertThat(actual).isNull();

		// no match on message type
		message = createMessage(SimpMessageType.CONNECT, user, "123", "user/joe/queue/foo");
		actual = this.resolver.resolveDestination(message);
		assertThat(actual).isNull();
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
