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
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.springframework.util.Assert;
import org.springframework.util.IdGenerator;
import org.springframework.util.JdkIdGenerator;
import org.springframework.web.server.WebSession;

/**
 * Simple Map-based storage for {@link WebSession} instances.
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @since 5.0
 */
public class InMemoryWebSessionStore implements WebSessionStore {

	private static final IdGenerator idGenerator = new JdkIdGenerator();


	private int maxSessions = 10000;

	private Clock clock = Clock.system(ZoneId.of("GMT"));

	private final Map<String, InMemoryWebSession> sessions = new ConcurrentHashMap<>();

	private final ExpiredSessionChecker expiredSessionChecker = new ExpiredSessionChecker();


	/**
	 * Set the maximum number of sessions that can be stored. Once the limit is
	 * reached, any attempt to store an additional session will result in an
	 * {@link IllegalStateException}.
	 * <p>By default set to 10000.
	 * @param maxSessions the maximum number of sessions
	 * @since 5.0.8
	 */
	public void setMaxSessions(int maxSessions) {
		this.maxSessions = maxSessions;
	}

	/**
	 * Return the maximum number of sessions that can be stored.
	 * @since 5.0.8
	 */
	public int getMaxSessions() {
		return this.maxSessions;
	}

	/**
	 * Configure the {@link Clock} to use to set the {@code lastAccessTime} on
	 * every created session and to calculate if the session has expired.
	 * <p>This may be useful to align to different time zones or to set the clock
	 * back in a test, for example, {@code Clock.offset(clock, Duration.ofMinutes(-31))}
	 * in order to simulate session expiration.
	 * <p>By default this is {@code Clock.system(ZoneId.of("GMT"))}.
	 * @param clock the clock to use
	 */
	public void setClock(Clock clock) {
		Assert.notNull(clock, "Clock is required");
		this.clock = clock;
		removeExpiredSessions();
	}

	/**
	 * Return the configured clock for session {@code lastAccessTime} calculations.
	 */
	public Clock getClock() {
		return this.clock;
	}

	/**
	 * Return an {@linkplain Collections#unmodifiableMap unmodifiable} copy of the
	 * map of sessions.
	 * <p>This could be used for management purposes, to list active sessions,
	 * to invalidate expired sessions, etc.
	 * @since 5.0.8
	 */
	public Map<String, WebSession> getSessions() {
		return Collections.unmodifiableMap(this.sessions);
	}


	@Override
	public Mono<WebSession> createWebSession() {

		// Opportunity to clean expired sessions
		Instant now = this.clock.instant();
		this.expiredSessionChecker.checkIfNecessary(now);

		return Mono.<WebSession>fromSupplier(() -> new InMemoryWebSession(now))
				.subscribeOn(Schedulers.boundedElastic())
				.publishOn(Schedulers.parallel());
	}

	@Override
	public Mono<WebSession> retrieveSession(String id) {
		Instant now = this.clock.instant();
		this.expiredSessionChecker.checkIfNecessary(now);
		InMemoryWebSession session = this.sessions.get(id);
		if (session == null) {
			return Mono.empty();
		}
		else if (session.isExpired(now)) {
			this.sessions.remove(id);
			return Mono.empty();
		}
		else {
			session.updateLastAccessTime(now);
			return Mono.just(session);
		}
	}

	@Override
	public Mono<Void> removeSession(String id) {
		this.sessions.remove(id);
		return Mono.empty();
	}

	@Override
	public Mono<WebSession> updateLastAccessTime(WebSession session) {
		return Mono.fromSupplier(() -> {
			Assert.isInstanceOf(InMemoryWebSession.class, session);
			((InMemoryWebSession) session).updateLastAccessTime(this.clock.instant());
			return session;
		});
	}

	/**
	 * Check for expired sessions and remove them.
	 * <p>Typically such checks are kicked off lazily during calls to
	 * {@link #createWebSession()} or {@link #retrieveSession}, no less than 60
	 * seconds apart.
	 * <p>This method can be called to force a check at a specific time.
	 * @since 5.0.8
	 */
	public void removeExpiredSessions() {
		this.expiredSessionChecker.removeExpiredSessions(this.clock.instant());
	}


	private class InMemoryWebSession implements WebSession {

		private final AtomicReference<String> id = new AtomicReference<>(String.valueOf(idGenerator.generateId()));

		private final Map<String, Object> attributes = new ConcurrentHashMap<>();

		private final Instant creationTime;

		private volatile Instant lastAccessTime;

		private volatile Duration maxIdleTime = Duration.ofMinutes(30);

		private final AtomicReference<State> state = new AtomicReference<>(State.NEW);


