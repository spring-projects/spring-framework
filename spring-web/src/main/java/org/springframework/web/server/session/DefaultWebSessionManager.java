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
import java.util.List;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.util.Assert;
import org.springframework.util.IdGenerator;
import org.springframework.util.JdkIdGenerator;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;


/**
 * Default implementation of {@link WebSessionManager} delegating to a
 * {@link WebSessionIdResolver} for session id resolution and to a
 * {@link WebSessionStore}
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class DefaultWebSessionManager implements WebSessionManager {

	private static final IdGenerator idGenerator = new JdkIdGenerator();


	private WebSessionIdResolver sessionIdResolver = new CookieWebSessionIdResolver();

	private WebSessionStore sessionStore = new InMemoryWebSessionStore();

	private Clock clock = Clock.system(ZoneId.of("GMT"));


	/**
	 * Configure the id resolution strategy.
	 * <p>By default an instance of {@link CookieWebSessionIdResolver}.
	 * @param sessionIdResolver the resolver to use
	 */
	public void setSessionIdResolver(WebSessionIdResolver sessionIdResolver) {
		Assert.notNull(sessionIdResolver, "WebSessionIdResolver is required.");
		this.sessionIdResolver = sessionIdResolver;
	}

	/**
	 * Return the configured {@link WebSessionIdResolver}.
	 */
	public WebSessionIdResolver getSessionIdResolver() {
		return this.sessionIdResolver;
	}

	/**
	 * Configure the persistence strategy.
	 * <p>By default an instance of {@link InMemoryWebSessionStore}.
	 * @param sessionStore the persistence strategy to use
	 */
	public void setSessionStore(WebSessionStore sessionStore) {
		Assert.notNull(sessionStore, "WebSessionStore is required.");
		this.sessionStore = sessionStore;
	}

	/**
	 * Return the configured {@link WebSessionStore}.
	 */
	public WebSessionStore getSessionStore() {
		return this.sessionStore;
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


	@Override
	public Mono<WebSession> getSession(ServerWebExchange exchange) {
		return Mono.defer(() ->
				retrieveSession(exchange)
						.flatMap(session -> removeSessionIfExpired(exchange, session))
						.map(session -> {
							Instant lastAccessTime = Instant.now(getClock());
							return new DefaultWebSession(session, lastAccessTime, s -> saveSession(exchange, s));
						})
						.switchIfEmpty(createSession(exchange))
						.doOnNext(session -> exchange.getResponse().beforeCommit(session::save)));
	}

	private Mono<DefaultWebSession> retrieveSession(ServerWebExchange exchange) {
		return Flux.fromIterable(getSessionIdResolver().resolveSessionIds(exchange))
				.concatMap(this.sessionStore::retrieveSession)
				.cast(DefaultWebSession.class)
				.next();
	}

	private Mono<DefaultWebSession> removeSessionIfExpired(ServerWebExchange exchange, DefaultWebSession session) {
		if (session.isExpired()) {
			this.sessionIdResolver.expireSession(exchange);
			return this.sessionStore.removeSession(session.getId()).then(Mono.empty());
		}
		return Mono.just(session);
	}

	private Mono<Void> saveSession(ServerWebExchange exchange, WebSession session) {
		if (session.isExpired()) {
			return Mono.error(new IllegalStateException(
					"Sessions are checked for expiration and have their " +
					"lastAccessTime updated when first accessed during request processing. " +
					"However this session is expired meaning that maxIdleTime elapsed " +
					"before the call to session.save()."));
		}

		if (!session.isStarted()) {
			return Mono.empty();
		}

		// Force explicit start
		session.start();

		if (hasNewSessionId(exchange, session)) {
			this.sessionIdResolver.setSessionId(exchange, session.getId());
		}

 		return this.sessionStore.storeSession(session);
	}

	private boolean hasNewSessionId(ServerWebExchange exchange, WebSession session) {
		List<String> ids = getSessionIdResolver().resolveSessionIds(exchange);
		return ids.isEmpty() || !session.getId().equals(ids.get(0));
	}

	private Mono<DefaultWebSession> createSession(ServerWebExchange exchange) {
		return Mono.fromSupplier(() ->
				new DefaultWebSession(idGenerator, getClock(),
						(oldId, session) -> this.sessionStore.changeSessionId(oldId, session),
						session -> saveSession(exchange, session)));
	}

}
