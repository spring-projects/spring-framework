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
package org.springframework.web.server.session;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.junit.Test;

import org.springframework.util.Assert;
import org.springframework.web.server.WebSession;

import static junit.framework.TestCase.assertSame;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
	public void retrieveExpiredSession() throws Exception {
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
	public void lastAccessTimeIsUpdatedOnRetrieve() throws Exception {
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
	public void expirationChecks() throws Exception {
		// Create 3 sessions
		WebSession session1 = this.store.createWebSession().block();
		assertNotNull(session1);
		session1.start();
		session1.save().block();

		WebSession session2 = this.store.createWebSession().block();
		assertNotNull(session2);
		session2.start();
		session2.save().block();

		WebSession session3 = this.store.createWebSession().block();
		assertNotNull(session3);
		session3.start();
		session3.save().block();

		// Fast-forward 31 minutes
		this.store.setClock(Clock.offset(this.store.getClock(), Duration.ofMinutes(31)));

		// Create 2 more sessions
		WebSession session4 = this.store.createWebSession().block();
		assertNotNull(session4);
		session4.start();
		session4.save().block();

		WebSession session5 = this.store.createWebSession().block();
		assertNotNull(session5);
		session5.start();
		session5.save().block();

		// Retrieve, forcing cleanup of all expired..
		assertNull(this.store.retrieveSession(session1.getId()).block());
		assertNull(this.store.retrieveSession(session2.getId()).block());
		assertNull(this.store.retrieveSession(session3.getId()).block());

		assertNotNull(this.store.retrieveSession(session4.getId()).block());
		assertNotNull(this.store.retrieveSession(session5.getId()).block());
	}



}