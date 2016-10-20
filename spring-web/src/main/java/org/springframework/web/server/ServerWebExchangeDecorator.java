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
package org.springframework.web.server;

import java.security.Principal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import reactor.core.publisher.Mono;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.Assert;

/**
 * Wraps another {@link ServerWebExchange} and delegates all methods to it.
 * Sub-classes can override specific methods, e.g. {@link #getPrincipal()} to
 * return the authenticated user for the request.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ServerWebExchangeDecorator implements ServerWebExchange {

	private final ServerWebExchange delegate;


	public ServerWebExchangeDecorator(ServerWebExchange delegate) {
		Assert.notNull(delegate, "'delegate' is required.");
		this.delegate = delegate;
	}


	public ServerWebExchange getDelegate() {
		return this.delegate;
	}

	// ServerWebExchange delegation methods...

	@Override
	public ServerHttpRequest getRequest() {
		return this.getDelegate().getRequest();
	}

	@Override
	public ServerHttpResponse getResponse() {
		return this.getDelegate().getResponse();
	}

	@Override
	public Map<String, Object> getAttributes() {
		return this.getDelegate().getAttributes();
	}

	@Override
	public <T> Optional<T> getAttribute(String name) {
		return this.getDelegate().getAttribute(name);
	}

	@Override
	public Mono<WebSession> getSession() {
		return this.getDelegate().getSession();
	}

	@Override
	public <T extends Principal> Optional<T> getPrincipal() {
		return this.getDelegate().getPrincipal();
	}

	@Override
	public boolean isNotModified() {
		return this.getDelegate().isNotModified();
	}

	@Override
	public boolean checkNotModified(Instant lastModified) {
		return this.getDelegate().checkNotModified(lastModified);
	}

	@Override
	public boolean checkNotModified(String etag) {
		return this.getDelegate().checkNotModified(etag);
	}

	@Override
	public boolean checkNotModified(String etag, Instant lastModified) {
		return this.getDelegate().checkNotModified(etag, lastModified);
	}


	@Override
	public String toString() {
		return getClass().getSimpleName() + " [delegate=" + getDelegate() + "]";
	}

}
