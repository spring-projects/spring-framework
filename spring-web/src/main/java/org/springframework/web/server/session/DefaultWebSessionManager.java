/*
 * Copyright 2002-2020 the original author or authors.
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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;

/**
 * Default implementation of {@link WebSessionManager} delegating to a
 * {@link WebSessionIdResolver} for session id resolution and to a
 * {@link WebSessionStore}.
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @since 5.0
 */
public class DefaultWebSessionManager implements WebSessionManager {

	private static final Log logger = LogFactory.getLog(DefaultWebSessionManager.class);


	private WebSessionIdResolver sessionIdResolver = new CookieWebSessionIdResolver();

	private WebSessionStore sessionStore = new InMemoryWebSessionStore();


	/**
	 * Configure the id resolution strategy.
	 * <p>By default an instance of {@link CookieWebSessionIdResolver}.
	 * @param sessionIdResolver the resolver to use
	 */
	public void setSessionIdResolver(WebSessionIdResolver sessionIdResolver) {
		Assert.notNull(sessionIdResolver, "WebSessionIdResolver is required");
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
		Assert.notNull(sessionStore, "WebSessionStore is required");
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
		return Mono.defer(() -> retrieveSession(exchange)
				.switchIfEmpty(createWebSession())
				.doOnNext(session -> exchange.getResponse().beforeCommit(() -> save(exchange, session))));
	}

	private Mono<WebSession> createWebSession() {
		Mono<WebSession> session = this.sessionStore.createWebSession();
		if (logger.isDebugEnabled()) {
			session = session.doOnNext(s -> logger.debug("Created new WebSession."));
		}
		return session;
	}

	private Mono<WebSession> retrieveSession(ServerWebExchange exchange) {
		return Flux.fromIterable(getSessionIdResolver().resolveSessionIds(exchange))
				.concatMap(this.sessionStore::retrieveSession)
				.next();
	}

	private Mono<Void> save(ServerWebExchange exchange, WebSession session) {
		List<String> ids = getSessionIdResolver().resolveSessionIds(exchange);

		if (!session.isStarted() || session.isExpired()) {
			if (!ids.isEmpty()) {
				// Expired on retrieve or while processing request, or invalidated..
				if (logger.isDebugEnabled()) {
					logger.debug("WebSession expired or has been invalidated");
				}
				this.sessionIdResolver.expireSession(exchange);
			}
			return Mono.empty();
		}

		if (ids.isEmpty() || !session.getId().equals(ids.get(0))) {
			this.sessionIdResolver.setSessionId(exchange, session.getId());
		}

		return session.save();
	}

}
