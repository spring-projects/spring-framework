/*
 * Copyright 2002-2018 the original author or authors.
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
package org.springframework.web.server.session;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.web.server.WebSession;

import static junit.framework.TestCase.assertSame;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link InMemoryWebSessionStore}.
 * @author Rob Winch
 */
public class InMemoryWebSessionStoreTests {

	private InMemoryWebSessionStore store = new InMemoryWebSessionStore();


	@Test
	public void startsSessionExplicitly() {
		WebSession session = this.store.createWebSession().block();
		assertNotNull(session);
		session.start();
		assertTrue(session.isStarted());
	}

	@Test
	public void startsSessionImplicitly() {
		WebSession session = this.store.createWebSession().block();
		assertNotNull(session);
		session.start();
		session.getAttributes().put("foo", "bar");
		assertTrue(session.isStarted());
	}

	@Test
	public void retrieveExpiredSession() {
		WebSession session = this.store.createWebSession().block();
		assertNotNull(session);
		session.getAttributes().put("foo", "bar");
		session.save().block();

		String id = session.getId();
		WebSession retrieved = this.store.retrieveSession(id).block();
		assertNotNull(retrieved);
		assertSame(session, retrieved);

		// Fast-forward 31 minutes
		this.store.setClock(Clock.offset(this.store.getClock(), Duration.ofMinutes(31)));
		WebSession retrievedAgain = this.store.retrieveSession(id).block();
		assertNull(retrievedAgain);
	}

	@Test
	public void lastAccessTimeIsUpdatedOnRetrieve() {
		WebSession session1 = this.store.createWebSession().block();
		assertNotNull(session1);
		String id = session1.getId();
		Instant time1 = session1.getLastAccessTime();
		session1.save().block();

		// Fast-forward a few seconds
		this.store.setClock(Clock.offset(this.store.getClock(), Duration.ofSeconds(5)));

		WebSession session2 = this.store.retrieveSession(id).block();
		assertNotNull(session2);
		assertSame(session1, session2);
		Instant time2 = session2.getLastAccessTime();
		assertTrue(time1.isBefore(time2));
	}

	@Test
	public void expirationCheckBasedOnTimeWindow() {

		DirectFieldAccessor accessor = new DirectFieldAccessor(this.store);
		Map<?,?> sessions = (Map<?, ?>) accessor.getPropertyValue("sessions");

		// Create 100 sessions
		IntStream.range(0, 100).forEach(i -> insertSession());

		// Force a new clock (31 min later) but don't use setter which would clean expired sessions
		Clock newClock = Clock.offset(this.store.getClock(), Duration.ofMinutes(31));
		accessor.setPropertyValue("clock", newClock);

		assertEquals(100, sessions.size());

		// Create 50 more which forces a time-based check (clock moved forward)
		IntStream.range(0, 50).forEach(i -> insertSession());
		assertEquals(50, sessions.size());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void expirationCheckBasedOnSessionCount() {

		DirectFieldAccessor accessor = new DirectFieldAccessor(this.store);
		Map<String, WebSession> sessions = (Map<String, WebSession>) accessor.getPropertyValue("sessions");

		// Create 100 sessions
		IntStream.range(0, 100).forEach(i -> insertSession());

		// Copy sessions (about to be expired)
		Map<String, WebSession> expiredSessions = new HashMap<>(sessions);

		// Set new clock which expires and removes above sessions
		this.store.setClock(Clock.offset(this.store.getClock(), Duration.ofMinutes(31)));
		assertEquals(0, sessions.size());

		// Re-insert expired sessions
		sessions.putAll(expiredSessions);
		assertEquals(100, sessions.size());

		// Create 600 more to go over the threshold
		IntStream.range(0, 600).forEach(i -> insertSession());
		assertEquals(600, sessions.size());
	}

	private WebSession insertSession() {
		WebSession session = this.store.createWebSession().block();
		assertNotNull(session);
		session.start();
		session.save().block();
		return session;
	}

}
