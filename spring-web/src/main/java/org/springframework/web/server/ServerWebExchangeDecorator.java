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
 * A convenient base class for classes that need to wrap another
 * {@link ServerWebExchange}. Pre-implements all methods by delegating to the
 * wrapped instance.
 *
 * <p>Note that if the purpose for wrapping is simply to override specific
 * properties, e.g. {@link #getPrincipal()}, consider using
 * {@link ServerWebExchange#mutate()} instead.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 *
 * @see ServerWebExchange#mutate()
 */
public class ServerWebExchangeDecorator implements ServerWebExchange {

	private final ServerWebExchange delegate;


	protected ServerWebExchangeDecorator(ServerWebExchange delegate) {
		Assert.notNull(delegate, "ServerWebExchange 'delegate' is required.");
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
