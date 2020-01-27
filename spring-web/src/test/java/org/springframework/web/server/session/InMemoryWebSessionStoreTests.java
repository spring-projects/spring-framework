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

package org.springframework.web.server.session;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.web.server.WebSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link InMemoryWebSessionStore}.
 * @author Rob Winch
 */
public class InMemoryWebSessionStoreTests {

	private InMemoryWebSessionStore store = new InMemoryWebSessionStore();


	@Test
	public void startsSessionExplicitly() {
		WebSession session = this.store.createWebSession().block();
		assertThat(session).isNotNull();
		session.start();
		assertThat(session.isStarted()).isTrue();
	}

	@Test
	public void startsSessionImplicitly() {
		WebSession session = this.store.createWebSession().block();
		assertThat(session).isNotNull();
		session.start();
		session.getAttributes().put("foo", "bar");
		assertThat(session.isStarted()).isTrue();
	}

	@Disabled // TODO: remove if/when Blockhound is enabled
	@Test // gh-24027
	public void createSessionDoesNotBlock() {
		Mono.defer(() -> this.store.createWebSession())
				.subscribeOn(Schedulers.parallel())
				.block();
	}

	@Test
	public void retrieveExpiredSession() {
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
	public void lastAccessTimeIsUpdatedOnRetrieve() {
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
	public void sessionInvalidatedBeforeSave() {
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
	public void expirationCheckPeriod() {

		DirectFieldAccessor accessor = new DirectFieldAccessor(this.store);
		Map<?,?> sessions = (Map<?, ?>) accessor.getPropertyValue("sessions");
		assertThat(sessions).isNotNull();

		// Create 100 sessions
		IntStream.range(0, 100).forEach(i -> insertSession());
		assertThat(sessions.size()).isEqualTo(100);

		// Force a new clock (31 min later), don't use setter which would clean expired sessions
		accessor.setPropertyValue("clock", Clock.offset(this.store.getClock(), Duration.ofMinutes(31)));
		assertThat(sessions.size()).isEqualTo(100);

		// Create 1 more which forces a time-based check (clock moved forward)
		insertSession();
		assertThat(sessions.size()).isEqualTo(1);
	}

	@Test
	public void maxSessions() {

		IntStream.range(0, 10000).forEach(i -> insertSession());
		assertThatIllegalStateException().isThrownBy(
				this::insertSession)
			.withMessage("Max sessions limit reached: 10000");
	}

	private WebSession insertSession() {
		WebSession session = this.store.createWebSession().block();
		assertThat(session).isNotNull();
		session.start();
		session.save().block();
		return session;
	}

}
