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
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.util.Assert;
import org.springframework.util.IdGenerator;
import org.springframework.util.JdkIdGenerator;
import reactor.core.publisher.Mono;

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

	private final Map<String, WebSession> sessions = new ConcurrentHashMap<>();


	@Override
	public Mono<WebSession> retrieveSession(String id) {
		return (this.sessions.containsKey(id) ? Mono.just(this.sessions.get(id)) : Mono.empty());
	}

	@Override
	public Mono<Void> removeSession(String id) {
		this.sessions.remove(id);
		return Mono.empty();
	}

	public Mono<WebSession> createWebSession() {
		return Mono.fromSupplier(() ->
				new DefaultWebSession(idGenerator, getClock(),
						(oldId, session) -> this.changeSessionId(oldId, session),
						this::storeSession));
	}

	public Mono<WebSession> updateLastAccessTime(WebSession webSession) {
		return Mono.fromSupplier(() -> {
			DefaultWebSession session = (DefaultWebSession) webSession;
			Instant lastAccessTime = Instant.now(getClock());
			return new DefaultWebSession(session, lastAccessTime);
		});
	}

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
		Assert.notNull(clock, "'clock' is required.");
		this.clock = clock;
	}

	/**
	 * Return the configured clock for session lastAccessTime calculations.
	 */
	public Clock getClock() {
		return this.clock;
	}

	private Mono<Void> changeSessionId(String oldId, WebSession session) {
		this.sessions.remove(oldId);
		this.sessions.put(session.getId(), session);
		return Mono.empty();
	}

	private Mono<Void> storeSession(WebSession session) {
		this.sessions.put(session.getId(), session);
		return Mono.empty();
	}
}
