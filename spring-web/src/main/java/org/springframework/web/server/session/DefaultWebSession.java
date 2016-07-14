/*
 * Copyright 2002-2016 the original author or authors.
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

import java.io.Serializable;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import reactor.core.publisher.Mono;

import org.springframework.util.Assert;

/**
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class DefaultWebSession implements ConfigurableWebSession, Serializable {

	private static final long serialVersionUID = -3567697426432961630L;


	private final String id;

	private final Map<String, Object> attributes;

	private final Clock clock;

	private final Instant creationTime;

	private volatile Instant lastAccessTime;

	private volatile Duration maxIdleTime;

	private AtomicReference<State> state = new AtomicReference<>();

	private volatile transient Supplier<Mono<Void>> saveOperation;


	/**
	 * Constructor to create a new session.
	 * @param id the session id
	 * @param clock for access to current time
	 */
	public DefaultWebSession(String id, Clock clock) {
		Assert.notNull(id, "'id' is required.");
		Assert.notNull(clock, "'clock' is required.");
		this.id = id;
		this.clock = clock;
		this.attributes = new ConcurrentHashMap<>();
		this.creationTime = Instant.now(clock);
		this.lastAccessTime = this.creationTime;
		this.maxIdleTime = Duration.ofMinutes(30);
		this.state.set(State.NEW);
	}

	/**
	 * Constructor to load existing session.
	 * @param id the session id
	 * @param attributes the attributes of the session
	 * @param clock for access to current time
	 * @param creationTime the creation time
	 * @param lastAccessTime the last access time
	 * @param maxIdleTime the configured maximum session idle time
	 */
	public DefaultWebSession(String id, Map<String, Object> attributes, Clock clock,
			Instant creationTime, Instant lastAccessTime, Duration maxIdleTime) {

		Assert.notNull(id, "'id' is required.");
		Assert.notNull(clock, "'clock' is required.");
		this.id = id;
		this.attributes = new ConcurrentHashMap<>(attributes);
		this.clock = clock;
		this.creationTime = creationTime;
		this.lastAccessTime = lastAccessTime;
		this.maxIdleTime = maxIdleTime;
		this.state.set(State.STARTED);
	}


	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public Map<String, Object> getAttributes() {
		return this.attributes;
	}

	@Override @SuppressWarnings("unchecked")
	public <T> Optional<T> getAttribute(String name) {
		return Optional.ofNullable((T) this.attributes.get(name));
	}

	@Override
	public Instant getCreationTime() {
		return this.creationTime;
	}

	@Override
	public void setLastAccessTime(Instant lastAccessTime) {
		this.lastAccessTime = lastAccessTime;
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
	public void setSaveOperation(Supplier<Mono<Void>> saveOperation) {
		Assert.notNull(saveOperation, "'saveOperation' is required.");
		this.saveOperation = saveOperation;
	}

	protected Supplier<Mono<Void>> getSaveOperation() {
		return this.saveOperation;
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
		return this.saveOperation.get();
	}

	@Override
	public boolean isExpired() {
		return (isStarted() && !this.maxIdleTime.isNegative() &&
				Instant.now(this.clock).minus(this.maxIdleTime).isAfter(this.lastAccessTime));
	}


	private enum State { NEW, STARTED }

}
