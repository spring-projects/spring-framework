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

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;


/**
 * Default implementation of {@link WebSessionManager} with a cookie-based web
 * session id resolution strategy and simple in-memory session persistence.
 *
 * @author Rossen Stoyanchev
 */
public class DefaultWebSessionManager implements WebSessionManager {

	private WebSessionIdResolver sessionIdResolver = new CookieWebSessionIdResolver();

	private WebSessionStore sessionStore = new InMemoryWebSessionStore();

	private Clock clock = Clock.systemDefaultZone();


	/**
	 * Configure the session id resolution strategy to use.
	 * <p>By default {@link CookieWebSessionIdResolver} is used.
	 * @param sessionIdResolver the resolver
	 */
	public void setSessionIdResolver(WebSessionIdResolver sessionIdResolver) {
		Assert.notNull(sessionIdResolver, "'sessionIdResolver' is required.");
		this.sessionIdResolver = sessionIdResolver;
	}

	/**
	 * Return the configured {@link WebSessionIdResolver}.
	 */
	public WebSessionIdResolver getSessionIdResolver() {
		return this.sessionIdResolver;
	}

	/**
	 * Configure the session persistence strategy to use.
	 * <p>By default {@link InMemoryWebSessionStore} is used.
	 * @param sessionStore the persistence strategy
	 */
	public void setSessionStore(WebSessionStore sessionStore) {
		Assert.notNull(sessionStore, "'sessionStore' is required.");
		this.sessionStore = sessionStore;
	}

	/**
	 * Return the configured {@link WebSessionStore}.
	 */
	public WebSessionStore getSessionStore() {
		return this.sessionStore;
	}

	/**
	 * Configure the {@link Clock} for access to current time. During tests you
	 * may use {code Clock.offset(clock, Duration.ofMinutes(-31))} to set the
	 * clock back for example to test changes after sessions expire.
	 * <p>By default {@link Clock#systemDefaultZone()} is used.
	 * @param clock the clock to use
	 */
	public void setClock(Clock clock) {
		Assert.notNull(clock, "'clock' is required.");
		this.clock = clock;
	}

	/**
	 * Return the configured clock for access to current time.
	 */
	public Clock getClock() {
		return this.clock;
	}


	@Override
	public Mono<WebSession> getSession(ServerWebExchange exchange) {
		return Mono.defer(() ->
				Flux.fromIterable(getSessionIdResolver().resolveSessionIds(exchange))
						.concatMap(this.sessionStore::retrieveSession)
						.next()
						.then(session -> validateSession(exchange, session))
						.otherwiseIfEmpty(createSession(exchange))
						.map(session -> extendSession(exchange, session)));
	}

	protected Mono<WebSession> validateSession(ServerWebExchange exchange, WebSession session) {
		if (session.isExpired()) {
			this.sessionIdResolver.setSessionId(exchange, "");
			return this.sessionStore.removeSession(session.getId()).cast(WebSession.class);
		}
		else {
			return Mono.just(session);
		}
	}

	protected Mono<WebSession> createSession(ServerWebExchange exchange) {
		String sessionId = UUID.randomUUID().toString();
		WebSession session = new DefaultWebSession(sessionId, getClock());
		return Mono.just(session);
	}

	protected WebSession extendSession(ServerWebExchange exchange, WebSession session) {
		if (session instanceof ConfigurableWebSession) {
			ConfigurableWebSession managed = (ConfigurableWebSession) session;
			managed.setSaveOperation(() -> saveSession(exchange, session));
			managed.setLastAccessTime(Instant.now(getClock()));
		}
		exchange.getResponse().beforeCommit(session::save);
		return session;
	}

	protected Mono<Void> saveSession(ServerWebExchange exchange, WebSession session) {

		Assert.isTrue(!session.isExpired(), "Sessions are checked for expiration and have their " +
				"access time updated when first accessed during request processing. " +
				"However this session is expired meaning that maxIdleTime elapsed " +
				"since then and before the call to session.save().");

		if (!session.isStarted()) {
			return Mono.empty();
		}

		// Force explicit start
		session.start();

		List<String> requestedIds = getSessionIdResolver().resolveSessionIds(exchange);
		if (requestedIds.isEmpty() || !session.getId().equals(requestedIds.get(0))) {
			this.sessionIdResolver.setSessionId(exchange, session.getId());
		}
		return this.sessionStore.storeSession(session);
	}

}