		public InMemoryWebSession(Instant creationTime) {
			this.creationTime = creationTime;
			this.lastAccessTime = this.creationTime;
		}

		@Override
		@SuppressWarnings("NullAway") // Dataflow analysis limitation
		public String getId() {
			return this.id.get();
		}

		@Override
		public Map<String, Object> getAttributes() {
			return this.attributes;
		}

		@Override
		public Instant getCreationTime() {
			return this.creationTime;
		}

		@Override
		public Instant getLastAccessTime() {
			return this.lastAccessTime;
		}

		@Override
		public void setMaxIdleTime(Duration maxIdleTime) {
			this.maxIdleTime = maxIdleTime;
		}

		@Override
		public Duration getMaxIdleTime() {
			return this.maxIdleTime;
		}

		@Override
		public void start() {
			this.state.compareAndSet(State.NEW, State.STARTED);
		}

		@Override
		@SuppressWarnings("NullAway") // Dataflow analysis limitation
		public boolean isStarted() {
			return this.state.get().equals(State.STARTED) || !getAttributes().isEmpty();
		}

		@Override
		public Mono<Void> changeSessionId() {
			return Mono.<Void>defer(() -> {
						String currentId = this.id.get();
						InMemoryWebSessionStore.this.sessions.remove(currentId);
						String newId = String.valueOf(idGenerator.generateId());
						this.id.set(newId);
						InMemoryWebSessionStore.this.sessions.put(this.id.get(), this);
						return Mono.empty();
					})
					.subscribeOn(Schedulers.boundedElastic())
					.publishOn(Schedulers.parallel())
					.then();
		}

		@Override
		public Mono<Void> invalidate() {
			this.state.set(State.EXPIRED);
			getAttributes().clear();
			InMemoryWebSessionStore.this.sessions.remove(this.id.get());
			return Mono.empty();
		}

		@Override
		@SuppressWarnings("NullAway") // Dataflow analysis limitation
		public Mono<Void> save() {

			checkMaxSessionsLimit();

			// Implicitly started session..
			if (!getAttributes().isEmpty()) {
				this.state.compareAndSet(State.NEW, State.STARTED);
			}

			if (isStarted()) {
				// Save
				InMemoryWebSessionStore.this.sessions.put(this.id.get(), this);

				// Unless it was invalidated
				if (this.state.get().equals(State.EXPIRED)) {
					InMemoryWebSessionStore.this.sessions.remove(this.id.get());
					return Mono.error(new IllegalStateException("Session was invalidated"));
				}
			}

			return Mono.empty();
		}

		private void checkMaxSessionsLimit() {
			if (sessions.size() >= maxSessions) {
				expiredSessionChecker.removeExpiredSessions(clock.instant());
				if (sessions.size() >= maxSessions && !sessions.containsKey(this.id.get())) {
					throw new IllegalStateException("Max sessions limit reached: " + sessions.size());
				}
			}
		}

		@Override
		public boolean isExpired() {
			return isExpired(clock.instant());
		}

		@SuppressWarnings("NullAway") // Dataflow analysis limitation
		private boolean isExpired(Instant now) {
			if (this.state.get().equals(State.EXPIRED)) {
				return true;
			}
			if (checkExpired(now)) {
				this.state.set(State.EXPIRED);
				return true;
			}
			return false;
		}

		private boolean checkExpired(Instant currentTime) {
			return isStarted() && !this.maxIdleTime.isNegative() &&
					currentTime.minus(this.maxIdleTime).isAfter(this.lastAccessTime);
		}

		private void updateLastAccessTime(Instant currentTime) {
			this.lastAccessTime = currentTime;
		}
	}


	private class ExpiredSessionChecker {

		/** Max time between expiration checks. */
		private static final int CHECK_PERIOD = 60 * 1000;

		private final Lock lock = new ReentrantLock();

		private Instant checkTime = clock.instant().plus(CHECK_PERIOD, ChronoUnit.MILLIS);

		public void checkIfNecessary(Instant now) {
			if (this.checkTime.isBefore(now)) {
				removeExpiredSessions(now);
			}
		}

		public void removeExpiredSessions(Instant now) {
			if (sessions.isEmpty()) {
				return;
			}
			if (this.lock.tryLock()) {
				try {
					Iterator<InMemoryWebSession> iterator = sessions.values().iterator();
					while (iterator.hasNext()) {
						InMemoryWebSession session = iterator.next();
						if (session.isExpired(now)) {
							iterator.remove();
							session.invalidate();
						}
					}
				}
				finally {
					this.checkTime = now.plus(CHECK_PERIOD, ChronoUnit.MILLIS);
					this.lock.unlock();
				}
			}
		}
	}


	private enum State { NEW, STARTED, EXPIRED }

}
