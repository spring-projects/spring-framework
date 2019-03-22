/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.server;

import java.security.Principal;
import java.util.function.Consumer;

import reactor.core.publisher.Mono;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Package-private implementation of {@link ServerWebExchange.Builder}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class DefaultServerWebExchangeBuilder implements ServerWebExchange.Builder {

	private final ServerWebExchange delegate;

	@Nullable
	private ServerHttpRequest request;

	@Nullable
	private ServerHttpResponse response;

	@Nullable
	private Mono<Principal> principalMono;


	DefaultServerWebExchangeBuilder(ServerWebExchange delegate) {
		Assert.notNull(delegate, "Delegate is required");
		this.delegate = delegate;
	}


	@Override
	public ServerWebExchange.Builder request(Consumer<ServerHttpRequest.Builder> consumer) {
		ServerHttpRequest.Builder builder = this.delegate.getRequest().mutate();
		consumer.accept(builder);
		return request(builder.build());
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
	public ServerWebExchange.Builder principal(Mono<Principal> principalMono) {
		this.principalMono = principalMono;
		return this;
	}

	@Override
	public ServerWebExchange build() {
		return new MutativeDecorator(this.delegate, this.request, this.response, this.principalMono);
	}


	/**
	 * An immutable wrapper of an exchange returning property overrides -- given
	 * to the constructor -- or original values otherwise.
	 */
	private static class MutativeDecorator extends ServerWebExchangeDecorator {

		@Nullable
		private final ServerHttpRequest request;

		@Nullable
		private final ServerHttpResponse response;

		@Nullable
		private final Mono<Principal> principalMono;

		public MutativeDecorator(ServerWebExchange delegate, @Nullable ServerHttpRequest request,
				@Nullable ServerHttpResponse response, @Nullable Mono<Principal> principalMono) {

			super(delegate);
			this.request = request;
			this.response = response;
			this.principalMono = principalMono;
		}

		@Override
		public ServerHttpRequest getRequest() {
			return (this.request != null ? this.request : getDelegate().getRequest());
		}

		@Override
		public ServerHttpResponse getResponse() {
			return (this.response != null ? this.response : getDelegate().getResponse());
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T extends Principal> Mono<T> getPrincipal() {
			return (this.principalMono != null ? (Mono<T>) this.principalMono : getDelegate().getPrincipal());
		}
	}

}

