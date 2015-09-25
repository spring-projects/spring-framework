/*
 * Copyright 2002-2015 the original author or authors.
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

	private MultiServerUserRegistry multiServerRegistry;

	private MessageConverter converter;


	@Before
	public void setUp() throws Exception {
		this.localRegistry = Mockito.mock(SimpUserRegistry.class);
		this.multiServerRegistry = new MultiServerUserRegistry(this.localRegistry);
		this.converter = new MappingJackson2MessageConverter();
	}

	@Test
	public void getUserFromLocalRegistry() throws Exception {

		SimpUser user = Mockito.mock(SimpUser.class);
		Set<SimpUser> users = Collections.singleton(user);
		when(this.localRegistry.getUsers()).thenReturn(users);
		when(this.localRegistry.getUser("joe")).thenReturn(user);

		assertEquals(1, this.multiServerRegistry.getUsers().size());
		assertSame(user, this.multiServerRegistry.getUser("joe"));
	}

	@Test
	public void getUserFromRemoteRegistry() throws Exception {

		TestSimpSession remoteSession = new TestSimpSession("remote-sess");
		remoteSession.addSubscriptions(new TestSimpSubscription("remote-sub", "/remote-dest"));
		TestSimpUser remoteUser = new TestSimpUser("joe");
		remoteUser.addSessions(remoteSession);
		SimpUserRegistry remoteUserRegistry = mock(SimpUserRegistry.class);
		when(remoteUserRegistry.getUsers()).thenReturn(Collections.singleton(remoteUser));

		MultiServerUserRegistry remoteRegistry = new MultiServerUserRegistry(remoteUserRegistry);
		Message<?> message = this.converter.toMessage(remoteRegistry.getLocalRegistryDto(), null);

		this.multiServerRegistry.addRemoteRegistryDto(message, this.converter, 20000);
		assertEquals(1, this.multiServerRegistry.getUsers().size());

		SimpUser user = this.multiServerRegistry.getUser("joe");
		assertNotNull(user);
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
	public void findUserFromRemoteRegistry() throws Exception {

		TestSimpSubscription subscription1 = new TestSimpSubscription("sub1", "/match");
		TestSimpSession session1 = new TestSimpSession("sess1");
		session1.addSubscriptions(subscription1);
		TestSimpUser user1 = new TestSimpUser("joe");
		user1.addSessions(session1);

		TestSimpSubscription subscription2 = new TestSimpSubscription("sub1", "/match");
		TestSimpSession session2 = new TestSimpSession("sess2");
		session2.addSubscriptions(subscription2);
		TestSimpUser user2 = new TestSimpUser("jane");
		user2.addSessions(session2);

		TestSimpSubscription subscription3 = new TestSimpSubscription("sub1", "/not-a-match");
		TestSimpSession session3 = new TestSimpSession("sess3");
		session3.addSubscriptions(subscription3);
		TestSimpUser user3 = new TestSimpUser("jack");
		user3.addSessions(session3);

		SimpUserRegistry remoteUserRegistry = mock(SimpUserRegistry.class);
		when(remoteUserRegistry.getUsers()).thenReturn(new HashSet<SimpUser>(Arrays.asList(user1, user2, user3)));

		MultiServerUserRegistry remoteRegistry = new MultiServerUserRegistry(remoteUserRegistry);
		Message<?> message = this.converter.toMessage(remoteRegistry.getLocalRegistryDto(), null);

		this.multiServerRegistry.addRemoteRegistryDto(message, this.converter, 20000);
		assertEquals(3, this.multiServerRegistry.getUsers().size());

		Set<SimpSubscription> matches = this.multiServerRegistry.findSubscriptions(new SimpSubscriptionMatcher() {
			@Override
			public boolean match(SimpSubscription subscription) {
				return subscription.getDestination().equals("/match");
			}
		});

		assertEquals(2, matches.size());

		Iterator<SimpSubscription> iterator = matches.iterator();
		Set<String> sessionIds = new HashSet<>(2);
		sessionIds.add(iterator.next().getSession().getId());
		sessionIds.add(iterator.next().getSession().getId());
		assertEquals(new HashSet<>(Arrays.asList("sess1", "sess2")), sessionIds);
	}

	@Test
	public void purgeExpiredRegistries() throws Exception {

		TestSimpUser remoteUser = new TestSimpUser("joe");
		remoteUser.addSessions(new TestSimpSession("remote-sub"));
		SimpUserRegistry remoteUserRegistry = mock(SimpUserRegistry.class);
		when(remoteUserRegistry.getUsers()).thenReturn(Collections.singleton(remoteUser));

		MultiServerUserRegistry remoteRegistry = new MultiServerUserRegistry(remoteUserRegistry);
		Message<?> message = this.converter.toMessage(remoteRegistry.getLocalRegistryDto(), null);

		long expirationPeriod = -1;
		this.multiServerRegistry.addRemoteRegistryDto(message, this.converter, expirationPeriod);
		assertEquals(1, this.multiServerRegistry.getUsers().size());

		this.multiServerRegistry.purgeExpiredRegistries();
		assertEquals(0, this.multiServerRegistry.getUsers().size());
	}

}
