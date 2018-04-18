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

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;

/**
 * Unit tests for {@link MultiServerUserRegistry}.
 *
 * @author Rossen Stoyanchev
 */
public class MultiServerUserRegistryTests {

	private SimpUserRegistry localRegistry;

	private MultiServerUserRegistry registry;

	private MessageConverter converter;


	@Before
	public void setup() throws Exception {
		this.localRegistry = Mockito.mock(SimpUserRegistry.class);
		this.registry = new MultiServerUserRegistry(this.localRegistry);
		this.converter = new MappingJackson2MessageConverter();
	}


	@Test
	public void getUserFromLocalRegistry() throws Exception {
		SimpUser user = Mockito.mock(SimpUser.class);
		Set<SimpUser> users = Collections.singleton(user);
		when(this.localRegistry.getUsers()).thenReturn(users);
		when(this.localRegistry.getUserCount()).thenReturn(1);
		when(this.localRegistry.getUser("joe")).thenReturn(user);

		assertEquals(1, this.registry.getUserCount());
		assertSame(user, this.registry.getUser("joe"));
	}

	@Test
	public void getUserFromRemoteRegistry() throws Exception {
		// Prepare broadcast message from remote server
		TestSimpUser testUser = new TestSimpUser("joe");
		TestSimpSession testSession = new TestSimpSession("remote-sess");
		testSession.addSubscriptions(new TestSimpSubscription("remote-sub", "/remote-dest"));
		testUser.addSessions(testSession);
		SimpUserRegistry testRegistry = mock(SimpUserRegistry.class);
		when(testRegistry.getUsers()).thenReturn(Collections.singleton(testUser));
		Object registryDto = new MultiServerUserRegistry(testRegistry).getLocalRegistryDto();
		Message<?> message = this.converter.toMessage(registryDto, null);

		// Add remote registry
		this.registry.addRemoteRegistryDto(message, this.converter, 20000);

		assertEquals(1, this.registry.getUserCount());
		SimpUser user = this.registry.getUser("joe");
		assertNotNull(user);
		assertTrue(user.hasSessions());
		assertEquals(1, user.getSessions().size());
		SimpSession session = user.getSession("remote-sess");
		assertNotNull(session);
		assertEquals("remote-sess", session.getId());
		assertSame(user, session.getUser());
		assertEquals(1, session.getSubscriptions().size());
		SimpSubscription subscription = session.getSubscriptions().iterator().next();
		assertEquals("remote-sub", subscription.getId());
		assertSame(session, subscription.getSession());
		assertEquals("/remote-dest", subscription.getDestination());
	}

	@Test
	public void findSubscriptionsFromRemoteRegistry() throws Exception {
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
		SimpUserRegistry userRegistry = mock(SimpUserRegistry.class);
		when(userRegistry.getUsers()).thenReturn(new HashSet<>(Arrays.asList(user1, user2, user3)));
		Object registryDto = new MultiServerUserRegistry(userRegistry).getLocalRegistryDto();
		Message<?> message = this.converter.toMessage(registryDto, null);

		// Add remote registry
		this.registry.addRemoteRegistryDto(message, this.converter, 20000);

		assertEquals(3, this.registry.getUserCount());
		Set<SimpSubscription> matches = this.registry.findSubscriptions(s -> s.getDestination().equals("/match"));
		assertEquals(2, matches.size());
		Iterator<SimpSubscription> iterator = matches.iterator();
		Set<String> sessionIds = new HashSet<>(2);
		sessionIds.add(iterator.next().getSession().getId());
		sessionIds.add(iterator.next().getSession().getId());
		assertEquals(new HashSet<>(Arrays.asList("sess1", "sess2")), sessionIds);
	}

	@Test  // SPR-13800
	public void getSessionsWhenUserIsConnectedToMultipleServers() throws Exception {
		// Add user to local registry
		TestSimpUser localUser = new TestSimpUser("joe");
		TestSimpSession localSession = new TestSimpSession("sess123");
		localUser.addSessions(localSession);
		when(this.localRegistry.getUser("joe")).thenReturn(localUser);

		// Prepare broadcast message from remote server
		TestSimpUser remoteUser = new TestSimpUser("joe");
		TestSimpSession remoteSession = new TestSimpSession("sess456");
		remoteUser.addSessions(remoteSession);
		SimpUserRegistry remoteRegistry = mock(SimpUserRegistry.class);
		when(remoteRegistry.getUsers()).thenReturn(Collections.singleton(remoteUser));
		Object remoteRegistryDto = new MultiServerUserRegistry(remoteRegistry).getLocalRegistryDto();
		Message<?> message = this.converter.toMessage(remoteRegistryDto, null);

		// Add remote registry
		this.registry.addRemoteRegistryDto(message, this.converter, 20000);


		assertEquals(1, this.registry.getUserCount());
		SimpUser user = this.registry.getUsers().iterator().next();
		assertTrue(user.hasSessions());
		assertEquals(2, user.getSessions().size());
		assertThat(user.getSessions(), containsInAnyOrder(localSession, remoteSession));
		assertSame(localSession, user.getSession("sess123"));
		assertEquals(remoteSession, user.getSession("sess456"));

		user = this.registry.getUser("joe");
		assertEquals(2, user.getSessions().size());
		assertThat(user.getSessions(), containsInAnyOrder(localSession, remoteSession));
		assertSame(localSession, user.getSession("sess123"));
		assertEquals(remoteSession, user.getSession("sess456"));
	}

	@Test
	public void purgeExpiredRegistries() throws Exception {
		// Prepare broadcast message from remote server
		TestSimpUser testUser = new TestSimpUser("joe");
		testUser.addSessions(new TestSimpSession("remote-sub"));
		SimpUserRegistry testRegistry = mock(SimpUserRegistry.class);
		when(testRegistry.getUsers()).thenReturn(Collections.singleton(testUser));
		Object registryDto = new MultiServerUserRegistry(testRegistry).getLocalRegistryDto();
		Message<?> message = this.converter.toMessage(registryDto, null);

		// Add remote registry
		this.registry.addRemoteRegistryDto(message, this.converter, -1);


		assertEquals(1, this.registry.getUserCount());
		this.registry.purgeExpiredRegistries();
		assertEquals(0, this.registry.getUserCount());
	}

}
