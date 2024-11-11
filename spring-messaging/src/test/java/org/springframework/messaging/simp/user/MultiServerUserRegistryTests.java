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

package org.springframework.messaging.simp.user;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MultiServerUserRegistry}.
 *
 * @author Rossen Stoyanchev
 */
class MultiServerUserRegistryTests {

	private final SimpUserRegistry localRegistry = mock();

	private final MultiServerUserRegistry registry = new MultiServerUserRegistry(this.localRegistry);

	private final MessageConverter converter = new MappingJackson2MessageConverter();


	@Test
	void getUserFromLocalRegistry() {
		SimpUser user = mock();
		Set<SimpUser> users = Collections.singleton(user);
		given(this.localRegistry.getUsers()).willReturn(users);
		given(this.localRegistry.getUserCount()).willReturn(1);
		given(this.localRegistry.getUser("joe")).willReturn(user);

		assertThat(this.registry.getUserCount()).isEqualTo(1);
		assertThat(this.registry.getUser("joe")).isSameAs(user);
	}

	@Test
	void getUserFromRemoteRegistry() {
		// Prepare broadcast message from remote server
		TestSimpUser testUser = new TestSimpUser("joe");
		TestSimpSession testSession = new TestSimpSession("remote-sess");
		testSession.addSubscriptions(new TestSimpSubscription("remote-sub", "/remote-dest"));
		testUser.addSessions(testSession);
		SimpUserRegistry testRegistry = mock();
		given(testRegistry.getUsers()).willReturn(Collections.singleton(testUser));
		Object registryDto = new MultiServerUserRegistry(testRegistry).getLocalRegistryDto();
		Message<?> message = this.converter.toMessage(registryDto, null);

		// Add remote registry
		this.registry.addRemoteRegistryDto(message, this.converter, 20000);

		assertThat(this.registry.getUserCount()).isEqualTo(1);
		SimpUser user = this.registry.getUser("joe");
		assertThat(user).isNotNull();
		assertThat(user.hasSessions()).isTrue();
		assertThat(user.getSessions()).hasSize(1);
		SimpSession session = user.getSession("remote-sess");
		assertThat(session).isNotNull();
		assertThat(session.getId()).isEqualTo("remote-sess");
		assertThat(session.getUser()).isSameAs(user);
		assertThat(session.getSubscriptions()).hasSize(1);
		SimpSubscription subscription = session.getSubscriptions().iterator().next();
		assertThat(subscription.getId()).isEqualTo("remote-sub");
		assertThat(subscription.getSession()).isSameAs(session);
		assertThat(subscription.getDestination()).isEqualTo("/remote-dest");
	}

	@Test
	void findSubscriptionsFromRemoteRegistry() {
		// Prepare broadcast message from remote server
		TestSimpUser user1 = new TestSimpUser("joe");
		TestSimpUser user2 = new TestSimpUser("jane");
		TestSimpUser user3 = new TestSimpUser("jack");
		TestSimpSession session1 = new TestSimpSession("sess1");
		TestSimpSession session2 = new TestSimpSession("sess2");
		TestSimpSession session3 = new TestSimpSession("sess3");
		session1.addSubscriptions(new TestSimpSubscription("sub1", "/match"));
		session2.addSubscriptions(new TestSimpSubscription("sub1", "/match"));
		session3.addSubscriptions(new TestSimpSubscription("sub1", "/not-a-match"));
		user1.addSessions(session1);
		user2.addSessions(session2);
		user3.addSessions(session3);
		SimpUserRegistry userRegistry = mock();
		given(userRegistry.getUsers()).willReturn(new HashSet<>(Arrays.asList(user1, user2, user3)));
		Object registryDto = new MultiServerUserRegistry(userRegistry).getLocalRegistryDto();
		Message<?> message = this.converter.toMessage(registryDto, null);

		// Add remote registry
		this.registry.addRemoteRegistryDto(message, this.converter, 20000);

		assertThat(this.registry.getUserCount()).isEqualTo(3);
		Set<SimpSubscription> matches = this.registry.findSubscriptions(s -> s.getDestination().equals("/match"));
		assertThat(matches).hasSize(2);
		Iterator<SimpSubscription> iterator = matches.iterator();
		Set<String> sessionIds = new HashSet<>(2);
		sessionIds.add(iterator.next().getSession().getId());
		sessionIds.add(iterator.next().getSession().getId());
		assertThat(sessionIds).isEqualTo(new HashSet<>(Arrays.asList("sess1", "sess2")));
	}

	@Test  // SPR-13800
	void getSessionsWhenUserIsConnectedToMultipleServers() {
		// Add user to local registry
		TestSimpUser localUser = new TestSimpUser("joe");
		TestSimpSession localSession = new TestSimpSession("sess123");
		localUser.addSessions(localSession);
		given(this.localRegistry.getUser("joe")).willReturn(localUser);

		// Prepare broadcast message from remote server
		TestSimpUser remoteUser = new TestSimpUser("joe");
		TestSimpSession remoteSession = new TestSimpSession("sess456");
		remoteUser.addSessions(remoteSession);
		SimpUserRegistry remoteRegistry = mock();
		given(remoteRegistry.getUsers()).willReturn(Collections.singleton(remoteUser));
		Object remoteRegistryDto = new MultiServerUserRegistry(remoteRegistry).getLocalRegistryDto();
		Message<?> message = this.converter.toMessage(remoteRegistryDto, null);

		// Add remote registry
		this.registry.addRemoteRegistryDto(message, this.converter, 20000);


		assertThat(this.registry.getUserCount()).isEqualTo(1);
		SimpUser user = this.registry.getUsers().iterator().next();
		assertThat(user.hasSessions()).isTrue();
		assertThat(user.getSessions()).hasSize(2);
		assertThat(user.getSessions()).containsExactlyInAnyOrder(localSession, remoteSession);
		assertThat(user.getSession("sess123")).isSameAs(localSession);
		assertThat(user.getSession("sess456")).isEqualTo(remoteSession);

		user = this.registry.getUser("joe");
		assertThat(user.getSessions()).hasSize(2);
		assertThat(user.getSessions()).containsExactlyInAnyOrder(localSession, remoteSession);
		assertThat(user.getSession("sess123")).isSameAs(localSession);
		assertThat(user.getSession("sess456")).isEqualTo(remoteSession);
	}

	@Test
	void purgeExpiredRegistries() {
		// Prepare broadcast message from remote server
		TestSimpUser testUser = new TestSimpUser("joe");
		testUser.addSessions(new TestSimpSession("remote-sub"));
		SimpUserRegistry testRegistry = mock();
		given(testRegistry.getUsers()).willReturn(Collections.singleton(testUser));
		Object registryDto = new MultiServerUserRegistry(testRegistry).getLocalRegistryDto();
		Message<?> message = this.converter.toMessage(registryDto, null);

		// Add remote registry
		this.registry.addRemoteRegistryDto(message, this.converter, -1);


		assertThat(this.registry.getUserCount()).isEqualTo(1);
		this.registry.purgeExpiredRegistries();
		assertThat(this.registry.getUserCount()).isEqualTo(0);
	}

}
