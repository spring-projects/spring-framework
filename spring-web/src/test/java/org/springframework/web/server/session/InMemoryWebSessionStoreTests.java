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

import org.junit.Test;

import org.springframework.web.server.WebSession;

import static junit.framework.TestCase.assertSame;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests.
 * @author Rob Winch
 */
public class InMemoryWebSessionStoreTests {

	private InMemoryWebSessionStore store = new InMemoryWebSessionStore();


	@Test
	public void constructorWhenImplicitStartCopiedThenCopyIsStarted() {
		WebSession original = this.store.createWebSession().block();
		assertNotNull(original);
		original.getAttributes().put("foo", "bar");

		WebSession copy = this.store.updateLastAccessTime(original).block();
		assertNotNull(copy);
		assertTrue(copy.isStarted());
	}

	@Test
	public void constructorWhenExplicitStartCopiedThenCopyIsStarted() {
		WebSession original = this.store.createWebSession().block();
		assertNotNull(original);
		original.start();

		WebSession copy = this.store.updateLastAccessTime(original).block();
		assertNotNull(copy);
		assertTrue(copy.isStarted());
	}

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
		session.save();

		String id = session.getId();
		WebSession retrieved = this.store.retrieveSession(id).block();
		assertNotNull(retrieved);
		assertSame(session, retrieved);

		this.store.setClock(Clock.offset(this.store.getClock(), Duration.ofMinutes(31)));
		WebSession retrievedAgain = this.store.retrieveSession(id).block();
		assertNull(retrievedAgain);
	}
}