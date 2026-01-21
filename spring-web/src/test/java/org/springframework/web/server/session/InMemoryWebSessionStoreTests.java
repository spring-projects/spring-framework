/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.server.session;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.web.server.WebSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link InMemoryWebSessionStore}.
 *
 * @author Rob Winch
 * @author Sam Brannen
 */
class InMemoryWebSessionStoreTests {

	private final InMemoryWebSessionStore store = new InMemoryWebSessionStore();


	@Test
	void startsSessionExplicitly() {
		WebSession session = this.store.createWebSession().block();
		assertThat(session).isNotNull();
		session.start();
		assertThat(session.isStarted()).isTrue();
	}

	@Test
	void startsSessionImplicitly() {
		WebSession session = this.store.createWebSession().block();
		assertThat(session).isNotNull();
		// We intentionally do not invoke start().
		// session.start();
		session.getAttributes().put("foo", "bar");
		assertThat(session.isStarted()).isTrue();
	}

	@Test // gh-24027, gh-26958
	void createSessionDoesNotBlock() {
		this.store.createWebSession()
				.doOnNext(session -> assertThat(Schedulers.isInNonBlockingThread()).isTrue())
				.block();
	}

	@Test
	void retrieveExpiredSession() {
		WebSession session = this.store.createWebSession().block();
		assertThat(session).isNotNull();
		session.getAttributes().put("foo", "bar");
		session.save().block();

		String id = session.getId();
		WebSession retrieved = this.store.retrieveSession(id).block();
		assertThat(retrieved).isNotNull();
		assertThat(retrieved).isSameAs(session);

		// Fast-forward 31 minutes
		this.store.setClock(Clock.offset(this.store.getClock(), Duration.ofMinutes(31)));
		WebSession retrievedAgain = this.store.retrieveSession(id).block();
		assertThat(retrievedAgain).isNull();
	}

	@Test
	void lastAccessTimeIsUpdatedOnRetrieve() {
		WebSession session1 = this.store.createWebSession().block();
		assertThat(session1).isNotNull();
		String id = session1.getId();
		Instant time1 = session1.getLastAccessTime();
		session1.start();
		session1.save().block();

		// Fast-forward a few seconds
		this.store.setClock(Clock.offset(this.store.getClock(), Duration.ofSeconds(5)));

		WebSession session2 = this.store.retrieveSession(id).block();
		assertThat(session2).isNotNull();
		assertThat(session2).isSameAs(session1);
		Instant time2 = session2.getLastAccessTime();
		assertThat(time1.isBefore(time2)).isTrue();
	}

	@Test // SPR-17051
	void sessionInvalidatedBeforeSave() {
		// Request 1 creates session
		WebSession session1 = this.store.createWebSession().block();
		assertThat(session1).isNotNull();
		String id = session1.getId();
		session1.start();
		session1.save().block();

		// Request 2 retrieves session
		WebSession session2 = this.store.retrieveSession(id).block();
		assertThat(session2).isNotNull();
		assertThat(session2).isSameAs(session1);

		// Request 3 retrieves and invalidates
		WebSession session3 = this.store.retrieveSession(id).block();
		assertThat(session3).isNotNull();
		assertThat(session3).isSameAs(session1);
		session3.invalidate().block();

		// Request 2 saves session after invalidated
		session2.save().block();

		// Session should not be present
		WebSession session4 = this.store.retrieveSession(id).block();
		assertThat(session4).isNull();
	}

	@Test
	void expirationCheckPeriod() {
		// Create 100 sessions
		IntStream.rangeClosed(1, 100).forEach(i -> insertSession());
		assertNumSessions(100);

		// Force a new clock (31 min later). Don't use setter which would clean expired sessions.
		DirectFieldAccessor accessor = new DirectFieldAccessor(this.store);
		accessor.setPropertyValue("clock", Clock.offset(this.store.getClock(), Duration.ofMinutes(31)));
		assertNumSessions(100);

		// Create 1 more which forces a time-based check (clock moved forward).
		insertSession();
		assertNumSessions(1);
	}

	@Test
	void maxSessions() {
		this.store.setMaxSessions(10);

		IntStream.rangeClosed(1, 10).forEach(i -> insertSession());
		assertThatIllegalStateException()
				.isThrownBy(this::insertSession)
				.withMessage("Max sessions limit reached: 10");
	}

	@Test
	void updateSession() {
		WebSession session = insertSession();

		StepVerifier.create(session.save())
				.expectComplete()
				.verify();
	}

	@Test  // gh-35013
	void updateSessionAfterMaxSessionLimitIsExceeded() {
		this.store.setMaxSessions(10);

		WebSession session = insertSession();
		assertNumSessions(1);

		IntStream.rangeClosed(1, 9).forEach(i -> insertSession());
		assertNumSessions(10);

		// Updating an existing session should succeed.
		StepVerifier.create(session.save())
				.expectComplete()
				.verify();
		assertNumSessions(10);

		// Saving an additional new session should fail.
		assertThatIllegalStateException()
				.isThrownBy(this::insertSession)
				.withMessage("Max sessions limit reached: 10");
		assertNumSessions(10);

		// Updating an existing session again should still succeed.
		StepVerifier.create(session.save())
				.expectComplete()
				.verify();
		assertNumSessions(10);
	}


	private WebSession insertSession() {
		WebSession session = this.store.createWebSession().block();
		assertThat(session).isNotNull();
		session.start();
		session.save().block();
		return session;
	}

	private void assertNumSessions(int numSessions) {
		assertThat(store.getSessions()).hasSize(numSessions);
	}

}
