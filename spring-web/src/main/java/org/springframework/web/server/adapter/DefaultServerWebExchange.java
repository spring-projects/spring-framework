/*
 * Copyright 2002-2015 the original author or authors.
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
package org.springframework.web.server.adapter;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import reactor.core.publisher.Mono;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import org.springframework.web.server.session.WebSessionManager;

/**
 * Default implementation of {@link ServerWebExchange}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class DefaultServerWebExchange implements ServerWebExchange {

	private final ServerHttpRequest request;

	private final ServerHttpResponse response;

	private final Map<String, Object> attributes = new ConcurrentHashMap<>();

	private final Mono<WebSession> sessionMono;


	public DefaultServerWebExchange(ServerHttpRequest request, ServerHttpResponse response,
			WebSessionManager sessionManager) {

		Assert.notNull(request, "'request' is required.");
		Assert.notNull(response, "'response' is required.");
		Assert.notNull(response, "'sessionManager' is required.");
		this.request = request;
		this.response = response;
		this.sessionMono = sessionManager.getSession(this).cache();
	}


	@Override
	public ServerHttpRequest getRequest() {
		return this.request;
	}

	@Override
	public ServerHttpResponse getResponse() {
		return this.response;
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
	public Mono<WebSession> getSession() {
		return this.sessionMono;
	}

}
