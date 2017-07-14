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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import reactor.core.publisher.Mono;

import org.springframework.util.Assert;
import org.springframework.web.server.WebSession;

/**
 * Default implementation of {@link org.springframework.web.server.WebSession}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class DefaultWebSession implements WebSession {

	private final String id;

	private final Map<String, Object> attributes;

	private final Clock clock;

	private final Function<WebSession, Mono<Void>> saveOperation;

	private final Instant creationTime;

	private final Instant lastAccessTime;

	private volatile Duration maxIdleTime;

	private final AtomicReference<State> state;


	/**
	 * Constructor for creating a brand, new session.
	 * @param id the session id
	 * @param clock for access to current time
	 */
	DefaultWebSession(String id, Clock clock, Function<WebSession, Mono<Void>> saveOperation) {
		Assert.notNull(id, "'id' is required.");
		Assert.notNull(clock, "'clock' is required.");
		this.id = id;
		this.clock = clock;
		this.saveOperation = saveOperation;
		this.attributes = new ConcurrentHashMap<>();
		this.creationTime = Instant.now(clock);
		this.lastAccessTime = this.creationTime;
		this.maxIdleTime = Duration.ofMinutes(30);
		this.state = new AtomicReference<>(State.NEW);
	}

	/**
	 * Constructor to refresh an existing session for a new request.
	 * @param existingSession the session to recreate
	 * @param lastAccessTime the last access time
	 * @param saveOperation save operation for the current request
	 */
	DefaultWebSession(DefaultWebSession existingSession, Instant lastAccessTime,
			Function<WebSession, Mono<Void>> saveOperation) {

		this.id = existingSession.id;
		this.attributes = existingSession.attributes;
		this.clock = existingSession.clock;
		this.creationTime = existingSession.creationTime;
		this.lastAccessTime = lastAccessTime;
		this.maxIdleTime = existingSession.maxIdleTime;
		this.saveOperation = saveOperation;
		this.state = existingSession.state;
	}

	/**
	 * For testing purposes.
	 */
	DefaultWebSession(DefaultWebSession existingSession, Instant lastAccessTime) {
		this.id = existingSession.id;
		this.attributes = existingSession.attributes;
		this.clock = existingSession.clock;
		this.creationTime = existingSession.creationTime;
		this.lastAccessTime = lastAccessTime;
		this.maxIdleTime = existingSession.maxIdleTime;
		this.saveOperation = existingSession.saveOperation;
		this.state = existingSession.state;
	}


	@Override
	public String getId() {
		return this.id;
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

	/**
	 * <p>By default this is set to 30 minutes.
	 * @param maxIdleTime the max idle time
	 */
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
	public boolean isStarted() {
		State value = this.state.get();
		return (State.STARTED.equals(value) || (State.NEW.equals(value) && !getAttributes().isEmpty()));
	}

	@Override
	public Mono<Void> save() {
		return this.saveOperation.apply(this);
	}

	@Override
	public boolean isExpired() {
		return (isStarted() && !this.maxIdleTime.isNegative() &&
				Instant.now(this.clock).minus(this.maxIdleTime).isAfter(this.lastAccessTime));
	}


	private enum State { NEW, STARTED }

}
