/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.mock.web.server;

import java.security.Principal;

import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;

import org.springframework.context.ApplicationContext;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.web.server.WebSession;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;
import org.springframework.web.server.session.DefaultWebSessionManager;
import org.springframework.web.server.session.WebSessionManager;

/**
 * Extension of {@link DefaultServerWebExchange} for use in tests, along with
 * {@link MockServerHttpRequest} and {@link MockServerHttpResponse}.
 *
 * <p>See static factory methods to create an instance.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public final class MockServerWebExchange extends DefaultServerWebExchange {

	private final Mono<Principal> principalMono;


	private MockServerWebExchange(
			MockServerHttpRequest request, @Nullable WebSessionManager sessionManager,
			@Nullable ApplicationContext applicationContext, @Nullable Principal principal) {

		super(request, new MockServerHttpResponse(),
				sessionManager != null ? sessionManager : new DefaultWebSessionManager(),
				ServerCodecConfigurer.create(), new AcceptHeaderLocaleContextResolver(),
				applicationContext);

		this.principalMono = (principal != null) ? Mono.just(principal) : Mono.empty();
	}


	@Override
	public MockServerHttpResponse getResponse() {
		return (MockServerHttpResponse) super.getResponse();
	}

	/**
	 * Return the user set via {@link Builder#principal(Principal)}.
	 * @since 6.2.7
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T extends Principal> Mono<T> getPrincipal() {
		return (Mono<T>) this.principalMono;
	}


	/**
	 * Create a {@link MockServerWebExchange} from the given mock request.
	 * @param request the request to use.
	 * @return the exchange
	 */
	public static MockServerWebExchange from(MockServerHttpRequest request) {
		return builder(request).build();
	}

	/**
	 * Variant of {@link #from(MockServerHttpRequest)} with a mock request builder.
	 * @param requestBuilder the builder for the mock request.
	 * @return the exchange
	 */
	public static MockServerWebExchange from(MockServerHttpRequest.BaseBuilder<?> requestBuilder) {
		return builder(requestBuilder).build();
	}

	/**
	 * Create a {@link Builder} starting with the given mock request.
	 * @param request the request to use.
	 * @return the exchange builder
	 * @since 5.1
	 */
	public static MockServerWebExchange.Builder builder(MockServerHttpRequest request) {
		return new MockServerWebExchange.Builder(request);
	}

	/**
	 * Variant of {@link #builder(MockServerHttpRequest)} with a mock request builder.
	 * @param requestBuilder the builder for the mock request.
	 * @return the exchange builder
	 * @since 5.1
	 */
	public static MockServerWebExchange.Builder builder(MockServerHttpRequest.BaseBuilder<?> requestBuilder) {
		return new MockServerWebExchange.Builder(requestBuilder.build());
	}


	/**
	 * Builder for a {@link MockServerWebExchange}.
	 * @since 5.1
	 */
	public static class Builder {

		private final MockServerHttpRequest request;

		private @Nullable WebSessionManager sessionManager;

		private @Nullable ApplicationContext applicationContext;

		private @Nullable Principal principal;

		public Builder(MockServerHttpRequest request) {
			this.request = request;
		}

		/**
		 * Set the session to use for the exchange.
		 * <p>This method is mutually exclusive with
		 * {@link #sessionManager(WebSessionManager)}.
		 * @param session the session to use
		 * @see MockWebSession
		 */
		public Builder session(WebSession session) {
			this.sessionManager = exchange -> Mono.just(session);
			return this;
		}

		/**
		 * Provide a {@code WebSessionManager} instance to use with the exchange.
		 * <p>This is mutually exclusive with {@link #session(WebSession)}.
		 * @param sessionManager the session manager to use
		 */
		public Builder sessionManager(WebSessionManager sessionManager) {
			this.sessionManager = sessionManager;
			return this;
		}

		/**
		 * Provide the {@code ApplicationContext} to expose through the exchange.
		 * @param applicationContext the context to use
		 * @since 6.2.5
		 */
		public Builder applicationContext(ApplicationContext applicationContext) {
			this.applicationContext = applicationContext;
			return this;
		}

		/**
		 * Provide a user to associate with the exchange.
		 * @param principal the principal to use
		 * @since 6.2.7
		 */
		public Builder principal(@Nullable Principal principal) {
			this.principal = principal;
			return this;
		}

		/**
		 * Build the {@code MockServerWebExchange} instance.
		 */
		public MockServerWebExchange build() {
			return new MockServerWebExchange(
					this.request, this.sessionManager, this.applicationContext, this.principal);
		}
	}

}
