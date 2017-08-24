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

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.lang.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;


/**
 * Default implementation of {@link WebSessionManager} delegating to a
 * {@link WebSessionIdResolver} for session id resolution and to a
 * {@link WebSessionStore}
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @since 5.0
 */
public class DefaultWebSessionManager implements WebSessionManager {

	private WebSessionIdResolver sessionIdResolver = new CookieWebSessionIdResolver();

	private WebSessionStore sessionStore = new InMemoryWebSessionStore();

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

	@Override
	public Mono<WebSession> getSession(ServerWebExchange exchange) {
		return Mono.defer(() ->
				retrieveSession(exchange)
						.flatMap(session -> removeSessionIfExpired(exchange, session))
						.flatMap(this.getSessionStore()::updateLastAccessTime)
						.switchIfEmpty(this.sessionStore.createWebSession())
						.map( session -> new ExchangeWebSession(exchange, session))
						.doOnNext(session -> exchange.getResponse().beforeCommit(session::save)));
	}

	private Mono<WebSession> retrieveSession(ServerWebExchange exchange) {
		return Flux.fromIterable(getSessionIdResolver().resolveSessionIds(exchange))
				.concatMap(this.sessionStore::retrieveSession)
				.next();
	}

	private Mono<WebSession> removeSessionIfExpired(ServerWebExchange exchange, WebSession session) {
		if (session.isExpired()) {
			this.sessionIdResolver.expireSession(exchange);
			return this.sessionStore.removeSession(session.getId()).then(Mono.empty());
		}
		return Mono.just(session);
	}

	class ExchangeWebSession implements WebSession {
		private final ServerWebExchange exchange;
		private final WebSession delegate;

		ExchangeWebSession(ServerWebExchange exchange, WebSession delegate) {
			this.exchange = exchange;
			this.delegate = delegate;
		}

		@Override
		public String getId() {
			return this.delegate.getId();
		}

		@Override
		public Map<String, Object> getAttributes() {
			return this.delegate.getAttributes();
		}

		@Override
		@Nullable public <T> T getAttribute(String name) {
			return this.delegate.getAttribute(name);
		}

		@Override
		public <T> T getRequiredAttribute(String name) {
			return this.delegate.getRequiredAttribute(name);
		}

		@Override
		public <T> T getAttributeOrDefault(String name, T defaultValue) {
			return this.delegate.getAttributeOrDefault(name, defaultValue);
		}

		@Override
		public void start() {
			this.delegate.start();
		}

		@Override
		public boolean isStarted() {
			return this.delegate.isStarted();
		}

		@Override
		public Mono<Void> changeSessionId() {
			return this.delegate.changeSessionId();
		}

		@Override
		public Mono<Void> save() {
			if (isExpired()) {
				return Mono.error(new IllegalStateException(
						"Sessions are checked for expiration and have their " +
								"lastAccessTime updated when first accessed during request processing. " +
								"However this session is expired meaning that maxIdleTime elapsed " +
								"before the call to session.save()."));
			}

			if (!isStarted()) {
				return Mono.empty();
			}

			// Force explicit start
			start();

			if (hasNewSessionId()) {
				DefaultWebSessionManager.this.sessionIdResolver.setSessionId(this.exchange, this.getId());
			}

			return this.delegate.save();
		}

		private boolean hasNewSessionId() {
			List<String> ids = getSessionIdResolver().resolveSessionIds(this.exchange);
			return ids.isEmpty() || !getId().equals(ids.get(0));
		}

		@Override
		public boolean isExpired() {
			return this.delegate.isExpired();
		}

		@Override
		public Instant getCreationTime() {
			return this.delegate.getCreationTime();
		}

		@Override
		public Instant getLastAccessTime() {
			return this.delegate.getLastAccessTime();
		}

		@Override
		public void setMaxIdleTime(Duration maxIdleTime) {
			this.delegate.setMaxIdleTime(maxIdleTime);
		}

		@Override
		public Duration getMaxIdleTime() {
			return this.delegate.getMaxIdleTime();
		}
	}
}
