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
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import reactor.core.publisher.Mono;

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


	private Clock clock = Clock.system(ZoneId.of("GMT"));

	private final Map<String, InMemoryWebSession> sessions = new ConcurrentHashMap<>();


	/**
	 * Configure the {@link Clock} to use to set lastAccessTime on every created
	 * session and to calculate if it is expired.
	 * <p>This may be useful to align to different timezone or to set the clock
	 * back in a test, e.g. {@code Clock.offset(clock, Duration.ofMinutes(-31))}
	 * in order to simulate session expiration.
	 * <p>By default this is {@code Clock.system(ZoneId.of("GMT"))}.
	 * @param clock the clock to use
	 */
	public void setClock(Clock clock) {
		Assert.notNull(clock, "Clock is required");
		this.clock = clock;
	}

	/**
	 * Return the configured clock for session lastAccessTime calculations.
	 */
	public Clock getClock() {
		return this.clock;
	}


	@Override
	public Mono<WebSession> createWebSession() {
		return Mono.fromSupplier(InMemoryWebSession::new);
	}

	@Override
	public Mono<WebSession> retrieveSession(String id) {
		InMemoryWebSession session = this.sessions.get(id);
		if (session == null) {
			return Mono.empty();
		}
		else if (session.isExpired()) {
			this.sessions.remove(id);
			return Mono.empty();
		}
		else {
			session.updateLastAccessTime();
			return Mono.just(session);
		}
	}

	@Override
	public Mono<Void> removeSession(String id) {
		this.sessions.remove(id);
		return Mono.empty();
	}

	public Mono<WebSession> updateLastAccessTime(WebSession webSession) {
		return Mono.fromSupplier(() -> {
			Assert.isInstanceOf(InMemoryWebSession.class, webSession);
			InMemoryWebSession session = (InMemoryWebSession) webSession;
			session.updateLastAccessTime();
			return session;
		});
	}


	private class InMemoryWebSession implements WebSession {

		private final AtomicReference<String> id;

		private final Map<String, Object> attributes;

		private final Instant creationTime;

		private volatile Instant lastAccessTime;

		private volatile Duration maxIdleTime;

		private volatile boolean started;


		InMemoryWebSession() {
			this.id = new AtomicReference<>(String.valueOf(idGenerator.generateId()));
			this.attributes = new ConcurrentHashMap<>();
			this.creationTime = Instant.now(getClock());
			this.lastAccessTime = this.creationTime;
			this.maxIdleTime = Duration.ofMinutes(30);
		}


		@Override
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
			this.started = true;
		}

		@Override
		public boolean isStarted() {
			return this.started || !getAttributes().isEmpty();
		}

		@Override
		public Mono<Void> changeSessionId() {
			String currentId = this.id.get();
			if (InMemoryWebSessionStore.this.sessions.remove(currentId) == null) {
				return Mono.error(new IllegalStateException(
						"Failed to change session id: " + currentId +
								" because the Session is no longer present in the store."));
			}
			String newId = String.valueOf(idGenerator.generateId());
			this.id.set(newId);
			InMemoryWebSessionStore.this.sessions.put(this.getId(), this);
			return Mono.empty();
		}

		@Override
		public Mono<Void> save() {
			InMemoryWebSessionStore.this.sessions.put(this.getId(), this);
			return Mono.empty();
		}

		@Override
		public boolean isExpired() {
			return (isStarted() && !this.maxIdleTime.isNegative() &&
					Instant.now(getClock()).minus(this.maxIdleTime).isAfter(this.lastAccessTime));
		}

		private void updateLastAccessTime() {
			this.lastAccessTime = Instant.now(getClock());
		}
	}

}
