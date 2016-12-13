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

import reactor.core.publisher.Mono;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;

/**
 * Package private implementation of {@link ServerWebExchange.Builder}.
 * 
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class DefaultServerWebExchangeBuilder implements ServerWebExchange.Builder {

	private final ServerWebExchange delegate;

	private ServerHttpRequest request;

	private ServerHttpResponse response;

	private Mono<Principal> user;

	private Mono<WebSession> session;

	private Mono<MultiValueMap<String, String>> formData;


	public DefaultServerWebExchangeBuilder(ServerWebExchange delegate) {
		Assert.notNull(delegate, "'delegate' is required.");
		this.delegate = delegate;
	}


	@Override
	public ServerWebExchange.Builder request(ServerHttpRequest request) {
		this.request = request;
		return this;
	}

	@Override
	public ServerWebExchange.Builder response(ServerHttpResponse response) {
		this.response = response;
		return this;
	}

	@Override
	public ServerWebExchange.Builder principal(Mono<Principal> user) {
		this.user = user;
		return this;
	}

	@Override
	public ServerWebExchange.Builder session(Mono<WebSession> session) {
		this.session = session;
		return this;
	}

	@Override
	public ServerWebExchange.Builder formData(Mono<MultiValueMap<String, String>> formData) {
		this.formData = formData;
		return this;
	}

	@Override
	public ServerWebExchange build() {
		return new MutativeDecorator(this.delegate, this.request, this.response,
				this.user, this.session, this.formData);
	}


	/**
	 * An immutable wrapper of an exchange returning property overrides -- given
	 * to the constructor -- or original values otherwise.
	 */
	private static class MutativeDecorator extends ServerWebExchangeDecorator {

		private final ServerHttpRequest request;

		private final ServerHttpResponse response;

		private final Mono<Principal> userMono;

		private final Mono<WebSession> session;

		private final Mono<MultiValueMap<String, String>> formData;


		public MutativeDecorator(ServerWebExchange delegate,
				ServerHttpRequest request, ServerHttpResponse response, Mono<Principal> user,
				Mono<WebSession> session, Mono<MultiValueMap<String, String>> formData) {

			super(delegate);
			this.request = request;
			this.response = response;
			this.userMono = user;
			this.session = session;
			this.formData = formData;
		}


		@Override
		public ServerHttpRequest getRequest() {
			return (this.request != null ? this.request : getDelegate().getRequest());
		}

		@Override
		public ServerHttpResponse getResponse() {
			return (this.response != null ? this.response : getDelegate().getResponse());
		}

		@Override
		public Mono<WebSession> getSession() {
			return (this.session != null ? this.session : getDelegate().getSession());
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T extends Principal> Mono<T> getPrincipal() {
			return (this.userMono != null ? (Mono<T>) this.userMono : getDelegate().getPrincipal());
		}

		@Override
		public Mono<MultiValueMap<String, String>> getFormData() {
			return (this.formData != null ? this.formData : getDelegate().getFormData());
		}
	}

}

